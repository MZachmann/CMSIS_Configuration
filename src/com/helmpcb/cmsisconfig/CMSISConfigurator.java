/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helmpcb.cmsisconfig;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author Amr Bekhit
 */
class CMSISConfigurator {

    //private ArrayList<String> lines = new ArrayList<String>();
    private StringBuilder sourceFile = new StringBuilder();
    private final String CONFIG_WIZARD_START_STRING = "<<< Use Configuration Wizard in Context Menu >>>";
    private final String CONFIG_WIZARD_END_STRING = "<<< end of configuration section >>>";
    public DefaultMutableTreeNode topNode;
    private String fileName;
    
    // Regular expressions to match the items of interest
    private String cCommentRegex = 
            "/\\*(?:[^*]|\\*(?!/))*+\\*/|" + // Match multi line comments
            "//.*";// Match single line comments
    private String assemblerCommentRegex = ";.*+";
    private String itemsOfInterestRegex = 
            "\"(?:[^\"\\\\\\r\\n]|\\\\.)*+\"|" + // Match string literals
            "(?:\\b|-)(?<!\\.)\\d++(?!\\.)(?:[uUlL]|[uU][lL]|[lL][uU])?\\b|" + // Match integer constants
            "\\b0[xX][\\da-fA-F]+(?:[uUlL]|[uU][lL]|[lL][uU])?\\b"; // Match hexadecimal constants

    
    private Pattern itemsOfInterestPattern;
//    private Pattern itemsOfInterestPattern = Pattern.compile(
//            "/\\*(?:[^*]|\\*(?!/))*+\\*/|" + // Match multi line comments
//            "//.*|" + // Match single line comments
//            "\"(?:[^\"\\\\\\r\\n]|\\\\.)*+\"|" + // Match string literals
//            "(?:\\b|-)(?<!\\.)\\d++(?!\\.)(?:[uUlL]|[uU][lL]|[lL][uU])?\\b|" + // Match integer constants
//            "\\b0[xX][\\da-fA-F]+(?:[uUlL]|[uU][lL]|[lL][uU])?\\b"); // Match hexadecimal constants

    public CMSISConfigurator(File file) throws FileNotFoundException, IOException, NodeException, TargetException {
        loadFile(file);
    }

    private void loadFile(File file) throws FileNotFoundException, IOException, NodeException, TargetException {
        byte[] buffer = new byte[(int) file.length()];
        BufferedInputStream r = new BufferedInputStream(new FileInputStream(file));
        // Read the whole file into the buffer
        r.read(buffer);
        r.close();

        sourceFile = new StringBuilder(new String(buffer));
        // Determine if this is an assembler or C file based on the extension
        String extension = getExtension(file.getName()).toLowerCase();
        String commentRegex;
        if (extension.equals("c") || extension.equals("h")) {
            // This is a C source file
            commentRegex = cCommentRegex;
        } else if (extension.equals("s") || extension.equals("inc")) {
            // This is an assembler file
            commentRegex = assemblerCommentRegex;
        } else {
            throw new IllegalArgumentException("File is neither an assembler or C source file (based on extension)");
        }
        
        itemsOfInterestPattern = Pattern.compile(commentRegex + "|" + itemsOfInterestRegex);
        // Now we need to go through the file and extract the node data and 
        // targets
        parseFile();

        fileName = file.getName();
    }

    public void saveFile(File file) throws IOException {
        StringBuffer outputFile = new StringBuffer();
        ArrayList<Target> targetList = new ArrayList<Target>();
        // Create a local copy of the target arrays
        targetList.addAll(Node.StringTargets);
        targetList.addAll(Node.NumericTargets);

        // Go through the original source file and run it through the regex again.
        // Iterate through all the matches, keeping count of the match number. If
        // the match number matches any of the numeric or string targets, then replace
        // the match with the target value.
        Matcher matcher = itemsOfInterestPattern.matcher(sourceFile);
        int matchNumber = 0;
        while (matcher.find()) {
            // We've found a match
            ++matchNumber;
            // Does this match number match any of the string or node targets?
            for (Target t : targetList) {
                if (t.getMatchNumber() == matchNumber) {
                    // We've found a match - replace the matched token with the 
                    // target value
                    matcher.appendReplacement(outputFile,
                            Matcher.quoteReplacement(t.toString()));
                    // Remove this target from the list
                    targetList.remove(t);
                    break;
                }
            }
        }
        matcher.appendTail(outputFile);

        // Now we just need to save the file to disk
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(outputFile.toString());
        writer.close();
    }

