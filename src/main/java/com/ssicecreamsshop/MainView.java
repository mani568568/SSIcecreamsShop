package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigurationDialog;
import com.ssicecreamsshop.utils.NetworkStatusIndicator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class MainView {

    // --- Navy Blue Theme Colors ---
    private static final String PRIMARY_NAVY = "#1A237E"; // Dark Navy (like Indigo 900)
    private static final String PRIMARY_NAVY_DARK = "#0D47A1"; // Even Darker Navy/Blue (like Blue 900) for hovers
    private static final String PRIMARY_NAVY_LIGHT = "#C5CAE9"; // Light Indigo/Blue (like Indigo 100) for backgrounds
    private static final String ACCENT_BLUE = "#2962FF"; // Bright Blue (like Blue A700) for accents, buttons
    private static final String ACCENT_BLUE_DARK = "#0039CB"; // Darker Accent Blue for hover

    private static final String TEXT_ON_NAVY = "white";
    private static final String TEXT_ON_WHITE = "#212121"; // Dark Gray for text on light backgrounds
    private static final String BORDER_COLOR_LIGHT = "#9FA8DA"; // Indigo 200 (lighter border)
    private static final String BACKGROUND_LIGHT_NEUTRAL = "#E8EAF6"; // Very light Indigo/off-white
    private static final String ICON_BACKGROUND_COLOR = "#E3F2FD"; // Light Blue for icon backdrop (Blue 50)
    private static final String ICON_BORDER_COLOR = PRIMARY_NAVY_LIGHT;
    private static final String SHADOW_COLOR = "rgba(0,0,0,0.25)";


    private static NetworkStatusIndicator networkIndicator;

    private static final double CARD_ICON_CONTAINER_SIZE = 220;
    private static final double CARD_ICON_IMAGE_SIZE = 180;
    private static final double CARD_PADDING_VERTICAL = 70;
    private static final double CARD_PADDING_HORIZONTAL = 90;
    private static final String CARD_LABEL_FONT_SIZE = "38px";
    private static final double CARD_SPACING = 40;
    private static final double MAIN_TITLE_FONT_SIZE = 68;
    private static final double ROOT_VBOX_SPACING = 40;


    public static void show() {
        if (networkIndicator != null) {
            networkIndicator.stopMonitoring();
        }
        networkIndicator = new NetworkStatusIndicator();

        Label title = new Label("Ice Cream Shop");
        title.setStyle("-fx-font-size: " + MAIN_TITLE_FONT_SIZE + "px; -fx-text-fill: " + PRIMARY_NAVY + "; -fx-font-weight: bold; -fx-font-family: 'Arial Black', 'Impact', sans-serif;");
        title.setEffect(new DropShadow(12, Color.web(SHADOW_COLOR)));


        Button configButton = new Button("⚙️ Configuration");
        String configBtnBase = "-fx-font-size: 14px; -fx-text-fill: " + TEXT_ON_NAVY + "; -fx-padding: 10 18; -fx-background-radius: 20px; -fx-font-weight: bold;";
        configButton.setStyle(configBtnBase + "-fx-background-color: " + ACCENT_BLUE + ";");
        configButton.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        configButton.setOnMouseEntered(e -> configButton.setStyle(configBtnBase + "-fx-background-color: " + ACCENT_BLUE_DARK + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 8, 0.3, 0, 2);"));
        configButton.setOnMouseExited(e -> configButton.setStyle(configBtnBase + "-fx-background-color: " + ACCENT_BLUE + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 3, 0, 0, 0);"));
        configButton.setOnAction(e -> ConfigurationDialog.show());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topControlsBox = new HBox(15, networkIndicator, spacer, configButton);
        topControlsBox.setAlignment(Pos.CENTER_LEFT);
        topControlsBox.setPadding(new Insets(0, 25, 0, 25));


        // --- Order Card ---
        Node iceCreamIconDisplay;
        try {
            Image img = new Image(MainView.class.getResourceAsStream("/images/ice_cream.png"));
            if (img != null && !img.isError()) {
                ImageView actualIcon = new ImageView(img);
                actualIcon.setFitWidth(CARD_ICON_IMAGE_SIZE);
                actualIcon.setFitHeight(CARD_ICON_IMAGE_SIZE);
                actualIcon.setPreserveRatio(true);
                iceCreamIconDisplay = createStyledIconContainer(actualIcon, PRIMARY_NAVY); // Pass color for placeholder text
            } else {
                throw new NullPointerException("Image loaded but has error or is null.");
            }
        } catch (Exception e) {
            System.err.println("Error loading image: /images/ice_cream.png. " + e.getMessage());
            iceCreamIconDisplay = createPlaceholderIconView("🍦", CARD_ICON_IMAGE_SIZE, PRIMARY_NAVY);
        }


        Label orderLabel = new Label("Place New Order");
        orderLabel.setStyle("-fx-font-size: " + CARD_LABEL_FONT_SIZE + "; -fx-text-fill: " + TEXT_ON_NAVY + "; -fx-font-weight: bold;");
        VBox orderCard = new VBox(CARD_SPACING, iceCreamIconDisplay, orderLabel);
        orderCard.setAlignment(Pos.CENTER);
        String orderCardBaseStyle = "-fx-background-color: " + PRIMARY_NAVY + "; -fx-background-radius: 30px; -fx-padding: " + CARD_PADDING_VERTICAL + "px " + CARD_PADDING_HORIZONTAL + "px;";
        orderCard.setStyle(orderCardBaseStyle + " -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 20, 0.35, 0, 5);");
        orderCard.setOnMouseEntered(e -> orderCard.setStyle(orderCardBaseStyle + " -fx-background-color: " + PRIMARY_NAVY_DARK + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 25, 0.55, 0, 8); transform: scale(1.03);"));
        orderCard.setOnMouseExited(e -> orderCard.setStyle(orderCardBaseStyle + " -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 20, 0.35, 0, 5); transform: scale(1.0);"));
        orderCard.setOnMouseClicked(e -> {
            if (networkIndicator != null) networkIndicator.stopMonitoring();
            NewOrderView.show();
        });

        // --- Inventory Card ---
        Node clipboardIconDisplay;
        try {
            Image img = new Image(MainView.class.getResourceAsStream("/images/clipboard.png"));
            if (img != null && !img.isError()) {
                ImageView actualIcon = new ImageView(img);
                actualIcon.setFitWidth(CARD_ICON_IMAGE_SIZE);
                actualIcon.setFitHeight(CARD_ICON_IMAGE_SIZE);
                actualIcon.setPreserveRatio(true);
                clipboardIconDisplay = createStyledIconContainer(actualIcon, ACCENT_BLUE_DARK);
            } else {
                throw new NullPointerException("Image loaded but has error or is null.");
            }
        } catch (Exception e) {
            System.err.println("Error loading image: /images/clipboard.png. " + e.getMessage());
            clipboardIconDisplay = createPlaceholderIconView("📋", CARD_ICON_IMAGE_SIZE, ACCENT_BLUE_DARK);
        }


        Label inventoryLabel = new Label("Manage Inventory");
        inventoryLabel.setStyle("-fx-font-size: " + CARD_LABEL_FONT_SIZE + "; -fx-text-fill: " + TEXT_ON_NAVY + "; -fx-font-weight: bold;");
        VBox inventoryCard = new VBox(CARD_SPACING, clipboardIconDisplay, inventoryLabel);
        inventoryCard.setAlignment(Pos.CENTER);
        String inventoryCardBaseStyle = "-fx-background-color: " + ACCENT_BLUE + "; -fx-background-radius: 30px; -fx-padding: " + CARD_PADDING_VERTICAL + "px " + CARD_PADDING_HORIZONTAL + "px;";
        inventoryCard.setStyle(inventoryCardBaseStyle + " -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 20, 0.35, 0, 5);");
        inventoryCard.setOnMouseEntered(e -> inventoryCard.setStyle(inventoryCardBaseStyle + " -fx-background-color: " + ACCENT_BLUE_DARK + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 25, 0.55, 0, 8); transform: scale(1.03);"));
        inventoryCard.setOnMouseExited(e -> inventoryCard.setStyle(inventoryCardBaseStyle + " -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 20, 0.35, 0, 5); transform: scale(1.0);"));
        inventoryCard.setOnMouseClicked(e -> {
            if (networkIndicator != null) networkIndicator.stopMonitoring();
            ManageInventoryView.show();
        });

        HBox buttonRow = new HBox(100, orderCard, inventoryCard);
        buttonRow.setAlignment(Pos.CENTER);

        VBox root = new VBox(ROOT_VBOX_SPACING, title, topControlsBox, buttonRow);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40, 60, 60, 60));
        root.setStyle("-fx-background-color: " + BACKGROUND_LIGHT_NEUTRAL + ";");

        StackPane rootPane = new StackPane(root);
        StackPane.setAlignment(root, Pos.CENTER);
        AppLauncher.setScreen(rootPane);
    }

    private static StackPane createStyledIconContainer(ImageView iconImageView, String iconTextColor) {
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(CARD_ICON_CONTAINER_SIZE, CARD_ICON_CONTAINER_SIZE);
        iconContainer.setMinSize(CARD_ICON_CONTAINER_SIZE, CARD_ICON_CONTAINER_SIZE);
        iconContainer.setMaxSize(CARD_ICON_CONTAINER_SIZE, CARD_ICON_CONTAINER_SIZE);

        Circle backgroundCircle = new Circle(CARD_ICON_CONTAINER_SIZE / 2);
        backgroundCircle.setFill(Color.web(ICON_BACKGROUND_COLOR)); // Kept a light, neutral icon background
        backgroundCircle.setStroke(Color.web(ICON_BORDER_COLOR)); // Use a light navy/blue for border
        backgroundCircle.setStrokeWidth(2.5);
        backgroundCircle.setEffect(new DropShadow(7, Color.web(SHADOW_COLOR)));

        iconContainer.getChildren().addAll(backgroundCircle, iconImageView);
        StackPane.setAlignment(iconImageView, Pos.CENTER);
        return iconContainer;
    }

    private static StackPane createPlaceholderIconView(String emoji, double imageSize, String iconTextColor) {
        Label placeholderLabel = new Label(emoji);
        placeholderLabel.setStyle("-fx-font-size: " + (imageSize * 0.75) + "px; -fx-text-fill: " + iconTextColor + ";");

        StackPane placeholderContainer = new StackPane(placeholderLabel);
        placeholderContainer.setPrefSize(CARD_ICON_CONTAINER_SIZE, CARD_ICON_CONTAINER_SIZE);
        placeholderContainer.setMinSize(CARD_ICON_CONTAINER_SIZE, CARD_ICON_CONTAINER_SIZE);
        placeholderContainer.setMaxSize(CARD_ICON_CONTAINER_SIZE, CARD_ICON_CONTAINER_SIZE);

        Circle backgroundCircle = new Circle(CARD_ICON_CONTAINER_SIZE / 2);
        backgroundCircle.setFill(Color.web(ICON_BACKGROUND_COLOR)); // Kept a light, neutral icon background
        backgroundCircle.setStroke(Color.web(ICON_BORDER_COLOR)); // Use a light navy/blue for border
        backgroundCircle.setStrokeWidth(2.5);
        backgroundCircle.setEffect(new DropShadow(7, Color.web(SHADOW_COLOR)));

        placeholderContainer.getChildren().add(0, backgroundCircle);
        StackPane.setAlignment(placeholderLabel, Pos.CENTER);

        return placeholderContainer;
    }


    public static void stopNetworkIndicator() {
        if (networkIndicator != null) {
            networkIndicator.stopMonitoring();
            networkIndicator = null;
        }
    }
}
