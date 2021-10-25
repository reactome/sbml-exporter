package org.reactome.server.tools.sbml.converter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.domain.model.NegativeRegulation;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.graph.domain.model.PositiveRegulation;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.tools.sbml.data.DataFactory;
import org.reactome.server.tools.sbml.data.model.Participant;
import org.reactome.server.tools.sbml.data.model.ParticipantDetails;
import org.reactome.server.tools.sbml.data.model.ReactionBase;
import org.reactome.server.tools.sbml.util.Utils;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.CompartmentalizedSBase;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.TidySBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For a given event this converter uses the {@link DataFactory} to retrieve its target data and proceeds with the
 * conversion to a {@link SBMLDocument} taking advantage of the methods in the {@link Helper} class.
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 * @author Sarah Keating (skeating@ebi.ac.uk)
 */
public class SbmlConverter {

    private static Logger logger = LoggerFactory.getLogger("sbml-exporter");

    // SBMLinformation variables. To be changed when targeting a different sbml level and version
    public static final short SBML_LEVEL = 3;
    public static final short SBML_VERSION = 1;

    // Prefixes used in the identifiers of the different SBML sections
    public static final String META_ID_PREFIX = "metaid_";
    public static final String PATHWAY_PREFIX = "pathway_";
    public static final String REACTION_PREFIX = "reaction_";
    public static final String SPECIES_PREFIX = "species_";
    public static final String COMPARTMENT_PREFIX = "compartment_";

    private AdvancedDatabaseObjectService ads;
    protected Pathway pathway;
    protected String targetStId;

    private SBMLDocument sbmlDocument = null;

    private long metaid_count = 0L;
    private Set<String> existingObjects = new HashSet<>();
    private final Integer reactomeVersion;

    protected SbmlConverter(String targetId, Integer version) {
        this.targetStId = targetId;
        this.reactomeVersion = version;
    }
    
    public SbmlConverter(Event event, Integer version, AdvancedDatabaseObjectService ads) {
        this.targetStId = event.getStId();
        this.reactomeVersion = version;
        this.ads = ads;
        if (event instanceof Pathway) {
            this.pathway = (Pathway) event;
        } else {
            List<Pathway> parents = event.getEventOf();
            this.pathway = parents.isEmpty() ? null : parents.get(0); //Only one can be chosen... so let's take the first one
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public SBMLDocument convert() {
        if (sbmlDocument != null) return sbmlDocument;

        sbmlDocument = new SBMLDocument(SBML_LEVEL, SBML_VERSION);

        String modelId, pathwayName;
        if (pathway != null) {
            modelId = PATHWAY_PREFIX + pathway.getDbId();
            pathwayName = pathway.getDisplayName();
        } else {
            modelId = "no_parent_pathway";
            pathwayName = "No parent pathway detected";
        }

        Model model = sbmlDocument.createModel(modelId);
        model.setName(pathwayName);
        model.setMetaId(META_ID_PREFIX + metaid_count++);
        Helper.addProvenanceAnnotation(sbmlDocument, reactomeVersion);
        Helper.addAnnotations(model, pathway);

        Collection<ParticipantDetails> participants = getParticipantDetails();
        for (ParticipantDetails p : participants) addParticipant(model, p);

        for (ReactionBase rxn : getReactionList()) {
            String id = REACTION_PREFIX + rxn.getDbId();
            Reaction rn = model.createReaction(id);
            rn.setMetaId(META_ID_PREFIX + metaid_count++);
            //noinspection deprecation
            rn.setFast(false);
            rn.setReversible(false);
            rn.setName(rxn.getDisplayName());

            addCompartment(rn, rxn.getCompartments());

            if (rxn.getInputs() != null && !rxn.getInputs().isEmpty()) addInputs(rxn.getDbId(), rn, rxn.getInputs());
            if (rxn.getOutpus() != null && !rxn.getOutpus().isEmpty()) addOutputs(rxn.getDbId(), rn, rxn.getOutpus());
            if (rxn.getCatalysts() != null && !rxn.getCatalysts().isEmpty()) addModifier(rxn.getDbId(), rn, rxn.getCatalysts(), Role.CATALYST);
            if (rxn.getPositiveRegulators() != null && !rxn.getPositiveRegulators().isEmpty()) addModifier(rxn.getDbId(), rn, rxn.getPositiveRegulators(), Role.POSITIVE_REGULATOR);
            if (rxn.getNegativeRegulators() != null && !rxn.getNegativeRegulators().isEmpty()) addModifier(rxn.getDbId(), rn, rxn.getNegativeRegulators(), Role.NEGATIVE_REGULATOR);

            Helper.addAnnotations(rn, rxn.getReactionLikeEvent());
            Helper.addCVTerms(rn, rxn);
        }

        return sbmlDocument;
    }
    
    /**
     * Refactored method for subclassing.
     * @param targetStId
     * @return
     */
    protected Collection<ParticipantDetails> getParticipantDetails() {
        return DataFactory.getParticipantDetails(targetStId, ads);
    }
    
    /**
     * Refactored method for subclassing.
     * @param targetStId
     * @return
     */
    protected Collection<ReactionBase> getReactionList() {
        return DataFactory.getReactionList(targetStId, ads);
    }

    public void writeToFile(String output) {
        if (sbmlDocument == null) throw new RuntimeException("Please call the convert method before writing to file");
        Utils.writeSBML(output, targetStId, sbmlDocument);
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
                if (explanation != null) Helper.addNotes(sr, explanation);

                existingObjects.add(sr_id);
            }
        }
    }

    private void addParticipant(Model model, ParticipantDetails participant) {
        String speciesId = SPECIES_PREFIX + participant.getPhysicalEntity().getDbId();

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

        addCompartment(s, pe.getCompartment());
    }

    private void addCompartment(CompartmentalizedSBase s, List<org.reactome.server.graph.domain.model.Compartment> compartments) {
        if (compartments.size() > 0) {
            org.reactome.server.graph.domain.model.Compartment compartment = compartments.get(0);
            addCompartment(s, compartment);
//            if (compartments.size() > 1) logger.warn(String.format("More than one compartment found for '%s'. ONLY the first one has been added", s.getId()));
        } else {
            logger.warn(String.format("No compartment found for '%s'", s.getId()));
        }
    }

    private void addCompartment(CompartmentalizedSBase s, org.reactome.server.graph.domain.model.Compartment compartment) {
        String comp_id = COMPARTMENT_PREFIX + compartment.getDbId();
        if (!existingObjects.contains(comp_id)) {
            Compartment c = sbmlDocument.getModel().createCompartment(comp_id);
            c.setMetaId(META_ID_PREFIX + metaid_count++);
            c.setName(compartment.getDisplayName());
            c.setConstant(true);
            Helper.addSBOTerm(c, SBOTermLookup.get(compartment));

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
