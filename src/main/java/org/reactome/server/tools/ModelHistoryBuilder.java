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

    public void createHistory(Pathway path){
        createHistoryFromEvent(path);
        for (Event e: path.getHasEvent()) {
            createHistoryFromEvent(e);
        }

        thisHistory.setCreatedDate(earliestCreatedDate);
        Collections.sort(modified);
        for (Date d: modified){
            thisHistory.setModifiedDate(d);
        }
        addModelHistory(thisHistory);
    }


     private void createHistoryFromEvent(Event path){
        addCreatedInformation(path.getCreated());
        addInformation(path.getModified());
        try {
            for (InstanceEdit edit : path.getAuthored()) {
                addInformation(edit);
            }
        }
        catch (NullPointerException e) {
        }
        try {
            for (InstanceEdit edit : path.getEdited()) {
                addInformation(edit);
            }
        }
        catch (NullPointerException e) {
        }
        try {
            for (InstanceEdit edit : path.getReviewed()) {
                addInformation(edit);
            }
        }
        catch (NullPointerException e) {
        }
        try {
            for (InstanceEdit edit : path.getRevised()) {
                addInformation(edit);
            }
        }
        catch (NullPointerException e) {
        }
    }

    private void addCreatedInformation(InstanceEdit edit) {
        if (edit == null || edit.getAuthor() == null) {
            return;
        }
        for (Person p : edit.getAuthor()){
            if (!authors.containsKey(p.getSurname())) {
                thisHistory.addCreator(createCreator(p));
                authors.put(p.getSurname(), p);
            }
        }

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

    private void addInformation(InstanceEdit edit) {
        if (edit == null) {
            return;
        }
        for (Person p : edit.getAuthor()){
            if (!authors.containsKey(p.getSurname())) {
                thisHistory.addCreator(createCreator(p));
                authors.put(p.getSurname(), p);
            }
        }
        Date thisdate = formatDate(edit.getDateTime());
        if (!modified.contains(thisdate)) {
            modified.add(thisdate);
        }
    }

    private Date formatDate(String datetime){
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
        Date date = null;
        try {
            date = format.parse(datetime);
        } catch(ParseException e){
        }
        return date;

    }

    private Creator createCreator(Person editor){
        Creator creator = new Creator();
        creator.setFamilyName(editor.getSurname());
        creator.setGivenName(editor.getFirstname());
        creator.setEmail(editor.getEMailAddress());
        for (Affiliation a : editor.getAffiliation()){
            for (String s : a.getName()){
                creator.setOrganisation(s);
            }
        }
        return creator;
    }
}
