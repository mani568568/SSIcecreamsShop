package com.ssicecreamsshop.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkStatusIndicator extends StackPane {

    private Circle statusDot;
    private Timeline heartbeatAnimation;
    private Timeline networkCheckTimeline;
    private PauseTransition initialDelayTransition;
    private ExecutorService networkCheckExecutor;
    private volatile boolean isConnected = false;

    public NetworkStatusIndicator() {
        statusDot = new Circle(10, Color.GRAY); // Increased dot radius to 10 for "a little big size"
        statusDot.setStroke(Color.DARKGRAY);
        statusDot.setStrokeWidth(1.5);

        heartbeatAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(statusDot.scaleXProperty(), 1.0),
                        new KeyValue(statusDot.scaleYProperty(), 1.0),
                        new KeyValue(statusDot.opacityProperty(), 1.0)
                ),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(statusDot.scaleXProperty(), 1.4),
                        new KeyValue(statusDot.scaleYProperty(), 1.4),
                        new KeyValue(statusDot.opacityProperty(), 0.7)
                ),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(statusDot.scaleXProperty(), 1.0),
                        new KeyValue(statusDot.scaleYProperty(), 1.0),
                        new KeyValue(statusDot.opacityProperty(), 1.0)
                )
        );
        heartbeatAnimation.setCycleCount(Timeline.INDEFINITE);

        getChildren().add(statusDot);
        Platform.runLater(this::updateStatusIndicatorVisuals); // Initial visual state
        startNetworkMonitoring();
    }

    private void startNetworkMonitoring() {
        // Ensure executor is only created once or if shutdown
        if (networkCheckExecutor == null || networkCheckExecutor.isShutdown()) {
            networkCheckExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setName("NetworkStatusChecker-Thread");
                t.setDaemon(true);
                return t;
            });
        }

        // Timeline for periodic checks (every 1 second)
        if (networkCheckTimeline == null) {
            networkCheckTimeline = new Timeline(
                    new KeyFrame(Duration.millis(1000), e -> { // Check every 1000ms
                        if (networkCheckExecutor != null && !networkCheckExecutor.isShutdown()) {
                            networkCheckExecutor.submit(this::checkNetworkStatus);
                        }
                    })
            );
            networkCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        }

        // Initial delay of 500ms before starting the periodic checks
        if (initialDelayTransition == null) {
            initialDelayTransition = new PauseTransition(Duration.millis(500));
            initialDelayTransition.setOnFinished(event -> {
                if (networkCheckTimeline != null && networkCheckTimeline.getStatus() != Timeline.Status.RUNNING) {
                    networkCheckTimeline.play();
                }
                // Perform an immediate first check after initial delay
                if (networkCheckExecutor != null && !networkCheckExecutor.isShutdown()) {
                    networkCheckExecutor.submit(this::checkNetworkStatus);
                }
            });
        }

        // Stop any previous transitions/timelines before starting new ones
        if(initialDelayTransition.getStatus() == Timeline.Status.RUNNING) initialDelayTransition.stop();
        if(networkCheckTimeline.getStatus() == Timeline.Status.RUNNING) networkCheckTimeline.stop();

        initialDelayTransition.playFromStart(); // Start the initial delay
    }

    private void checkNetworkStatus() {
        boolean currentlyConnected = testConnectivity("8.8.8.8", 53, 800); // Google DNS
        if (!currentlyConnected) {
            currentlyConnected = testConnectivity("1.1.1.1", 53, 800); // Cloudflare DNS
        }

        if (currentlyConnected != isConnected) {
            isConnected = currentlyConnected;
            Platform.runLater(this::updateStatusIndicatorVisuals);
        }
    }

    private boolean testConnectivity(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void updateStatusIndicatorVisuals() {
        if (isConnected) {
            statusDot.setFill(Color.LIMEGREEN);
            statusDot.setStroke(Color.rgb(0,100,0)); // Darker Green
            if (heartbeatAnimation.getStatus() != Timeline.Status.RUNNING) {
                heartbeatAnimation.playFromStart();
            }
        } else {
            statusDot.setFill(Color.rgb(255, 69, 58)); // iOS-like Red
            statusDot.setStroke(Color.rgb(139,0,0)); // Dark Red
            if (heartbeatAnimation.getStatus() == Timeline.Status.RUNNING) {
                heartbeatAnimation.stop();
            }
            statusDot.setScaleX(1.0);
            statusDot.setScaleY(1.0);
            statusDot.setOpacity(1.0);
        }
    }

    public void stopMonitoring() {
        if (initialDelayTransition != null && initialDelayTransition.getStatus() == Timeline.Status.RUNNING) {
            initialDelayTransition.stop();
        }
        if (networkCheckTimeline != null && networkCheckTimeline.getStatus() == Timeline.Status.RUNNING) {
            networkCheckTimeline.stop();
        }
        if (networkCheckExecutor != null && !networkCheckExecutor.isShutdown()) {
            networkCheckExecutor.shutdownNow(); // Forcefully shut down to avoid TimeUnit in awaitTermination
            networkCheckExecutor = null; // Allow GC
        }
        if (heartbeatAnimation != null && heartbeatAnimation.getStatus() == Timeline.Status.RUNNING) {
            heartbeatAnimation.stop();
        }
        System.out.println("Network monitoring stopped.");
    }
}
