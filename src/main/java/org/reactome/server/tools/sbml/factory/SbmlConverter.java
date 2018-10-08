package org.reactome.server.tools.sbml.factory;

import jodd.util.StringUtil;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.sbml.fetcher.DataFactory;
import org.reactome.server.tools.sbml.fetcher.model.Participant;
import org.reactome.server.tools.sbml.fetcher.model.ReactionBase;
import org.sbml.jsbml.*;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.xml.XMLNode;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.sbml.jsbml.JSBML.getJSBMLDottedVersion;

public class SbmlConverter {

    /**
     * sbml information variables
     * these can be changed if we decide to target a different sbml level and version
     */
    private static final short SBML_LEVEL = 3;
    private static final short SBML_VERSION = 1;

    private static final String META_ID_PREFIX = "metaid_";
    private static final String PATHWAY_PREFIX = "pathway_";
    private static final String REACTION_PREFIX = "reaction_";
    private static final String SPECIES_PREFIX = "species_";
    private static final String COMPARTMENT_PREFIX = "compartment_";

    private static final String REACTOME_URI = "https://reactome.org/content/detail/";

    private Pathway pathway;
    private SBMLDocument sbmlDocument;

    private long metaid_count = 0L;
    private Set<String> existingObjects = new HashSet<>();

    public SbmlConverter(Pathway pathway) {
        this.pathway = pathway;
    }

    public SBMLDocument convert() {
        if (sbmlDocument != null) return sbmlDocument;

        sbmlDocument = new SBMLDocument(SBML_LEVEL, SBML_VERSION);

        String modelId = PATHWAY_PREFIX + pathway.getDbId();
        Model model = sbmlDocument.createModel(modelId);
        model.setName(pathway.getDisplayName());
        model.setMetaId(META_ID_PREFIX + metaid_count++);
        addAnnotations(model, pathway);
        addProvenanceAnnotation();

        for (ReactionBase rxn : DataFactory.getReactionList(pathway.getStId())) {
            String id = REACTION_PREFIX + rxn.getDbId();
            Reaction rn = model.createReaction(id);
            rn.setMetaId(META_ID_PREFIX + metaid_count++);
            rn.setFast(false);
            rn.setReversible(false);
            rn.setName(rxn.getDisplayName());


            addInputs(rxn.getDbId(), rn, rxn.getInputs());
            addOutputs(rxn.getDbId(), rn, rxn.getOutpus());
            addModifier(rxn.getDbId(), rn, rxn.getCatalysts(), Role.CATALYST);
            addModifier(rxn.getDbId(), rn, rxn.getPositiveRegulators(), Role.POSITIVE_REGULATOR);
            addModifier(rxn.getDbId(), rn, rxn.getNegativeRegulators(), Role.NEGATIVE_REGULATOR);

            addAnnotations(rn, rxn);

            //NotesBuilder notes = new NotesBuilder(rn);
            //notes.addPathwayNotes(event);
        }

        return sbmlDocument;
    }

    private void addInputs(Long reactionDbId, Reaction rn, List<Participant> participants) {
        for (Participant participant : participants) {
            String sr_id = Role.INPUT.getIdentifier(reactionDbId, participant.getPhysicalEntity());


            if (!existingObjects.contains(sr_id)) {
                String speciesId = SPECIES_PREFIX + participant.getPhysicalEntity().getDbId();

                SpeciesReference sr = rn.createReactant(sr_id, speciesId);

                sr.setConstant(true);
                addSBOTerm(sr, Role.INPUT.term);
                sr.setStoichiometry(participant.getStoichiometry());

                addParticipant(participant);

                existingObjects.add(sr_id);
            }
        }
    }

    private void addOutputs(Long reactionDbId, Reaction rn, List<Participant> participants) {
        for (Participant participant : participants) {
            String sr_id = Role.OUTPUT.getIdentifier(reactionDbId, participant.getPhysicalEntity());

            if (!existingObjects.contains(sr_id)) {
                String speciesId = SPECIES_PREFIX + participant.getPhysicalEntity().getDbId();

                SpeciesReference sr = rn.createProduct(sr_id, speciesId);
                sr.setConstant(true);
                addSBOTerm(sr, Role.OUTPUT.term);
                sr.setStoichiometry(participant.getStoichiometry());

                addParticipant(participant);

                existingObjects.add(sr_id);
            }
        }
    }

