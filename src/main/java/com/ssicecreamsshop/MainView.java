package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigurationDialog;
// import com.ssicecreamsshop.utils.NetworkStatusIndicator; // REMOVED
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class MainView {

    // --- Navy Blue & Yellow Theme Colors ---
    private static final String PRIMARY_NAVY = "#1A237E";
    private static final String PRIMARY_NAVY_DARK = "#283593";
    private static final String ACCENT_YELLOW = "#FFC107";
    private static final String ACCENT_YELLOW_DARK = "#FFA000";
    private static final String ACCENT_BLUE = "#2962FF";
    private static final String ACCENT_BLUE_DARK = "#0039CB";
    private static final String TEXT_ON_DARK = "white";
    private static final String TEXT_ON_YELLOW = "#212121";
    private static final String BACKGROUND_LIGHT = "#E8EAF6";
    private static final String ICON_BACKGROUND_COLOR = "white";
    private static final String ICON_BORDER_COLOR = "#CFD8DC";
    private static final String SHADOW_COLOR = "rgba(26, 35, 126, 0.3)";

    // Adjusted sizes for three cards
    private static final double CARD_ICON_CONTAINER_SIZE = 150;
    private static final double CARD_ICON_IMAGE_SIZE = 110;
    private static final double CARD_PADDING_VERTICAL = 50;
    private static final double CARD_PADDING_HORIZONTAL = 60;
    private static final String CARD_LABEL_FONT_SIZE = "28px";
    private static final double CARD_SPACING = 30;
    private static final double MAIN_TITLE_FONT_SIZE = 64;
    private static final double ROOT_VBOX_SPACING = 30;

    public static void show() {
        Label title = new Label("Ice Cream Shop");
        title.setStyle("-fx-font-size: " + MAIN_TITLE_FONT_SIZE + "px; -fx-text-fill: " + PRIMARY_NAVY + "; -fx-font-weight: bold; -fx-font-family: 'Arial Black', 'Impact', sans-serif;");
        title.setEffect(new DropShadow(15, Color.web(SHADOW_COLOR)));

        Button configButton = new Button("⚙️ Configuration");
        String configBtnBase = "-fx-font-size: 14px; -fx-text-fill: " + TEXT_ON_YELLOW + "; -fx-padding: 10 18; -fx-background-radius: 20px; -fx-font-weight: bold;";
        configButton.setStyle(configBtnBase + "-fx-background-color: " + ACCENT_YELLOW + ";");
        configButton.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        configButton.setOnMouseEntered(e -> configButton.setStyle(configBtnBase + "-fx-background-color: " + ACCENT_YELLOW_DARK + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 8, 0.3, 0, 2);"));
        configButton.setOnMouseExited(e -> configButton.setStyle(configBtnBase + "-fx-background-color: " + ACCENT_YELLOW + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 3, 0, 0, 0);"));
        configButton.setOnAction(e -> ConfigurationDialog.show());

        HBox configBar = new HBox(configButton);
        configBar.setAlignment(Pos.TOP_RIGHT);
        configBar.setPadding(new Insets(10, 25, 10, 25));

        // --- Place Order Card ---
        Node iceCreamIconDisplay = createIconNode("/images/ice_cream.png", "🍦", PRIMARY_NAVY);
        VBox orderCard = createActionCard("Place New Order", iceCreamIconDisplay, PRIMARY_NAVY, PRIMARY_NAVY_DARK, e -> NewOrderView.show());

        // --- Add Item Card ---
        Node addItemIconDisplay = createIconNode("/images/clipboard.png", "➕", ACCENT_BLUE_DARK);
        VBox addItemCard = createActionCard("Add New Item", addItemIconDisplay, ACCENT_BLUE, ACCENT_BLUE_DARK, e -> ManageInventoryView.show());

        // --- Update Stock Card (NEW) ---
        Node updateStockIconDisplay = createIconNode("/images/inventory_update.png", "📦", ACCENT_YELLOW_DARK); // Suggest using a new icon
        VBox updateStockCard = createActionCard("Update Stock", updateStockIconDisplay, ACCENT_YELLOW, ACCENT_YELLOW_DARK, e -> UpdateInventoryView.show());
        // Custom text color for yellow card
        updateStockCard.getChildren().forEach(node -> {
            if (node instanceof Label) ((Label)node).setStyle("-fx-font-size: " + CARD_LABEL_FONT_SIZE + "; -fx-text-fill: " + TEXT_ON_YELLOW + "; -fx-font-weight: bold;");
        });


        HBox buttonRow = new HBox(80, orderCard, addItemCard, updateStockCard);
        buttonRow.setAlignment(Pos.CENTER);

        VBox contentBox = new VBox(ROOT_VBOX_SPACING, title, buttonRow);
        contentBox.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(configBar);
        root.setCenter(contentBox);
        root.setStyle("-fx-background-color: " + BACKGROUND_LIGHT + ";");
        root.setPadding(new Insets(20, 40, 60, 40));

        AppLauncher.setScreen(root);
    }

    private static VBox createActionCard(String text, Node icon, String baseColor, String hoverColor, javafx.event.EventHandler<javafx.scene.input.MouseEvent> handler) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: " + CARD_LABEL_FONT_SIZE + "; -fx-text-fill: " + TEXT_ON_DARK + "; -fx-font-weight: bold;");
        VBox card = new VBox(CARD_SPACING, icon, label);
        card.setAlignment(Pos.CENTER);
        String cardBaseStyle = "-fx-background-color: " + baseColor + "; -fx-background-radius: 30px; -fx-padding: " + CARD_PADDING_VERTICAL + "px " + CARD_PADDING_HORIZONTAL + "px;";
        card.setStyle(cardBaseStyle + " -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 20, 0.35, 0, 5);");
        card.setOnMouseEntered(e -> card.setStyle(cardBaseStyle + " -fx-background-color: " + hoverColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 25, 0.55, 0, 8); transform: scale(1.03);"));
        card.setOnMouseExited(e -> card.setStyle(cardBaseStyle + " -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 20, 0.35, 0, 5); transform: scale(1.0);"));
        card.setOnMouseClicked(handler);
        return card;
    }

    private static Node createIconNode(String imagePath, String emoji, String placeholderTextColor) {
        try {
            Image img = new Image(MainView.class.getResourceAsStream(imagePath));
            if (img != null && !img.isError()) {
                ImageView actualIcon = new ImageView(img);
                actualIcon.setFitWidth(CARD_ICON_IMAGE_SIZE);
                actualIcon.setFitHeight(CARD_ICON_IMAGE_SIZE);
                actualIcon.setPreserveRatio(true);
                return createStyledIconContainer(actualIcon);
            } else {
                throw new NullPointerException("Image loaded but has error or is null.");
            }
        } catch (Exception e) {
            return createPlaceholderIconView(emoji, CARD_ICON_IMAGE_SIZE, placeholderTextColor);
        }
    }

    private static StackPane createStyledIconContainer(ImageView iconImageView) {
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(CARD_ICON_CONTAINER_SIZE, CARD_ICON_CONTAINER_SIZE);
        Circle backgroundCircle = new Circle(CARD_ICON_CONTAINER_SIZE / 2, Color.web(ICON_BACKGROUND_COLOR));
        backgroundCircle.setStroke(Color.web(ICON_BORDER_COLOR));
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
        Circle backgroundCircle = new Circle(CARD_ICON_CONTAINER_SIZE / 2, Color.web(ICON_BACKGROUND_COLOR));
        backgroundCircle.setStroke(Color.web(ICON_BORDER_COLOR));
        backgroundCircle.setStrokeWidth(2.5);
        backgroundCircle.setEffect(new DropShadow(7, Color.web(SHADOW_COLOR)));
        placeholderContainer.getChildren().add(0, backgroundCircle);
        StackPane.setAlignment(placeholderLabel, Pos.CENTER);
        return placeholderContainer;
    }
}
