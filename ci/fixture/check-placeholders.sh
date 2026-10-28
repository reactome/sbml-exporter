#!/usr/bin/env bash
# Fails if a person's name could have survived in the fixture graph (spec 001, T007):
# every Person must carry extract.py's placeholders and nothing else that names them,
# and every InstanceEdit's display name and note must be a placeholder.
# Usage: ci/fixture/check-placeholders.sh [ci/fixture/fixture.cypher]
# The grep patterns match the fixture's literal backticks, so single quotes are meant.
# shellcheck disable=SC2016
set -euo pipefail
fixture=${1:-$(dirname "$0")/fixture.cypher}
status=0

while IFS= read -r line; do
  if ! [[ $line =~ \`firstname\`:\ \"Placeholder\" && $line =~ \`surname\`:\ \"Person[0-9]+\" &&
          $line =~ \`initial\`:\ \"P\" && $line =~ \`displayName\`:\ \"Person[0-9]+,\ P\" ]] ||
     [[ $line =~ \`(orcidId|project)\` ]]; then
    echo "a Person that is not a placeholder: ${line:0:120}"
    status=1
  fi
done < <(grep -E '^CREATE \(:[^{]*`Person`' "$fixture")

while IFS= read -r line; do
  if ! [[ $line =~ \`displayName\`:\ \"Placeholder\ edit\ [0-9]+\" ]] ||
     { [[ $line =~ \`note\`: ]] && ! [[ $line =~ \`note\`:\ \"Placeholder\ note\" ]]; }; then
    echo "an InstanceEdit that is not a placeholder: ${line:0:120}"
    status=1
  fi
done < <(grep -E '^CREATE \(:[^{]*`InstanceEdit`' "$fixture")

people=$(grep -cE '^CREATE \(:[^{]*`Person`' "$fixture" || true)
edits=$(grep -cE '^CREATE \(:[^{]*`InstanceEdit`' "$fixture" || true)
echo "$people people, $edits instance edits checked"
exit $status
