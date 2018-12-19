//package edu.wisc.cs.scraping_tool;
//
//import java.io.IOException;
//import java.io.OutputStream;
//import java.io.PrintStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//import java.util.function.Consumer;
//import javafx.scene.control.TextArea;
//import javafx.scene.control.TextField;
//import javafx.scene.input.KeyEvent;
//import javafx.scene.layout.BorderPane;
//
//public class Console extends OutputStream {
//    protected final TextArea textArea;
//    protected final TextField textField = new TextField();
//
//    protected final List<String> history = new ArrayList<>();
//    protected int historyPointer = 0;
//
//    private Consumer<String> onMessageReceivedHandler;
//
//    public void setOnMessageReceivedHandler(final Consumer<String> onMessageReceivedHandler) {
//        this.onMessageReceivedHandler = onMessageReceivedHandler;
//    }
//
//    public void clear() {
//        GUIUtils.runSafe(() -> textArea.clear());
//    }
//
//    public void print(final String text) {
//        Objects.requireNonNull(text, "text");
//        GUIUtils.runSafe(() -> textArea.appendText(text));
//    }
//
//    public void println(final String text) {
//        Objects.requireNonNull(text, "text");
//        GUIUtils.runSafe(() -> textArea.appendText(text + System.lineSeparator()));
//    }
//
//    public void println() {
//        GUIUtils.runSafe(() -> textArea.appendText(System.lineSeparator()));
//    }
//
//    @Override
//    public void write(int arg0) throws IOException {
//        Objects.requireNonNull(arg0, "text");
//        GUIUtils.runSafe(() -> textArea.appendText(arg0));
//        
//    }
//
//}


