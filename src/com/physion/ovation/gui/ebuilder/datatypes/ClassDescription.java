package com.physion.ovation.gui.ebuilder.datatypes;

import java.util.ArrayList;

/**
 * This is the description of a class.  The DataModel object creates
 * all of these for the system.
 *
 * TODO: Would it be useful to engineers to have a method to get an
 * Attribute of a class via its name?
 */
public class ClassDescription {

    /**
     * The name of this class.  E.g. "Epoch", "Response".
     */
    private String name = "ERROR: name not set.";

    /**
     * The list of Attributes this class has.
     */
    private ArrayList<Attribute> attributes = new ArrayList<Attribute>();

    /**
     * The parent class of this class, if it has one.
     * Please note that the EntityBase class does not have a parent class.
     */
    private ClassDescription parentClass = null;


    /**
     * Create a ClassDescription with no attributes.
     * You will probably need to call addAttribute to add some attributes.
     */
    public ClassDescription(String name, ClassDescription parentClass) {
        this.name = name;
        this.parentClass = parentClass;
    }


    /**
     * Override the equals() method so we can do some extra checking.
     */
    @Override
    public boolean equals(Object rhs) {

        if (rhs == null)
            return(false);

        if (!(rhs instanceof ClassDescription))
            return(false);

        ClassDescription other = (ClassDescription)rhs;

        if (this == rhs)
            return(true);

        if (!this.name.equals(other.name))
            return(false);

        if ((this.parentClass == null) && (other.parentClass != null))
            return(false);
        if ((this.parentClass != null) && (other.parentClass == null))
            return(false);
        if ((this.parentClass != null) && (other.parentClass != null))
            if (!this.parentClass.equals(other.parentClass))
                return(false);

        if (this.attributes.size() != other.attributes.size())
            return(false);
        for (int index = 0; index < this.attributes.size(); index++) {
            if (!this.attributes.get(index).equals(
                other.attributes.get(index))) {
                return(false);
            }
        }

        return(true);
    }


    public String getName() {
        return(name);
    }


    public void setParentClass(ClassDescription parentClass) {
        this.parentClass = parentClass;
    }


    public ClassDescription getParentClass() {
        return(parentClass);
    }


    /*
    public boolean isInstanceof(ClassDescription classDescription) {

        if (classDescription == null) {
            return(false);
        }

        while (classDescription != null) {
            if (this.equals(classDescription))
                return(true);
            classDescription = classDescription.getParentClass();
        }

        return(false);
    }
    */
    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }


    /**
     * Returns the list of all the attributes that this class has.
     * I.e. it returns this class's direct attributes, plus all the
     * attributes of its ancestor classes.
     */
    public ArrayList<Attribute> getAllAttributes() {

        ArrayList<Attribute> allAttributes = new ArrayList<Attribute>();

        /**
         * First add our attributes to the list we will return.
         */
        allAttributes.addAll(attributes);

        /**
         * Now recursively add our parent attributes.
         */
        if (parentClass != null)
            allAttributes.addAll(parentClass.getAllAttributes());

        return(allAttributes);
    }


    /**
     * Please make sure you want this method and not the
     * method getAllAttributes().
     *
     * Please note, this method returns a reference to this
     * ClassDescription's internal list, so don't mess with it.
     */
    /*
    public ArrayList<Attribute> getAttributeList() {
        return(attributes);
    }
    */


    public boolean containsAttribute(Attribute attribute) {

        for (Attribute att : getAllAttributes()) {
            if (att.equals(attribute))
                return(true);
        }

        return(false);
    }


    /**
     * Get a copy of the Attribute with the passed in queryName
     * if such an attribute exists in this ClassDescription.
     * Returns null if it doesn't.
     *
     * @param queryName A string like:  "incomplete", "protocolID",
     * "properties".  Not a displayName like "My Property".
     */
    public Attribute getAttribute(String queryName) {

        if (queryName.startsWith("my")) {
            System.out.println("Temporary hack in getAttribute");
            queryName = queryName.substring(2);
        }
        
        for (Attribute att : getAllAttributes()) {
            if (att.getBaseQueryName().equals(queryName))
                return(new Attribute(att));
        }

        return(null);
    }


    public String toString() {
        return(name);
    }


    /**
     * Get a String version of this class useful for debugging.
     */
    public String toStringDebug() {
        return(toStringDebug(""));
    }


    /**
     * Get a string version of this class useful for debugging and
     * indent the string using the passed in indent string.
     * We do this to show a nested hierarchy of a class and its
     * child classes and their attributes.
     */
    public String toStringDebug(String indent) {

        String string;

        string = indent+name;

        for (Attribute attribute : attributes) {
            string += "\n"+indent;
            string += " "+attribute;
        }

        if (parentClass != null) {
            string += "\n"+indent+" Parent Class:\n";
            string += parentClass.toStringDebug(indent+"  ");
        }

        return(string);
    }


    /**
     * This is a simple test program for this class.
     */
    public static void main(String[] args) {

        System.out.println("ClassDescription test is starting...");

        /**
         * Create a ClassDescription for all the classes we will
         * need.  Please note, when we first create them, they
         * aren't valid because we might not have not set the parent class
         * or attributes.
         */
        ClassDescription entityBaseCD =
            new ClassDescription("EntityBase", null);
        ClassDescription taggableEntityBaseCD =
            new ClassDescription("TaggableEntityBase", entityBaseCD);
        ClassDescription userCD =
            new ClassDescription("User", taggableEntityBaseCD);
        ClassDescription keywordTagCD =
            new ClassDescription("KeywordTag", entityBaseCD);

        /**
         * Initialize values of the EntityBase class.
         */
        Attribute attribute = new Attribute("owner", Type.REFERENCE,
                                  userCD, Cardinality.TO_ONE);
        entityBaseCD.addAttribute(attribute);

        attribute = new Attribute("uuid", Type.UTF_8_STRING);
        entityBaseCD.addAttribute(attribute);

        attribute = new Attribute("incomplete", Type.BOOLEAN);
        entityBaseCD.addAttribute(attribute);

        /**
         * Initialize values of the TaggableEntityBase class.
         */
        attribute = new Attribute("keywords", Type.REFERENCE,
                                  keywordTagCD, Cardinality.TO_MANY);
        taggableEntityBaseCD.addAttribute(attribute);

        /**
         * Initialize values of the User class.
         */
        attribute = new Attribute("userName", Type.UTF_8_STRING);
        userCD.addAttribute(attribute);

        /**
         * Initialize values of the KeywordTag class.
         */
        attribute = new Attribute("tag", Type.UTF_8_STRING);
        keywordTagCD.addAttribute(attribute);

        /**
         * Print out values.
         */
        //System.out.println("\nentityBaseCD.toString() below:\n"+entityBaseCD);
        System.out.println("\nuserCD.toString() below:\n"+userCD.toStringDebug());
        System.out.println("\nkeywordTagCD.toString() below:\n"+keywordTagCD.toStringDebug());
        //System.out.println("\ntaggableEntityBaseCD.toString() below:\n"+
        //                   taggableEntityBaseCD);
        System.out.println("ClassDescription test is ending.");
    }
}
