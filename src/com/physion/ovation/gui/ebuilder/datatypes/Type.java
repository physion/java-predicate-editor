package com.physion.ovation.gui.ebuilder.datatypes;


/**
 * Please note, "REFERENCE" is not a primitive type.  That value is
 * used to denote types that are a reference to a class of some sort.
 */
public enum Type {

    /**
     * These are the "primitive" types.
     */
    BOOLEAN, UTF_8_STRING, INT_16, INT_32, FLOAT_64, DATE_TIME,

    /**
     * This is a "reference" type.  I.e. a reference to a class of
     * some sort.
     */
    REFERENCE,

    /**
     * This is a "parameters map" type.
     */
    PARAMETERS_MAP,

    /**
     * This is "per-user" reference type.
     */
    PER_USER,

    /**
     * This is "per-user parameters map" reference type.
     * As of September 2011, the only attribute of this
     * type is the EntityBase.properties attribute.
     */
    PER_USER_PARAMETERS_MAP;


    /**
     * This returns true if this Type is a "primitive" type.
     * E.g. an int, float, boolean, etc.
     */
    public boolean isPrimitive() {
        return((this == BOOLEAN) ||
               (this == UTF_8_STRING) ||
               (this == INT_16) ||
               (this == INT_32) ||
               (this == FLOAT_64) ||
               (this == DATE_TIME));
    }


    public String toString() {

        switch (this) {
            case BOOLEAN:
                return("boolean");
            case UTF_8_STRING:
                return("string");
            case INT_16:
                return("short");
            case INT_32:
                return("int");
            case FLOAT_64:
                return("float");
            case DATE_TIME:
                return("time");
            case REFERENCE:
                return("reference");
            case PARAMETERS_MAP:
                return("parameters map");
            case PER_USER_PARAMETERS_MAP:
                return("per-user parameters map");
            default:
                return("ERROR");
        }
    }


    /*
    public Type parse(String stringValue) {

        if (Type.INT_32.toString().equals(stringType)) {
            return(Type.INT_32);
        }

        System.err.println("ERROR: Unhandled stringValue = "+stringValue);
        return(Type.INT_32);
    }
    */


    /**
     * This is a simple test program for this class.
     */
    public static void main(String[] args) {

        System.out.println("Type test is starting...");

        for (Type type : Type.values())
            System.out.println(type+".isPrimitive() = "+type.isPrimitive());

        System.out.println("Type test is ending.");
    }
}
