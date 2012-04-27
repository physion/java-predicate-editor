/**
 * Copyright (c) 2011. Physion Consulting LLC
 * All rights reserved.
 */
package com.physion.ebuilder.datamodel;

import com.physion.ebuilder.datatypes.*;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import javax.swing.event.EventListenerList;
import java.io.*;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;


/**
 * A RowData object is one row in the GUI.  The very first row in the
 * GUI is the "root" row.  Each row can have 0 or more child rows.
 * A row has an "attributePath", which is how a RowData object defines
 * the attribute that it is describing.  For example:
 *
 *      epochGroups.epoch.label == "Test 21"
 *
 * is describing an attributePath to the label attribute, and says
 * that we are looking for Epochs whose label attribute is "Test 21".
 *
 * You can ask a RowData object whether it contains a legal value
 * by calling the containsLegalValue() method.  We can make the
 * containsLegalValue() code more clever if we want to catch more
 * user errors before the user attempts to use the PQL string that
 * s/he is creating.
 *
 * TODO: Perhaps change the structure a bit so the root row is NOT a
 * RowData object?  It really is a bit of a different thing given that
 * it is used to set the Class Under Qualification.
 *
 * TODO: Perhaps change how the "is null" and "is not null" values are
 * handled.  Right now, I set the attributeOperator to be "is null" or
 * "is not null".  But, I also have to have the last Attribute on the
 * RowData's attributePath be the Attribute.IS_NULL or IS_NOT_NULL value.
 * This seems a bit redundant.  The problem is, I need the Attribute on
 * the path in order to have the comboBox that holds "is null" etc. be created.
 * So, maybe we should not also store that information in the attributeOperator
 * member data?
 */
