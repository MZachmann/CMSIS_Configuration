/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helmpcb.cmsisconfig;

import java.awt.Component;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 *
 * @author Amr Bekhit
 */
abstract class Node
        extends DefaultMutableTreeNode {

    protected String tag;
    protected String text;
    protected String tooltip;
    public static ArrayList<NumericTarget> NumericTargets = new ArrayList<NumericTarget>();
    public static ArrayList<StringTarget> StringTargets = new ArrayList<StringTarget>();

    public static Node generate(String tag, String text) throws NodeException {
        // Find out which node type this is by looking at the first
        // character after the opening angle bracket
        char identifier = tag.charAt(1);
        switch (identifier) {
            case 'h':
                // This is a heading node
                return new HeadingNode(tag, text);
            case 'e':
                // This is a heading with enable node
                return new HeadingWithEnable(tag, text);
            case 'q':
                // This is an option with checkbox
                return new OptionWithCheckbox(tag, text);
            case 'o':
                // This is a numeric option
                return new NumericOption(tag, text);
            case 's':
                // This is a string option
                return new StringOption(tag, text);
            case '#':
                // This is an arithmetic modifier
                return new ArithmeticModifier(tag, text);
            case '/':
                // Thi is an escape node
                return new EscapeNode(tag, text);
            default:
                // This is either an option node, or an invalid node
                if (Character.isDigit(identifier)) {
                    // This is one of the numeric modifier nodes. Find out which
                    // type:
                    if (tag.indexOf('=') != -1) {
                        // This is a selection value modifier
                        return new SelectionValueModifier(tag, text);
                    } else {
                        // This is a numeric range modifier
                        return new NumericRangeModifier(tag, text);
                    }
                } else {
                    // This is an unknown item
                    return null;
                }
        }
    }

    public static class NodeRenderer
            extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            if (value instanceof InputNode) {
                //System.out.println(value.getClass().getName());
            }
            
            this.setText(value.toString());
            return this;

        }
    }

    public Node(String tag, String text) {
        super();
        this.tag = tag;
        this.text = text;
    }

    public void addToolTip(String tooltip) {
        this.tooltip += tooltip;
    }

    @Override
    public String toString() {
        return text;
    }
}

abstract class InputNode
        extends Node {

    protected JComponent control;
    protected int skipValue;
    protected int startBit;
    // Regex to extract the details from a node
    private static final Pattern NODE_CONTENTS_PATTERN =
            Pattern.compile("<\\w(\\d*+)(?:\\.(\\d++)(?:\\.{2}(\\d++))?)?>");

    protected class TagInfo {

        private int[] values = new int[3];

        public int getSkipValue() {
            return values[0] == -1 ? 0 : values[0];
        }

        public int getStartBit() {
            return values[1];
        }

        public int getEndBit() {
            return values[2];
        }
    }
    protected InputNode.TagInfo tagInfo;

    public InputNode(String tag, String text) throws NodeException {
        super(tag, text);
        // Parse the tag and extract any options
        tagInfo = extractTagInfo();

        skipValue = tagInfo.getSkipValue();
        startBit = tagInfo.getStartBit();
    }

    public int getSkipValue() {
        return skipValue;
    }

    private InputNode.TagInfo extractTagInfo() throws NodeException {
        InputNode.TagInfo info = new InputNode.TagInfo();
        Matcher matcher = NODE_CONTENTS_PATTERN.matcher(tag);

        if (matcher.find()) {
            // Extract all the options.
            for (int g = 1; g <= matcher.groupCount(); ++g) {
                String token = matcher.group(g);
                // Does the value exist?
                if (token == null || token.trim().isEmpty()) {
                    // No value specified
                    info.values[g - 1] = -1;
                } else {
                    // A value was specified. Try and convert it to a number
                    try {
                        info.values[g - 1] = Integer.parseInt(token);
                    } catch (NumberFormatException ex) {
                        // The value was not a valid number
                        throw new NodeException("Invalid value for item "
                                + g + " in node " + tag);
                    }
                }
            }
        } else {
            // The tag was not in the format we expected
            throw new NodeException("Node not in the expected format: " + tag);
        }

        return info;
    }

    public abstract JComponent getControl();
}

abstract class ModifyableInputNode
        extends InputNode {

    public ModifyableInputNode(String tag, String text) throws NodeException {
        super(tag, text);
    }

    public abstract void addModifier(ModifierNode modifier) throws NodeException;
}

abstract class ModifierNode
        extends Node {

    private boolean isHexadecimal = false;

    public ModifierNode(String tag, String text) {
        super(tag, text);
        this.allowsChildren = false;
    }

    protected long ParseNumber(String s) throws NumberFormatException {
        // Check to see if the number is hexadecimal
        if (s.length() > 2 && s.toLowerCase().charAt(1) == 'x') {
            // This is a hexadecimal number
            isHexadecimal = true;
            return Long.parseLong(s.substring(2), 16);
        } else {
            // This is a regular decimal number
            return Long.parseLong(s);
        }
    }

    public boolean isHexadecimal() {
        return isHexadecimal;
    }
}
