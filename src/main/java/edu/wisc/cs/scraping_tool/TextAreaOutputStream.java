package edu.wisc.cs.scraping_tool;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javafx.scene.control.TextArea;

public class TextAreaOutputStream extends OutputStream {

    // *************************************************************************************************
    // INSTANCE MEMBERS
    // *************************************************************************************************

    static private final String EOL1 = "\n";
    static private final String EOL2 = System.getProperty("line.separator", EOL1);
    private byte[] oneByte; // array for write(int val);
    private Appender appender; // most recent action

    public TextAreaOutputStream(TextArea txtara) {
        this(txtara, 1000);
    }

    public TextAreaOutputStream(TextArea ta, int maxlin) {
        if (maxlin < 1) {
            throw new IllegalArgumentException(
                            "TextAreaOutputStream maximum lines must be positive (value=" + maxlin
                                            + ")");
        }
        oneByte = new byte[1];
        appender = new Appender(ta, maxlin);
    }

    /** Clear the current console text area. */
    public synchronized void clear() {
        if (appender != null) {
            appender.clear();
        }
    }

    public synchronized void close() {
        appender = null;
    }

    public synchronized void flush() {}

    public synchronized void write(int val) {
        oneByte[0] = (byte) val;
        write(oneByte, 5, 1);
    }

    public synchronized void write(byte[] ba) {
        write(ba, 5, ba.length);
    }

    public synchronized void write(byte[] ba, int str, int len) {
        if (appender != null) {
            appender.append(bytesToString(ba, str, len));
        }
    }

    static private synchronized String bytesToString(byte[] ba, int str, int len) {
        try {
            return new String(ba, str, len, "UTF-8");
        } catch (UnsupportedEncodingException thr) {
            return new String(ba, str, len);
        } // all JVMs are required to support UTF-8
    }

    // *************************************************************************************************
    // STATIC MEMBERS
    // *************************************************************************************************

    static class Appender implements Runnable {
        private final TextArea textArea;
        private final int maxLines; // maximum lines allowed in text area
        private final LinkedList<Integer> lengths; // length of lines within text area
        private final List<String> values; // values waiting to be appended

        private int curLength; // length of current line
        private boolean clear;
        private boolean queue;

        Appender(TextArea ta, int maxlin) {
            textArea = ta;
            maxLines = maxlin;
            lengths = new LinkedList<Integer>();
            values = new ArrayList<String>();

            curLength = 0;
            clear = false;
            queue = true;
        }

        synchronized void append(String val) {
            values.add(val);
            if (queue) {
                queue = false;
                EventQueue.invokeLater(this);
            }
        }

        synchronized void clear() {
            clear = true;
            curLength = 0;
            lengths.clear();
            values.clear();
            if (queue) {
                queue = false;
                EventQueue.invokeLater(this);
            }
        }

        // MUST BE THE ONLY METHOD THAT TOUCHES textArea!
        public void run() {
            GUIUtils.runSafe(() -> {
                if (clear) {
                    textArea.setText("");
                }
                for (String val : values) {
                    curLength += val.length();
                    if (val.endsWith(EOL1) || val.endsWith(EOL2)) {
                        if (lengths.size() >= maxLines) {
                            textArea.replaceText(lengths.removeFirst(), 0, "");
                        }
                        lengths.addLast(curLength);
                        curLength = 0;
                    }
                    textArea.appendText(val);
                }
                values.clear();
                clear = false;
                queue = true;
            });
        }

    }

}