public class RowData
    implements RowDataListener, Serializable {

	private static final long serialVersionUID = 1L;

	/**
     * File name used just for testing.
     */
    public static final String SAVE_FILE_NAME = "testSaved.RDTree";

    /**
     * If a row has a second collection operator, this is the default
     * value for it.
     */
    public static final CollectionOperator DEFAULT_COLLECTION_OPERATOR2 =
        CollectionOperator.ANY;

    /**
     * This is the "topmost", or "root" class that is the ancestor
     * of ALL other rows.
     *
     * Please note this value only makes sense on the root RowData object
     * in the expression tree.
     */
    private ClassDescription classUnderQualification;

    /**
     * This RowData object is a child row of its parentRow.
     *
     * If our parentRow member data is null, then this RowData instance
     * is the "root" row for the whole tree.  I.e. it defines the
     * "Class Under Qualification" and the topmost operator.
     *
     * All rows except the root row have a non-null parentRow.
     * The class of the root row is stored in the root row's member
     * data "classUnderQualification".
     */
    private RowData parentRow;

    /**
     * This is the path to the childmost (i.e. rightmost) attribute that
     * this row is specifying.
     *
     * For example, if this row's starting class, (i.e. the rightmost
     * class of its parentRow), might be the class "Epoch", and then
     * attributePath could be a list containing the Attributes:
     *
     *      epochGroup  (is of type EpochGroup)
     *      source      (is of type Source)
     *      label       (is of type string)
     *
     * So the above would be specifying the "label" attribute of the
     * "source" attribute of the "epochGroup" of the "Epoch" class.
     */
    private List<Attribute> attributePath;

    /**
     * The operator the user selected for this attribute.
     * For example, Operator.EQUALS, Operator.LESS_THAN,
     * Operator.IS_NULL.
     *
     * Please note, this might be null if this row is a "compound" row
     * that ends with Any, All, or None.
     */
    private Operator attributeOperator;

    /**
     * If the attributeOperator is set to something AND it is not
     * set to "is null" or "is not null", then this value is the
     * value that the user entered as the desired value for the
     * attribute.
     *
     * This member data can be a:  Boolean, String, Integer, Short,
     * Double, Date, or null.
     * Note, as of September 2011, there is no Float (int32) value.
     *
     * If this is being used to hold the value of a row's Count, it
     * is of type Integer.  If the row does not have a value, this
     * should be set to null.
     *
     * For example, if the attributePath is:  epochGroup.source.label
     * then attributeValue might be the String object "Test 21".
     */
    private Object attributeValue;

    /**
     * If the user is specifying a "keyed" "My Property" or "Any Property"
     * attribute, the propName member data will be set to the "key"
     * that the user entered in the row.
     */
    private String propName;

    /**
     * This is the type of the "keyed" property.  E.g. int, float, date.
     */
    private Type propType;

    /**
     * If this row is a "compound" row, this will be set to
     * Any, All, or None.
     *
     * If this is set to Count, then the
     * attributeOperator member data will be set to some sort of
     * operator such as ==, >, or "is null".
     *
     * If this is null, then this row is an "attribute" row as
     * opposed to a "compound" row.
     */
    private CollectionOperator collectionOperator;

    /**
     * This is the second CollectionOperator that appears
     * in a row that ends with an Attribute that has
     * Cardinality.TO_MANY relationship to its parent class.
     *
     * TODO: Think of a better name for this value.
     */
    private CollectionOperator collectionOperator2;

    /**
     * This is the list of this row's direct children.
     */
    private List<RowData> childRows;

    /**
     * This is the list of listeners to changes in this RowData.
     */
    private EventListenerList rowDataListenerList;

    /**
     * This counter is used to avoid sending multiple change
     * events due to one method call making or causing multiple
     * changes to this RowData.  If it is non-zero, then
     * we are in the process of making changes to the RowData
     * and should NOT fire events.  If a caller makes a change
     * to a RowData object that will cascade a number of other
     * changes, we don't want to send change events for all
     * the "collateral changes" the call caused.  We only want
     * to send the BEFORE and AFTER messages associated with
     * the "original" RowData method call that was made.
     */
    private int changeLevel;


    /**
     * Create a RowData object that has no values set.
     */
    public RowData() {
        init();
    }


    /**
     * Initialize values for this RowData object.
     */
    private void init() {

        attributePath = new ArrayList<Attribute>();
        childRows = new ArrayList<RowData>();
        rowDataListenerList = new EventListenerList();
        parentRow = null;
        attributeOperator = null;
        attributeValue = null;
        propName = null;
        propType = null;
        collectionOperator = null;
        collectionOperator2 = null;
        changeLevel = 0;
    }


    /**
     * Create a RowData that is a "deep" copy of another RowData.
     * By "deep" copy, I mean a copy whose attributePath contains
     * copies of the other's Attributes.
     *
     * @param other The other RowData object of which we will be
     * a copy.  If this is null, then this method is equivalent
     * to calling the empty RowData constructor.
     */
    public RowData(RowData other) {

        init();

        if (other == null) {
            return;
        }

        this.classUnderQualification = other.classUnderQualification;
        this.parentRow = other.parentRow;
        this.attributeOperator = other.attributeOperator;
        this.attributeValue = other.attributeValue;
        this.propName = other.propName;
        this.propType = other.propType;
        this.collectionOperator = other.collectionOperator;
        this.collectionOperator2 = other.collectionOperator2;

        for (Attribute attribute : other.getAttributePath()) {
            this.addAttribute(new Attribute(attribute));
        }

        for (RowData childRow : other.getChildRows()) {
            this.addChildRow(new RowData(childRow));
        }
    }


    /**
     * Create a default rootRow that has no children.  Return the
     * rootRow we created AND set the RowData.rootRow static member
     * data to the rootRow we just created.
     */
    public static RowData createRootRow() {

        RowData rootRow = new RowData();
        rootRow.setClassUnderQualification(
            DataModel.getClassDescription("Epoch"));
        rootRow.setCollectionOperator(CollectionOperator.ANY);
        return(rootRow);
    }


    /**
     * Get the number of descendents from this RowData.  I.e. this is returns
     * the count of our direct children, PLUS all their children, and all
     * their children, and so on.
     * This is NOT the same thing as getting the number of this RowData's
     * "immediate" children.
     */
    public int getDescendentCount() {

        int count = getChildRows().size();
        for (RowData childRow : getChildRows()) {
            count += childRow.getDescendentCount();
        }
        return(count);
    }


    /**
     * This returns the RowData object that is at the specified "index".
     * This method is intended to be used to get the RowData object at
     * the specified index as far as a List GUI widget is concerned.
     * This RowData object is at index 0.  Its first child is at index 1.
     * If the first child has a child, then that child is at index 2.
     * If the first child does not have a child, then the second child
     * is at index 2.  (I.e. the first child's sibling.)
     * This sounds confusing, but a picture should clear up the confusion.
     *
     * A simple "picture" of RowData objects and their "indexes" as
     * far as a List widget is concerned, makes this more obvious:
     *
     *      RowData 0
     *          RowData 1
     *          RowData 2
     *          RowData 3
     *               RowData 4
     *          RowData 5
     *               RowData 6
     *               RowData 7
     *               RowData 8
     *          RowData 9
     *
     * So, just to repeat, "this" RowData object is returned if
     * you pass an index of 0.  This RowData's first child is at
     * index 1.
     */
    public RowData getChild(int index) {

        if (index == 0)
            return(this);

        for (RowData childRow : childRows) {

            index--;
            RowData rd = childRow.getChild(index);
            if (rd != null)
                return(rd);

            index -= childRow.getDescendentCount();

            if (index == 0)
                return(childRow);
        }

        return(null);
    }


    /**
     * Get the index of the passed in rowData.  The index is
     * relative to this RowData.  So the first RowData below
     * this RowData is at index 0.  The returned index is NOT
     * simply the index of the passed in rowData relative to
     * its parent RowData.
     *
     * Please read the comments for the getChild() method to
     * understand how the index is calculated.
     *
     * @return The index of the passed in rowData.  Returns -1
     * if the passed in rowData does not exist as a child of
     * this RowData.
     */
    public int getIndex(RowData rowData) {

        List<RowData> rows = getRows();
        int index = 0;
        for (RowData rd : rows) {
            if (rd == rowData)
                return(index);
            index++;
        }
        return(-1);
    }


    /**
     * Get this RowData and all its descendents as an ArrayList of
     * RowData objects.
     */
    public List<RowData> getRows() {

        int count = getDescendentCount()+1;

        List<RowData> rows = new ArrayList<RowData>();
        for (int index = 0; index < count; index++)
            rows.add(getChild(index));

        return(rows);
    }


    /**
     * Add a listener to this RowData.
     * The listener will be notified when anything in this RowData
     * or any of its descendents changes.  So, you only need to
     * listen to the root RowData of an expression tree to be notified
     * if anything anywhere in the tree changes.  The RowDataEvent
     * that the listener gets will have a reference to the actual
     * RowData object that was changed.
     */
    public void addRowDataListener(RowDataListener listener) {
        rowDataListenerList.add(RowDataListener.class, listener);
    }


    /**
     * Remove a listener from this RowData.
     */
    public void removeRowDataListener(RowDataListener listener) {
        rowDataListenerList.remove(RowDataListener.class, listener);
    }


    /**
     * Fire a RowDataEvent that originated with THIS RowData object.
     * See RowDataEvent for information about timing and changeType.
     */
    private void fireRowDataEvent(int timing, int changeType) {
        fireRowDataEvent(new RowDataEvent(null, this, this,
                                          timing, changeType));
    }


    /**
     * Fire a RowDataEvent that originated with THIS RowData object.
     * See RowDataEvent for information about childRowData,
     * timing and changeType.
     */
    private void fireRowDataEvent(int timing, int changeType,
                                  RowData childRowData) {
        fireRowDataEvent(new RowDataEvent(childRowData, this, this,
                                          timing, changeType));
    }


    /**
     * Notify all listeners that have registered interest for
     * notification on the passed in RowDataEvent.
     *
     * Please see the comments for the changeLevel member data
     * for more information about its purpose.
     */
    private void fireRowDataEvent(RowDataEvent rowDataEvent) {

        if (rowDataEvent.getTiming() == RowDataEvent.TIMING_AFTER)
            changeLevel--;

        if (changeLevel < 0) {
            /**
             * This might happen if someone changes the code
             * and doesn't have properly matched TIMING_BEFORE
             * and TIMING_AFTER calls to fireRowDataEvent.
             */
            System.err.println("ERROR:  changeLevel < 0\n"+
                "If this happens, there is a bug in the code.\n"+
                "Setting changeLevel to 0 as a temporay fix.");
            changeLevel = 0;
        }

        /**
         * If we are in the process of making changes,
         * which is shown by, (changeLevel > 0), don't
         * send an event.  After we are done making changes,
         * and the assorted "nested" calls to fireRowDataEvent()
         * have decremented changeLevel back to 0, a call
         * to fireRowDataEvent() will cause an event to be
         * sent.
         *
         * Please note, changeLevel should never be negative.
         */
        if (changeLevel == 0) {
            //System.out.println("Send RowDataEvent("+timing+", "+changeType+
            //    ") for this: "+this.getRowString());

            RowDataListener listenerList[] =
                rowDataListenerList.getListeners(RowDataListener.class);
            for (RowDataListener listener : listenerList) {

                /**
                 * Call the listener, but do it in a try/catch block
                 * just in case the listener throws an exception.
                 * We don't want to let our changeLevel counter
                 * get out of whack.
                 */
                try {
                    listener.rowDataChanged(rowDataEvent);
                }
                catch (Exception e) {
                    System.err.println("RowDataListener threw an exception:");
                    e.printStackTrace();
                }
            }
        }

        if (rowDataEvent.getTiming() == RowDataEvent.TIMING_BEFORE)
            changeLevel++;
    }


    /**
     * This method is called when one of our direct children RowData
     * objects is changed.  Please note, because each row listens
     * to its children, the message eventually gets passed up to
     * the root row that the GUI is listening to.
     */
    @Override
    public void rowDataChanged(RowDataEvent event) {

        /**
         * Create a new RowDataEvent that keeps the "originalRowData"
         * and "childRowData" values, but updates the "rowData" value
         * to be this.
         */
        RowDataEvent newEvent = new RowDataEvent(event.getChildRowData(),
                                                 event.getOriginalRowData(),
                                                 this,
                                                 event.getTiming(),
                                                 event.getChangeType());
        fireRowDataEvent(newEvent);
    }


    /**
     * Remove the specified child RowData object from this RowData object's
     * list of direct children.
     *
     * Please note, removing row "A" from its parent row "B" is NOT considered
     * a change to row "A", (as far as notifying listeners is concerned),
     * but is considered a change to the parent row "B".
     */
    public void removeChildRow(RowData childRow) {

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_CHILD_DELETE,
                         childRow);
        childRows.remove(childRow);
        childRow.removeRowDataListener(this);
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_CHILD_DELETE,
                         childRow);
    }


    /**
     * Remove this RowData object from its parent's list of direct children.
     * This is functionally equivalent to calling:
     *
     *      getParentRow().removeChildRow(this);
     *
     * Please note, removing row "A" from its parent row "B" is NOT considered
     * a change to row "A", (as far as notifying listeners is concerned),
     * but is considered a change to the parent row "B".
     */
    public void removeFromParent() {

        if ((getParentRow() == null) ||
            (getParentRow().getChildRows() == null)) {
            System.err.println("ERROR:  removeFromParent called on a row\n"+
                "without a parent, or whose parent doesn't have children.\n"+
                "This is a coding error of some sort.");
            return;
        }

        /**
         * Get our parent row and tell it to remove us.
         *
         * Note that we do NOT call fireRowDataEvent() before or
         * after we call removeChildRow(this).  The call to
         * removeChildRow(this) does that.
         */
        getParentRow().removeChildRow(this);
    }


    /**
     * Create a child row for this row that is of type Compound Row.
     */
    public void createCompoundRow() {

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_CHILD_ADD);
        RowData compoundRow = new RowData();
        compoundRow.setParentRow(this);
        compoundRow.setCollectionOperator(CollectionOperator.ANY);
        addChildRow(compoundRow);
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_CHILD_ADD);
    }


    /**
     * Create a child row for this row that is of type Attribute Row.
     */
    public void createAttributeRow() {

        ClassDescription classDescription = null;

        /**
         * Figure out what the ClassDescription should be
         * for the Attribute Row we will create.  I.e. this is
         * the class that will be used for the leftmost
         * attribute comboBox in this row.
         */
        if ((this != getRootRow()) && isSimpleCompoundRow()) {
            /**
             * This is a "simple" Compound Row that only has
             * a collection operator in it.  E.g. Any/All/None.
             * Look "upwards" in this row's hierarchy to figure
             * out what class it should be.
             */
            RowData parent = getParentRow();
            while (classDescription == null) {
                Attribute attribute = parent.getChildmostAttribute();
                if (attribute != null)
                    classDescription = attribute.getClassDescription();
                parent = parent.getParentRow();
                if (parent == null)
                    classDescription = getClassUnderQualification();
            }
        }
        else {
            /**
             * This is a row whose childmost attribute,
             * (rightmost comboBox), is a reference to a class.
             * So, whatever that class is will be the Class Description
             * for the child row we will create.
             */
            Attribute attribute = getChildmostAttribute();
            if (this == getRootRow())
                classDescription = getClassUnderQualification();
            else
                classDescription = attribute.getClassDescription();
        }

        if (classDescription == null) {
            System.err.println("ERROR: In createAttributeRow "+
                "classDescription == null.  This should never happen.");
            return;
        }

        /*
        ArrayList<Attribute> attributes = classDescription.getAllAttributes();
        if (attributes.isEmpty()) {
            System.err.println("ERROR: In createAttributeRow "+
                "attributes.isEmpty == true.  This should never happen.");
            return;
        }
        */

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_CHILD_ADD);
        RowData attributeRow = new RowData();
        attributeRow.addAttribute(Attribute.SELECT_ATTRIBUTE);
        addChildRow(attributeRow);
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_CHILD_ADD);
    }


    /**
     * Returns true if this row is a Compound Row, (simple or not).
     * I.e. this means the row ends with a "compound" Collection Operator
     * Any, All, or None.  A row that ends with Count is NOT a Compound Row.
     *
     * For example, a row like one of these:
     *
     *      epochGroups.epochs Any of the following
     *      resources None of the following
     *      Any of the following
     *      None of the following
     */
    public boolean isCompoundRow() {

        if (collectionOperator == null)
            return(false);

        return(collectionOperator.isCompoundOperator());
    }


    /**
     * Returns true if this is a "simple" Compound Row.
     * I.e. a row that contains ONLY a Collection Operator comboBox.
     *
     * For example, a row like one of these:
     *
     *      Any of the following
     *      All of the following
     *      None of the following
     */
    public boolean isSimpleCompoundRow() {

        if (isCompoundRow() == false)
            return(false);

        if ((attributePath == null) || attributePath.isEmpty())
            return(true);

        return(false);
    }


    /**
     * Set the topmost class that is being qualified.  I.e. this is
     * the class selected in the first row in the GUI.
     */
    public void setClassUnderQualification(
        ClassDescription classUnderQualification) {

        if (this != getRootRow()) {
            System.err.println(
                "WARNING:  This RowData object is not the \"root\"\n"+
                "RowData object.  Are you confused?\n"+
                "Setting the Class Under Qualification really only makes\n"+
                "sense on the root RowData object.\n"+
                "I will assume that is what you meant to do, and do that.");
        }

        if (getClassUnderQualification() == classUnderQualification) {
            return;
        }

        if ((getClassUnderQualification() != null) &&
            getClassUnderQualification().equals(classUnderQualification)) {
            return;
        }

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE, RowDataEvent.TYPE_CUQ);
        getRootRow().classUnderQualification = classUnderQualification;

        if (!getRootRow().getChildRows().isEmpty()) {
            //System.out.println("INFO:  Clearing all childRows.");
            getRootRow().getChildRows().clear();
        }
        fireRowDataEvent(RowDataEvent.TIMING_AFTER, RowDataEvent.TYPE_CUQ);
    }


    /**
     * Get the topmost class that is being qualified.  I.e. this is
     * the class selected in the first row in the GUI.
     */
    public ClassDescription getClassUnderQualification() {
        return(getRootRow().classUnderQualification);
    }


    /**
     * Get the current rootRow.
     */
    public RowData getRootRow() {

        RowData rootRow = this;
        while (rootRow.getParentRow() != null)
            rootRow = rootRow.getParentRow();
        return(rootRow);
    }


    /**
     * Returns true if this RowData object is the root of the tree.
     * I.e. it returns true if this is the first row that contains
     * the Class Under Qualification selection.
     */
    public boolean isRootRow() {
        return(this == getRootRow());
    }


    /**
     * Set the parent of this row.
     */
    public void setParentRow(RowData parentRow) {

        if (this.parentRow == parentRow)
            return;

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE, RowDataEvent.TYPE_PARENT);
        this.parentRow = parentRow;
        fireRowDataEvent(RowDataEvent.TIMING_AFTER, RowDataEvent.TYPE_PARENT);
    }


    /**
     * Get the RowData object that is the parent of this RowData object.
     * If this is the root row, this will be null.
     */
    private RowData getParentRow() {
        return(parentRow);
    }


    /**
     * Set the first, (possibly only), collection operator for this row.
     */
    public void setCollectionOperator(CollectionOperator collectionOperator) {

        if (this.collectionOperator == collectionOperator)
            return;

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_COLLECTION_OPERATOR);
        this.collectionOperator = collectionOperator;

        /**
         * Blank out the attribute operator if the user
         * is selecting Any, All, or None.
         *
         * Set the attribute operator and attribute value
         * to legal values if the user is selecting Count.
         */
        if ((collectionOperator != null) &&
            collectionOperator.isCompoundOperator()) {

            setAttributeOperator(null);
            setAttributeValue(null);

            Attribute childmost = getChildmostAttribute();
            if ((childmost != null) && (childmost.getCardinality() ==
                 Cardinality.TO_MANY) &&
                (childmost.getType() != Type.PER_USER_PARAMETERS_MAP)) {

                /**
                 * Set collectionOperator2 to a default value if
                 * it is currently null.
                 */
                if (getCollectionOperator2() == null) {
                    setCollectionOperator2(DEFAULT_COLLECTION_OPERATOR2);
                }
            }
            else {
                setCollectionOperator2(null);
            }
        }
        else if ((collectionOperator != null) &&
                 (collectionOperator == CollectionOperator.COUNT)) {
            /**
             * collectionOperator is being set to COUNT.
             */
            setAttributeOperator(Operator.OPERATORS_ARITHMATIC[0]);
            setAttributeValue(new Integer(0));

            setCollectionOperator2(null);

            /**
             * Empty this row's list of children.
             * (Only rows with collection operator Any/All/None have
             * child rows.)
             */
            clearChildRows();
        }
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_COLLECTION_OPERATOR);
    }


    /**
     * Get the first, (possibly only), collection operator for this row.
     */
    public CollectionOperator getCollectionOperator() {
        return(collectionOperator);
    }


    /**
     * Set the second collection operator for this row.
     * Not all rows that have a collection operator will have two
     * collection operators.
     *
     * For example, a row whose childmost/rightmost Attribute
     * has a to-many relationship will have a second collection
     * operator that the user needs to set.  Below is an example
     * of the Epoch class's response Attribute:
     *
     *      Epoch | None
     *        Epoch | responses None have Any
     *          Response | uuid == "xyz"
     *
     * Below is an example of the Epoch class's mykeywords Attribute:
     *
     *      Epoch | All
     *        Epoch | My Keywords None have Any
     *          KeywordTag | uuid == "xyz"
     *
     */
    public void setCollectionOperator2(CollectionOperator collectionOperator2) {

        if (this.collectionOperator2 == collectionOperator2)
            return;

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_COLLECTION_OPERATOR);

        this.collectionOperator2 = collectionOperator2;

        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_COLLECTION_OPERATOR);
    }


    /**
     * Get the second collection operator for this row.
     */
    public CollectionOperator getCollectionOperator2() {
        return(collectionOperator2);
    }


    /**
     * Set the attributeOperator of this row.
     *
     * @param attributeOperator A value such as, but not limited to:
     * Operator.EQUALS, Operator.LESS_THAN, Operator.IS_NULL, Operator.IS_TRUE.
     */
    public void setAttributeOperator(Operator attributeOperator) {

        //System.out.println("Enter RowData.setAttributeOperator("+
        //                   attributeOperator+")");

        if (this.attributeOperator == attributeOperator)
            return;

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_ATTRIBUTE_OPERATOR);
        this.attributeOperator = attributeOperator;

        /**
         * Blank out the attributeValue if the attributeOperator
         * is being set to the operator "is true", "is false", "is null"
         * "is not null".  (If the operator is set to one of those
         * values, the attributeValue member data is meaningless.)
         */
        if ((attributeOperator == Operator.IS_TRUE) ||
            (attributeOperator == Operator.IS_FALSE) ||
            (attributeOperator == Operator.IS_NULL) ||
            (attributeOperator == Operator.IS_NOT_NULL)) {
            setAttributeValue(null);
        }

        Attribute childmost = getChildmostAttribute();
        //System.out.println("childmost = "+childmost);

        /**
         * If the attributeOperator is being set to something
         * other than Operator.IS_NULL and the last attribute
         * is Attribute.IS_NULL, remove that last attribute
         * because it is no longer appropriate.
         *
         * Do the same for IS_NOT_NULL.
         */

        if ((attributeOperator != Operator.IS_NULL) &&
            Attribute.IS_NULL.equals(childmost)) {
            attributePath.remove(attributePath.size()-1);
        }
        if ((attributeOperator != Operator.IS_NOT_NULL) &&
            Attribute.IS_NOT_NULL.equals(childmost)) {
            attributePath.remove(attributePath.size()-1);
        }

        if ((childmost != null) &&
            (childmost.getType() != Type.PARAMETERS_MAP) &&
            (childmost.getType() != Type.PER_USER_PARAMETERS_MAP)) {
            /**
             * Try to help the engineer who is creating a RowData
             * object by adding the proper Attribute.IS_NULL or
             * Attribute.IS_NOT_NULL to the end of this RowData's
             * attributePath if it is not already there.
             */
            if ((attributeOperator == Operator.IS_NULL) &&
                !Attribute.IS_NULL.equals(childmost)) {
                addAttribute(Attribute.IS_NULL);
            }
            else if ((attributeOperator == Operator.IS_NOT_NULL) &&
                     !Attribute.IS_NOT_NULL.equals(childmost)) {
                addAttribute(Attribute.IS_NOT_NULL);
            }
        }

        /**
         * If we are a time value, and the operator is being set
         * to something other than "is null" or "is not null",
         * make sure the attributeValue is a Date.
         */
        if ((getChildmostAttribute() != null) &&
                (Type.DATE_TIME.equals(getChildmostAttribute().getType()) ||
                        Type.DATE_TIME.equals(getPropType())) &&
                (attributeOperator != Operator.IS_NULL) &&
                (attributeOperator != Operator.IS_NOT_NULL) &&
                ((getAttributeValue() == null) ||
                        !(getAttributeValue() instanceof DateTime))) {
            setAttributeValue(new DateTime().withZone(DateTimeZone.UTC));
        }

        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_ATTRIBUTE_OPERATOR);
    }


    /**
     * Get the attributeOperator of this row.
     *
     * @return An Operator object such as, but not limited to:
     * Operator.EQUALS, Operator.LESS_THAN, Operator.IS_NULL,
     * Operator.IS_TRUE.
     */
    public Operator getAttributeOperator() {
        return(attributeOperator);
    }


    /**
     * Set the Attribute at the passed in index.
     * Note, setting a Attribute that is to the left of the row's
     * current rightmost Attribute will "trim" off Attributes
     * to the right of the Attribute being set.
     */
    public void setAttribute(int index, Attribute attribute) {

        /**
         * Check if the new value is different from the current value.
         */
        if (index < getAttributeCount()) {
            Attribute currentValue = getAttribute(index);
            if (currentValue == attribute) {
                return;
            }
            if ((currentValue != null) && currentValue.equals(attribute)) {
                return;
            }
        }

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_ATTRIBUTE);

        /**
         * Possibly trim the attributePath.
         * I.e. remove Attributes that are "after" the one being changed.
         *
         * TODO: Perhaps change the code so if we actually need to trim
         * the attributePath, we send a TYPE_ATTRIBUTE_PATH event
         * instead of a TYPE_ATTRIBUTE event.
         */
        trimAttributePath(index);

        /**
         * Make sure the attributePath is long enough.
         */
        while (index >= getAttributeCount())
            addAttribute(null);

        getAttributePath().set(index, attribute);

        if (index == (getAttributeCount()-1)) {
            /**
             * This is the rightmost attribute in the attributePath.
             * Make sure the operator and other values are
             * appropriate for whatever type of attribute this is.
             */
            setRowDataValuesAppropriately();
        }

        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_ATTRIBUTE);
    }


    /**
     * This method is called after a user has selected an Attribute
     * in the rightmost attribute comboBox.
     *
     * Make sure the operator and other values are
     * appropriate for whatever the currently selected
     * childmost/rightmost attribute is.
     *
     * TODO: This code could be more clever and try to use
     * whatever the current attributeValue is even if the
     * type changes.  For example, if the user changes the
     * type from int to string, we could set the attributeValue
     * to the string version of the int.  If the user changes
     * the type from string to int, we could try to convert
     * whatever the string is into an int using Integer.parseInt().
     * If parseInt() throws an exception, we simply set the
     * attributeValue to 0 like we already do.
     * This sort of behavior might save a user a little bit
     * of effort and make the GUI a little more friendly.
     */
    private void setRowDataValuesAppropriately() {

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_UNDEFINED);
        Attribute childmost = getChildmostAttribute();

        /**
         * If the row is not a per-user parameters map or a parameters map,
         * the propType and propName values have no meaning for this row.
         * So, set them to null.
         */
        if ((childmost.getType() != Type.PER_USER_PARAMETERS_MAP) &&
            (childmost.getType() != Type.PARAMETERS_MAP)) {
            setPropType(null);
            setPropName(null);
        }

        /**
         * Set the collection operator (Count/Any/All/None) to
         * be null if the selectedAttribute does not use
         * a collection operator, or set it to Count if it does
         * use a collection operator.
         */
        if ((childmost.getCardinality() != Cardinality.TO_MANY) ||
            (childmost.getType() == Type.PER_USER_PARAMETERS_MAP)) {
            setCollectionOperator(null);
        }
        else {
            setCollectionOperator(CollectionOperator.COUNT);
        }

        if ((getCollectionOperator() == CollectionOperator.COUNT) ||
            (childmost.getCardinality() != Cardinality.TO_MANY) ||
            (childmost.getType() == Type.PER_USER_PARAMETERS_MAP)) {
            /**
             * A row of this type does not display a second collection
             * operator.
             */
            setCollectionOperator2(null);
        }
        else if (getCollectionOperator2() == null) {
            /**
             * A row of this type should have a second collection operator,
             * but this row's second collection operator is currently set
             * to null.  Set it to some non-null default value.
             */
            setCollectionOperator2(DEFAULT_COLLECTION_OPERATOR2);
        }

        /**
         * If the user set the value of a primitive type,
         * that means we need to be sure the operator is
         * initialized to an appropriate value for that
         * type.  E.g. "==" for an int or string, "is true" for
         * a boolean.
         *
         * Also make sure the attributeValue contains a value of
         * the appropriate type.
         */
        if (childmost.equals(Attribute.SELECT_ATTRIBUTE)) {
            setCollectionOperator(null);
            setCollectionOperator2(null);
            setAttributeOperator(null);
            setAttributeValue(null);
        }
        else if (childmost.equals(Attribute.IS_NULL) ||
                 childmost.equals(Attribute.IS_NOT_NULL)) {

            /**
             * The selectedAttribute is one of our special
             * Attribute.IS_NULL or Attribute.IS_NOT_NULL values,
             * so set the attributeOperator to "is null" or
             * "is not null".
             */
            setAttributeOperator(Operator.fromString(
                childmost.getDisplayName()));
        }
        else if ((childmost.getType() == Type.PER_USER_PARAMETERS_MAP) ||
                 (childmost.getType() == Type.PARAMETERS_MAP)) {

            setPropType(Type.INT_32);
            setAttributeOperator(Operator.OPERATORS_ARITHMATIC[0]);
            setPropName(null);
            setAttributeValue(new Integer(0));
        }
        else if (childmost.isPrimitive()) {

            /**
             * The user set the value of a primitive type,
             * so we need to be sure the operator is
             * initialized to an appropriate value for that
             * type.  E.g. "==" for an int or string, "is true" for
             * a boolean.
             *
             * Also make sure the attributeValue contains a value of
             * the appropriate type.
             */
            possiblyAdjustOperatorAndValue(childmost.getType());
        }

        if (!childmost.isPrimitive() && !childmost.isSpecial() &&
            (childmost.getType() != Type.PER_USER_PARAMETERS_MAP) &&
            (childmost.getType() != Type.PARAMETERS_MAP) &&
            (getCollectionOperator() == null)) {

            /**
             * The rightmost Attribute is a class, as opposed
             * to a "primitive" type such as int, float, string,
             * so the GUI will need to display another comboBox
             * to its right that the user can use to choose an Attribute of
             * that class or choose a special item such as "is null",
             * "is not null", "Any Property", "My Property".
             * We force the GUI to do this by adding the special
             * Attribute.SELECT_ATTRIBUTE value to the end of
             * our attributePath.
             */
            addAttribute(Attribute.SELECT_ATTRIBUTE);
        }

        /**
         * Make sure that if we have child rows they
         * are appropriate for the childmost attribute
         * of this row.  If they aren't, remove them.
         */
        if ((childmost.getClassDescription() == null) ||
            (getCollectionOperator() == null) ||
            !getCollectionOperator().isCompoundOperator()) {

            /**
             * This row cannot have children, so remove any
             * children we currently have.
             */
            clearChildRows();
        }
        else {
            /**
             * Make sure the child rows are of the same
             * class as the parent row.  In the current
             * implementation, all child rows are the
             * same class, so this code simply tests the
             * first child row and then removes all
             * child rows if the first one is of the wrong
             * type.  If, in the future, a parent row
             * can have a heterogeneous set of child rows,
             * the code below will have to be changed
             * to a for-loop that looks at all the child
             * rows.
             */
            if (getChildRows().size() > 0) {

                RowData firstChildRow = getChildRows().get(0);
                Attribute attribute = null;
                if (firstChildRow.getAttributePath().size() > 0) {
                    attribute = firstChildRow.getAttribute(0);
                    if (!childmost.getClassDescription().
                        containsAttribute(attribute)) {
                        clearChildRows();
                    }
                }
            }
        }

        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_UNDEFINED);
    }


    /**
     * Remove Attributes from the attributePath that are
     * after the passed in index.  So, the index you
     * pass in is the index of the last attribute you
     * want in the list.  Attributes after that index are
     * removed from the list.  For example, if you
     * pass 3 for the lastAttributeIndex parameter,
     * this method will remove, (if they exist), the
     * Attributes at index 4, 5, 6...
     */
    public void trimAttributePath(int lastAttributeIndex) {

        /**
         * If the attributePath isn't even as long as the
         * caller thinks it is, just return.
         */
        if (getAttributeCount() <= (lastAttributeIndex+1))
            return;

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_ATTRIBUTE_PATH);
        getAttributePath().subList(lastAttributeIndex+1,
                                   getAttributeCount()).clear();
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_ATTRIBUTE_PATH);
    }


    /**
     * Returns the number of Attribute objects in the attributePath.
     * Please note, a "special" Attribute such as Attribute.IS_NULL
     * is part of this count.
     */
    public int getAttributeCount() {
        return(getAttributePath().size());
    }


    /**
     * Add an Attribute to the end of this RowData's attributePath.
     *
     * In addition to simply adding the Attribute to the attributePath,
     * this method also makes any other necessary changes to the
     * other member data associated with this RowData object.
     * For example, if the user adds the Attribute.IS_NULL to the
     * attributePath, this method also sets the attributeOperator
     * to "is null".
     */
    public void addAttribute(Attribute attribute) {

        //System.out.println("Enter RowData.addAttribute("+attribute+")");

        if (attribute == null) {
            String s = "Bad call to RowData.addAttribute() with a "+
                "null attribute parameter.";
            throw(new IllegalArgumentException(s));
        }

        if (attributePath == null)
            attributePath = new ArrayList<Attribute>();

        Attribute childmost = getChildmostAttribute();
        if (childmost != null) {
            if (childmost.isSpecial() && attribute.isSpecial()) {
                String s = "Bad call to RowData.addAttribute() with a "+
                    "special Attribute: "+attribute+".  The attributePath "+
                    "already ends with the special Attribute: "+childmost+" "+
                    "so you can't add another one.  Note that calls to "+
                    "setAttributeOperator(Operator.IS_NULL), for example, "+
                    "can add the Attribute.IS_NULL automatically if it is "+
                    "not already at the end of the attributePath.";
                throw(new IllegalArgumentException(s));
                /**
                 * Please note, we could just "fix" the above
                 * described problem by doing this:
                 *
                 *      attributePath.remove(attributePath.size()-1);
                 *
                 * I.e. just remove the Attribute that is no longer
                 * valid.  That might be better.
                 */
            }
        }

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_ATTRIBUTE_PATH);
        attributePath.add(attribute);

        if (attribute.equals(Attribute.IS_NULL) &&
            (attributeOperator != Operator.IS_NULL)) {
            setAttributeOperator(Operator.IS_NULL);
        }
        else if (attribute.equals(Attribute.IS_NOT_NULL) &&
                 (attributeOperator != Operator.IS_NOT_NULL)) {
            setAttributeOperator(Operator.IS_NOT_NULL);
        }
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_ATTRIBUTE_PATH);
    }


    /**
     * This returns a COPY of the Attribute at the specified index.
     * So, any changes you make to it will have no effect on this
     * RowData's attributePath.
     */
    public Attribute getAttribute(int index) {
        return(new Attribute(getAttributePath().get(index)));
    }


    /**
     * This returns this RowData's ArrayList of Attribute objects.
     * This method creates an empty ArrayList if the attributePath
     * is currently null.
     *
     * Please note, it is returning this RowData's member data.
     * It is NOT returning a copy, so if you make changes to
     * it, you are affecting this RowData object.
     * Instead of calling this method, you should probably
     * be calling the addAttribute() and getAttribute() methods.
     */
    public List<Attribute> getAttributePath() {

        if (attributePath == null)
            attributePath = new ArrayList<Attribute>();

        return(attributePath);
    }


    /**
     * This is simply a convenience method that calls the
     * other version of this method with the debugVersion
     * flag set to false.
     */
    public String getAttributePathString() {
        return(getAttributePathString(false));
    }


    /**
     * Get a string that represents this RowData's attributePath
     * that can be used in a query string, OR as a string useful
     * for testing/debugging.
     *
     * @param debugVersion Set this to true if you want a
     * "debug" version of the attribute path that has the
     * isMine flag shown in it.  Set this to false if you
     * want a version of this string that can be used for
     * a query.  The returned value is in the format that
     * the Objectivity software would like.
     */
    public String getAttributePathString(boolean debugVersion) {

        String string = "";


        boolean first = true;
        for (Attribute attribute : attributePath) {

            if (attribute != null) {
                if ((!attribute.equals(Attribute.SELECT_ATTRIBUTE) &&
                     !attribute.equals(Attribute.IS_NULL) &&
                     !attribute.equals(Attribute.IS_NOT_NULL)) ||
                    (debugVersion == true)) {
                    /**
                     * Put a dot between each attribute on the path.
                     */
                    if (first) {
                        string += " ";
                        first = false;
                    }
                    else {
                        string += ".";
                    }

                    if (debugVersion)
                        string += attribute.getDisplayName();
                    else
                        string += attribute.getQueryName();

                }
                else {
                    /**
                     * We don't display a string for this type of attribute.
                     */
                }
            }
            else {
                /**
                 * If this happens, the code has a bug.
                 * Perhaps throw an IllegalArgumentException?
                 */
                System.err.println("ERROR:  null Attribute in attributePath.");
                string += "ERROR: attribute == null";
            }
        }

        return(string);
    }


    /**
     * Set the attributeValue for this row.
     *
     * @param attributeValue An object of the proper type that will
     * be the attributeValue.  Note, Boolean values are not legal.
     * Boolean values are handled by the attributeOperator being
     * set to Operator.IS_TRUE or Operator.IS_FALSE.
     */
    public void setAttributeValue(Object attributeValue) {

        //System.out.println("Enter setAttributeValue("+attributeValue+")");

        if (attributeValue instanceof Boolean) {
            String s = "Boolean values are handled with the operator.  "+
                "Do NOT set an attributeValue.  Instead set the "+
                "attributeOperator to be Operator.IS_TRUE or "+
                "Operator.IS_FALSE";
            (new Exception(s)).printStackTrace();
            return;
        }

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_ATTRIBUTE_VALUE);
        this.attributeValue = attributeValue;
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_ATTRIBUTE_VALUE);
    }


    private transient Logger logger = Logger.getLogger(getClass().getCanonicalName());
    /**
     * Set the value of attributeValue using a String.
     * The GUI currently has widgets that let the user enter
     * an Integer or Date value, so the only thing this method
     * really needs to handle is converting a user entered String
     * into a floating point value.
     *
     * @param stringValue A String such as "43.2", or null.
     */
    public void setAttributeValueUsingString(String stringValue) {

        Object value = null;

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_UNDEFINED);
        Attribute attribute = getChildmostAttribute();
        if (attribute == null) {
            setAttributeValue(null);
        }
        else {
            /**
             * Strip off leading/trailing whitespace.
             */
            if (stringValue != null)
                stringValue = stringValue.trim();

            /**
             * The call to convertStringToAttributeValue might try to
             * convert the string to a number, (if the type is a number),
             * so guard against exceptions during that procedure call.
             */
            try {
                value = convertStringToAttributeValue(stringValue,
                                                      attribute.getType());
                setAttributeValue(value);
            }
            catch (NumberFormatException e) {
                //System.out.println("Field does not contain a legal number.");
                /**
                 * The user entered a string which cannot be
                 * converted into a number, so set the attributeValue
                 * to Double.NaN (Not A Number).
                 * This allows our containsLegalValue() method to
                 * see that we do not currently contain a legal value.
                 */
                if ((attribute.getType() == Type.FLOAT_64) ||
                    (propType == Type.FLOAT_64)) {
                    setAttributeValue(Double.NaN);
                }
            }
            catch (Exception e) {
                logger.error("Field does not contain a legal value.");
                e.printStackTrace();
            }
        }
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_UNDEFINED);
    }


    /**
     * Convert the passed in stringValue to an Object of the passed in type.
     */
    private Object convertStringToAttributeValue(String stringValue,
                                                 Type type) {

        Object value;

        if (type == null) {
            (new Exception("test")).printStackTrace();
            return(null);
        }

        switch (type) {
            case BOOLEAN:
                value = new Boolean(stringValue);
            break;
            case UTF_8_STRING:
                value = stringValue;
            break;
            case INT_16:
                value = new Short(stringValue);
            break;
            case INT_32:
                value = new Integer(stringValue);
            break;
            case FLOAT_64:
                value = new Double(stringValue);
            break;
            case DATE_TIME:
                /**
                 * We don't really need to bother with this
                 * one because it won't happen.
                 * But if this does become necessary, using
                 * SimpleDateFormat.parse() would probably be
                 * involved.  For now, just ignore the passed
                 * in value and set it to null.
                 */
                value = null;
            break;
            case PER_USER_OR_CUSTOM_REFERENCE_OPERATOR:
            case PARAMETERS_MAP:
            case PER_USER_PARAMETERS_MAP:
                value = convertStringToAttributeValue(stringValue,
                                                      getPropType());
            break;
            default:
                if (stringValue.isEmpty()) {
                    value = stringValue;
                }
                else {
                    System.err.println("ERROR: Unhandled attribute type.");
                    System.err.println("stringValue = "+stringValue);
                    System.err.println("type = "+type);
                    value = new String("ERROR");
                }
        }
        return(value);
    }


    /**
     * Get the attributeValue.  Note, an Object is returned.  The caller
     * needs to look at other information about the RowData to know
     * what type of object it is.  For example, use getPropType() if
     * the row is a parameters map type of value.
     */
    public Object getAttributeValue() {
        return(attributeValue);
    }


    /**
     * Set the property name, "key", for the user defined Attribute
     * this row describes.
     */
    public void setPropName(String propName) {

        if (this.propName == propName)
            return;

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_PROP_NAME);
        this.propName = propName;
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_PROP_NAME);
    }


    /**
     * Get the property name, "key", for the user defined Attribute
     * this row describes.
     */
    public String getPropName() {
        return(propName);
    }


    /**
     * Set the property type for the user defined Attribute
     * this row describes.  E.g. Type.INT_16, Type.DATE_TIME.
     */
    public void setPropType(Type propType) {

        if (this.propType == propType)
            return;

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_PROP_TYPE);
        this.propType = propType;
        possiblyAdjustOperatorAndValue(propType);
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_PROP_TYPE);
    }


    /**
     * Get the property type for the user defined Attribute
     * this row describes.  E.g. Type.INT_16, Type.DATE_TIME.
     */
    public Type getPropType() {
        return(propType);
    }


    /**
     * This method will possibly adjust the attributeOperator
     * and/or attributeValue of this row if the current
     * attributeOperator is not a legal operator for the
     * passed in type, or the attributeValue is not legal for
     * the passed in type.
     *
     * This method is called, for example, when the user changes
     * the property type of a "keyed" property from string to int.
     * If the current operator is "==", then we can leave it alone
     * because the "==" operator is legal for an int also.
     * But, if the current operator is "~~=", we cannot leave that
     * operator value in place because it is not legal for an int.
     */
    private void possiblyAdjustOperatorAndValue(Type type) {

        if (type == null) {
            return;
        }

        Operator attributeOperator = getAttributeOperator();
        switch (type) {
            case BOOLEAN:
                if (!Operator.isOperatorBoolean(attributeOperator)) {
                    /**
                     * Operator is not currently a legal
                     * boolean operator, so set it to
                     * a boolean operator.
                     */
                    setAttributeOperator(Operator.OPERATORS_BOOLEAN[0]);
                }
                /**
                 * Booleans are handled using the Operator.IS_TRUE
                 * and Operator.IS_FALSE.  So no value is set.
                 */
                setAttributeValue(null);
            break;
            case UTF_8_STRING:
                if (!Operator.isOperatorString(attributeOperator)) {
                    /**
                     * Operator is not currently a legal
                     * string operator, so set it to
                     * a string operator.
                     */
                    setAttributeOperator(Operator.OPERATORS_STRING[0]);
                }
                if (!(getAttributeValue() instanceof String))
                    setAttributeValue(new String(""));
            break;
            case INT_16:
                if (!Operator.isOperatorArithmatic(attributeOperator)) {
                    /**
                     * Operator is not currently a legal
                     * numeric operator, so set it to
                     * a numeric operator.
                     */
                    setAttributeOperator(Operator.OPERATORS_ARITHMATIC[0]);
                }
                if (!(getAttributeValue() instanceof Short))
                    setAttributeValue(new Short((short)0));
            break;
            case INT_32:
                if (!Operator.isOperatorArithmatic(attributeOperator)) {
                    setAttributeOperator(Operator.OPERATORS_ARITHMATIC[0]);
                }
                if (!(getAttributeValue() instanceof Integer))
                    setAttributeValue(new Integer(0));
            break;
            case FLOAT_64:
                if (!Operator.isOperatorArithmatic(attributeOperator)) {
                    setAttributeOperator(Operator.OPERATORS_ARITHMATIC[0]);
                }
                if (!(getAttributeValue() instanceof Double))
                    setAttributeValue(new Double((double)0.0));
            break;
            case DATE_TIME:
                if (!Operator.isOperatorDateTime(attributeOperator)) {
                    setAttributeOperator(Operator.OPERATORS_DATE_TIME[0]);
                }
                if (!(getAttributeValue() instanceof LocalDateTime))
                    setAttributeValue(new LocalDateTime(DateTimeZone.UTC));
            break;
            default:
                System.err.println("ERROR:  Unhandled type.\n"+
                    "In RowData.possiblyAdjustOperatorAndValue().\n"+
                    "type = "+type);
        }
    }


    /**
     * This returns this RowData's list of child RowDatas.
     * It does NOT return a copy, so don't mess with it.
     */
    public List<RowData> getChildRows() {

        if (childRows == null)
            childRows = new ArrayList<RowData>();
        return(childRows);
    }


    /**
     * Add a child RowData object to this RowData's list of children.
     * Note that this method automatically sets the childRow's parent
     * to be this RowData.
     */
    public void addChildRow(RowData childRow) {

        fireRowDataEvent(RowDataEvent.TIMING_BEFORE,
                         RowDataEvent.TYPE_CHILD_ADD,
                         childRow);
        childRow.setParentRow(this);
        childRows.add(childRow);

        childRow.addRowDataListener(this);
        fireRowDataEvent(RowDataEvent.TIMING_AFTER,
                         RowDataEvent.TYPE_CHILD_ADD,
                         childRow);
    }


    /**
     * Add multiple childRows to this RowData's list of children.
     */
    public void addChildRows(List<RowData> childRows) {

        for (RowData rowData : childRows) {
            addChildRow(rowData);
        }
    }


    /**
     * Clear the list of child rows.
     *
     * Currently, this is a private method, and is only
     * called as part of some other change to this
     * RowData object, so we don't call fireRowDataEvent().
     * If this method becomes public, add calls to
     * fireRowDataEvent().
     */
    private void clearChildRows() {
        childRows = new ArrayList<RowData>();
    }


    /**
     * Get the number of "levels" that this row is indented.
     * I.e. this is the number of ancestors it has.
     */
    public int getIndentCount() {

        int count = 0;
        for (RowData rowData = this.getParentRow(); rowData != null;
             rowData = rowData.getParentRow())
            count++;
        return(count);
    }


    /**
     * Get the amount a RowString should be indented.
     * This method can be deleted if we switch to using
     * something other than a JLabel to create the indent widget.
     */
    public String getIndentString() {

        String indentString = "";
        for (RowData rowData = this.getParentRow(); rowData != null;
             rowData = rowData.getParentRow()) {

            /**
             * Increase the number of spaces in this string if
             * you want the indent of rows to be greater.
             */
            indentString += "      ";
        }
        return(indentString);
    }


    /**
     * Get a string version of this whole tree usefull for debugging.
     * I.e. a string version of this RowData and all its descendents.
     */
    public String toString() {
        return(toString(true, ""));
    }


    /**
     * Get a string version of this whole tree usefull for debugging.
     * I.e. a string version of this RowData and all its descendents.
     *
     * All rows should have the indent string prepended to the row.
     * This method calls itself recursively, increasing the indent
     * amount for each level deeper a row is nested in the tree.
     *
     * @param debubVersion Pass true if you want the string to
     * be more useful to a human.  Pass false if you want it to
     * look more like what will be passed to query software.
     *
     * @param indent The amount to indent this row.  E.g. "    "
     */
    public String toString(boolean debugVersion, String indent) {

        String string = getRowString(debugVersion, indent);

        for (RowData childRow : childRows) {
            /**
             * I am currently using two spaces as the indent level for
             * string debug output, but you can change this below.
             */
            string += "\n"+childRow.toString(debugVersion, indent+"  ");
        }

        return(string);
    }


    /**
     * Get the String representation of JUST THIS row.  I.e. not this
     * row and its children.
     */
    public String getRowString() {
        return(getRowString(true, ""));
    }


    /**
     * Get the String representation of JUST THIS row.  I.e. not this
     * row and its children.  The row will have the passed in indent
     * string prepended to it.
     *
     * @param debugVersion If this is true, the string will give
     * more information useful to a programmer but it will also be
     * more cluttered.
     *
     * @param indent The amount to indent this row.  E.g. "    "
     */
    public String getRowString(boolean debugVersion, String indent) {

        String string = indent;

        if ((debugVersion) || (this.isRootRow())) {
            if (getParentClass() != null)
                string += getParentClass().getName()+" |";
            else
                string += "ERROR: No Parent Class"+" |";
        }

        /**
         * Do a quick sanity check.
         */
        if ((collectionOperator != null) &&
            (collectionOperator != CollectionOperator.COUNT) &&
            (attributeOperator != null)) {
            string += "ERROR: RowData is in an inconsistent state.";
            string += "\ncollectionOperator = "+collectionOperator;
            string += "\nattributeOperator = "+attributeOperator;
            return(string);
        }

        string += getAttributePathString(debugVersion);

        if (collectionOperator != null) {
            string += " "+collectionOperator;
        }

        if (collectionOperator2 != null) {
            string += " have "+collectionOperator2;
        }

        if (propName != null) {
            string += "."+propName;
        }
        if (propType != null) {
            string += "("+propType+")";
        }

        if (attributeOperator != null)
            string += " "+attributeOperator;

        if (attributeValue != null) {
            string += " \""+attributeValue+"\"";
        }

        return(string);
    }


    /**
     * Get the "childmost" or "rightmost" Attribute that is specified
     * by this row.
     */
    public Attribute getChildmostAttribute() {

        if ((attributePath == null) || attributePath.isEmpty()) {
            return(null);
        }
        else
            return(new Attribute(attributePath.get(attributePath.size()-1)));
    }


    /**
     * Get the class that this RowData considers its "parent" class.
     * I.e. this is the class whose list of attributes will be the
     * leftmost comboBox's selections in THIS row.
     *
     * If our parent row has an attributePath, this will will be
     * the class of the last, (rightmost), attribute on the attributePath.
     *
     * If our parent row does not have an attributePath, then this
     * will be the parent class of our parent row.
     *
     * If this is the root row, then our "parent" class is the
     * classUnderQualification.
     */
    public ClassDescription getParentClass() {

        if (parentRow == null) {
            return(classUnderQualification);
        }
        else if (parentRow.getChildmostAttribute() == null) {
                return(parentRow.getParentClass());
        }
        else {
            return(parentRow.getChildmostAttribute().getClassDescription());
        }
    }


