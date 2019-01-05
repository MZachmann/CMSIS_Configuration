/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helmpcb.cmsisconfig;

import static com.helmpcb.cmsisconfig.Node.fxfont;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

/**
 *
 * @author Amr Bekhit
 */
class EscapeNode
        extends Node {

    public EscapeNode(String tag, String text) {
        super(tag, text);
        this.allowsChildren = false;
    }
}

class HeadingNode
        extends Node {
    // <h>

    public HeadingNode(String tag, String text) {
        super(tag, text);
    }
}

class HeadingWithEnable
        extends InputNode
        implements ItemListener {
    // <e>

    public HeadingWithEnable(String tag, String text) throws NodeException {
        super(tag, text);
        skipValue += Node.NumericTargets.size();
        generateControl();
    }

    private void generateControl() {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.addItemListener(this);
        control = checkBox;
    }
    
    @Override
    public JComponent getControl() {
        JCheckBox checkBox = (JCheckBox) control;

        if (Node.NumericTargets.get(skipValue).getValue(startBit) == 0) {
            checkBox.setSelected(false);
        } else {
            checkBox.setSelected(true);
        }

        return checkBox;
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        // The checkbox state has changed, so update the target value
        JCheckBox checkBox = (JCheckBox) control;

        if (checkBox.isSelected()) {
            Node.NumericTargets.get(skipValue).setValue(1, startBit);
        } else {
            Node.NumericTargets.get(skipValue).setValue(0, startBit);
        }
    }
    
    @Override
    public void doRender(DefaultTreeCellRenderer drawer, boolean isSelected, boolean hasFocus)
    {
        long nodeVal = Node.NumericTargets.get(skipValue).getValue(startBit);
        if(!hasValue())
        {
            drawer.setForeground(new Color(144, 144, 144));
        }
        else if(nodeVal == 0)
        {
            drawer.setForeground(new Color(120, 60, 0));
        }
        else
        {
            drawer.setForeground(new Color(0, 120, 0));
        }
        drawer.setFont(isSelected ? fxbold : fxfont);
        drawer.setText(toString());
    }
}

class OptionWithCheckbox
        extends InputNode
        implements ItemListener {
    // <q>

    public OptionWithCheckbox(String tag, String text) throws NodeException {
        super(tag, text);
        this.allowsChildren = false;
        skipValue += Node.NumericTargets.size();
        generateControl();
    }

    private void generateControl() {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.addItemListener(this);
        control = checkBox;
    }

    @Override
    public JComponent getControl() {
        JCheckBox checkBox = (JCheckBox) control;

        if (Node.NumericTargets.get(skipValue).getValue(startBit) == 0) {
            checkBox.setSelected(false);
        } else {
            checkBox.setSelected(true);
        }

        return checkBox;
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        // The checkbox state has changed, so update the target value
        JCheckBox checkBox = (JCheckBox) control;

        if (checkBox.isSelected()) {
            Node.NumericTargets.get(skipValue).setValue(1, startBit);
        } else {
            Node.NumericTargets.get(skipValue).setValue(0, startBit);
        }
    }
    
        @Override
    public void doRender(DefaultTreeCellRenderer drawer, boolean isSelected, boolean hasFocus)
    {
        drawer.setFont(isSelected ? fxbold : fxfont);
        if(!hasValue())
        {
            drawer.setForeground(new Color(144, 144, 144));
        }
        else if(Node.NumericTargets.get(skipValue).getValue(startBit) == 0)
        {
            drawer.setForeground(new Color(160, 60, 0));
        }
        else
        {
            drawer.setForeground(new Color(0, 160, 0));
        }
        drawer.setText(toString());
    }
}

class StringOption
        extends InputNode {
    // <s>

    public StringOption(String tag, String text) throws NodeException {
        super(tag, text);
        this.allowsChildren = false;
        skipValue += Node.StringTargets.size();
        generateControl();
    }

    public int getMaxStringSize() {
        return startBit;
    }

    private void generateControl() {
        JTextField textField = new JTextField(40);
        textField.getDocument().addDocumentListener(new TextFieldDocumentListener());
        control = textField;
    }

    @Override
    public JComponent getControl() {
        JTextField textField = (JTextField) control;
        textField.setText(Node.StringTargets.get(skipValue).getStringValue());
        return control;
    }
    
    @Override
    public void doRender(DefaultTreeCellRenderer drawer, boolean isSelected, boolean hasFocus)
    {
        drawer.setForeground(Color.BLACK);
        drawer.setFont(fxfont);
        drawer.setText(toString() + "'" + Node.StringTargets.get(skipValue).getStringValue() + "'");
    }


    private class TextFieldDocumentListener
            implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateStringTarget();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateStringTarget();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
        }

        private void updateStringTarget() {
            JTextField textField = (JTextField) control;
            Node.StringTargets.get(skipValue).setValue(textField.getText());
        }
    }
}

