package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigurationDialog;
// import com.ssicecreamsshop.utils.ExcelExportUtil; // Removed as export is no longer here
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
// import javafx.stage.Stage; // No longer needed here for export

public class MainView {

    public static void show() {
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

        // --- Excel Export Button (REMOVED) ---
        // Button excelExportButton = new Button("📤 Export to Excel");
        // ... (styling and action for export button removed)


        // HBox for top controls
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        // Removed excelExportButton from here. Only configButton remains on the right.
        // If you want other buttons on the left, they would go before the spacer.
        // For now, let's assume only config button is desired at the top right.
        HBox topControlsBox = new HBox(spacer, configButton);
        topControlsBox.setAlignment(Pos.CENTER_RIGHT); // Aligns the HBox content (spacer pushes config to right)
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
        orderCard.setOnMouseClicked(e -> NewOrderView.show());

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
        inventoryCard.setOnMouseClicked(e -> ManageInventoryView.show());


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
}