/*
    public ClassDescription getChildmostClass() {

        Attribute childmostAttribute = getChildmostAttribute();

        if (childmostAttribute == null) {
            return(classUnderQualification);
        }
        else if (parentRow.getChildmostAttribute() == null) {
                return(parentRow.getParentClass());
        }
        else {
            return(parentRow.getChildmostAttribute().getClassDescription());
        }
    }
*/

    /**
     * This method returns a list of RowData objects that contain
     * illegal values.  It checks this RowData object and all its
     * descedents.  This means that the list of illegal RowData
     * objects could contain this RowData object and all its
     * descendents.
     *
     * This public getIllegalRows() method is simply a wrapper
     * to create the initial empty ArrayList and then call
     * the private getIllegalRows() method that will add values
     * to the list.  The other getIllegalRows() method does the
     * real work.
     *
     * This method never returns null, but will return an
     * empty list if there are no illegal rows.
     */
    public List<RowData> getIllegalRows() {

        List<RowData> illegalRows = new ArrayList<RowData>();
        getIllegalRows(illegalRows);
        return(illegalRows);
    }


    /**
     * This method adds this RowData object to the pass in list
     * of illegalRows if this RowData object is illegal.
     * Then this method calls itself recursively to possibly
     * have its descendents add themselves to the list.
     *
     * Please see the containsLegalValue() method for information
     * about what constitutes an illegal RowData.
     */
    private void getIllegalRows(List<RowData> illegalRows) {

        /**
         * First check that the values in this row are valid.
         */
        if (containsLegalValue() == false) {
            illegalRows.add(this);
        }

        /**
         * Now recursively check all of our descendent rows.
         */
        for (RowData childRow : getChildRows())
            childRow.getIllegalRows(illegalRows);
    }


    /**
     * This method returns true if the current value of this
     * RowData object is legal.  It does NOT check the values
     * of its child RowData objects.
     *
     * A few notes on what is valid and not valid:
     *
     *      A tree that has no rows in it, not even a root row,
     *      is considered not valid.  (This case should never actually
     *      occur in the present GUI.)
     *
     *      A root row that has no children is considered not valid.
     *      For example, the user has selected Epoch as the Class
     *      Under Qualification, but has not set any children for it.
     *
     *      A compound row that has no children is considered not valid.
     *      For example, the user has created a compound row as a
     *      child of another row, and has selected Any/All/None
     *      as the collection operator, but has not created any child
     *      rows that are in that collection.
     *
     *      If a row has a "keyed" property field, the name of the key
     *      cannot be blank.
     *
     *      If a row's attributeValue is supposed to be a floating
     *      point number, but the value is currently Double.NaN,
     *      then we are not valid.  (The GUI sets attributeValue to
     *      Double.Nan when the user enters an illegal string into
     *      the value text field.)
     */
    public boolean containsLegalValue() {

        /**
         * If the user is supposed to have entered a number
         * as the attributeValue, (e.g. the text field in
         * the GUI contains "22.3", "2E3"), test whether
         * attributeValue is "Not A Number".
         */

        if ((attributeValue instanceof Double) &&
            ((Double)attributeValue).isNaN())
            return(false);

        if ((attributeValue instanceof Float) &&
            ((Float)attributeValue).isNaN())
            return(false);

        /**
         * If we are a compound row, then we should have at least
         * one child row.
         */
        if ((isCompoundRow()) && (getChildRows().size() < 1)) {
            //System.out.println("Illegal: a compound row with no children.  "+
            //                   "rowData: "+getRowString());
            return(false);
        }

        /**
         * Make sure all of the attributes on our attributePath
         * are legal.
         */
        if (attributePathIsLegal() == false) {
            return(false);
        }

        /**
         * If the user needs to specify a "key" (property name)
         * in this row, make sure the value is not null/blank.
         */
        Attribute attribute = getChildmostAttribute();
        if ((attribute != null) &&
            ((attribute.getType() == Type.PARAMETERS_MAP) ||
             (attribute.getType() == Type.PER_USER_PARAMETERS_MAP))) {
            if ((getPropName() == null) ||
                (getPropName().toString().trim().isEmpty())) {
                //System.out.println("Illegal:  key is blank.");
                return(false);
            }
        }

        return(true);
    }


    /**
     * This returns true if the attributePath is legal.
     * Please note, as of October 2011, all we are doing
     * is making sure that there are no "Select Attribute"
     * attributes in the path.
     */
    private boolean attributePathIsLegal() {

        for (Attribute attribute : getAttributePath()) {
            if (attribute.equals(Attribute.SELECT_ATTRIBUTE)) {
                //System.out.println("Illegal:  attributePath ends with "+
                //                   "\"Select Attribute\".  "+
                //                   "rowData: "+getRowString());
                return(false);
            }
        }

        return(true);
    }


    /**
     * This method creates a RowData initialized with a few values
     * for testing purposes.
     */
    public static RowData createTestRowData() {

        ClassDescription epochCD =
            DataModel.getClassDescription("Epoch");
        ClassDescription epochGroupCD =
            DataModel.getClassDescription("EpochGroup");
        ClassDescription sourceCD =
            DataModel.getClassDescription("Source");
        //ClassDescription resourceCD =
        //    DataModel.getClassDescription("Resource");
        //ClassDescription derivedResponseCD =
        //    DataModel.getClassDescription("DerivedResponse");

        /**
         * Now create some RowData values.
         */

        /**
         * Create the "root" RowData object.  All of the other
         * rows are children of this row.
         */
        RowData rootRow = RowData.createRootRow();

        RowData rowData;

        /**
         * Start creating child rows of the rootRow.
         */

        /**
         * Create a row:
         *
         *      epochGroup.source is null
         */
        rowData = new RowData();
        rowData.addAttribute(epochCD.getAttribute("epochGroup"));
        rowData.addAttribute(epochGroupCD.getAttribute("source"));
        rowData.addAttribute(Attribute.IS_NULL);
        rootRow.addChildRow(rowData);

        /**
         * Create a startTime and endTime "Date/Time" row.
         */
        rowData = new RowData();
        rowData.addAttribute(epochCD.getAttribute("startTime"));
        rowData.setAttributeOperator(Operator.GREATER_THAN_EQUALS);
        rowData.setAttributeValue(new GregorianCalendar(2011, 0, 1).getTime());
        rootRow.addChildRow(rowData);

        rowData = new RowData();
        rowData.addAttribute(epochCD.getAttribute("endTime"));
        rowData.setAttributeOperator(Operator.LESS_THAN_EQUALS);
        rowData.setAttributeValue(new LocalDateTime(DateTimeZone.UTC));
        rootRow.addChildRow(rowData);

        /**
         * Create a "My Property" row.
         */
        rowData = new RowData();
        rowData.addAttribute(epochCD.getAttribute("nextEpoch"));
        rowData.addAttribute(epochCD.getAttribute("myproperties"));
        rowData.setPropName("animalID");
        rowData.setPropType(Type.INT_32);
        rowData.setAttributeOperator(Operator.LESS_THAN_EQUALS);
        rowData.setAttributeValue(new Integer(32));
        rootRow.addChildRow(rowData);

        /**
         * Create a "Parameters Map" row of type int.
         */
        rowData = new RowData();
        rowData.addAttribute(epochCD.getAttribute("protocolParameters"));
        rowData.setPropName("stimulusFrequency");
        rowData.setPropType(Type.INT_32);
        rowData.setAttributeOperator(Operator.EQUALS);
        rowData.setAttributeValue(new Integer(27));
        rootRow.addChildRow(rowData);

        /**
         * Create a "Parameters Map" row of type string.
         */
        rowData = new RowData();
        rowData.addAttribute(epochCD.getAttribute("protocolParameters"));
        rowData.setPropName("stimulusName");
        rowData.setPropType(Type.UTF_8_STRING);
        rowData.setAttributeOperator(Operator.MATCHES_CASE_INSENSITIVE);
        rowData.setAttributeValue("caffeine");
        rootRow.addChildRow(rowData);

        /**
         * Create a "Per User" derivedResponse row.
         */
        rowData = new RowData();
        rowData.addAttribute(epochCD.getAttribute("derivedResponses"));
        rowData.setCollectionOperator(CollectionOperator.ALL);
        rootRow.addChildRow(rowData);

        /**
         * Create a row that ends with a string value.
         */
        rowData = new RowData();
        rowData.addAttribute(epochCD.getAttribute("epochGroup"));
        rowData.addAttribute(epochGroupCD.getAttribute("source"));
        rowData.addAttribute(sourceCD.getAttribute("label"));
        rowData.setAttributeOperator(Operator.EQUALS);
        rowData.setAttributeValue("Test 27");
        rootRow.addChildRow(rowData);

        /**
         * Create another child row.
         */
        rowData = new RowData();
        rowData.addAttribute(epochCD.getAttribute("resources"));
        rowData.setCollectionOperator(CollectionOperator.NONE);

        rootRow.addChildRow(rowData);

        /**
         * Create another child row.
         */
        rowData = new RowData();
        rowData.setCollectionOperator(CollectionOperator.ALL);
        rowData.addAttribute(epochCD.getAttribute("epochGroup"));
        rowData.addAttribute(epochGroupCD.getAttribute("epochs"));
        rootRow.addChildRow(rowData);

        /**
         * Create a child row of the above row.
         */
        RowData rowData2 = new RowData();
        rowData2.addAttribute(epochCD.getAttribute("startTime"));
        rowData2.setAttributeOperator(Operator.GREATER_THAN_EQUALS);
        rowData2.setAttributeValue(new GregorianCalendar(2010, 0, 1).getTime());
        rowData.addChildRow(rowData2);

        //System.out.println("rootRow:\n"+rootRow.toString());

        return(rootRow);
    }


    /**
     * Write out this RowData to the passed in outputStream.
     *
     * @param outputStream The ObjectOutputStream into which this
     * RowData will be written.
     *
     * @throws IOException This will be whatever exception was generated
     * by the call to outputStream.writeObject().
     */
    public void writeRowData(ObjectOutputStream outputStream)
        throws IOException {

        /**
         * "Sterilize" the RowData object by creating a copy.
         * This gets rid of information such as GUI component
         * listeners and other things like that.  We just want
         * the data values.
         */

        System.out.println("rowData write attribute value: " + getAttributeValue());
        RowData rowData = new RowData(this);
        outputStream.writeObject(rowData);
    }


    /**
     * Write out this RowData to the passed in file.
     * This method is only being used for development/testing
     * purposes currently.
     *
     * @param fileName The name of the file into which this
     * RowData will be written.
     */
    public void writeRowData(String fileName) {

        try {
            FileOutputStream outputFile = new FileOutputStream(fileName);
            ObjectOutputStream outputStream = new ObjectOutputStream(
                outputFile);
            writeRowData(outputStream);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Read a RowData object from the passed in inputStream.
     *
     * @param inputStream The ObjectInputStream from which an
     * RowData will be read.
     *
     * @param The read in RowData.  Returns null if there
     * was an error.
     *
     * @throws IOException This will be whatever exception was generated
     * by the call to inputStream.readObject().
     *
     * @throws ClassNotFoundException This will be thrown if the
     * call to inputStream.readObject() did not know what class
     * was read in.  This should never happen.  If it does, the
     * file that was read in is probably corrupted or of an incorrect
     * version if you see an InvalidClassException.
     */
    public static RowData readRowData(ObjectInputStream inputStream)
        throws IOException, ClassNotFoundException {

        Object obj = null;
        try {
            obj = inputStream.readObject();
        }
        catch (InvalidClassException e) {

            String s = "The object that was read in was a RowData "+
                "object, but it, or one of the classes it contains, "+
                "might be of the wrong version.  For example, you are "+
                "using an input file that was written using an old "+
                "version of one of the classes.  If so, delete the old "+
                "input file.";
            throw(new IOException(s, e));
        }

        if (obj instanceof RowData) {
            return((RowData)obj);
        }
        else {
            /**
             * The object we read in was not a RowData.
             * This should never happen.
             */
            String s = "The object that was read in was not a "+
                "RowData object but it was some sort of "+
                "object that we know about: "+obj.getClass().getName()+".  "+
                "There is some sort of bug/inconsistency in the code.";
            throw(new IOException(s));
        }
    }


    /**
     * Read a RowData object from the passed in file.
     * This method is only being used for development/testing
     * purposes currently.
     *
     * @param fileName The name of the file from which an
     * RowData will be read.
     *
     * @param The read in RowData.  Returns null if there
     * was an error.
     */
    public static RowData readRowData(String fileName) {

        try {
            FileInputStream inputFile = new FileInputStream(fileName);
            ObjectInputStream inputStream = new ObjectInputStream(inputFile);
            return(readRowData(inputStream));
        }
        catch (FileNotFoundException e) {
            /**
             * Ignore it.  The first time we are run, no output
             * file will have been written.
             */
            System.err.println("File not found: "+fileName);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return(null);
    }


    /**
     * Test the serialization of this object by writing it out
     * and reading it in and seeing if the read in value is the
     * same as the written out value.
     *
     * @return Returns true if the value that we read in
     * matched the original value that we wrote out.
     */
    public boolean testSerialization() {

        writeRowData(SAVE_FILE_NAME);
        RowData rowData = readRowData(SAVE_FILE_NAME);
        boolean same = rowData.toString(true, "").equals(
            this.toString(true, ""));

        //System.out.println("rowData:\n"+rowData);

        if (same)
            logger.debug("RowData written and read versions are "+
                               "the same.");
        else
            logger.error("ERROR:  RowData written and read versions are "+
                               "different.");

        return(same);
    }


    /**
     * This is a simple test program for this class.
     * Full testing is done as part of the TranslatorTests class.
     */
    public static void main(String[] args) {

        System.out.println("RowData test is starting...");

        RowData rootRow;

        rootRow = createTestRowData();
        System.out.println(rootRow);
        rootRow.testSerialization();

        System.out.println("RowData test is ending.");
    }
}
