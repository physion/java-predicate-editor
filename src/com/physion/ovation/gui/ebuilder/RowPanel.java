package com.physion.ovation.gui.ebuilder;

import java.util.EventObject;
import java.util.ArrayList;
import java.util.Date;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTextField;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.BorderFactory;

import com.lavantech.gui.comp.DateTimePicker;
import com.lavantech.gui.comp.TimePanel;

import com.physion.ovation.gui.ebuilder.datamodel.RowData;
import com.physion.ovation.gui.ebuilder.datamodel.DataModel;
import com.physion.ovation.gui.ebuilder.datamodel.CollectionOperator;
import com.physion.ovation.gui.ebuilder.datatypes.Attribute;
import com.physion.ovation.gui.ebuilder.datatypes.Cardinality;
import com.physion.ovation.gui.ebuilder.datatypes.Type;
import com.physion.ovation.gui.ebuilder.datatypes.ClassDescription;


/**
 * This class creates all the widgets that are used to display and
 * end a row.
 *
 * We create all the widgets we will need in our constructor, and
 * thereafter we simply add or remove them from the JPanel based on
 * the RowData value for that row.
 *
 * TODO:  Pull parts of this code out into utility methods.
 */
class RowPanel
    extends JPanel
    implements ActionListener, DocumentListener, ChangeListener {

    /**
     * A comboBox dropdown list will dispaly at least this many
     * items before it starts to use a scrollbar.
     */
    private static final int MAX_ROWS_IN_COMBOBOX_DROPDOWN = 30;

    /**
     * Minimum width of a text field.  Please note, the actual
     * number of characters that will fit depend on the font
     * used and the characters in the field.  E.g. 'W' is wider
     * than 'l' in most fonts.
     */
    private static final int MIN_TEXT_COLUMNS = 8;
    private static final int MIN_SPINNER_COLUMNS = 8;

    /**
     * Inset in pixels between Components in a row.
     * We typically use this on the left side of a component.
     */
    private static final int INSET = 7;
    private static final Insets LEFT_INSETS = new Insets(0,INSET,0,0);

    private InvisibleButton deleteButton;
    private InvisibleButton createCompoundRowButton;
    private InvisibleButton createAttributeRowButton;

    /**
     * This holds all the JComboBoxes we use in this row.
     * Note that a user can, theoretically, have an infinitely
     * long path of attributes.  For example,
     *
     *      nextEpoch.prevEpoch.nextEpoch.prevEpoch...
     *
     * Because of this, we generate the JComboBoxes as we need them.
     */
    private ArrayList<JComboBox> comboBoxes = new ArrayList<JComboBox>();

    /**
     * This text field is used to enter the value of a "primitive"
     * attribute that is a string or a float.
     */
    private JTextField valueTextField;

    /**
     * This spinner is used to enter the value of a "primitive"
     * attribute that is an INT_16.
     */
    private JSpinner valueSpinnerInt16;

    /**
     * This spinner is used to enter the value of a "primitive"
     * attribute that is an INT_32.  It is also used for other
     * integer values in a row.  For example, the value of a Count
     * in a TO_MANY relationship.
     */
    private JSpinner valueSpinnerInt32;

    /**
     * This spinner is used to enter a Count value.
     * It only holds numbers >= 0.
     */
    private JSpinner countSpinnerInt32;

    /**
     * This text field is used to enter the user created "key" for the 
     * "properties" attribute.
     */
    private JTextField propNameTextField;

    /**
     * This comboBox allows the user to select what the type of the
     * "keyed" "properties" attribute will be.
     * E.g. int, string, boolean, time.
     */
    private JComboBox propTypeComboBox;

    /**
     * This comboBox allows the user to select the operator that
     * will be used for a row that contains a "keyed" "properties"
     * definition.
     */
    private JComboBox operatorComboBox;

    /**
     * This is the widget that is displayed to let a user select
     * a date and time.
     */
    private DateTimePicker dateTimePicker;

    /**
     * This is simply a "spacer" that we put on the left side of
     * the row to indent the widgets to the right of it.
     */
    private JLabel indentWidget;

    /**
     * This holds all the buttons on the right side of the row.
     * I.e. the +/++/- buttons.
     */
    private JPanel buttonPanel;

    /**
     * This label never changes.
     */
    private final JLabel ofTheFollowingLabel = new JLabel(" of the following");

    /**
     * This is the ExpressionPanel that contains this RowPanel.
     */
    private ExpressionPanel expressionPanel;


    private RowData rowData;

    private boolean inProcess = false;

    private int gridx;
    private boolean someWidgetFillingEmptySpace;


    /**
     * Create whatever components this renderer will need.
     * Other methods add or remove the components depending on
     * what RowData this row is displaying/editing.
     */
    public RowPanel(RowData rowData) {

        this.rowData = rowData;

        setBorder(BorderFactory.createEmptyBorder(4,10,4,10));

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        /**
         * Create a component we will use on the left side of this
         * RowPanel to indent all the other widgets to the right
         * by some amount.  Currently, I am simply using a JLabel
         * filled with an adustible number of spaces to do this.
         * Simple, and it works.  There are many other ways to do
         * this though:  A JPanel with a minimum size, A JLabel or
         * JPanel that is empty, but has an EmptyBorder whose size
         * you change.
         */
        indentWidget = new JLabel();

        deleteButton = new InvisibleButton("-");
        deleteButton.addActionListener(this);

        createAttributeRowButton = new InvisibleButton("+");
        createAttributeRowButton.addActionListener(this);

        createCompoundRowButton = new InvisibleButton("++");
        createCompoundRowButton.addActionListener(this);

        valueTextField = new JTextField();
        valueTextField.setColumns(MIN_TEXT_COLUMNS);
        valueTextField.getDocument().addDocumentListener(this);

        valueSpinnerInt16 = new JSpinner(new SpinnerNumberModel(
            0, Short.MIN_VALUE, Short.MAX_VALUE, 1));
        valueSpinnerInt16.addChangeListener(this);
        ((JSpinner.NumberEditor)valueSpinnerInt16.getEditor()).getTextField().
            setColumns(MIN_SPINNER_COLUMNS);

        valueSpinnerInt32 = new JSpinner(new SpinnerNumberModel(
            0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        valueSpinnerInt32.addChangeListener(this);
        ((JSpinner.NumberEditor)valueSpinnerInt32.getEditor()).getTextField().
            setColumns(MIN_SPINNER_COLUMNS);

        /**
         * This spinner is used when we are setting the Count for
         * an attribute that has a to-many relationship.
         */
        countSpinnerInt32 = new JSpinner(new SpinnerNumberModel(
            0, 0, Integer.MAX_VALUE, 1));
        countSpinnerInt32.addChangeListener(this);
        ((JSpinner.NumberEditor)countSpinnerInt32.getEditor()).getTextField().
            setColumns(MIN_SPINNER_COLUMNS);

        /**
         * This text field is used to display/edit the name of
         * a "keyed" property.
         */
        propNameTextField = new JTextField();
        propNameTextField.setColumns(MIN_TEXT_COLUMNS);
        propNameTextField.getDocument().addDocumentListener(this);

        dateTimePicker = new DateTimePicker();
        TimePanel timePanel = dateTimePicker.getTimePanel();
        timePanel.setSecDisplayed(false);
        dateTimePicker.addActionListener(this);

        /**
         * Create the comboBox used to choose the type of a "keyed"
         * property.  For example, an attribute like one of these:
         *
         *      nextEpoch.properties.animalID(int) <= 123
         *      protocolParameters.stimulusName(string) == "caffeine"
         *
         * The model, (i.e. the selectable items), for this comboBox
         * never changes, so we can set the value of the model now.
         */
        propTypeComboBox = createComboBox(new DefaultComboBoxModel(
            DataModel.PROP_TYPES));

        /**
         * Create the comboBox used to choose the operator for
         * a "keyed" property value.  The operator changes depending
         * on the type of the property.  For example, an int/float
         * value has operators like ==,!=,<,>,<=,>=, while a string
         * value has those operators plus ~==, and ~~==.
         */
        operatorComboBox = createComboBox(null);

        buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        GridBagConstraints gc;
        gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.VERTICAL;
        //gc.insets = LEFT_INSETS;
        gc.insets = new Insets(0,INSET*3,0,0);
        buttonPanel.add(createAttributeRowButton, gc);

        gc = new GridBagConstraints();
        gc.gridx = 1;
        gc.fill = GridBagConstraints.VERTICAL;
        gc.insets = LEFT_INSETS;
        buttonPanel.add(createCompoundRowButton, gc);

        gc = new GridBagConstraints();
        gc.gridx = 2;
        gc.fill = GridBagConstraints.VERTICAL;
        gc.insets = LEFT_INSETS;
        buttonPanel.add(deleteButton, gc);

        initializeComponents();
    }


    /**
     * Get the comboBox at the specified index.  Create it if it
     * does not already exist.
     */
    private JComboBox getComboBox(int index) {

        while (index >= comboBoxes.size())
            comboBoxes.add(createComboBox(null));

        return(comboBoxes.get(index));
    }


    /**
     * Create a JComboBox that uses the passed in model.
     * Pass null if you just want a default model that you
     * later change to something else.
     */
    private JComboBox createComboBox(ComboBoxModel model) {

        JComboBox comboBox;
        if (model == null)
            comboBox = new JComboBox();
        else
            comboBox = new JComboBox(model);

        comboBox.setEditable(false);

        /**
         * Set the number of items that the dropdown will
         * display before it adds a scrollbar.
         */
        comboBox.setMaximumRowCount(MAX_ROWS_IN_COMBOBOX_DROPDOWN);
        comboBox.addActionListener(this);

        /**
         * Change default keyboard behavior of the comboBox so
         * it does NOT select the value when using the keyboard
         * to travers values in the list.
         * Details about this behavior can be googled.
         * For example:  http://tinyurl.com/cbkeyboardtrav
         */
        comboBox.putClientProperty("JComboBox.isTableCellEditor",
                                   Boolean.TRUE);
        return(comboBox);
    }


    /**
     * This method lays out the components, (e.g. comboBoxes),
     * that will be needed to display this RowPanel's current
     * rowData value.
     * It also sets the data models the comboBoxes will use
     * and sets the selected value in the comboBoxes to the
     * corresponding values in this RowPanel's rowData value.
     */
    private void initializeComponents() {

        if (inProcess)
            return;

        inProcess = true;
        initializeComponentsProtected();
        inProcess = false;
    }


    /**
     * Layout whatever components the RowData for this row needs.
     * This method places the assorted comboBoxes, text fields, labels,
     * and buttons in a panel using the GridBagLayout layout manager.
     */
    private void initializeComponentsProtected() {

        GridBagConstraints gc;


        /**
         * First, remove whatever components used to be in the
         * row.
         */
        removeAll();

        /**
         * Now start adding the components that are needed to
         * display and edit our current rowData value.
         *
         * Start our "gridx" counter that is incremented every time
         * we add another widget to this row.  This counter is always
         * set to the gridx location of the next widget to be placed
         * in this RowPanel's GridBagLayout.
         *
         * The indentWidget we use to indent rows is always the first/leftmost
         * widget.
         */
        gridx = 0;

        /**
         * If a row is filled with widgets that do
         * not stretch, we need to have the cell that
         * holds the buttons on the right side of the
         * row use the empty space.
         *
         * This flag gets set to true if some other widget
         * uses the extra space.  E.g. the valueTextField
         * will use the extra space if it exists in this row.
         */
        someWidgetFillingEmptySpace = false;

        gc = new GridBagConstraints();
        gc.gridx = gridx++;
        add(indentWidget, gc);
        if (rowData != null)
            indentWidget.setText(rowData.getIndentString());
        else
            indentWidget.setText("");

        /**
         * Now add the components that are needed.
         */

        if (rowData.isRootRow()) {
            layoutRootRow();
        }
        else if (rowData.isSimpleCompoundRow()) {
            /**
             * A "simple" compound row is a row that
             * only contains a Collection Operator comboBox,
             * (i.e. Any/All/None), and the -, +, ++ buttons
             * on the right side.
             */
            layoutSimpleCompoundRow();
        }
        else {
            /**
             * An attribute row.  For example:
             *
             *      epochGroup.epochs.label == "Test 21"
             */
            layoutAttributeRow();
        }

        /**
         * Show/hide the +,++,- buttons.
         */
        layoutButtons();

        repaint();  // Might actually have to call validate() here.
    }


    /**
     * Set the model for the passed in comboBox to be the attributes
     * of the passed in classDescription.
     *
     * In addition, we will (optionally) prepend the special
     * Attribute.SELECT_ATTRIBUTE attribute, and we will append
     * the special Attribute.IS_NULL and Attribute.IS_NOT_NULL,
     * and we will append the special Attribute.MY_PROPERTY and
     * Attribute.ANY_PROPERTY.
     *
     * Any attributes that are of type Type.PER_USER will cause
     * two entries to be added to the comboBox model.  One entry
     * will be prepended with the string "My" and the other with
     * the string "All".  For example, if the attribute was called
     * "analysisRecords", instead of inserting an entry called
     * "analysisRecords" into the comboBox, we will insert two
     * entries:  "My analysisRecords" and "All analysisRecords".
     *
     * @param comboBox - The JComboBox whose model and selectedItem
     * we will set.
     *
     * @param classDescription - We will set the comboBox's model to
     * be the list of attributes of this ClassDescription.  (We also
     * might add a few more special values to the list.)
     * 
     * @param appendNulls - If this is true, we will append the
     * special Attribute.IS_NULL and IS_NOT_NULL to the end of the
     * list of the choices.
     *
     * @param selectedItem - After setting the model, this method sets
     * the selected item to this value.  Pass null if you do not want to
     * set the selected item.
     */
    private void setComboBoxModel(JComboBox comboBox,
                                  ClassDescription classDescription,
                                  boolean appendNulls,
                                  Object selectedItem) {

        ArrayList<Attribute> attributes;
        if (classDescription.getAllAttributes() != null)
            attributes = classDescription.getAllAttributes();
        else
            attributes = new ArrayList<Attribute>();

        ArrayList<Attribute> copy = new ArrayList<Attribute>();

        /**
         * First, prepend the special "Select Attribute" attribute
         * if requested.
         */
        //if (prependSelectAttribute)
        //    copy.add(Attribute.SELECT_ATTRIBUTE);
        copy.add(Attribute.SELECT_ATTRIBUTE);

        /**
         * Go through the list of attributes adding them to our
         * copy of the ArrayList.  But, if an attribute is of type
         * Type.PER_USER or Type.PER_USER_PARAMETERS_MAP,
         * insert two entries into our copy, one entry has its
         * isMine flag set to true, and the other false.
         *
         * This messing around is needed because we want the
         * user to have two versions of these attributes that
         * s/he can choose.  E.g. EntityBase.properties becomes
         * "My Property" and "Any Property" as comboBox choices.
         * E.g. Epoch.derivedResponses becomes "My derivedResponses"
         * and "All derivedResponses".
         */
        for (Attribute attribute : attributes) {

            if (attribute.getType() == Type.PER_USER) {
                Attribute newAttribute = new Attribute(attribute);
                newAttribute.setIsMine(true);
                copy.add(newAttribute);
                newAttribute = new Attribute(attribute);
                newAttribute.setIsMine(false);
                copy.add(newAttribute);
            }
            else if (attribute.getType() == Type.PER_USER_PARAMETERS_MAP) {
                Attribute newAttribute = new Attribute(attribute);
                newAttribute.setIsMine(true);
                copy.add(newAttribute);
                newAttribute = new Attribute(attribute);
                newAttribute.setIsMine(false);
                copy.add(newAttribute);
            }
            else {
                copy.add(attribute);
            }
        }

        if (appendNulls) {
            copy.add(Attribute.IS_NULL);
            copy.add(Attribute.IS_NOT_NULL);
        }

        /**
         * All the monkey business with the list of Attributes is
         * finished, so create a DefaultComboBoxModel out of the list
         * Attributes and install it in the comboBox.
         */
        Attribute[] values;
        values = copy.toArray(new Attribute[0]);
        setComboBoxModel(comboBox, values, selectedItem);
    }


    /**
     * @param selectedItem - After setting the model, this method sets
     * the selected item to this value.  Pass null if you do not want to
     * set the selected item.
     */
    private void setComboBoxModel(JComboBox comboBox, Object[] items,
                                  Object selectedItem) {

        DefaultComboBoxModel model = new DefaultComboBoxModel(items);
        comboBox.setModel(model);
        if (selectedItem != null)
            comboBox.setSelectedItem(selectedItem);

        if (((DefaultComboBoxModel)(comboBox.getModel())).
            getIndexOf(selectedItem) < 0) {

            System.err.println("ERROR:  Desired selectedItem not found in "+
                "list.\nselectedItem = "+selectedItem+"\nItems in list:\n");
            for (Object item : items) {
                //System.out.println("  "+((Attribute)item).toStringDebug());
                System.out.println("  "+item);
            }
        }
    }


    /**
     * This method is called when the user clicks on a +,++,- button
     * on the right side of a row, or when the user sets a time/date
     * value using the dateTimePicker.
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        //System.out.println("Enter actionPerformed on: "+
        //                   e.getSource().getClass());

        if (e.getSource() == createCompoundRowButton) {
            rowData.createCompoundRow();
            getExpressionPanel().createRowPanels();
        }
        else if (e.getSource() == createAttributeRowButton) {
            rowData.createAttributeRow();
            getExpressionPanel().createRowPanels();
        }
        else if (e.getSource() == deleteButton) {
            rowData.removeFromParent();
            getExpressionPanel().createRowPanels();
        }
        else if (e.getSource() instanceof DateTimePicker) {
            dateTimeChanged();
        }
        else if (e.getSource() instanceof JComboBox) {
            comboBoxChanged((JComboBox)e.getSource());
        }
        else {
            System.err.println("ERROR: actionPerformed() does not handle "+
                "events from this widget.  event = "+e);
        }
    }


    /**
     * Get the ExpressionPanel that contains this RowPanel.
     * The ExpressionPanel is the panel that arranges all the
     * RowPanels in a list.
     */
    private ExpressionPanel getExpressionPanel() {

        Container parent = getParent();
        while ((parent != null) &&
               !(parent instanceof ExpressionPanel))
            parent = getParent();

        return((ExpressionPanel)parent);
    }


    /**
     * This method is called when the user picks a new date or time
     * value in the dateTimePicker.
     */
    private void dateTimeChanged() {

        Date date = dateTimePicker.getDate();
        rowData.setAttributeValue(date);
    }


    /**
     * This is called when a value in a comboBox is changed by the
     * user OR programmatically.
     */
    /*
    @Override
    public void itemStateChanged(ItemEvent e) {
        System.out.println("Enter itemStateChanged = "+e);
        if (e.getStateChange() != ItemEvent.SELECTED)
            return;
        comboBoxChanged((JComboBox)e.getSource());
    }
    */


    /**
     * This method is called when the user changes the selected
     * item in a comboBox.
     *
     * TODO: Clean up and comment this.
     */
    private void comboBoxChanged(JComboBox comboBox) {

        if (inProcess)
            return;

        //System.out.println("Enter comboBoxChanged");

        /**
         * Change the appropriate value in this row's RowData
         * object.
         */

        if (rowData.isRootRow()) {
            /**
             * The first/topmost row is being edited.  So we need to
             * adjust the value of the "root" row.  Also known as the
             * Class Under Qualification.
             */
            if (comboBox == comboBoxes.get(0)) {
                /**
                 * User is changing the value of the Class Under Qualification.
                 */
                ClassDescription classDescription =
                    (ClassDescription)comboBox.getSelectedItem();
                /*
                System.out.println("selected classDescription = "+
                    classDescription);
                System.out.println("selected classDescription = "+
                    classDescription.hashCode());
                */
                if (!rowData.getClassUnderQualification().equals(
                    classDescription)) {
                    rowData.setClassUnderQualification(classDescription);
                }
            }
            else if (comboBox == comboBoxes.get(1)) {
                /**
                 * User is changing the value of the Collection Operator.
                 */
                CollectionOperator collectionOperator =
                    (CollectionOperator)comboBox.getSelectedItem();
                /*
                System.out.println("selected collectionOperator = "+
                    collectionOperator);
                System.out.println("selected collectionOperator = "+
                    collectionOperator.hashCode());
                */
                if (!rowData.getCollectionOperator().equals(
                    collectionOperator)) {
                    rowData.setCollectionOperator(collectionOperator);
                }
            }

            getExpressionPanel().createRowPanels();
        }
        else {
            /**
             * User is editing a row other than the first row.
             */
            //System.out.println("A row other than first row being changed.");

            /**
             * TODO:  Put this in its own method?
             */
            if (comboBox == propTypeComboBox) {
                //System.out.println("Property Type is being changed.");
                /**
                 * User has changed the type of a "keyed" "properties"
                 * attribute in a "My/Any Property" row.
                 */
                rowData.setPropType((Type)propTypeComboBox.getSelectedItem());
            }
            else if (comboBox == operatorComboBox) {
                /**
                 * User has changed the operator for a "keyed" property.
                 */
                //System.out.println("Operator is being changed.");
                rowData.setAttributeOperator(
                    operatorComboBox.getSelectedItem().toString());
            }
            else {
                //System.out.println("comboBox.getSelectedItem() = "+
                //    comboBox.getSelectedItem());
            }

            ArrayList<Attribute> attributes = rowData.getAttributePath();

            Object selectedObject = comboBox.getSelectedItem();
            if (selectedObject instanceof Attribute) {

                /**
                 * User selected an Attribute in a comboBox dropdown.
                 * Set the values "to the right" of the comboBox to
                 * appropriate values.
                 *
                 * TODO:  Put all this business logic stuff into
                 * the RowData object so all the code that worries about
                 * keeping a RowData object "internally" consistent is
                 * in the RowData object itself and not scattered throughout
                 * this "view" code.
                 */

                Attribute selectedAttribute = (Attribute)selectedObject;
                //System.out.println("selectedAttribute = "+selectedAttribute);

                if ((selectedAttribute.getType() !=
                     Type.PER_USER_PARAMETERS_MAP) &&
                    (selectedAttribute.getType() != Type.PARAMETERS_MAP)) {
                    rowData.setPropType(null);
                    rowData.setPropName(null);
                }

                /**
                 * Set the collection operator (Count/Any/All/None) to
                 * be null if the selectedAttribute does not use
                 * a collection operator, or set it to Count if it does
                 * use a collection operator.
                 */
                if ((selectedAttribute.getCardinality() !=
                     Cardinality.TO_MANY) ||
                     (selectedAttribute.getType() ==
                      Type.PER_USER_PARAMETERS_MAP)) {
                    rowData.setCollectionOperator(null);
                }
                else {
                    rowData.setCollectionOperator(CollectionOperator.COUNT);
                    rowData.setAttributeOperator(
                        DataModel.OPERATORS_ARITHMATIC[0]);
                    rowData.setAttributeValue(new Integer(0));
                }

                /**
                 * Set the attribute operator to "is null" or "is not null"
                 * if the selectedAttribute is one of our special
                 * Attribute.IS_NULL or Attribute.IS_NOT_NULL values.
                 *
                 * Otherwise, set the attribute operator and other settings
                 * appropriately for the selectedAttribute's type.
                 */
                if (selectedAttribute.equals(Attribute.IS_NULL) ||
                    selectedAttribute.equals(Attribute.IS_NOT_NULL)) {
                    //System.out.println("Setting attributeOperator to: "+
                    //    selectedAttribute.getName());
                    rowData.setAttributeOperator(
                        selectedAttribute.getDisplayName());
                }
                else if ((selectedAttribute.getType() ==
                          Type.PER_USER_PARAMETERS_MAP) ||
                         (selectedAttribute.getType() == Type.PARAMETERS_MAP)) {
                    rowData.setPropType(Type.INT_32);
                    rowData.setAttributeOperator(
                        DataModel.OPERATORS_ARITHMATIC[0]);
                    rowData.setPropName(null);
                    rowData.setAttributeValue(null);
                }
                else if (selectedAttribute.getType() == Type.BOOLEAN) {
                    rowData.setAttributeOperator(DataModel.OPERATOR_TRUE);
                }

                /*
                System.out.println("After op rowData: "+rowData.getRowString());
                */

                /**
                 * Figure out which comboBox was changed and then
                 * set the corresponding Attribute in the row's
                 * attributePath.
                 */

                int comboBoxIndex = comboBoxes.indexOf(comboBox);

                if (comboBoxIndex < 0) {
                    System.err.println("ERROR:  In comboBoxChanged.  "+
                        "comboBoxIndex = "+comboBoxIndex+
                        ".  This should never happen.");
                }
                //System.out.println("comboBoxIndex = "+comboBoxIndex);

                if (attributes.size() > comboBoxIndex) {
                    /**
                     * The user is setting the value of an Attribute
                     * that is already in this RowData's attributePath.
                     */
                    attributes.set(comboBoxIndex, selectedAttribute);
                }
                else if (attributes.size() == comboBoxIndex) {
                    /**
                     * This is the rightmost comboBox and this RowData
                     * is having this entry in its attributePath set
                     * to an "initial" value.  I.e. the comboBox used
                     * to say "Select Attribute" before the user selected
                     * a value for the first time.
                     */
                    //attributes.add(selectedAttribute);
                    rowData.addAttribute(selectedAttribute);
                }
                else if (attributes.size() < comboBoxIndex) {
                    /**
                     * This should never happen.
                     */
                    System.err.println("ERROR: Coding error.  "+
                        "Too many comboBoxes "+
                        "or too few Attributes in the class's attributePath.");
                }
                /*
                System.out.println("After at rowData: "+rowData.getRowString());
                */

                /**
                 * Remove Attributes that are "after" the one being changed.
                 */
                attributes.subList(comboBoxIndex+1, attributes.size()).clear();

                /**
                 * If the user set the value of a primitive type,
                 * that means we need to be sure the operator is
                 * initialized to an appropriate value for that
                 * type.  E.g. "==" for an int or string, "is true" for
                 * a boolean.
                 *
                 * Also make sure the attributeValue contains a value of
                 * the appropriate type.
                 *
                 * TODO:  Add methods to the RowData class that are
                 * used to access the attributePath that automatically
                 * handle this sort of business logic.
                 */
                Attribute childmostAttribute = rowData.getChildmostAttribute();
                if (childmostAttribute.isPrimitive()) {

                    String attributeOperator = rowData.getAttributeOperator();
                    switch (childmostAttribute.getType()) {
                        case BOOLEAN:
                            if (!DataModel.isOperatorBoolean(
                                attributeOperator)) {
                                /**
                                 * Operator is not currently a legal
                                 * boolean operator, so set it to
                                 * a boolean operator.
                                 */
                                attributeOperator =
                                    DataModel.OPERATORS_BOOLEAN[0];
                            }
                        break;
                        case UTF_8_STRING:
                            if (!DataModel.isOperatorString(
                                attributeOperator)) {
                                /**
                                 * Operator is not currently a legal
                                 * string operator, so set it to
                                 * a string operator.
                                 */
                                attributeOperator =
                                    DataModel.OPERATORS_STRING[0];
                            }
                            //rowData.setAttributeValue(new String(""));
                        break;
                        case INT_16:
                            if (!DataModel.isOperatorArithmatic(
                                attributeOperator)) {
                                /**
                                 * Operator is not currently a legal
                                 * numeric operator, so set it to
                                 * a numeric operator.
                                 */
                                attributeOperator =
                                    DataModel.OPERATORS_ARITHMATIC[0];
                            }
                            rowData.setAttributeValue(new Short((short)0));
                        break;    
                        case INT_32:
                            if (!DataModel.isOperatorArithmatic(
                                attributeOperator)) {
                                attributeOperator =
                                    DataModel.OPERATORS_ARITHMATIC[0];
                            }
                            rowData.setAttributeValue(new Integer(0));
                        break;
                        case FLOAT_64:
                            if (!DataModel.isOperatorArithmatic(
                                attributeOperator)) {
                                attributeOperator =
                                    DataModel.OPERATORS_ARITHMATIC[0];
                            }
                            rowData.setAttributeValue(new Long((long)0.0));
                        break;
                        case DATE_TIME:
                            if (!DataModel.isOperatorArithmatic(
                                attributeOperator)) {
                                attributeOperator =
                                    DataModel.OPERATORS_ARITHMATIC[0];
                            }
                            rowData.setAttributeValue(new Date());
                        break;
                        default:
                            System.err.println("ERROR: Unhandled operator.");
                            attributeOperator = "ERROR";
                    }

                    rowData.setAttributeOperator(attributeOperator);
                }
            }
            else if ((selectedObject instanceof String) &&
                     rowData.getChildmostAttribute().isPrimitive()) {

                //System.out.println("User selected primitive operator "+
                //                   selectedObject);
                /**
                 * The user has selected a value in primitive operator
                 * comboBox.  E.g. ==, !=, >.
                 */
                rowData.setAttributeOperator((String)selectedObject);
            }
            else if (selectedObject instanceof CollectionOperator) {
                /**
                 * User is changing the value of the Collection Operator.
                 * E.g. the user is changing the value of Count, Any, All,
                 * None.
                 */
                CollectionOperator collectionOperator =
                    (CollectionOperator)selectedObject;
                //System.out.println("selected collectionOperator = "+
                //    collectionOperator);
                if (!rowData.getCollectionOperator().equals(
                    collectionOperator)) {
                    rowData.setCollectionOperator(collectionOperator);
                    /**
                     * Collection operator has been set to Count, so set
                     * the attribute value to 0.
                     * Move this code to RowData object because this
                     * is business logic that the GUI should not know
                     * about.
                     */
                    if (collectionOperator == CollectionOperator.COUNT) {
                        rowData.setAttributeValue(new Integer(0));
                    }
                }
            }
            else {
                //System.out.println("selectedObject = "+selectedObject);
            }

            System.out.println("rowData's new value: "+
                               rowData.getRowString(false, ""));
            System.out.println("Debug Version: "+rowData.getRowString());

            initializeComponents();
        }

        /**
         * Because we remove all widgets and then add them back in
         * again every time we update the value of a row, we need
         * to request the focus back again.
         *
         * We might want to change the code to be more "clever" and
         * have it make the minimal set of changes to the widgets
         * in a row, but that will require more code and have a
         * greater chance of introducing bugs.
         */
        comboBox.requestFocus();

        System.out.println("\nrootRow:\n"+
                           rowData.getRootRow().toString(false, ""));
        System.out.println("\nrootRow:\n"+rowData.getRootRow());
    }


    private void layoutButtons() {

        /**
         * The very first row cannot be deleted.
         * All other rows can always be deleted.
         */
        if (rowData.isRootRow()) {
            deleteButton.setDraw(false);
        }
        else {
            deleteButton.setDraw(true);
        }

        /**
         * See if this row can have child rows.
         * Based on that, show/hide the +, ++ buttons.
         */
        if ((rowData != null) && rowData.isCompoundRow()) {
            createCompoundRowButton.setDraw(true);
            createAttributeRowButton.setDraw(true);
        }
        else {
            createCompoundRowButton.setDraw(false);
            createAttributeRowButton.setDraw(false);
        }

        /**
         * Add the panel that holds the -/+/++ buttons to
         * the far right side of this row.
         * If there is no other widget that will fill
         * the extra space in the row, tell the GridBagLayout
         * manager that the buttonPanel will do it.
         */
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = gridx++;
        gc.anchor = GridBagConstraints.EAST;
        if (someWidgetFillingEmptySpace == false)
            gc.weightx = 1;
        add(buttonPanel, gc);
    }


    private void layoutRootRow() {

        GridBagConstraints gc;


        /**
         * The first/topmost row always has the
         * Class Under Qualification comboBox and the
         * Collection Operator comboBox, (and only those comboBoxes).
         */

        gc = new GridBagConstraints();
        gc.gridx = gridx++;
        gc.insets = LEFT_INSETS;
        add(getComboBox(0), gc);

        gc = new GridBagConstraints();
        gc.gridx = gridx++;
        gc.insets = LEFT_INSETS;
        add(getComboBox(1), gc);

        gc = new GridBagConstraints();
        gc.gridx = gridx++;
        gc.weightx = 1;
        someWidgetFillingEmptySpace = true;
        gc.anchor = GridBagConstraints.WEST;
        add(ofTheFollowingLabel, gc);

        /**
         * This is the first row, so it has two comboBoxes.
         * The leftmost comboBox contains the list of possible choices
         * for the Class Under Qualification.  The comboBox on the
         * right contains the Any/All/None CollectionOperator.
         *
         * TODO:  Create the comboBox models only once and reuse them?
         * Create a cache of them?
         */
        
        ClassDescription[] values =
            DataModel.getInstance().getPossibleCUQs().
            toArray(new ClassDescription[0]);

        setComboBoxModel(getComboBox(0), values,
                         rowData.getClassUnderQualification());

        /**
         * Now set the model and selected value of the 
         * Collection Operator combobox.
         */
        setComboBoxModel(getComboBox(1), CollectionOperator.
                         getCompoundCollectionOperators(),
                         rowData.getRootRow().getCollectionOperator());
    }


    /*
    private void removeComboBoxesAfterIndex(int index) {

        for (index++; index < comboBoxes.length; index++) {
            remove(comboBoxes[index]);
        }
    }
    */


    /**
     * A "simple" compound row is a row that
     * only contains a Collection Operator comboBox,
     * and the -, +, ++ buttons on the right side.
     */
    private void layoutSimpleCompoundRow() {

        GridBagConstraints gc;


        gc = new GridBagConstraints();
        gc.gridx = gridx++;
        gc.insets = LEFT_INSETS;
        add(getComboBox(0), gc);

        gc = new GridBagConstraints();
        gc.gridx = gridx++;
        gc.weightx = 1;
        someWidgetFillingEmptySpace = true;
        gc.anchor = GridBagConstraints.WEST;
        add(ofTheFollowingLabel, gc);

        /**
         * This is a "simple" Compound Row.  I.e. it only has
         * the Collection Operator comboBox in it.
         * Set the comboBox model.
         */
        setComboBoxModel(getComboBox(0), CollectionOperator.
                         getCompoundCollectionOperators(),
                         rowData.getCollectionOperator());
    }


    /**
     * This is an Attribute Row that contains one
     * or more comboBoxes for selecting attributes,
     * possibly a Collection Operator comboBox,
     * possibly a true/false comboBox, possibly
     * an Attribute Operator (==, !=, <, >, ...) comboBox,
     * or any number of other widgets.  It also contains
     * the +, ++, - buttons.
     */
    private void layoutAttributeRow() {

        GridBagConstraints gc;

        //ArrayList<Attribute> attributes = rowData.getAttributePath();
        //System.out.println("Add comboBoxes for: "+rowData.getRowString());

        /**
         * We are an Attribute Row, so the widgets we contain
         * are based on the values in this row's RowData object.
         *
         * Add one comboBox for every Attribute on this row's
         * attributePath.  For example, this row would 
         * cause this loop to add two comboBoxes:
         *
         *      epochGroup.source isNull
         */
        //int comboBoxIndex = 0;
        //for (Attribute attribute : attributes) {
        int index;
        for (index = 0; index < rowData.getAttributeCount(); index++) {

            //System.out.println("Adding comboBox at gridx "+gridx);
            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.insets = LEFT_INSETS;
            add(getComboBox(index), gc);

            if (index == 0) {
                /**
                 * Set the model and selected item of the leftmost
                 * comboBox.  The leftmost comboBox is filled with
                 * the attributes of the parentClass.  I.e. the
                 * class of its parent row.
                 * Also set the selected item in the comboBox.
                 */
                ClassDescription parentClass = rowData.getParentClass();
                setComboBoxModel(getComboBox(index), parentClass,
                                 false, rowData.getAttribute(index));
            }
            else {
                /**
                 * This is NOT the leftmost comboBox.
                 * Each comboBox is filled with the attributes of
                 * the class of the comboBox to its left.
                 * Also set the selected item in the comboBox.
                 */
                Attribute att = rowData.getAttribute(index-1);
                setComboBoxModel(getComboBox(index),
                                 att.getClassDescription(), true,
                                 rowData.getAttribute(index));
            }
        }

        /**
         * We have inserted comboBoxes for every Attribute on this
         * RowData's attributePath.  Now insert any other widgets
         * that are needed  based on what the childmost (i.e. rightmost)
         * attribute is in this row.
         */

        Attribute rightmostAttribute = rowData.getChildmostAttribute();
        if (rightmostAttribute == null) {
            System.err.println("ERROR: rightmostAttribute == null\n"+
                "This probably means the rowData for this row was not\n"+
                "properly set up or initialized.  If this is the very\n"+
                "first row in the tree, you probably should be calling\n"+
                "RowData.createRootRow() to create it.");
            return;
        }

        //System.out.println("rightmostAttribute = "+
        //    rightmostAttribute.toStringDebug());
        if (!rightmostAttribute.isPrimitive() &&
            !rightmostAttribute.isSpecial() &&
            (rightmostAttribute.getType() != Type.PER_USER_PARAMETERS_MAP) &&
            (rightmostAttribute.getType() != Type.PARAMETERS_MAP) &&
            (rowData.getCollectionOperator() == null)) {
            /**
             * The rightmost Attribute is a class, as opposed
             * to a "primitive" type such as int, float, string,
             * so we need to display another comboBox to its right
             * that the user can use to choose an Attribute of
             * that class or choose a special item such as "is null",
             * "is not null", "Any Property", "My Property".
             */
            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.insets = LEFT_INSETS;
            add(getComboBox(index++), gc);

            /**
             * Set the comboBox model to hold attributes of
             * the class that is selected in the comboBox to our left.
             */
            setComboBoxModel(getComboBox(rowData.getAttributeCount()),
                             rightmostAttribute.getClassDescription(),
                             true, Attribute.SELECT_ATTRIBUTE);
        }
        else if (rightmostAttribute.isPrimitive()) {

            //System.out.println("Rightmost attribute is a primitive type.");
            /**
             * The rightmost Attribute is a primitive Attribute
             * such as an int, float, string, date/time, so now place the
             * comboBox that will hold operators such
             * as ==, !=, >, is true.
             */
            //System.out.println("Adding operator comboBox at gridx "+gridx);
            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.insets = LEFT_INSETS;
            add(getComboBox(index++), gc);

            /**
             * Now add the widget the user can use to edit the
             * value.  E.g. a text field or a time/date picker.
             */

            if (rightmostAttribute.getType() == Type.DATE_TIME) {
                gc = new GridBagConstraints();
                gc.gridx = gridx++;
                gc.fill = GridBagConstraints.BOTH;
                gc.insets = LEFT_INSETS;
                add(dateTimePicker, gc);
            }
            else if (rightmostAttribute.getType() == Type.INT_16) {
                gc = new GridBagConstraints();
                gc.gridx = gridx++;
                gc.weightx = 0.1;
                gc.fill = GridBagConstraints.BOTH;
                gc.insets = LEFT_INSETS;
                add(valueSpinnerInt16, gc);
            }
            else if (rightmostAttribute.getType() == Type.INT_32) {
                gc = new GridBagConstraints();
                gc.gridx = gridx++;
                gc.weightx = 0.1;
                gc.fill = GridBagConstraints.BOTH;
                gc.insets = LEFT_INSETS;
                add(valueSpinnerInt32, gc);
            }
            else if (rightmostAttribute.getType() != Type.BOOLEAN) {
                /**
                 * Place a text field into which the user can enter an
                 * attribute value of some sort.
                 */
                //System.out.println("Adding text field at gridx "+gridx);
                gc = new GridBagConstraints();
                gc.gridx = gridx++;
                gc.weightx = 1;
                someWidgetFillingEmptySpace = true;
                gc.fill = GridBagConstraints.BOTH;
                gc.insets = LEFT_INSETS;
                add(valueTextField, gc);
            }

            /**
             * Set the comboBox model to hold operators appropriate
             * for the Type (int, string, float, boolean) of the
             * Attribute.
             */
            int widgetIndex = rowData.getAttributeCount();
            if (rightmostAttribute.getType() == Type.BOOLEAN) {
                getComboBox(widgetIndex).setModel(
                    new DefaultComboBoxModel(DataModel.OPERATORS_BOOLEAN));
            }
            else if (rightmostAttribute.getType() == Type.UTF_8_STRING) {
                getComboBox(widgetIndex).setModel(
                    new DefaultComboBoxModel(
                        DataModel.OPERATORS_STRING));
            }
            else {
                getComboBox(widgetIndex).setModel(
                    new DefaultComboBoxModel(
                        DataModel.OPERATORS_ARITHMATIC));
            }

            /*
            System.out.println("rightmostAttribute.getType() = "+
                               rightmostAttribute.getType());
            System.out.println("rowData.getAttributeValue() = "+
                               rowData.getAttributeValue());
            */

            /**
             * Set the selected value.
             */
            if (rightmostAttribute.getType() == Type.BOOLEAN) {
                if (DataModel.OPERATOR_TRUE.equals(
                    rowData.getAttributeOperator())) {
                    getComboBox(widgetIndex).setSelectedItem(
                        DataModel.OPERATOR_TRUE);
                }
                else {
                    getComboBox(widgetIndex).setSelectedItem(
                        DataModel.OPERATOR_FALSE);
                }
            }
            else if (rightmostAttribute.getType() == Type.DATE_TIME) {

                getComboBox(widgetIndex).setSelectedItem(
                    rowData.getAttributeOperator());
                Date attributeValue = (Date)rowData.getAttributeValue();
                if (attributeValue == null)
                    attributeValue = new Date();
                dateTimePicker.setDate(attributeValue);
            }
            else if (rightmostAttribute.getType() == Type.INT_16) {
                getComboBox(widgetIndex).setSelectedItem(
                    rowData.getAttributeOperator());
                Object attributeValue = rowData.getAttributeValue();
                if ((attributeValue == null) ||
                    attributeValue.toString().isEmpty())
                    attributeValue = new Short((short)0);
                else
                    attributeValue = new Short(attributeValue.toString());
                valueSpinnerInt16.setValue(attributeValue);
            }
            else if (rightmostAttribute.getType() == Type.INT_32) {
                getComboBox(widgetIndex).setSelectedItem(
                    rowData.getAttributeOperator());
                Object attributeValue = rowData.getAttributeValue();
                if ((attributeValue == null) ||
                    attributeValue.toString().isEmpty())
                    attributeValue = new Integer(0);
                else
                    attributeValue = new Integer(attributeValue.toString());
                valueSpinnerInt16.setValue(attributeValue);
            }
            else {
                getComboBox(widgetIndex).setSelectedItem(
                    rowData.getAttributeOperator());
                Object attributeValue = rowData.getAttributeValue();
                if (attributeValue == null)
                    attributeValue = "";
                valueTextField.setText(attributeValue.toString());
            }
        }
        else if ((rightmostAttribute.getType() ==
                  Type.PER_USER_PARAMETERS_MAP) ||
                 (rightmostAttribute.getType() == Type.PARAMETERS_MAP)) {

            //System.out.println("Rightmost attribute is "+
            //    "\"My/Any Property\" or PARAMETERS_MAP");

            /**
             * The rightmost attribute is either "My Property" or
             * "Any Property", or is of Type.PARAMETERS_MAP,
             * so add the widgets that are to the right of that
             * attribute.  For example, for a row that looks like this: 
             *
             *      epochGroup.source.My Property.animalID string == X123
             *
             * or this:
             *
             *      protocolParameters.stimulusFrequency int == 5
             *
             * For the first example, we would need to add a text
             * field where the user can enter "animalID",
             * a comboBox where the user can select the type of the value
             * (e.g. int, string, float, boolean),
             * a comboBox to select the operator (e.g. ==, !=, >),
             * and a text field where the user can enter a value.
             * Note that for boolean types, we would not have a
             * value text field, but instead the operator comboBox would
             * let the user choose, "is true" or "is false".
             */

            /** 
             * Add the propNameTextField where the user can enter
             * the "keyed" property name.  "animalID" in the example
             * in the comments above.
             */
            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.BOTH;
            someWidgetFillingEmptySpace = true;
            gc.insets = LEFT_INSETS;
            add(propNameTextField, gc);
            
            /** 
             * Add the propTypeComboBox where the user can select the
             * the type of the "keyed" property.  "string" or "int" in
             * the example in the comments above.
             */
            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.insets = LEFT_INSETS;
            add(propTypeComboBox, gc);

            /** 
             * Add the attributeOperatorComboBox where the user can
             * select the operator for the "keyed" property.  "==" in the
             * example in the comments above.
             *
             * Later, other code will set the model of this comboBox
             * depending on the selected value in the propTypeComboBox.
             */
            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.insets = LEFT_INSETS;
            add(operatorComboBox, gc);

            /**
             * Set the model of the operatorComboBox depending on the
             * selected value in the propTypeComboBox.
             */
            if ((rowData.getPropType() == Type.INT_32) ||
                (rowData.getPropType() == Type.FLOAT_64) ||
                (rowData.getPropType() == Type.DATE_TIME)) {
                operatorComboBox.setModel(new DefaultComboBoxModel(
                    DataModel.OPERATORS_ARITHMATIC));
            }
            else if (rowData.getPropType() == Type.UTF_8_STRING) {
                operatorComboBox.setModel(new DefaultComboBoxModel(
                    DataModel.OPERATORS_STRING));
            }
            else if (rowData.getPropType() == Type.BOOLEAN) {
                operatorComboBox.setModel(new DefaultComboBoxModel(
                    DataModel.OPERATORS_BOOLEAN));
            }

            if (rowData.getPropType() == Type.DATE_TIME) {
                
                /** 
                 * Add the dateTimePicker where the user can enter the
                 * the value of the "keyed" property. 
                 */
                gc = new GridBagConstraints();
                gc.gridx = gridx++;
                gc.fill = GridBagConstraints.BOTH;
                gc.insets = LEFT_INSETS;
                add(dateTimePicker, gc);
            }
            else if (rowData.getPropType() == Type.INT_32) {
                gc = new GridBagConstraints();
                gc.gridx = gridx++;
                gc.weightx = 0.1;
                gc.fill = GridBagConstraints.BOTH;
                gc.insets = LEFT_INSETS;
                add(valueSpinnerInt32, gc);
            }
            else if (rowData.getPropType() != Type.BOOLEAN) {

                /** 
                 * Add the valueTextField where the user can enter the
                 * the value of the "keyed" property.  "x123" in the
                 * example in the comments above.
                 */
                gc = new GridBagConstraints();
                gc.gridx = gridx++;
                gc.weightx = 1;
                gc.fill = GridBagConstraints.BOTH;
                gc.insets = LEFT_INSETS;
                add(valueTextField, gc);
            }

            propNameTextField.setText(rowData.getPropName());
            propTypeComboBox.setSelectedItem(rowData.getPropType());
            operatorComboBox.setSelectedItem(
                rowData.getAttributeOperator());
            if (rowData.getPropType() == Type.INT_32) {

                int value = 0;
                try {
                    value = Integer.parseInt(rowData.getAttributeValue().
                        toString());
                }
                catch(Exception e) {
                }
                valueSpinnerInt32.setValue(value);
            }
            else if ((rowData.getPropType() == Type.FLOAT_64) ||
                     (rowData.getPropType() == Type.UTF_8_STRING)) {
                
                if (rowData.getAttributeValue() != null)
                    valueTextField.setText(
                        rowData.getAttributeValue().toString());
                else
                    valueTextField.setText("");
            }
            else if (rowData.getPropType() == Type.DATE_TIME) {
                Date attributeValue = (Date)rowData.getAttributeValue();
                if (attributeValue == null)
                    attributeValue = new Date();
                dateTimePicker.setDate(attributeValue);
            }
            else if (rowData.getPropType() == Type.BOOLEAN) {
                /**
                 * No valueTextField is displayed in this case because
                 * the operatorComboBox serves that function.
                 * I.e.  "is true" and "is false" is both an operator
                 * and a "value".
                 */
            }
        }
        else if (rowData.getCollectionOperator() ==
                 CollectionOperator.COUNT) {

            /**
             * This row says something like:
             *
             *      epochGroups.epochs Count == 5
             */

            /** 
             * Add comboBox for the Collection Operator which
             * will display the value "Count".
             */
            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.insets = LEFT_INSETS;
            add(getComboBox(index++), gc);

            /** 
             * Add comboBox for the Attribute Operator which
             * will display a value like ==,!=, <, etc.
             */
            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.insets = LEFT_INSETS;
            add(operatorComboBox, gc);

            /** 
             * Add count spinner.
             */
            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.fill = GridBagConstraints.BOTH;
            gc.insets = LEFT_INSETS;
            add(countSpinnerInt32, gc);
        }
        else if (rowData.getCollectionOperator() != null) {

            /**
             * This row says something like:
             *
             *      epochGroups.epochs Any of the following
             */

            /** 
             * Add comboBox for the Collection Operator.
             */
            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.insets = LEFT_INSETS;
            add(getComboBox(index++), gc);
        }
        else {
            /**
             * The last Attribute on the right is a primitive
             * or it is the special "Select Attribute" Attribute.
             * So we don't need any more comboBoxes to the right
             * of the last one in this row.
             */
        }

        if (rowData.isCompoundRow()) {

            gc = new GridBagConstraints();
            gc.gridx = gridx++;
            gc.weightx = 1;
            someWidgetFillingEmptySpace = true;
            gc.anchor = GridBagConstraints.WEST;
            add(ofTheFollowingLabel, gc);
        }

        /**
         * By this point, all the models and values of the comboBoxes
         * that correspond to Attributes on this RowData's attributePath
         * have been set if the row does not end in an Attribute
         * that is of a TO_MANY relationship.  E.g. if the row does not
         * look like one of these examples:
         *
         *      epochGroup.epochs Count == 5
         *      epochGroup.epochs All of the following
         *
         * If it does end in a TO_MANY relationship, initialize
         * the last two or three comboBox model(s) and value(s).
         * E.g. the row ends in either:
         *
         *      (collection operator) (arithmatic operator) (text field)
         *      (collection operator) ("of the following" label)
         */
        Attribute childmostAttribute = rowData.getChildmostAttribute();
        if (childmostAttribute.getCardinality() == Cardinality.TO_MANY) {

            /**
             * The item selected in the "childmost" (i.e. last)
             * Attribute in this RowData's attributePath is an
             * Attribute that has a to-many relationship with the
             * class that contains it.  For example, a row like
             * one of these:
             *
             *      epochGroup.epochs Count == 5
             *      resources Count == 27
             *      resources Any
             *      resources None
             *
             * So, there is at least a comboBox
             * to the right of the attribute comboBox that the user
             * can use to select the Collection Operator to use:
             * Any, All, None, Count.
             *
             * Set that comboBox's model to the list of all the
             * Collection Operators: Any, All, None, Count.
             */
            int widgetIndex = rowData.getAttributeCount();
            DefaultComboBoxModel model = new DefaultComboBoxModel(
                CollectionOperator.values());
            getComboBox(widgetIndex).setModel(model);

            /**
             * Set the value of the Collection Operator comboBox
             * to be this row's value.
             */
            getComboBox(widgetIndex).setSelectedItem(
                rowData.getCollectionOperator());
            widgetIndex++;

            /**
             * Now set the value of the operatorComboBox and spinner if the
             * collection operator is currently set to Count.
             */
            if (rowData.getCollectionOperator() ==
                CollectionOperator.COUNT) {

                /**
                 * This row is something like:
                 *
                 *      epochGroup.epochs Count == 5
                 *
                 * Set the operator that is used for the Count.
                 * E.g. ==, >, <=
                 */
                operatorComboBox.setModel(
                    new DefaultComboBoxModel(
                        DataModel.OPERATORS_ARITHMATIC));
                operatorComboBox.setSelectedItem(
                    rowData.getAttributeOperator());
                /*TODO: delete this
                comboBoxes[widgetIndex].setModel(
                    new DefaultComboBoxModel(
                        DataModel.OPERATORS_ARITHMATIC));

                comboBoxes[widgetIndex].setSelectedItem(
                    rowData.getAttributeOperator());
                widgetIndex++;
                */

                /**
                 * Set the value in the spinner.
                 */
                Object attributeValue = rowData.getAttributeValue();
                int intValue = 0;
                if ((attributeValue != null) &&
                    (attributeValue instanceof Integer)) {
                    intValue = ((Integer)attributeValue).intValue();
                }
                countSpinnerInt32.setValue(intValue);
            }
        }
    }


    /**
     * We are required to implement this method because we are
     * a DocumentListener.
     */
    @Override
    public void insertUpdate(DocumentEvent event) {
        textFieldChanged(event.getDocument());
    }


    /**
     * We are required to implement this method because we are
     * a DocumentListener.
     */
    @Override
    public void removeUpdate(DocumentEvent event) {
        textFieldChanged(event.getDocument());
    }


    /**
     * We are required to implement this method because we are
     * a DocumentListener.
     */
    @Override
    public void changedUpdate(DocumentEvent event) {
    }


    /**
     * This is called when the value in any of our JSpinners change.
     */
    @Override
    public void stateChanged(ChangeEvent event) {

        Object value = ((JSpinner)event.getSource()).getValue();
        rowData.setAttributeValue(value);
        //rowData.setAttributeValueUsingString(value.toString());
    }


    /**
     * This is called when the text field changes.  E.g. as the user
     * types into it.
     */
    private void textFieldChanged(Document document) {

        if (document == valueTextField.getDocument())
            //rowData.setAttributeValue(valueTextField.getText());
            rowData.setAttributeValueUsingString(valueTextField.getText());
        else if (document == propNameTextField.getDocument())
            rowData.setPropName(propNameTextField.getText());
    }


    /**
     * This is a quick and dirty way to put a line between rows.
     * A more proper solution is to create a Border subclass that
     * draws the line.
     *
     * All this method does is call our superclass's normal paint()
     * method and then draws a line at the bottom of this panel.
     *
     * If/when we decide whether we want to have lines between rows
     * and/or do "zebra" striping of rows, cleanup or remove this
     * code.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.drawLine(0, getHeight()-1, getWidth()-1, getHeight()-1);
    }
}
