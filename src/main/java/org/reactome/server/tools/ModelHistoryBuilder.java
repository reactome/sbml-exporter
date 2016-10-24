package org.reactome.server.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.reactome.server.graph.domain.model.*;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.SBase;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class ModelHistoryBuilder extends AnnotationBuilder {
    private History thisHistory = null;
    private Map<String, Person> authors = new HashMap<String, Person>();
    private Date earliestCreatedDate = null;
    private List<java.util.Date> modified = new ArrayList<java.util.Date>();


    ModelHistoryBuilder(SBase sbase) {
        super(sbase);
        thisHistory = new History();

    }

    /**
     * Creates an SBML History object from the pathway. It recurses through
     * all Events contained in the Pathway.
     *
     * @param path   Pathway from ReactomeDB
     */
    void createHistory(Pathway path){
        createHistoryFromEvent(path);
        if (path.getHasEvent() != null) {
            for (Event e : path.getHasEvent()) {
                createHistoryFromEvent(e);
            }
        }

        thisHistory.setCreatedDate(earliestCreatedDate);
        Collections.sort(modified);
        for (Date d: modified){
            thisHistory.setModifiedDate(d);
        }
        addModelHistory(thisHistory);
    }

    void createHistory(List<Event> listOfEvents){
        for (Event e : listOfEvents) {
            createHistoryFromEvent(e);
        }

        thisHistory.setCreatedDate(earliestCreatedDate);
        Collections.sort(modified);
        for (Date d: modified){
            thisHistory.setModifiedDate(d);
        }
        addModelHistory(thisHistory);
    }

    /**
     * Gathers information from a particular Event regarding contributors
     * and dates.
     *
     * @param path  Event from ReactomeDB
     */
    private void createHistoryFromEvent(Event path){
        addCreatedInformation(path.getCreated());
        addInformation(path.getModified());

        // Steve suggests adding authored and revised but not reviewed/edited
        if (path.getAuthored() != null) {
            for (InstanceEdit edit : path.getAuthored()) {
                addInformation(edit);
            }
        }
        // discussion decided that these categories should not be included
/*
        if (path.getEdited() != null) {
            for (InstanceEdit edit : path.getEdited()) {
                addInformation(edit);
            }
        }
        if (path.getReviewed() != null) {
            for (InstanceEdit edit : path.getReviewed()) {
                addInformation(edit);
            }
        }
*/
        if (path.getRevised() != null) {
            for (InstanceEdit edit : path.getRevised()) {
                addInformation(edit);
            }
        }
    }

    /**
     * Adds information regarding the creator of a model.
     * This differs from addInformation in that it uses teh dates to establish the earliest
     * created date.
     *
     * @param edit  InstanceEdit from ReactomeDB
     */
    private void addCreatedInformation(InstanceEdit edit) {
        if (edit == null) {
            return;
        }
        addCreators(edit.getAuthor());
        Date thisdate = formatDate(edit.getDateTime());
        if (earliestCreatedDate == null) {
            earliestCreatedDate = thisdate;
        }
        else if (thisdate.compareTo(earliestCreatedDate) < 0){
            if (!modified.contains(earliestCreatedDate)) {
                modified.add(earliestCreatedDate);
            }
            earliestCreatedDate = thisdate;
        }
        else {
            if (!modified.contains(thisdate)) {
                modified.add(thisdate);
            }
        }

    }

    /**
     * Adds information about any modifications to the model. It adds additional
     * creators and lists the dates modified.
     *
     * @param edit  InstanceEdit from ReactomeDB
     */
    private void addInformation(InstanceEdit edit) {
        if (edit == null) {
            return;
        }
        addCreators(edit.getAuthor());
        Date thisdate = formatDate(edit.getDateTime());
        if (!modified.contains(thisdate)) {
            modified.add(thisdate);
        }
    }

    /**
     * Adds any person listed to the creators of the model ensuring
     * no repetitions.
     *
     * @param editors  List<Person> from the InstanceEdit (from ReactomeDB)
     */
    private void addCreators(List<Person> editors){
        if (editors != null && editors.size() > 0) {
            for (Person p : editors) {
                if (!authors.containsKey(p.getSurname())) {
                    thisHistory.addCreator(createCreator(p));
                    authors.put(p.getSurname(), p);
                }
            }
        }
    }

    /**
     * Creates a Date object from the string stored in ReactomeDB.
     *
     * @param datetime  String the date times as stored in ReactomeDB
     *
     * @return          Date object created from the String or null if this
     *                  cannot be parsed.
     */
    private Date formatDate(String datetime){
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
        Date date;
        try {
            date = format.parse(datetime);
        } catch(ParseException e){
            date = null;
        }
        return date;

    }

    /**
     * Creates an SBML Creator object from the information from ReactomeDB.
     *
     * @param editor    Person from ReactomeDB
     *
     * @return          Creator object for JSBML
     */
    private Creator createCreator(Person editor){
        String entry;
        Creator creator = new Creator();

        entry = ((editor.getSurname() == null) ? "" : editor.getSurname());
        creator.setFamilyName(entry);

        entry = ((editor.getFirstname() == null) ? "" : editor.getFirstname());
        creator.setGivenName(entry);

        entry = ((editor.getEMailAddress() == null) ? "" : editor.getEMailAddress());
        if (!entry.equals("")) {
            creator.setEmail(entry);
        }

        if (editor.getAffiliation() != null) {
            for (Affiliation a : editor.getAffiliation()) {
                for (String s : a.getName()) {
                    creator.setOrganisation(s);
                }
            }
        }
        return creator;
    }
}