    private void addModifier(Long reactionDbId, Reaction rn, List<Participant> participants, Role role) {
        for (Participant participant : participants) {
            String sr_id = role.getIdentifier(reactionDbId, participant.getPhysicalEntity());

            if (!existingObjects.contains(sr_id)) {
                String speciesId = SPECIES_PREFIX + participant.getPhysicalEntity().getDbId();

                ModifierSpeciesReference sr = rn.createModifier(sr_id, speciesId);
                addSBOTerm(sr, role.term);

                String explanation = null;
                switch (role) {
                    case POSITIVE_REGULATOR:
                        explanation = (new PositiveRegulation()).getExplanation();
                        break;
                    case NEGATIVE_REGULATOR:
                        explanation = (new NegativeRegulation()).getExplanation();
                }

                if (explanation != null) addNotes(rn, explanation);

                addParticipant(participant);

                existingObjects.add(sr_id);
            }
        }
    }

    private void addParticipant(Participant participant) {
        String speciesId = SPECIES_PREFIX + participant.getPhysicalEntity().getDbId();

        if (!existingObjects.contains(speciesId)) {
            Model model = sbmlDocument.getModel();

            PhysicalEntity pe = participant.getPhysicalEntity();
            Species s = model.createSpecies(speciesId);
            s.setMetaId(META_ID_PREFIX + metaid_count++);
            s.setName(pe.getDisplayName());
            // set other required fields for SBML L3
            s.setBoundaryCondition(false);
            s.setHasOnlySubstanceUnits(false);
            s.setConstant(false);
            addSBOTerm(s, SBOTermLookup.get(pe));
            addAnnotations(s, participant);

            org.reactome.server.graph.domain.model.Compartment compartment;
            // TODO: what if there is more than one compartment listed
            if (pe.getCompartment() != null && pe.getCompartment().size() > 0) {
                compartment = pe.getCompartment().get(0);
                addCompartment(s, compartment);
            } else {
                //TODO: Log this situation!
                //log.warn("Encountered a Physical Entity with no compartment: " + pe.getStId());
            }

            existingObjects.add(speciesId);
        }
    }

    private void addCompartment(Species s, org.reactome.server.graph.domain.model.Compartment compartment) {
        String comp_id = COMPARTMENT_PREFIX + compartment.getDbId();
        if (!existingObjects.contains(comp_id)) {
            Compartment c = sbmlDocument.getModel().createCompartment(comp_id);
            c.setMetaId(META_ID_PREFIX + metaid_count++);
            c.setName(compartment.getDisplayName());
            c.setConstant(true);
            addSBOTerm(c, SBOTermLookup.get(compartment));

            /* TODO: MISSING
            if (addAnnotations){
                 String refId = (thisPathway != null) ? thisPathway.getStId() : "listOfEvents";
                 CVTermBuilder cvterms = new CVTermBuilder(c, refId);
                 cvterms.createCompartmentAnnotations(comp);
             }
             */
            addCVTerm(c, CVTerm.Qualifier.BQB_IS, compartment.getUrl());

            existingObjects.add(comp_id);
        }
        s.setCompartment(comp_id);
    }

