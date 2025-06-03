package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigurationDialog; // Import the configuration dialog
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button; // Import Button
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainView {

    public static void show() {
        // Title Label
        Label title = new Label("Ice Cream Shop");
        title.setStyle("-fx-font-size: 54px; -fx-text-fill: #1f2937; -fx-font-weight: bold;");

        // --- Configuration Button ---
        Button configButton = new Button("⚙️ Configuration");
        configButton.setStyle("-fx-font-size: 14px; -fx-background-color: #607d8b; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 8px;");
        configButton.setOnMouseEntered(e -> configButton.setStyle("-fx-font-size: 14px; -fx-background-color: #546e7a; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0.1, 0, 1);"));
        configButton.setOnMouseExited(e -> configButton.setStyle("-fx-font-size: 14px; -fx-background-color: #607d8b; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 8px;"));
        configButton.setOnAction(e -> ConfigurationDialog.show());

        // HBox for top controls (like the config button)
        HBox topControlsBox = new HBox(configButton);
        topControlsBox.setAlignment(Pos.TOP_RIGHT); // Align button to the top-right
        topControlsBox.setPadding(new Insets(0, 0, 20, 0)); // Add some space below the button


        // Ice Cream Icon for Order Card
        ImageView iceCreamIcon = null;
        // Try to load from the default resource path first, then consider configured path if needed
        // For main view icons, keeping them as resources is usually fine.
        // The configurable paths are primarily for menu item images.
        try {
            Image img = new Image(MainView.class.getResourceAsStream("/images/ice_cream.png"));
            iceCreamIcon = new ImageView(img);
        } catch (NullPointerException e) {
            System.err.println("Error loading image: /images/ice_cream.png. Make sure it's in the resources/images folder.");
            iceCreamIcon = new ImageView();
            // Label placeholder = new Label("Order Icon"); // Not added to scene, just for info
            // placeholder.setStyle("-fx-border-color: black; -fx-padding: 20px;");
            iceCreamIcon.setFitWidth(50);
            iceCreamIcon.setFitHeight(50);
        }
        if (iceCreamIcon.getImage() != null && !iceCreamIcon.getImage().isError()) {
            iceCreamIcon.setFitWidth(400);
            iceCreamIcon.setFitHeight(400);
        } else { // Fallback if image failed to load or is error
            iceCreamIcon.setFitWidth(400); // Keep size consistent
            iceCreamIcon.setFitHeight(400);
            // You could set a placeholder graphic here if desired
        }


        // Order Card
        Label orderLabel = new Label("Place New Order");
        orderLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-weight: bold;");
        VBox orderCard = new VBox(50, iceCreamIcon, orderLabel);
        orderCard.setAlignment(Pos.CENTER);
        orderCard.setStyle("-fx-background-color: #1e3a8a; -fx-background-radius: 36px; -fx-padding: 80px;");
        orderCard.setOnMouseEntered(e -> orderCard.setStyle("-fx-background-color: #1c3276; -fx-background-radius: 36px; -fx-padding: 80px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.5, 0, 0);"));
        orderCard.setOnMouseExited(e -> orderCard.setStyle("-fx-background-color: #1e3a8a; -fx-background-radius: 36px; -fx-padding: 80px;"));
        orderCard.setOnMouseClicked(e -> NewOrderView.show());

        // Clipboard Icon for Inventory Card (similar logic for image loading)
        ImageView clipboardIcon = null;
        try {
            Image img = new Image(MainView.class.getResourceAsStream("/images/clipboard.png"));
            clipboardIcon = new ImageView(img);
        } catch (NullPointerException e) {
            System.err.println("Error loading image: /images/clipboard.png. Make sure it's in the resources/images folder.");
            clipboardIcon = new ImageView();
            // Label placeholder = new Label("Inventory Icon");
            // placeholder.setStyle("-fx-border-color: black; -fx-padding: 20px;");
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
        inventoryCard.setOnMouseClicked(e -> ManageInventoryView.show());


        // Row for Cards
        HBox buttonRow = new HBox(100, orderCard, inventoryCard);
        buttonRow.setAlignment(Pos.CENTER);

        // Root VBox - now includes topControlsBox
        VBox root = new VBox(50, topControlsBox, title, buttonRow);
        root.setAlignment(Pos.CENTER);
        // Adjusted padding to accommodate the config button potentially being at the top
        root.setPadding(new Insets(30, 50, 50, 50));
        root.setStyle("-fx-background-color: #ffe4e1;");

        StackPane rootPane = new StackPane(root);
        StackPane.setAlignment(root, Pos.CENTER);
        AppLauncher.setScreen(rootPane);
    }
}
