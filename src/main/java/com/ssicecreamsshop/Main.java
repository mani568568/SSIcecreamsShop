package com.ssicecreamsshop;

/**
 * A separate main class to launch the JavaFX application from a shaded JAR.
 * This class does NOT extend javafx.application.Application and serves as the
 * true entry point for the executable JAR.
 */
public class Main {
    public static void main(String[] args) {
        // This call will trigger the JavaFX lifecycle correctly.
        AppLauncher.main(args);
    }
}