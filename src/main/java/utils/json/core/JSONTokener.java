package utils.json.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Copyright (c) 2002 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/**
 * A JSONTokener takes a source string and extracts characters and tokens from
 * it. It is used by the JSONObject and JSONArray constructors to parse
 * JSON source strings.
 * @author JSON.org
 * @version 2
 */
public class JSONTokener {
    // Standard format for date expression
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                                         secondardDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public static final char
            STR_DOUBLE_QUOTE = '"', STR_SINGLE_QUOTE = '\'', BACK_SLASH = '/', FORWARD_SLASH = '\\',
            STAR = '*', SHARP = '#', NEW_LINE = '\n', CARRIAGE_RETURN = '\r', TAB = '\t', SPACE = ' ',
            CURLY_BRAKET_OPEN = '{', CURLY_BRAKET_CLOSE = '}',
            SQUARE_BRAKET_OPEN = '[', SQUARE_BRAKET_CLOSE = ']',
            BRAKET_OPEN = '(', BRAKET_CLOSE = ')';

    /**
     * The index of the next character.
     */
    private int myIndex;
    
    /**
     * The source string being tokenized.
     */
    private String mySource;
    
    /**
     * Construct a JSONTokener from a string.
     *
     * @param s     A source string.
     */
    public JSONTokener(String s) {
        this.myIndex = 0;
        this.mySource = s;
    }


    /**
     * Back up one character. This provides a sort of lookahead capability,
     * so that you can test for a digit or letter before attempting to parse
     * the next number or identifier.
     */
    public void back() {
        if (this.myIndex > 0) {
            this.myIndex -= 1;
        }
    }



