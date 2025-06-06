package com.ssicecreamsshop.utils;

import com.ssicecreamsshop.utils.icons.TelegramIcon;
import com.ssicecreamsshop.utils.icons.WifiIcon;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.animation.KeyFrame; // Added for Timeline
import javafx.animation.Timeline;  // Added for Timeline
import javafx.util.Duration;     // Added for Duration

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService; // Keep for background tasks
import java.util.concurrent.Executors;    // Keep for background tasks


public class GlobalStatusManager {
    private IconStatusIndicator networkIndicator;
    private IconStatusIndicator telegramIndicator;
    private HBox statusBar;
    // private ScheduledExecutorService networkCheckScheduler; // Replaced
    private Timeline networkCheckTimeline; // Added
    private ExecutorService networkTaskExecutor; // For running network I/O off FX thread

    // Theme Colors for Icons
    private static final Color NETWORK_CONNECTED_COLOR = Color.LIMEGREEN; // Bright green
    private static final Color NETWORK_DISCONNECTED_COLOR = Color.rgb(189, 195, 199, 0.8); // Light Gray, slightly transparent
    private static final Color TELEGRAM_ACTIVE_COLOR = Color.rgb(0, 136, 204); // Standard Telegram Blue
    private static final Color TELEGRAM_INACTIVE_COLOR = Color.rgb(189, 195, 199, 0.8); // Light Gray, slightly transparent

    public GlobalStatusManager() {
        networkIndicator = new IconStatusIndicator(
                new WifiIcon(NETWORK_CONNECTED_COLOR),
                new WifiIcon(NETWORK_DISCONNECTED_COLOR)
        );
        telegramIndicator = new IconStatusIndicator(
                new TelegramIcon(TELEGRAM_ACTIVE_COLOR),
                new TelegramIcon(TELEGRAM_INACTIVE_COLOR)
        );
        telegramIndicator.setActive(false); // Bot is inactive by default

        statusBar = new HBox(10);
        statusBar.getChildren().addAll(networkIndicator, telegramIndicator);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        // statusBar.setStyle("-fx-background-color: #f0f0f0;");

        // Initialize executor for network tasks
        networkTaskExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("GlobalNetworkStatus-TaskExecutor");
            t.setDaemon(true);
            return t;
        });

        startNetworkMonitoring();
    }

    public HBox getStatusBarNode() {
        return statusBar;
    }

    private void startNetworkMonitoring() {
        if (networkCheckTimeline == null) {
            networkCheckTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, e -> { // Perform check immediately on start
                        if (!networkTaskExecutor.isShutdown()) {
                            networkTaskExecutor.submit(this::checkNetworkStatus);
                        }
                    }),
                    new KeyFrame(Duration.seconds(2)) // Subsequent checks every 2 seconds
            );
            networkCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        }

        // Ensure it's not already running
        if (networkCheckTimeline.getStatus() != Timeline.Status.RUNNING) {
            networkCheckTimeline.playFromStart();
        }
    }

    private void checkNetworkStatus() {
        boolean currentlyConnected = testConnectivity("8.8.8.8", 53, 1000); // Google DNS
        if (!currentlyConnected) {
            currentlyConnected = testConnectivity("1.1.1.1", 53, 1000); // Cloudflare DNS
        }
        // Ensure UI update is on FX thread
        final boolean finalConnectionStatus = currentlyConnected;
        Platform.runLater(() -> networkIndicator.setActive(finalConnectionStatus));
    }

    private boolean testConnectivity(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false; // Can't connect or timeout
        }
    }

    public void setTelegramBotStatus(boolean isActive) {
        // Ensure UI update is on FX thread
        Platform.runLater(() -> telegramIndicator.setActive(isActive));
    }

    public void telegramBotStarting() {
        // Ensure UI update is on FX thread
        Platform.runLater(() -> telegramIndicator.setActive(false));
    }

    public void stopAllMonitoring() {
        if (networkCheckTimeline != null && networkCheckTimeline.getStatus() == Timeline.Status.RUNNING) {
            networkCheckTimeline.stop();
        }
        if (networkTaskExecutor != null && !networkTaskExecutor.isShutdown()) {
            networkTaskExecutor.shutdownNow(); // Attempt to stop tasks immediately
        }
        System.out.println("Global status monitoring services stopped.");
    }
}
