package com.ssicecreamsshop.utils;

import com.ssicecreamsshop.utils.icons.TelegramIcon;
import com.ssicecreamsshop.utils.icons.WifiIcon;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class GlobalStatusManager {
    private IconStatusIndicator networkIndicator;
    private IconStatusIndicator telegramIndicator;
    private HBox statusBar;
    private Timeline networkCheckTimeline;
    private ExecutorService networkTaskExecutor;

    // --- Navy & Yellow Theme Colors for Icons ---
    private static final Color NETWORK_CONNECTED_COLOR = Color.web("#FFC107"); // Vibrant Yellow/Amber
    private static final Color NETWORK_DISCONNECTED_COLOR = Color.rgb(189, 195, 199, 0.8);
    private static final Color TELEGRAM_ACTIVE_COLOR = Color.web("#03A9F4"); // Light Blue for contrast
    private static final Color TELEGRAM_INACTIVE_COLOR = Color.rgb(189, 195, 199, 0.8);

    public GlobalStatusManager() {
        networkIndicator = new IconStatusIndicator(
                new WifiIcon(NETWORK_CONNECTED_COLOR),
                new WifiIcon(NETWORK_DISCONNECTED_COLOR)
        );
        telegramIndicator = new IconStatusIndicator(
                new TelegramIcon(TELEGRAM_ACTIVE_COLOR),
                new TelegramIcon(TELEGRAM_INACTIVE_COLOR)
        );
        telegramIndicator.setActive(false);

        statusBar = new HBox(12); // Increased spacing
        statusBar.getChildren().addAll(networkIndicator, telegramIndicator);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(5, 10, 5, 10));

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
                    new KeyFrame(Duration.ZERO, e -> {
                        if (!networkTaskExecutor.isShutdown()) {
                            networkTaskExecutor.submit(this::checkNetworkStatus);
                        }
                    }),
                    new KeyFrame(Duration.seconds(2))
            );
            networkCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        }

        if (networkCheckTimeline.getStatus() != Timeline.Status.RUNNING) {
            networkCheckTimeline.playFromStart();
        }
    }

    private void checkNetworkStatus() {
        boolean currentlyConnected = testConnectivity("8.8.8.8", 53, 1000);
        if (!currentlyConnected) {
            currentlyConnected = testConnectivity("1.1.1.1", 53, 1000);
        }
        final boolean finalConnectionStatus = currentlyConnected;
        Platform.runLater(() -> networkIndicator.setActive(finalConnectionStatus));
    }

    private boolean testConnectivity(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void setTelegramBotStatus(boolean isActive) {
        Platform.runLater(() -> telegramIndicator.setActive(isActive));
    }

    public void telegramBotStarting() {
        Platform.runLater(() -> telegramIndicator.setActive(false));
    }

    public void stopAllMonitoring() {
        if (networkCheckTimeline != null && networkCheckTimeline.getStatus() == Timeline.Status.RUNNING) {
            networkCheckTimeline.stop();
        }
        if (networkTaskExecutor != null && !networkTaskExecutor.isShutdown()) {
            networkTaskExecutor.shutdownNow();
        }
        System.out.println("Global status monitoring services stopped.");
    }
}
