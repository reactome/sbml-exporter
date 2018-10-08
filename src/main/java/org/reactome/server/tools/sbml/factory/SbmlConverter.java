package org.reactome.server.tools.sbml.factory;

import org.reactome.server.graph.domain.model.NegativeRegulation;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.graph.domain.model.PositiveRegulation;
import org.reactome.server.tools.sbml.fetcher.DataFactory;
import org.reactome.server.tools.sbml.fetcher.model.Participant;
import org.reactome.server.tools.sbml.fetcher.model.ReactionBase;
import org.sbml.jsbml.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Helper.addProvenanceAnnotation(sbmlDocument);
        Helper.addAnnotations(model, pathway);

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

            Helper.addAnnotations(rn, rxn.getReactionLikeEvent());
            Helper.addCVTerms(rn, rxn);
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
                Helper.addSBOTerm(sr, Role.INPUT.term);
                sr.setStoichiometry(participant.getStoichiometry());

                addParticipant(sbmlDocument.getModel(), participant);

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
                Helper.addSBOTerm(sr, Role.OUTPUT.term);
                sr.setStoichiometry(participant.getStoichiometry());

                addParticipant(sbmlDocument.getModel(), participant);

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
                Helper.addSBOTerm(sr, role.term);

                String explanation = null;
                switch (role) {
                    case POSITIVE_REGULATOR:
                        explanation = (new PositiveRegulation()).getExplanation();
                        break;
                    case NEGATIVE_REGULATOR:
                        explanation = (new NegativeRegulation()).getExplanation();
                }

                Helper.addNotes(rn, explanation);

                addParticipant(sbmlDocument.getModel(), participant);

                existingObjects.add(sr_id);
            }
        }
    }

    private void addParticipant(Model model, Participant participant) {
        String speciesId = SPECIES_PREFIX + participant.getPhysicalEntity().getDbId();

        if (!existingObjects.contains(speciesId)) {
            PhysicalEntity pe = participant.getPhysicalEntity();
            Species s = model.createSpecies(speciesId);
            s.setMetaId(META_ID_PREFIX + metaid_count++);
            s.setName(pe.getDisplayName());
            // set other required fields for SBML L3
            s.setBoundaryCondition(false);
            s.setHasOnlySubstanceUnits(false);
            s.setConstant(false);
            Helper.addSBOTerm(s, SBOTermLookup.get(pe));
            Helper.addAnnotations(s, participant);

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
            Helper.addSBOTerm(c, SBOTermLookup.get(compartment));

            /* TODO: MISSING
            if (addAnnotations){
                 String refId = (thisPathway != null) ? thisPathway.getStId() : "listOfEvents";
                 CVTermBuilder cvterms = new CVTermBuilder(c, refId);
                 cvterms.createCompartmentAnnotations(comp);
             }
             */
            Helper.addCVTerm(c, CVTerm.Qualifier.BQB_IS, compartment.getUrl());

            existingObjects.add(comp_id);
        }
        s.setCompartment(comp_id);
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

}
