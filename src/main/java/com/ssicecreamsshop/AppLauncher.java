package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigManager;
import com.ssicecreamsshop.utils.GlobalStatusManager; // Import GlobalStatusManager
import com.ssicecreamsshop.utils.TelegramBotService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane; // Import BorderPane
import javafx.stage.Stage;

public class AppLauncher extends Application {
    private static Stage primaryStage;
    private TelegramBotService telegramBotService;
    private GlobalStatusManager globalStatusManager;
    private static BorderPane rootLayout; // Main layout for the scene

    // IMPORTANT: Replace with your actual bot token and username from BotFather
    private static final String BOT_TOKEN = "7732826808:AAFSzM4UkkIcBkR7NPhO_HMxuQOEEjF2EOc";
    private static final String BOT_USERNAME = "ssicecreams568_bot";

    public static void setScreen(Parent viewRoot) {
        if (rootLayout != null) {
            // Ensure this is run on the JavaFX Application Thread if called from another thread
            Platform.runLater(() -> rootLayout.setCenter(viewRoot));
        } else {
            System.err.println("CRITICAL Error: rootLayout is null in setScreen. Scene might not be set up correctly for view switching.");
            // Fallback or error handling might be needed here
        }
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        globalStatusManager = new GlobalStatusManager(); // Create the status manager FIRST

        // Initialize Telegram Bot Service
        if ("YOUR_BOT_TOKEN_HERE".equals(BOT_TOKEN) || "YOUR_BOT_USERNAME_HERE".equals(BOT_USERNAME) || BOT_TOKEN.isEmpty() || BOT_USERNAME.isEmpty()) {
            System.err.println("CRITICAL ERROR: Telegram Bot Token or Username not set/valid in AppLauncher.java!");
            System.err.println("Please replace placeholders with your actual bot credentials.");
            if (globalStatusManager != null) {
                globalStatusManager.setTelegramBotStatus(false); // Explicitly set as inactive
            }
        } else {
            // Pass the globalStatusManager to the bot service so it can update the icon
            telegramBotService = new TelegramBotService(BOT_TOKEN, BOT_USERNAME, globalStatusManager);
            new Thread(() -> telegramBotService.startBot()).start();
        }

        ConfigManager.ensureDefaultPathsExist();

        rootLayout = new BorderPane();
        rootLayout.setTop(globalStatusManager.getStatusBarNode()); // Add status bar to the top

        // Set a default background or placeholder for the center initially
        // rootLayout.setCenter(new javafx.scene.layout.Pane()); // Or some initial loading screen

        Scene scene = new Scene(rootLayout, 1280, 800); // Initial scene with BorderPane
        primaryStage.setScene(scene);

        MainView.show(); // This will call setScreen, placing MainView in the center of rootLayout

        primaryStage.setTitle("Ice Cream Shop");
        primaryStage.setMaximized(true);

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Window close requested. Shutting down...");
            if (telegramBotService != null) {
                telegramBotService.stopBot();
            }
            if (globalStatusManager != null) {
                globalStatusManager.stopAllMonitoring();
            }
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("AppLauncher.stop() called. Cleaning up resources.");
        if (telegramBotService != null) {
            telegramBotService.stopBot();
        }
        if (globalStatusManager != null) {
            globalStatusManager.stopAllMonitoring();
        }
        super.stop();
        System.out.println("Application stopped.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
