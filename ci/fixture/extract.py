"""Writes the fixture graph (spec 001, T007) from a private copy of a release's graph.

Usage: python ci/fixture/extract.py <neo4j-http-url> <stId>... > ci/fixture/fixture.cypher

It talks to Neo4j's HTTP endpoint for database `graph.db`, and never to a shared or
production graph. From the given events it follows the relationships the converter
reads, in their direction (OUTGOING, INCOMING), taking each node with its full label
set and every property. Some nodes are read only for themselves (a species, a
compartment, a person, a publication, an orthologous entity): they are taken as
leaves, without their own relationships. The
`DBInfo` node is taken as it is.

People's names never leave the graph: every `Person` is renamed `Placeholder`
`PersonN`, its ORCID and project dropped, and every `InstanceEdit`'s display name
(which carries a surname) and note replaced. `check-placeholders.sh` checks the
output.
"""
import json
import sys
import urllib.request

# The relationships the converter reads, directly or through graph-core's getters, by
# direction. Outgoing ones are followed from every node reached; a reaction's
# cross-references only from an event, since nothing else's are written.
OUTGOING = {
    "hasEvent", "input", "output", "catalystActivity", "physicalEntity", "activity",
    "regulatedBy", "regulator", "hasComponent", "hasMember", "hasCandidate", "repeatedUnit",
    "referenceEntity", "referenceDatabase", "hasModifiedResidue", "psiMod", "summation",
    "literatureReference", "goBiologicalProcess", "disease", "compartment", "inferredTo",
    # A species run finds its pathways through their species (the release's step converts by species)
    "species",
}
# Incoming: an event's history (InstanceEdit -> event) and its authors (Person -> InstanceEdit),
# and what an entity was inferred from (other -> entity).
INCOMING = {"created": "Event", "modified": "Event", "authored": "Event", "revised": "Event",
            "author": "InstanceEdit", "inferredTo": "PhysicalEntity"}
# Reached, but read only for themselves: taken as leaves.
LEAVES = {"Person", "Publication", "Species", "Taxon", "GO_Term", "Disease", "ReferenceDatabase",
          "DatabaseIdentifier", "Compartment", "PsiMod"}


def follow(labels, kind, outgoing, other):
    """Return (take the relationship, expand from the node at its other end)."""
    if outgoing:
        if kind == "crossReference":
            return "Event" in labels, False
        if kind not in OUTGOING:
            return False, False
        return True, kind != "inferredTo" and not LEAVES.intersection(other)
    if INCOMING.get(kind) not in labels:
        return False, False
    return True, kind != "inferredTo" and not LEAVES.intersection(other)


def run(url, statement, **parameters):
    body = json.dumps({"statements": [{"statement": statement, "parameters": parameters}]}).encode()
    request = urllib.request.Request(url.rstrip("/") + "/db/graph.db/tx/commit", body,
                                     {"Content-Type": "application/json"})
    with urllib.request.urlopen(request) as response:
        answer = json.load(response)
    if answer["errors"]:
        raise RuntimeError(answer["errors"])
    return [row["row"] for row in answer["results"][0]["data"]]


def literal(value):
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return repr(value)
    if isinstance(value, list):
        return "[" + ", ".join(literal(v) for v in value) + "]"
    return json.dumps(value)  # Cypher reads JSON's string escapes


def properties(props):
    return "{" + ", ".join(f"`{k}`: {literal(v)}" for k, v in sorted(props.items())) + "}"


def anonymise(labels, props, people):
    if "Person" in labels:
        n = people.setdefault(props["dbId"], len(people) + 1)
        props = {k: v for k, v in props.items() if k not in ("orcidId", "project")}
        props.update(firstname="Placeholder", surname=f"Person{n}", initial="P", displayName=f"Person{n}, P")
    if "InstanceEdit" in labels:
        props["displayName"] = f"Placeholder edit {props['dbId']}"
        if "note" in props:
            props["note"] = "Placeholder note"
    return props


def main(argv):
    if len(argv) < 3:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    url, events = argv[1], argv[2:]
    nodes, rels = {}, set()
    frontier = [r[0] for r in run(url, "MATCH (e:Event) WHERE e.stId IN $ids RETURN id(e)", ids=events)]
    if len(frontier) != len(events):
        raise SystemExit(f"found {len(frontier)} of {len(events)} events")
    expanded = set()
    while frontier:
        expanded.update(frontier)
        rows = run(url, "MATCH (n)-[r]-(m) WHERE id(n) IN $ids "
                        "RETURN id(n), labels(n), type(r), startNode(r) = n, properties(r), id(m), labels(m)",
                   ids=frontier)
        frontier = []
        for here, labels, kind, outgoing, props, there, other in rows:
            take, expand = follow(labels, kind, outgoing, other)
            if not take:
                continue
            start, end = (here, there) if outgoing else (there, here)
            rels.add((start, kind, json.dumps(props, sort_keys=True), end))
            nodes.setdefault(there, None)
            if expand and there not in expanded and there not in frontier:
                frontier.append(there)
    ids = sorted(expanded | set(nodes))
    info = run(url, "MATCH (d:DBInfo) RETURN id(d)")[0][0]
    ids.append(info)
    people = {}
    out = ["// The fixture graph (spec 001, T007): written by ci/fixture/extract.py; see README.md.",
           "CREATE INDEX fixture_dbId IF NOT EXISTS FOR (n:DatabaseObject) ON (n.dbId);",
           "CREATE INDEX fixture_stId IF NOT EXISTS FOR (n:DatabaseObject) ON (n.stId);"]
    key = {}
    for node_id, labels, props in run(url, "MATCH (n) WHERE id(n) IN $ids RETURN id(n), labels(n), properties(n) "
                                           "ORDER BY n.dbId", ids=ids):
        key[node_id] = props["dbId"]
        props = anonymise(labels, props, people)
        out.append(f"CREATE (:{':'.join('`' + l + '`' for l in sorted(labels))} {properties(props)});")
    for start, kind, props, end in sorted(rels, key=lambda r: (key[r[0]], r[1], key[r[3]], r[2])):
        out.append(f"MATCH (a:DatabaseObject {{dbId: {key[start]}}}), (b:DatabaseObject {{dbId: {key[end]}}}) "
                   f"CREATE (a)-[:`{kind}` {properties(json.loads(props))}]->(b);")
    print("\n".join(out))
    print(f"{len(ids)} nodes, {len(rels)} relationships, {len(people)} people", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