    /**
     * Get the hex value of a character (base16).
     * @param c A character between '0' and '9' or between 'A' and 'F' or
     * between 'a' and 'f'.
     * @return  An int between 0 and 15, or -1 if c was not a hex digit.
     */
    public static int dehexchar(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - ('A' - 10);
        }
        if (c >= 'a' && c <= 'f') {
            return c - ('a' - 10);
        }
        return -1;
    }


    /**
     * Determine if the source string still contains characters that next()
     * can consume.
     * @return true if not yet at the end of the source.
     */
    public boolean more() {
        return this.myIndex < this.mySource.length();
    }


    /**
     * Get the next character in the source string.
     *
     * @return The next character, or 0 if past the end of the source string.
     */
    public char next() {
        if (more()) {
            char c = this.mySource.charAt(this.myIndex);
            this.myIndex += 1;
            return c;
        }
        return 0;
    }


    /**
     * Consume the next character, and check that it matches a specified
     * character.
     * @param c The character to match.
     * @return The character.
     * @throws JSONException if the character does not match.
     */
    public char next(char c) throws JSONException {
        char n = next();
        if (n != c) {
            throw syntaxError("Expected '" + c + "' and instead saw '" +
                    n + "'");
        }
        return n;
    }


    /**
     * Get the next n characters.
     *
     * @param n     The number of characters to take.
     * @return      A string of n characters.
     * @throws JSONException
     *   Substring bounds error if there are not
     *   n characters remaining in the source string.
     */
     public String next(int n) throws JSONException {
         int i = this.myIndex;
         int j = i + n;
         if (j >= this.mySource.length()) {
            throw syntaxError("Substring bounds error");
         }
         this.myIndex += n;
         return this.mySource.substring(i, j);
     }


    /**
     * Get the next char in the string, skipping whitespace
     * and comments (slashslash, slashstar, and hash).
     * @throws JSONException
     * @return  A character, or 0 if there are no more characters.
     */
    public char nextClean() throws JSONException {
        for (;;) {
            char c = next();
            if (c == BACK_SLASH) {
                switch (next()) {
                case BACK_SLASH:
                    do {
                        c = next();
                    } while (c != NEW_LINE && c != CARRIAGE_RETURN && c != 0);
                    break;
                case STAR:
                    for (;;) {
                        c = next();
                        if (c == 0) {
                            throw syntaxError("Unclosed comment");
                        }
                        if (c == STAR) {
                            if (next() == BACK_SLASH) {
                                break;
                            }
                            back();
                        }
                    }
                    break;
                default:
                    back();
                    return BACK_SLASH;
                }
            } else if (c == SHARP) {
                do {
                    c = next();
                } while (c != NEW_LINE && c != CARRIAGE_RETURN && c != 0);
            } else if (c == 0 || c > SPACE) {
                return c;
            }
        }
    }


    /**
     * Return the characters up to the next close quote character.
     * Backslash processing is done. The formal JSON format does not
     * allow strings in single quotes, but an implementation is allowed to
     * accept them.
     * @param quote The quoting character, either
     *      <code>"</code>&nbsp;<small>(double quote)</small> or
     *      <code>'</code>&nbsp;<small>(single quote)</small>.
     * @return      A String.
     * @throws JSONException Unterminated string.
     */
    public String nextString(char quote) throws JSONException {
        char c;
        StringBuilder builder = new StringBuilder();
        for (;;) {
            c = next();
            switch (c) {
            case 0:
            case NEW_LINE:
            case CARRIAGE_RETURN:
                throw syntaxError("Unterminated string");
            case FORWARD_SLASH:
                c = next();
                switch (c) {
                case 'b':
                    builder.append('\b');
                    break;
                case 't':
                    builder.append(TAB);
                    break;
                case 'n':
                    builder.append(NEW_LINE);
                    break;
                case 'f':
                    builder.append('\f');
                    break;
                case 'r':
                    builder.append(CARRIAGE_RETURN);
                    break;
                case 'u':
                    builder.append((char)Integer.parseInt(next(4), 16));
                    break;
                case 'x' :
                    builder.append((char) Integer.parseInt(next(2), 16));
                    break;
                default:
                    builder.append(c);
                }
                break;
            default:
                if (c == quote) {
                    return builder.toString();
                }
                builder.append(c);
            }
        }
    }


    /**
     * Get the text up but not including the specified character or the
     * end of line, whichever comes first.
     * @param  d A delimiter character.
     * @return   A string.
     */
    public String nextTo(char d) {
        StringBuilder builder = new StringBuilder();
        for (;;) {
            char c = next();
            if (c == d || c == 0 || c == NEW_LINE || c == CARRIAGE_RETURN) {
                if (c != 0) {
                    back();
                }
                return builder.toString().trim();
            }
            builder.append(c);
        }
    }


    /**
     * Get the text up but not including one of the specified delimeter
     * characters or the end of line, whichever comes first.
     * @param delimiters A set of delimiter characters.
     * @return A string, trimmed.
     */
    public String nextTo(String delimiters) {
        char c;
        StringBuilder builder = new StringBuilder();
        for (;;) {
            c = next();
            if (delimiters.indexOf(c) >= 0 || c == 0 ||
                    c == NEW_LINE || c == CARRIAGE_RETURN) {
                if (c != 0) {
                    back();
                }
                return builder.toString().trim();
            }
            builder.append(c);
        }
    }

    public Object nextObject(Object holder) throws JSONException {
        boolean isJson = false;
        if (holder instanceof JSONObject) {
            isJson = true;
        } else if (holder instanceof Map) {
            isJson = false;
        } else {
            throw syntaxError("Invalid object holder");
        }

        char c;
        String key;

        try {
            if (nextClean() != '{') {
                return null;
            }
            for (;;) {
                c = nextClean();
                switch (c) {
                    case 0:
                        return null;
                    case '}':
                        return holder;
                    default:
                        back();
                        key = nextValue(isJson).toString();
                }

                /*
                * The key is followed by ':'. We will also tolerate '=' or '=>'.
                */

                c = nextClean();

                if (c == '=') {
                    if (next() != '>') {
                        back();
                    }
                } else if (c != ':') {
                    return null;
                }
                if (isJson) {
                    ((JSONObject)holder).put(key, nextValue(isJson));
                } else {
                    ((Map<String, Object>)holder).put(key, nextValue(isJson));
                }

                switch (nextClean()) {
                    case ';':
                    case ',':
                        if (nextClean() == '}') {
                            return holder;
                        }
                        back();
                        break;
                    case '}':
                        return holder;
                    default:
                        return null;
                }
            }
        } catch (JSONException je) {
            return null;
        }
    }

    public Object nextArray(Object holder) throws JSONException {
        boolean isJson = false;
        if (holder instanceof JSONArray) {
            isJson = true;
        } else if (holder instanceof List) {
            isJson = false;
        } else {
            throw syntaxError("Invalid object holder");
        }

        try {
            char c = nextClean();
            char q;
            if (c == '[') {
                q = ']';
            } else if (c == '(') {
                q = ')';
            } else {
                return null;
            }
            if (nextClean() == ']') {
                return holder;
            }
            back();
            for (;;) {
                if (nextClean() == ',') {
                    back();
                    if (isJson) {
                        ((JSONArray)holder).put((Object) null);
                    } else {
                        ((List<Object>)holder).add(null);
                    }
                } else {
                    back();
                    if (isJson) {
                        ((JSONArray)holder).put(nextValue(isJson));
                    } else {
                        ((List<Object>)holder).add(nextValue(isJson));
                    }
                }
                c = nextClean();
                switch (c) {
                    case ';':
                    case ',':
                        if (nextClean() == ']') {
                            return holder;
                        }
                        back();
                        break;
                    case ']':
                    case ')':
                        if (q != c) {
                            return null;
                        }
                        return holder;
                    default:
                        return null;
                }
            }
        } catch (JSONException je) {
            return null;
        }
    }

    /**
     * Get the next value. The value can be a Boolean, Double, Integer,
     * JSONArray, JSONObject, Long, or String, or the JSONObject.NULL object.
     * @throws JSONException If syntax error.
     *
     * @return An object.
     */
    public Object nextValue(boolean isJson) throws JSONException {
        char c = nextClean();
        String s;

        switch (c) {
            case STR_DOUBLE_QUOTE:
            case STR_SINGLE_QUOTE:
                String nextString = nextString(c);
                if (nextString.length() == 19) {
                    try {
                        try {
                            return dateFormat.parse(nextString);
                        } catch (ParseException npe) {
                            return secondardDateFormat.parse(nextString);
                        }
                    } catch (ParseException pe) {
                        return nextString;
                    }
                } else {
                    return nextString;
                }
            case CURLY_BRAKET_OPEN:
                back();
                if (isJson) {
                    return new JSONObject(this);
                } else {
                    return nextObject(new HashMap<String, Object>());
                }
            case SQUARE_BRAKET_OPEN:
            case BRAKET_OPEN:
                back();
                if (isJson) {
                    return new JSONArray(this);
                } else {
                    return nextArray(new ArrayList<Object>());
                }
        }

        /*
         * Handle unquoted text. This could be the values true, false, or
         * null, or it can be a number. An implementation (such as this one)
         * is allowed to also accept non-standard forms.
         *
         * Accumulate characters until we reach the end of the text or a
         * formatting character.
         */

        StringBuilder builder = new StringBuilder();
        char b = c;
        while (c >= SPACE && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
            builder.append(c);
            c = next();
        }
        back();

        /*
         * If it is true, false, or null, return the proper value.
         */

        s = builder.toString().trim();
        if (s.equals("")) {
            throw syntaxError("Missing value");
        }
        if (s.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (s.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (s.equalsIgnoreCase("null")) {
            return (isJson)? JSONObject.NULL : null;
        }
        if (s.equalsIgnoreCase("-Infinity")) {
            return Double.NEGATIVE_INFINITY;
        }
        if (s.equalsIgnoreCase("Infinity")) {
            return Double.POSITIVE_INFINITY;
        }
        if (s.equalsIgnoreCase("NaN")) {
            return Double.NaN;
        }

        /*
         * If it might be a number, try converting it. We support the 0- and 0x-
         * conventions. If a number cannot be produced, then the value will just
         * be a string. Note that the 0-, 0x-, plus, and implied string
         * conventions are non-standard. A JSON parser is free to accept
         * non-JSON forms as long as it accepts all correct JSON forms.
         */

        if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+') {
            if (b == '0') {
                if (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
                    try {
                        return new Integer(Integer.parseInt(s.substring(2), 16));
                    } catch (Exception e) {
                        /* Ignore the error */
                    }
                } else {
                    try {
                        return new Integer(Integer.parseInt(s, 8));
                    } catch (Exception e) {
                        /* Ignore the error */
                    }
                }
            }
            
            boolean hasDot = s.indexOf(".") != -1;
            
            if (hasDot) {
                try {
                    BigDecimal bigDecimal = new BigDecimal(s);
                    Double doub = new Double(s);
                    
                    if (
                            doub.doubleValue() == Double.POSITIVE_INFINITY ||
                            doub.doubleValue() == Double.NEGATIVE_INFINITY ||
                            doub.doubleValue() == Double.NaN
                    ) {
                        return bigDecimal;
                    } else if (bigDecimal.compareTo(new BigDecimal(doub.toString())) == 0) {
                        return doub;
                    } else {
                        return bigDecimal;
                    }
                } catch (NumberFormatException nfe) {
                    return s;
                }
            } else {
                try {
                    return new Integer(s);
                } catch (NumberFormatException nfeInt) {
                    try {
                        return new Long(s);
                    } catch (NumberFormatException nfeLong) {
                        try {
                            return new BigInteger(s);
                        } catch (Exception eBigInteger) {
                            try {
                                // Is it a date??
                                try {
                                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
                                } catch (ParseException e) {
                                    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(s);
                                }
                            } catch (ParseException pe) {
                                // We know it is an unexpected string finally
                                return s;
                            }
                        }
                    }
                }
            }
        }
        // May it be a date string?
        try {
            try {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
            } catch (ParseException e) {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(s);
            }
        } catch (ParseException pe) {
            // Oh, it is an unexpected string, return it anyway
            return s;
        }
    }


    /**
     * Skip characters until the next character is the requested character.
     * If the requested character is not found, no characters are skipped.
     * @param to A character to skip to.
     * @return The requested character, or zero if the requested character
     * is not found.
     */
    public char skipTo(char to) {
        char c;
        int index = this.myIndex;
        do {
            c = next();
            if (c == 0) {
                this.myIndex = index;
                return c;
            }
        } while (c != to);
        back();
        return c;
    }


    /**
     * Skip characters until past the requested string.
     * If it is not found, we are left at the end of the source.
     * @param to A string to skip past.
     */
    public boolean skipPast(String to) {
        this.myIndex = this.mySource.indexOf(to, this.myIndex);
        if (this.myIndex < 0) {
            this.myIndex = this.mySource.length();
            return false;
        } 
        this.myIndex += to.length();
        return true;

    }


    /**
     * Make a JSONException to signal a syntax error.
     *
     * @param message The error message.
     * @return  A JSONException object, suitable for throwing
     */
    public JSONException syntaxError(String message) {
        return new JSONException(message + toString());
    }


    /**
     * Make a printable string of this JSONTokener.
     *
     * @return " at character [this.myIndex] of [this.mySource]"
     */
    public String toString() {
        return " at character " + this.myIndex + " of " + this.mySource;
    }
}