    private void addAnnotations(Species s, Participant participant) {
        PhysicalEntity pe = participant.getPhysicalEntity();

        addNotes(s, participant.getExplanation());

        List<String> summations = pe.getSummation().stream()
                .map(Summation::getText)
                .peek(this::removeTags)
                .collect(Collectors.toList());
        addNotes(s, summations);

        List<String> litrefs = participant.getPhysicalEntity().getLiteratureReference()
                .stream()
                .filter(l -> l instanceof LiteratureReference)
                .map(l -> ((LiteratureReference) l).getUrl())
                .collect(Collectors.toList());
        addCVTerm(s, CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, litrefs);

        if (pe instanceof Complex || pe instanceof EntitySet || pe instanceof Polymer) {
            addCVTerm(s, CVTerm.Qualifier.BQB_IS, REACTOME_URI + pe.getStId());
            addCVTerm(s, CVTerm.Qualifier.BQB_HAS_PART, participant.getUrls());
        } else {
            participant.addUrl(REACTOME_URI + pe.getStId());
            addCVTerm(s, CVTerm.Qualifier.BQB_IS, participant.getUrls());

            if (pe instanceof EntityWithAccessionedSequence) {
                EntityWithAccessionedSequence ewas = (EntityWithAccessionedSequence) pe;

                List<String> psis = ewas.getHasModifiedResidue()
                        .stream()
                        .filter(r -> r instanceof TranslationalModification && ((TranslationalModification) r).getPsiMod()!=null)
                        .map(r -> ((TranslationalModification) r).getPsiMod().getUrl())
                        .collect(Collectors.toList());
                addCVTerm(s, CVTerm.Qualifier.BQB_HAS_VERSION, psis);
            }
        }

        List<String> infTos = pe.getInferredTo()
                .stream()
                .map(p -> REACTOME_URI + p.getStId())
                .collect(Collectors.toList());
        addCVTerm(s, CVTerm.Qualifier.BQB_IS_HOMOLOG_TO, infTos);

        List<String> infFroms = pe.getInferredFrom()
                .stream()
                .map(p -> REACTOME_URI + p.getStId())
                .collect(Collectors.toList());
        addCVTerm(s, CVTerm.Qualifier.BQB_IS_HOMOLOG_TO, infFroms);
    }

    private void addAnnotations(Reaction reaction, ReactionBase rxn) {
        addCVTerm(reaction, CVTerm.Qualifier.BQB_IS, REACTOME_URI + rxn.getStId());
        addCVTerm(reaction, CVTerm.Qualifier.BQB_IS, rxn.getGoTerms());
        addCVTerm(reaction, CVTerm.Qualifier.BQB_IS, rxn.getEcNumbers());
        addCVTerm(reaction, CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, rxn.getLiteratureRefs());
        addCVTerm(reaction, CVTerm.Qualifier.BQB_OCCURS_IN, rxn.getDiseases());
        addCVTerm(reaction, CVTerm.Qualifier.BQM_HAS_INSTANCE, rxn.getCrossReferences());

        //addCreators(rxn.getReactionLikeEvent(), reaction);
    }

    private void addAnnotations(SBase sBase, Event event) {
        History history = new History();
        InstanceEdit created = event.getCreated();
        created.getAuthor().forEach(c -> history.addCreator(getCreator(c)));
        history.setCreatedDate(formatDate(created.getDateTime()));

        InstanceEdit modified = event.getModified();
        modified.getAuthor().forEach(m -> history.addCreator(getCreator(m)));
        history.addModifiedDate(formatDate(modified.getDateTime()));

        for (InstanceEdit authored : event.getAuthored()) {
            authored.getAuthor().forEach(a -> history.addCreator(getCreator(a)));
            history.addModifiedDate(formatDate(authored.getDateTime()));
        }

        for (InstanceEdit revised : event.getRevised()) {
            revised.getAuthor().forEach(r -> history.addCreator(getCreator(r)));
            history.addModifiedDate(formatDate(revised.getDateTime()));
        }

        //TODO: Add the remaining pathway info to the SBML
        List<String> summations = event.getSummation().stream()
                .map(Summation::getText)
                .peek(this::removeTags)
                .collect(Collectors.toList());
        addNotes(sBase, summations);

        Annotation annotation = new Annotation();
        annotation.setHistory(history);

        List<String> uris = new ArrayList<>();
        uris.add(REACTOME_URI + event.getStId());
        GO_BiologicalProcess go = event.getGoBiologicalProcess();
        if (go != null) uris.add(go.getUrl());
        addCVTerm(annotation, CVTerm.Qualifier.BQB_IS, uris);

        List<String> litRefs = event.getLiteratureReference()
                .stream()
                .filter(s -> s instanceof LiteratureReference)
                .map(p -> ((LiteratureReference) p).getUrl())
                .collect(Collectors.toList());
        addCVTerm(annotation, CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, litRefs);

        sBase.setAnnotation(annotation);
    }

    private void addSBOTerm(SBase sBase, Integer term) {
        if (term >= 0 && term <= 9999999) {
            sBase.setSBOTerm(term);
        } else {
            //TODO: log
        }
    }