class NumericOption
        extends ModifyableInputNode
        implements ItemListener, ChangeListener {
    // <o>

    private int endBit;
    private ArrayList<SelectionValueModifier> SelectionValues = new ArrayList<SelectionValueModifier>();
    private NumericRangeModifier numericRangeModifier;
    private ArithmeticModifier arithmeticModifier = new ArithmeticModifier("<#*1>", "");

    public NumericOption(String tag, String text) throws NodeException {
        super(tag, text);
        this.allowsChildren = false;
        skipValue += Node.NumericTargets.size();
        endBit = tagInfo.getEndBit();
        // Check to see if the end bit is valid
        if (endBit != -1 && endBit <= startBit) {
            // Invalid end bit
            throw new NodeException("End bit cannot be smaller than start bit: " + tag);
        }
    }

    @Override
    public void doRender(DefaultTreeCellRenderer drawer, boolean isSelected, boolean hasFocus)
    {
        drawer.setForeground(Color.BLACK);
        drawer.setFont(isSelected ? fxbold : fxfont);
        if(arithmeticModifier != null)
        {
            long value = arithmeticModifier.getModifiedValueFromTarget(
                            Node.NumericTargets.get(skipValue).getValue(startBit, endBit));
            String numval;
            if(SelectionValues.size() > 0)
            {
                int idx = findMatchingSelectionValueModifier(value);
                if( idx >= 0 && idx < SelectionValues.size())
                {
                    numval = '*' + SelectionValues.get(idx).text; // indicate match
                }
                else
                    numval = Long.toString(value);
            }
            else
                numval = Long.toString(value);
               
            drawer.setText(toString() + '[' + numval + ']');
        }
        else
        {
            drawer.setText(toString());
        }
    }

    @Override
    public void addModifier(ModifierNode modifier) throws NodeException {
        // Only add arithmetic and range modifiers if this node
        // does not have selection values
        if (modifier instanceof ArithmeticModifier) {
            arithmeticModifier = (ArithmeticModifier) modifier;
        } else if (modifier instanceof NumericRangeModifier) {
            // Check to see if it's valid to add this modifier to this node
            if (SelectionValues.size() > 0) {
                // We can't add this modifier to this node as the node already
                // has some selection values
                throw new NodeException("Cannot add numeric range modifier '" + modifier.tag
                        + "' to numeric option '" + this.tag + "' with selection values.");
            } else {
                // Add the modifier to this node
                numericRangeModifier = (NumericRangeModifier) modifier;
            }
        } else if (modifier instanceof SelectionValueModifier) {
            // Check to see if it's valid to add this selection modifier to this 
            // node:
            if (numericRangeModifier != null) {
                // We've tried to add a selection value modifier to a node which
                // alredy has a numeric range
                throw new NodeException("Cannot add selection value '" + modifier.tag
                        + ":" + modifier.text + "' to a node with a numeric range modifier.");
            } else {
                // Add this selection modifier to the list
                SelectionValues.add((SelectionValueModifier) modifier);
            }
        } else {
            // Unknown modifier type
            throw new NodeException("Unknown modifier " + modifier.tag);
        }
    }

    private void generateControl() {
        // Choose which type of control to generate depending on the contents
        // of this node
        if (SelectionValues.size() > 0) {
            // We need to create a combo box
            JComboBox<SelectionValueModifier> comboBox =
                    new JComboBox<SelectionValueModifier>(
                    SelectionValues.toArray(new SelectionValueModifier[0]));
            comboBox.addItemListener(this);
            control = comboBox;
        } else {
            // We need to create a spin box or a checkbox
            if (startBit >= 0 && endBit < 0) {
                // This node's target is only 1 bit, so create a checkbox
                JCheckBox checkBox = new JCheckBox(text);
                checkBox.addItemListener(this);
                control = checkBox;
            } else {
                // This node's target is several bits wide, so create a spinner
                Long initialValue, maxValue, minValue, stepValue;

                if (numericRangeModifier != null) {
                    // A numeric range is specified
                    minValue = numericRangeModifier.getRangeStart();
                    maxValue = numericRangeModifier.getRangeStop();
                    stepValue = numericRangeModifier.getRangeStep();
                } else {
                    // No numeric range specified, so use defaults
                    minValue = 0L;
                    maxValue = Integer.MAX_VALUE * 2L + 1L;
                    stepValue = 1L;
                }

                initialValue = arithmeticModifier.getModifiedValueFromTarget(
                        Node.NumericTargets.get(skipValue).getValue(startBit, endBit));

                // Check to see that the initial value lies within the max and min values
                if (initialValue < minValue) {
                    initialValue = minValue;
                } else if (initialValue > maxValue) {
                    initialValue = maxValue;
                }
                
                SpinnerModel model = new SpinnerNumberModel(
                        initialValue, minValue, maxValue, stepValue);

                JSpinner spinner = new JSpinner(model);
                spinner.addChangeListener(this);
                JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
                editor.getTextField().setColumns(15);
                
                // See if we need the spinner to display numbers in hexadecimal 
                // format
                if (numericRangeModifier == null || numericRangeModifier.isHexadecimal()) {
                    // Configure the spinner to use a hex formatter
                    editor.getTextField().setFormatterFactory(new HexFormatterFactory());
                }

                //e.getTextField().set
                control = spinner;
            }
        }
    }

    @Override
    public JComponent getControl() {
        if (control == null) {
            generateControl();
        }

        // Update the values of controls
        updateControl();

        return control;
    }

    private void updateControl() {
        long modifiedValue = arithmeticModifier.getModifiedValueFromTarget(
                Node.NumericTargets.get(skipValue).getValue(startBit, endBit));

        if (control instanceof JComboBox) {
            // Set the selected item in the combo box
            JComboBox<SelectionValueModifier> comboBox =
                    (JComboBox<SelectionValueModifier>) control;
            // Find the item in the combo box that has the same value as the target one
            int matchingIndex = findMatchingSelectionValueModifier(modifiedValue);
            if (matchingIndex >= 0) {
                // We found a matching item, so select it
                comboBox.setSelectedIndex(matchingIndex);
            } else {
                // The target contains a value that is not in the list
                // Change its background to pink to alert the user
                comboBox.setBackground(Color.pink);
                comboBox.setSelectedIndex(0);
            }
        } else if (control instanceof JSpinner) {
            // Set the value of the spinner
            JSpinner spinner = (JSpinner) control;
            SpinnerNumberModel model = (SpinnerNumberModel)spinner.getModel();
            // Make sure we're not outside the range of the spinner
            if (modifiedValue < (Long)model.getMinimum()) {
                modifiedValue = (Long)model.getMinimum();
            } else if (modifiedValue > (Long)model.getMaximum()) {
                modifiedValue = (Long)model.getMaximum();
            }
            
            spinner.setValue(modifiedValue);
        } else if (control instanceof JCheckBox) {
            // Set the value of the checkbox
            JCheckBox checkBox = (JCheckBox) control;
            checkBox.setSelected(
                    Node.NumericTargets.get(skipValue).getValue(startBit) == 0 ? false : true);
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (control instanceof JComboBox) {
            // The user has selected a new combo box value, so update
            // the target.
            JComboBox<SelectionValueModifier> comboBox =
                    (JComboBox<SelectionValueModifier>) control;
            SelectionValueModifier selectedValue = (SelectionValueModifier) comboBox.getSelectedItem();
            Node.NumericTargets.get(skipValue).setValue(
                    arithmeticModifier.getTargetFromModifiedValue(selectedValue.getValue()),
                    startBit, endBit);
        } else if (control instanceof JCheckBox) {
            // The checkbox value has changed, so update the target
            JCheckBox checkBox = (JCheckBox) control;
            if (checkBox.isSelected()) {
                Node.NumericTargets.get(skipValue).setValue(1, startBit);
            } else {
                Node.NumericTargets.get(skipValue).setValue(0, startBit);
            }
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        // The user has selected a different value for the spinner
        JSpinner spinner = (JSpinner) control;
        Node.NumericTargets.get(skipValue).setValue(
                arithmeticModifier.getTargetFromModifiedValue((Long)spinner.getValue()),
                startBit, endBit);
    }

    private int findMatchingSelectionValueModifier(long value) {
        int matchingIndex = -1;

        for (int i = 0; i < SelectionValues.size(); ++i) {
            if (SelectionValues.get(i).getValue() == value) {
                matchingIndex = i;
                break;
            }
        }

        return matchingIndex;
    }

    private class HexFormatterFactory
            extends DefaultFormatterFactory {

        @Override
        public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
            return new HexFormatter();
        }
    }

    private class HexFormatter
            extends DefaultFormatter {

        @Override
        public Object stringToValue(String text) throws ParseException {
            try {
                return Long.valueOf(text, 16);
            } catch (NumberFormatException ex) {
                throw new ParseException(text, 0);
            }
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            return Long.toHexString((Long) value).toUpperCase();
        }
    }
}

class NumericRangeModifier
        extends ModifierNode {

    // The following regular expression validates numeric range modifiers
    private static final Pattern NUMERIC_RANGE_PATTERN =
            Pattern.compile(
            "<((?:0[xX][\\da-fA-F]++)|\\d++)-((?:0[xX][\\da-fA-F]++)|\\d++)(?::((?:0[xX][\\da-fA-F]++)|\\d++))?>");
    private long rangeStart;
    private long rangeStop;
    private long rangeStep;

    public NumericRangeModifier(String tag, String text) throws NodeException {
        super(tag, text);
        extractTagInfo();
    }

    private void extractTagInfo() throws NodeException {
        Matcher matcher = NUMERIC_RANGE_PATTERN.matcher(tag);

        if (matcher.find()) {
            // This tag is in the correct format. Extract the different parts
            try {
                rangeStart = ParseNumber(matcher.group(1));
                rangeStop = ParseNumber(matcher.group(2));
                if (matcher.group(3) != null) {
                    // A step value is specified
                    rangeStep = ParseNumber(matcher.group(3));
                } else {
                    // No step value was specified, so assume a step value of 1
                    rangeStep = 1;
                }
            } catch (NumberFormatException ex) {
                // We failed to convert one of the fields in the modifier to a number
                throw new NodeException("Invalid field in modifier: " + tag);
            }
        } else {
            // The tag was not in the expected format
            throw new NodeException("Tag not in expected format: " + tag);
        }
    }

    public long getRangeStart() {
        return rangeStart;
    }

    public long getRangeStop() {
        return rangeStop;
    }

    public long getRangeStep() {
        return rangeStep;
    }
}

class SelectionValueModifier
        extends ModifierNode {
    // <N=xxxx>

    private static final Pattern SELECTION_PATTERN = Pattern.compile(
            "<((?:0[xX][\\da-fA-F]++)|\\d++)=>");
    private long value;

    public SelectionValueModifier(String tag, String text) throws NodeException {
        super(tag, text);
        extractTagInfo();
    }

    private void extractTagInfo() throws NodeException {
        Matcher matcher = SELECTION_PATTERN.matcher(tag);

        if (matcher.find()) {
            // Extract the value
            try {
                value = ParseNumber(matcher.group(1));
                //value = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ex) {
                // The tag does not have a valid number
                throw new NodeException("Invalid number in modifier: " + tag);
            }
        } else {
            // The tag is not in the expected format
            throw new NodeException("Tag not in expected format: " + tag);
        }
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return text;
    }
}

class ArithmeticModifier
        extends ModifierNode {
    // <#[+-/*]NN>

    private static final Pattern ARITHMETIC_PATTERN = Pattern.compile(
            "<#([-/+*])((?:0[xX][\\da-fA-F]++)|\\d++)>");
    private char operation;
    private long operationValue;

    public ArithmeticModifier(String tag, String text) throws NodeException {
        super(tag, text);
        extractTagInfo();
    }

    private void extractTagInfo() throws NodeException {
        Matcher matcher = ARITHMETIC_PATTERN.matcher(tag);

        if (matcher.find()) {
            // Extract the value
            operation = matcher.group(1).charAt(0);
            try {
                operationValue = ParseNumber(matcher.group(2));
            } catch (NumberFormatException ex) {
                // The tag does not have a valid number
                throw new NodeException("Invalid field in modifier: " + tag);
            }
        } else {
            // The tag is not in the expected format
            throw new NodeException("Tag not in expected format: " + tag);
        }
    }

    public long getModifiedValueFromTarget(long targetValue) {
        long modifiedValue;

        switch (operation) {
            case '+':
                modifiedValue = targetValue - operationValue;
                break;
            case '-':
                modifiedValue = targetValue + operationValue;
                break;
            case '*':
                modifiedValue = targetValue / operationValue;
                break;
            case '/':
                modifiedValue = targetValue * operationValue;
                break;
            default:
                throw new UnsupportedOperationException("Invalid operator " + operation);
        }

        return modifiedValue;
    }

    public long getTargetFromModifiedValue(long modifiedValue) {
        long targetValue;

        switch (operation) {
            case '+':
                targetValue = modifiedValue + operationValue;
                break;
            case '-':
                targetValue = modifiedValue - operationValue;
                break;
            case '*':
                targetValue = modifiedValue * operationValue;
                break;
            case '/':
                targetValue = modifiedValue / operationValue;
                break;
            default:
                throw new UnsupportedOperationException("Invalid operator " + operation);
        }

        return targetValue;
    }
}