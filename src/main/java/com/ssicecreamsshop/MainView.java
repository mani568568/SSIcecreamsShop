package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigurationDialog;
import com.ssicecreamsshop.utils.ExcelExportUtil;
import com.ssicecreamsshop.utils.NetworkStatusIndicator; // Import NetworkStatusIndicator
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainView {

    private static NetworkStatusIndicator networkIndicator; // Declare network indicator

    public static void show() {
        // Instantiate the network indicator
        // It's important to manage its lifecycle, especially the stopMonitoring() method.
        // If MainView can be hidden and reshown, this might need to be a singleton or managed by AppLauncher.
        // For now, we create a new one each time MainView.show() is called.
        // If an old one exists, try to stop it.
        if (networkIndicator != null) {
            networkIndicator.stopMonitoring();
        }
        networkIndicator = new NetworkStatusIndicator();

        // Title Label
        Label title = new Label("Ice Cream Shop");
        title.setStyle("-fx-font-size: 54px; -fx-text-fill: #1f2937; -fx-font-weight: bold;");

        // --- Configuration Button ---
        Button configButton = new Button("⚙️ Configuration");
        String configButtonStyle = "-fx-font-size: 14px; -fx-background-color: #607d8b; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 8px;";
        String configButtonHoverStyle = "-fx-font-size: 14px; -fx-background-color: #546e7a; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0.1, 0, 1);";
        configButton.setStyle(configButtonStyle);
        configButton.setOnMouseEntered(e -> configButton.setStyle(configButtonHoverStyle));
        configButton.setOnMouseExited(e -> configButton.setStyle(configButtonStyle));
        configButton.setOnAction(e -> ConfigurationDialog.show());

        // HBox for top controls
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Add network indicator to the left of the spacer
        HBox topControlsBox = new HBox(15, networkIndicator, spacer, configButton);
        topControlsBox.setAlignment(Pos.CENTER_LEFT); // Align items to left, spacer will push configButton to right
        topControlsBox.setPadding(new Insets(0, 0, 20, 0));


        // Ice Cream Icon for Order Card
        ImageView iceCreamIcon = null;
        try {
            Image img = new Image(MainView.class.getResourceAsStream("/images/ice_cream.png"));
            iceCreamIcon = new ImageView(img);
        } catch (NullPointerException e) {
            System.err.println("Error loading image: /images/ice_cream.png. Make sure it's in the resources/images folder.");
            iceCreamIcon = new ImageView();
            iceCreamIcon.setFitWidth(50);
            iceCreamIcon.setFitHeight(50);
        }
        if (iceCreamIcon.getImage() != null && !iceCreamIcon.getImage().isError()) {
            iceCreamIcon.setFitWidth(400);
            iceCreamIcon.setFitHeight(400);
        } else {
            iceCreamIcon.setFitWidth(400);
            iceCreamIcon.setFitHeight(400);
        }


        // Order Card
        Label orderLabel = new Label("Place New Order");
        orderLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-weight: bold;");
        VBox orderCard = new VBox(50, iceCreamIcon, orderLabel);
        orderCard.setAlignment(Pos.CENTER);
        orderCard.setStyle("-fx-background-color: #1e3a8a; -fx-background-radius: 36px; -fx-padding: 80px;");
        orderCard.setOnMouseEntered(e -> orderCard.setStyle("-fx-background-color: #1c3276; -fx-background-radius: 36px; -fx-padding: 80px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.5, 0, 0);"));
        orderCard.setOnMouseExited(e -> orderCard.setStyle("-fx-background-color: #1e3a8a; -fx-background-radius: 36px; -fx-padding: 80px;"));
        orderCard.setOnMouseClicked(e -> {
            if (networkIndicator != null) networkIndicator.stopMonitoring(); // Stop before switching view
            NewOrderView.show();
        });

        // Clipboard Icon for Inventory Card
        ImageView clipboardIcon = null;
        try {
            Image img = new Image(MainView.class.getResourceAsStream("/images/clipboard.png"));
            clipboardIcon = new ImageView(img);
        } catch (NullPointerException e) {
            System.err.println("Error loading image: /images/clipboard.png. Make sure it's in the resources/images folder.");
            clipboardIcon = new ImageView();
            clipboardIcon.setFitWidth(50);
            clipboardIcon.setFitHeight(50);
        }
        if (clipboardIcon.getImage() != null && !clipboardIcon.getImage().isError()) {
            clipboardIcon.setFitWidth(400);
            clipboardIcon.setFitHeight(400);
        } else {
            clipboardIcon.setFitWidth(400);
            clipboardIcon.setFitHeight(400);
        }

        // Inventory Card
        Label inventoryLabel = new Label("Manage Inventory");
        inventoryLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-weight: bold;");
        VBox inventoryCard = new VBox(50, clipboardIcon, inventoryLabel);
        inventoryCard.setAlignment(Pos.CENTER);
        inventoryCard.setStyle("-fx-background-color: #0ea5e9; -fx-background-radius: 36px; -fx-padding: 80px;");
        inventoryCard.setOnMouseEntered(e -> inventoryCard.setStyle("-fx-background-color: #0d94d2; -fx-background-radius: 36px; -fx-padding: 80px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.5, 0, 0);"));
        inventoryCard.setOnMouseExited(e -> inventoryCard.setStyle("-fx-background-color: #0ea5e9; -fx-background-radius: 36px; -fx-padding: 80px;"));
        inventoryCard.setOnMouseClicked(e -> {
            if (networkIndicator != null) networkIndicator.stopMonitoring(); // Stop before switching view
            ManageInventoryView.show();
        });


        // Row for Cards
        HBox buttonRow = new HBox(100, orderCard, inventoryCard);
        buttonRow.setAlignment(Pos.CENTER);

        // Root VBox
        VBox root = new VBox(50, topControlsBox, title, buttonRow);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30, 50, 50, 50));
        root.setStyle("-fx-background-color: #ffe4e1;");

        StackPane rootPane = new StackPane(root);
        StackPane.setAlignment(root, Pos.CENTER);
        AppLauncher.setScreen(rootPane);
    }

    // Method to explicitly stop monitoring if MainView is being replaced by AppLauncher directly
    // This might be needed if AppLauncher switches views without MainView's click handlers being involved.
    public static void stopNetworkIndicator() {
        if (networkIndicator != null) {
            networkIndicator.stopMonitoring();
            networkIndicator = null; // Allow garbage collection
        }
    }
}
