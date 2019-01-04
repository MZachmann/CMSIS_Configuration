/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helmpcb.cmsisconfig;

/**
 *
 * @author Amr Bekhit
 */
abstract class Target {

    private int matchNumber;
    protected String stringValue;

    public Target(int matchNumber, String stringValue) {
        this.matchNumber = matchNumber;
        this.stringValue = stringValue;
    }

    public int getMatchNumber() {
        return matchNumber;
    }

    public String getStringValue() {
        return stringValue;
    }
}

class NumericTarget
        extends Target {

    private boolean isHexadecimal;
    private long value;
    private String suffix = "";

    public NumericTarget(int matchNumber, String stringValue) throws TargetException {
        super(matchNumber, stringValue);
        // Strip out any suffixes from the string
        if (stringValue.length() >= 2) {
            // Extract the last two letters
            String lastTwoLetters = stringValue.substring(
                    stringValue.length() - 2);
            // Are they the U or L prefixes?
            char lastLetter = lastTwoLetters.toUpperCase().charAt(1);
            char nextToLastLetter = lastTwoLetters.toUpperCase().charAt(0);
            if (lastLetter == 'U' || lastLetter == 'L') {
                // This literal has a suffix. Check to see if the suffix
                // is one or two characters long
                if (nextToLastLetter == 'U' || nextToLastLetter == 'L') {
                    // It's a two-character suffix
                    suffix = lastTwoLetters;
                    stringValue = stringValue.substring(0,
                            stringValue.length() - 2);
                } else {
                    // It's a one-character suffix
                    suffix = "" + lastTwoLetters.charAt(1);
                    stringValue = stringValue.substring(0,
                            stringValue.length() - 1);
                }
            }
        }
        // Identify the radix of this target
        try {
            if (stringValue.length() > 2
                    && stringValue.toLowerCase().charAt(1) == 'x') {
                // This is a hexadecimal number
                isHexadecimal = true;
                value = Long.parseLong(stringValue.substring(2), 16);
            } else {
                // This is decimal number
                isHexadecimal = false;
                value = Long.parseLong(stringValue);
            }
        } catch (NumberFormatException ex) {
            // We failed to convert the string value to a number
            throw new TargetException("Unable to convert target '" + stringValue
                    + "' to a number.");
        }
    }

    public long getValue() {
        return getValue(-1, -1);
    }

    public long getValue(int startBit) {
        return getValue(startBit, -1);
    }

    public long getValue(int startBit, int endBit) {
        validateBitRange(startBit, endBit);
        if (startBit < 0) {
            // No start bit specified, so return the whole value
            return this.value;
        } else if (endBit < 0) {
            // No end bit specified, so just return the required bit
            return (this.value & (1L << startBit)) == 0 ? 0 : 1;
        } else {
            // User has specified a bit range.
            // Generate a mask representing the bits to modify
            int maskWidth = endBit - startBit + 1;
            long mask = ((1L << maskWidth) - 1) << startBit;

            return (this.value & mask) >> startBit;
        }
    }

    public void setValue(long value) {
        setValue(value, -1, -1);
    }

    public void setValue(long value, int startBit) {
        setValue(value, startBit, -1);
    }

    public void setValue(long value, int startBit, int endBit) {
        validateBitRange(startBit, endBit);
        if (startBit < 0) {
            // No start bit specified, so just set the entire value
            this.value = value;
        } else if (endBit < 0) {
            // No end bit specified, so just set or clear the required bit
            if (value == 0) {
                this.value = clearBit(this.value, startBit);
            } else {
                this.value = setBit(this.value, startBit);
            }
        } else {
            // The user has specified a bit range, so apply the specified bits 
            // to the value
            this.value = applyBitmap(value, startBit, endBit);
        }
    }

    public void parseStringValue(String stringValue, int startBit, int endBit) throws TargetException {
        long numericValue;
        // Try and convert the string into a numeric value depending on whether
        // this target is decimal or hexadecimal
        try {
            if (stringValue.length() > 2 && stringValue.toLowerCase().charAt(1) == 'x') {
                // This is a string in the form 0xNNN
                numericValue = Long.parseLong(stringValue.substring(2), 16);
            } else {
                // This is either a hexadecimal without the 0x prefix, 
                // or decimal number
                // See if it's a decimal number
                try {
                    numericValue = Long.parseLong(stringValue);
                } catch (NumberFormatException ex) {
                    // We failed to parse the string as a decimal number, so
                    // try and parse it as a hexadecimal one
                    numericValue = Long.parseLong(stringValue, 16);
                }
            }
        } catch (NumberFormatException ex) {
            // We couldn't parse this string as a number
            throw new TargetException("Invalid number " + stringValue);
        }
        // By this point, we've converting the string into a number. Now
        // we need to apply it to our value
        setValue(numericValue, startBit, endBit);
    }
    
    @Override
    public String getStringValue() {
        return getStringValue(-1, -1);
    }

    public String getStringValue(int startBit, int endBit) {
        long numericValue = getValue(startBit, endBit);
        // Depending on the radix of this target, generate its string representation
        if (isHexadecimal) {
            return "0x" + Long.toHexString(numericValue).toUpperCase();
        } else {
            return Long.toString(numericValue);
        }
    }

    @Override
    public String toString() {
        return getStringValue() + suffix;
    }

    private long setBit(long value, int bit) {
        if (bit < 0) {
            throw new ArithmeticException("Invalid bit position " + bit);
        }

        return value | (1L << bit);
    }

    private long clearBit(long value, int bit) {
        if (bit < 0) {
            throw new ArithmeticException("Invalid bit position " + bit);
        }

        return value & ~(1L << bit);
    }

    private long applyBitmap(long value, int startBit, int endBit) {
        // Shift the value to the right position
        value <<= startBit;
        // Generate a mask representing the bits to modify
        int maskWidth = endBit - startBit + 1;
        long mask = ((1L << maskWidth) - 1) << startBit;

        return this.value ^ ((this.value ^ value) & mask);
    }

    private void validateBitRange(int startBit, int endBit) {
        if ((startBit < 0 && endBit >= 0) || (endBit >= 0 && startBit >= endBit)) {
            throw new ArithmeticException("Invalid bit range: "
                    + startBit + " - " + endBit);




        }
    }
}

class StringTarget
        extends Target {

    public StringTarget(int matchNumber, String stringValue) {
        super(matchNumber, stringValue);
        // Remove the quotes for the string value
        this.stringValue = stringValue.substring(1, stringValue.length() - 1);
    }

    public void setValue(String value) {
        this.stringValue = value;
    }

    @Override
    public String toString() {
        return '"' + stringValue + '"';
    }
}
