package de.spacemate.app;

import javafx.application.Application;

/**
 * Entry point. JavaFX requires launching via Application.launch(),
 * not calling new SpaceMateApp() directly.
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(SpaceMateApp.class, args);
    }
}