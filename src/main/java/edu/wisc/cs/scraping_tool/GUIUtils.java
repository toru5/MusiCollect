package edu.wisc.cs.scraping_tool;

import java.util.Objects;
import javafx.application.Platform;

public final class GUIUtils {
    private GUIUtils() {
        throw new UnsupportedOperationException();
    }

    public static void runSafe(final Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        }
        else {
            Platform.runLater(runnable);
        }
    }
}