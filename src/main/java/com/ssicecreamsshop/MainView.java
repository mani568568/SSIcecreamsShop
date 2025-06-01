package com.ssicecreamsshop;

import javafx.geometry.Pos;
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

        // Ice Cream Icon for Order Card
        ImageView iceCreamIcon = null;
        try {
            // Attempt to load the image
            Image img = new Image(MainView.class.getResourceAsStream("/images/ice_cream.png"));
            iceCreamIcon = new ImageView(img);
        } catch (NullPointerException e) {
            System.err.println("Error loading image: /images/ice_cream.png. Make sure it's in the resources/images folder.");
            // Fallback: Create a placeholder label if image fails to load
            iceCreamIcon = new ImageView(); // Create an empty ImageView
            Label placeholder = new Label("Order Icon");
            placeholder.setStyle("-fx-border-color: black; -fx-padding: 20px;");
            // You might want to set a fixed size for the placeholder ImageView if the image is missing
            iceCreamIcon.setFitWidth(50); // Example size
            iceCreamIcon.setFitHeight(50); // Example size
        }
        if (iceCreamIcon.getImage() != null) { // Check if image was loaded successfully before setting size
            iceCreamIcon.setFitWidth(400);
            iceCreamIcon.setFitHeight(400);
        }


        // Order Card
        Label orderLabel = new Label("Place New Order");
        orderLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-weight: bold;");
        VBox orderCard = new VBox(50, iceCreamIcon, orderLabel);
        orderCard.setAlignment(Pos.CENTER);
        orderCard.setStyle("-fx-background-color: #1e3a8a; -fx-background-radius: 36px; -fx-padding: 80px;");
        // Add hover effect for better UX
        orderCard.setOnMouseEntered(e -> orderCard.setStyle("-fx-background-color: #1c3276; -fx-background-radius: 36px; -fx-padding: 80px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.5, 0, 0);"));
        orderCard.setOnMouseExited(e -> orderCard.setStyle("-fx-background-color: #1e3a8a; -fx-background-radius: 36px; -fx-padding: 80px;"));
        orderCard.setOnMouseClicked(e -> NewOrderView.show());

        // Clipboard Icon for Inventory Card
        ImageView clipboardIcon = null;
        try {
            // Attempt to load the image
            Image img = new Image(MainView.class.getResourceAsStream("/images/clipboard.png"));
            clipboardIcon = new ImageView(img);
        } catch (NullPointerException e) {
            System.err.println("Error loading image: /images/clipboard.png. Make sure it's in the resources/images folder.");
            // Fallback: Create a placeholder label if image fails to load
            clipboardIcon = new ImageView(); // Create an empty ImageView
            Label placeholder = new Label("Inventory Icon");
            placeholder.setStyle("-fx-border-color: black; -fx-padding: 20px;");
            // You might want to set a fixed size for the placeholder ImageView if the image is missing
            clipboardIcon.setFitWidth(50); // Example size
            clipboardIcon.setFitHeight(50); // Example size
        }
        if (clipboardIcon.getImage() != null) { // Check if image was loaded successfully before setting size
            clipboardIcon.setFitWidth(400);
            clipboardIcon.setFitHeight(400);
        }

        // Inventory Card
        Label inventoryLabel = new Label("Manage Inventory");
        inventoryLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-weight: bold;");
        VBox inventoryCard = new VBox(50, clipboardIcon, inventoryLabel);
        inventoryCard.setAlignment(Pos.CENTER);
        inventoryCard.setStyle("-fx-background-color: #0ea5e9; -fx-background-radius: 36px; -fx-padding: 80px;");
        // Add hover effect
        inventoryCard.setOnMouseEntered(e -> inventoryCard.setStyle("-fx-background-color: #0d94d2; -fx-background-radius: 36px; -fx-padding: 80px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.5, 0, 0);"));
        inventoryCard.setOnMouseExited(e -> inventoryCard.setStyle("-fx-background-color: #0ea5e9; -fx-background-radius: 36px; -fx-padding: 80px;"));
        // inventoryCard.setOnMouseClicked(e -> ManageInventoryView.show()); // Assuming you have this view

        // Row for Cards
        HBox buttonRow = new HBox(100, orderCard, inventoryCard);
        buttonRow.setAlignment(Pos.CENTER);

        // Root VBox
        VBox root = new VBox(100, title, buttonRow);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #ffe4e1; -fx-padding: 100;"); // Misty Rose background

        // Set the screen using AppLauncher
        // Wrap root in a StackPane to ensure it centers correctly and handles resizing well if AppLauncher.setScreen expects a single root that fills space
        StackPane rootPane = new StackPane(root);
        StackPane.setAlignment(root, Pos.CENTER); // Ensure VBox is centered in StackPane
        AppLauncher.setScreen(rootPane);
    }
}