    private void parseFile() throws NodeException, TargetException {
        // Clear the target arrays
        Node.NumericTargets.clear();
        Node.StringTargets.clear();
        // Generate the top level node
        topNode = new DefaultMutableTreeNode(fileName);
        DefaultMutableTreeNode lastNodeInTree = topNode;
        boolean insideConfigWizardSection = false;
        boolean finishedConfigWizard = false;
        // The following regex gets all string literals, comments, and numeric constants
        // Now, go through the source file and process any tags we find
        Matcher itemsOfInterestMatcher = itemsOfInterestPattern.matcher(sourceFile);
        int matchNumber = 0;
        while (itemsOfInterestMatcher.find()) {
            // We've found a token
            ++matchNumber;
            String token = itemsOfInterestMatcher.group();
            // Find out what type it is
            switch (token.charAt(0)) {
                case '"':
                    // This is a string literal. Add it to the string targets
                    Node.StringTargets.add(new StringTarget(matchNumber, token));
                    break;
                case ';':
                case '/':
                    // This is a single- or multi-line comment. Extract all
                    // the tags we can find inside.
                    if (!finishedConfigWizard) {
                        // First, split the comment into lines
                        String[] lines = token.split("[\\r\\n]");
                        // Now, go through each line and extract any nodes that 
                        // might be inside
                        for (String line : lines) {
                            if (!insideConfigWizardSection) {
                                // We're not currently inside a configuration 
                                // wizard section. Check to see if this line starts one
                                if (line.contains(CONFIG_WIZARD_START_STRING)) {
                                    // We're now inside a config wizard section, so we'll start
                                    // extracting tags starting from the next line.
                                    insideConfigWizardSection = true;
                                }
                            } else {
                                // We're currently in a configuration wizard section.
                                // check to see if we've left it
                                if (line.contains(CONFIG_WIZARD_END_STRING)) {
                                    // We've reached the end of the configuration wizard section
                                    // so stop processing
                                    finishedConfigWizard = true;
                                    break;
                                } else {
                                    // We're still inside the configuration wizard section
                                    // so extract any nodes in this line.
                                    lastNodeInTree = extractNodes(line, lastNodeInTree);
                                }
                            }
                        }
                    }
                    break;
                default:
                    // This is a numberic target. Add it to the numeric targets
                    Node.NumericTargets.add(new NumericTarget(matchNumber, token));
                    break;
            }
        }
    }

    private DefaultMutableTreeNode extractNodes(String line, DefaultMutableTreeNode lastNodeInTree)
            throws NodeException {
        // Start going through the text in the line searching for tags

        // The following regex searches for any tags enclosed by angle brackets,
        // followed by the text after them that is not part of a tag
        Pattern tagPattern = Pattern.compile("(<[^<>\\n\\r]*>)([^<\\r\\n]*)");
        Matcher tagMatcher = tagPattern.matcher(line);

        while (tagMatcher.find()) {
            // Generate a node based on the text we found
            DefaultMutableTreeNode node =
                    Node.generate(tagMatcher.group(1), tagMatcher.group(2).trim());
            // Let's see how we place the node in the tree
            if (node != null) {
                if (node instanceof ModifierNode) {
                    // This is a modifier tag. Let's see if the last node in
                    // the tree can accept modifiers
                    if (lastNodeInTree instanceof ModifyableInputNode) {
                        ((NumericOption) lastNodeInTree).addModifier((ModifierNode) node);
                        node = lastNodeInTree;
                    } else {
                        // This modifier node is in an invalid location as
                        // the last node in the tree doesn't accept them.
                        throw new NodeException("Modifier " + tagMatcher.group(1)
                                + " has an invalid parent " + lastNodeInTree);
                    }
                } else if (node instanceof EscapeNode) {
                    // This is an escape tag, so check to see if the last node or 
                    // its parent need to be closed.
                    if (lastNodeInTree.getAllowsChildren()) {
                        // This escape sequence closes the current node
                        node = (DefaultMutableTreeNode) lastNodeInTree.getParent();
                    } else if (((DefaultMutableTreeNode) lastNodeInTree.getParent()).getAllowsChildren()) {
                        // This escape sequence closes the current node's parent
                        node = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) lastNodeInTree.getParent()).getParent();
                    } else {
                        // This escape node is in an invalid location
                        throw new NodeException("Escape " + tagMatcher.group(1) + " does not close a valid tag");
                    }
                } else {
                    // This is a normal node
                    if (lastNodeInTree.getAllowsChildren()) {
                        // Add this node to the current node's children
                        lastNodeInTree.add(node);
                    } else if (((DefaultMutableTreeNode) lastNodeInTree.getParent()).getAllowsChildren()) {
                        // Add this node to the current node's parent
                        ((DefaultMutableTreeNode) lastNodeInTree.getParent()).add(node);
                    } else {
                        // Unable to find a parent to add this node to
                        throw new NodeException("Unable to find parent to add node " + tagMatcher.group(1) + " to.");
                    }
                }
                lastNodeInTree = node;
            }
        }

        return lastNodeInTree;
    }
    
    private String getExtension(String name) {
        String extension = "";
        
        // Find the location of the extension
        int extLocation = name.lastIndexOf(".");
        
        // Did we find an extension?
        if (extLocation != -1) {
            // We did, so extract it
            extension = name.substring(extLocation + 1);
        }
        
        return extension;
    }
}