    private void addCVTerm(SBase sBase, CVTerm.Qualifier qualifier, List<String> uris) {
        addCVTerm(sBase, qualifier, uris.toArray(new String[uris.size()]));
    }

    private void addCVTerm(SBase sBase, CVTerm.Qualifier qualifier, String... uris) {
        if(uris.length>0) {
            CVTerm term = new CVTerm(qualifier);
            Arrays.stream(uris).forEach(term::addResourceURI);
            sBase.addCVTerm(term);
        }
    }

    private void addCVTerm(Annotation annotation, CVTerm.Qualifier qualifier, List<String> uris){
        if(!uris.isEmpty()) {
            CVTerm term = new CVTerm(qualifier);
            uris.forEach(term::addResourceURI);
            annotation.addCVTerm(term);
        }
    }

    /**
     * Open and closing tags for notes elements
     */
    private String openNotes = "<notes><p xmlns=\"http://www.w3.org/1999/xhtml\">";
    private String closeNotes = "</p></notes>";

    void addNotes(SBase sBase, List<String> content) {
        addNotes(sBase, content.toArray(new String[content.size()]));
    }

    void addNotes(SBase sBase, String... content) {
        if(content.length > 0) {
            String notes = openNotes + StringUtil.join(content, System.lineSeparator()) + closeNotes;
            XMLNode node;
            try {
                node = XMLNode.convertStringToXMLNode(notes);
                sBase.appendNotes(node);
            } catch (Exception e) {
                //TODO: log this situation
            }
        }
    }

    /**
     * Remove any html tags from the text.
     *
     * @param notes String to be adjusted.
     * @return String with any <></> removed.
     */
    private String removeTags(String notes) {
        // if we have an xhtml tags in the text it messes up parsing
        // copied from old reactome code with some additions
        notes = notes.replaceAll("<->", " to ");
        notes = notes.replaceAll("\\p{Cntrl}+", " ");
        notes = notes.replaceAll("</*[a-zA-Z][^>]*>", " ");
        notes = notes.replaceAll("<>", " interconverts to ");
        notes = notes.replaceAll("<", " ");
        notes = notes.replaceAll("\n+", "  ");
        return notes.replaceAll("&+", "  ");
    }

    /**
     * Write the SBMLDocument to a String.
     *
     * @return String representing the SBMLDocument.
     */
    public String toString() {
        SBMLWriter sbmlWriter = new TidySBMLWriter();
        String output;
        try {
            output = sbmlWriter.writeSBMLToString(sbmlDocument);
        } catch (Exception e) {
            output = "failed to write";
        }
        return output;
    }

    /**
     * Creates an SBML Creator object from the information from ReactomeDB.
     *
     * @param person Person from ReactomeDB
     * @return Creator object for JSBML
     */
    private Creator getCreator(Person person) {
        Creator creator = new Creator();
        creator.setFamilyName(person.getSurname() == null ? "" : person.getSurname());
        creator.setGivenName(person.getFirstname() == null ? "" : person.getFirstname());
        person.getAffiliation().forEach(a -> creator.setOrganisation(a.getName().get(0)));
        return creator;
    }


    /**
     * Creates a Date object from the string stored in ReactomeDB.
     *
     * @param datetime String the date times as stored in ReactomeDB
     * @return Date object created from the String or null if this
     * cannot be parsed.
     */
    private Date formatDate(String datetime) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
        Date date;
        try {
            date = format.parse(datetime);
        } catch (ParseException e) {
            date = null;
        }
        return date;

    }

    /**
     * Adds information about the reactomeDB version and jsbml version
     */
    void addProvenanceAnnotation() {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat();
        Integer version = ReactomeGraphCore.getService(GeneralService.class).getDBInfo().getVersion();
        String jsbml = String.format("" +
                        "<annotation>" +
                        "<p xmlns=\"http://www.w3.org/1999/xhtml\">" +
                        "SBML generated from Reactome version %d on %s using JSBML version %s." +
                        "</p>" +
                        "</annotation>",
                version, dateFormat.format(date), getJSBMLDottedVersion());
        XMLNode node;
        try {
            node = XMLNode.convertStringToXMLNode(jsbml);
            sbmlDocument.appendNotes(node);
        } catch (Exception e) {
            //TODO: log this situation
        }
    }
}
