package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigManager;
import com.ssicecreamsshop.utils.GlobalStatusManager;
import com.ssicecreamsshop.utils.TelegramBotService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image; // Import the Image class
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class AppLauncher extends Application {
    private static Stage primaryStage;
    private TelegramBotService telegramBotService;
    private GlobalStatusManager globalStatusManager;
    private static BorderPane rootLayout;

    // IMPORTANT: Replace with your actual bot token and username from BotFather
    private static final String BOT_TOKEN = "7732826808:AAFSzM4UkkIcBkR7NPhO_HMxuQOEEjF2EOc";
    private static final String BOT_USERNAME = "ssicecreams568_bot";

    public static void setScreen(Parent viewRoot) {
        if (rootLayout != null) {
            Platform.runLater(() -> rootLayout.setCenter(viewRoot));
        } else {
            System.err.println("CRITICAL Error: rootLayout is null in setScreen. Scene might not be set up correctly for view switching.");
        }
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        globalStatusManager = new GlobalStatusManager();

        // Initialize Telegram Bot Service
        if ("YOUR_BOT_TOKEN_HERE".equals(BOT_TOKEN) || "YOUR_BOT_USERNAME_HERE".equals(BOT_USERNAME) || BOT_TOKEN.isEmpty() || BOT_USERNAME.isEmpty()) {
            System.err.println("CRITICAL ERROR: Telegram Bot Token or Username not set/valid in AppLauncher.java!");
            if (globalStatusManager != null) {
                globalStatusManager.setTelegramBotStatus(false);
            }
        } else {
            telegramBotService = new TelegramBotService(BOT_TOKEN, BOT_USERNAME, globalStatusManager);
            new Thread(() -> telegramBotService.startBot()).start();
        }

        ConfigManager.ensureDefaultPathsExist();

        rootLayout = new BorderPane();
        rootLayout.setTop(globalStatusManager.getStatusBarNode());

        Scene scene = new Scene(rootLayout, 1280, 800);
        primaryStage.setScene(scene);

        MainView.show();

        primaryStage.setTitle("Ice Cream Shop");

        // --- Set Application Icon ---
        // This is the code that sets the icon in the title bar
        try {
            // It looks for 'app_icon.png' in the 'src/main/resources/images/' folder.
            Image appIcon = new Image(AppLauncher.class.getResourceAsStream("/images/app_icon.png"));
            primaryStage.getIcons().add(appIcon);
            System.out.println("Application icon set successfully.");
        } catch (Exception e) {
            System.err.println("Error loading application icon: /images/app_icon.png - " + e.getMessage());
            System.err.println("Make sure the image file is in the correct resources folder.");
        }

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
