package com.ssicecreamsshop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class NewOrderView {

    private static VBox cartBox;
    private static Label totalLabel;
    private static VBox menuVBox; // Displays menu items (TitledPanes)
    private static TextField searchField;
    private static Map<String, Integer> cartItems = new HashMap<>(); // Flavor -> Quantity

    // Prices for each ice cream flavor
    private static final Map<String, Integer> prices = Map.of(
            "Vanilla", 30, "Chocolate", 35, "Strawberry", 40,
            "Mango", 45, "Cookie Dough", 50, "Mint", 38
    );

    // Categories and their respective ice cream flavors
    private static final Map<String, List<String>> categories = Map.of(
            "Cones", List.of("Vanilla", "Strawberry", "Mango", "Mint"),
            "Cups", List.of("Cookie Dough"),
            "Bars", List.of("Chocolate")
    );

    // Store TitledPane references for expand/collapse all
    private static List<TitledPane> categoryPanes;

    public static void show() {
        // --- Top Bar: Back Button, Control Buttons ---
        Button backButton = new Button("← Back to Home");
        backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4a5568; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 8;");
        backButton.setOnMouseEntered(e -> backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #2d3748; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 8;"));
        backButton.setOnMouseExited(e -> backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4a5568; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 8;"));
        backButton.setOnAction(e -> MainView.show());

        Button expandAllBtn = new Button("Expand All");
        styleControlButton(expandAllBtn);
        expandAllBtn.setOnAction(e -> toggleAllCategoryPanes(true));

        Button collapseAllBtn = new Button("Collapse All");
        styleControlButton(collapseAllBtn);
        collapseAllBtn.setOnAction(e -> toggleAllCategoryPanes(false));

        HBox controlButtons = new HBox(10, expandAllBtn, collapseAllBtn);
        controlButtons.setAlignment(Pos.CENTER_LEFT);

        // Top bar container
        HBox topBar = new HBox(30, backButton, controlButtons); // Increased spacing
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 30, 15, 30)); // Adjusted padding
        topBar.setStyle("-fx-background-color: #e2e8f0;"); // Light gray background for top bar

        // --- Left Section: Menu ---
        // Search bar
        searchField = new TextField();
        searchField.setPromptText("Search Ice Cream Flavors...");
        searchField.setStyle("-fx-font-size: 14px; -fx-padding: 8px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        searchField.setMaxWidth(Double.MAX_VALUE); // Allow search to take available width
        searchField.textProperty().addListener((obs, oldVal, newVal) -> populateMenu(newVal));

        // Container for menu items (TitledPanes)
        menuVBox = new VBox(15); // Spacing between TitledPanes
        menuVBox.setPadding(new Insets(20));
        menuVBox.setStyle("-fx-background-color: #f7fafc;"); // Very light background for menu area
        populateMenu(""); // Initial population of the menu

        // Scrollable pane for the menu
        ScrollPane menuScroll = new ScrollPane(menuVBox);
        menuScroll.setFitToWidth(true);
        menuScroll.setFitToHeight(true);
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Hide horizontal scrollbar
        menuScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;"); // Make ScrollPane background transparent

        // --- Right Section: Cart ---
        cartBox = new VBox(10); // Spacing for items in cart
        cartBox.setPadding(new Insets(15));
        cartBox.setStyle("-fx-background-color: #fffff0; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;"); // Ivory background, light border

        ScrollPane cartScroll = new ScrollPane(cartBox);
        cartScroll.setFitToWidth(true);
        cartScroll.setFitToHeight(true);
        cartScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cartScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        totalLabel = new Label("Total: ₹0");
        totalLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c5282;"); // Dark blue text
        totalLabel.setPadding(new Insets(10, 0, 0, 0)); // Add some top padding

        Button clearBtn = new Button("Clear Cart");
        styleActionButton(clearBtn, "#ef4444", "#dc2626"); // Reddish color
        clearBtn.setOnAction(e -> {
            // Confirmation Dialog for clearing cart
            Alert confirmClear = new Alert(Alert.AlertType.CONFIRMATION);
            confirmClear.setTitle("Confirm Clear Cart");
            confirmClear.setHeaderText("Are you sure you want to empty your cart?");
            confirmClear.setContentText("This action cannot be undone.");
            Optional<ButtonType> result = confirmClear.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                cartItems.clear();
                refreshCart();
            }
        });

        Button placeOrderBtn = new Button("Place Order");
        styleActionButton(placeOrderBtn, "#10b981", "#059669"); // Greenish color
        placeOrderBtn.setOnAction(e -> {
            if (cartItems.isEmpty()) {
                Alert emptyCartAlert = new Alert(Alert.AlertType.WARNING, "Your cart is empty. Please add items to place an order.", ButtonType.OK);
                emptyCartAlert.setHeaderText(null);
                emptyCartAlert.setTitle("Empty Cart");
                emptyCartAlert.showAndWait();
                return;
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Order placed successfully!", ButtonType.OK);
            alert.setHeaderText(null);
            alert.setTitle("Order Confirmation");
            alert.showAndWait();
            cartItems.clear();
            refreshCart();
            MainView.show(); // Go back to main view after placing order
        });

        HBox actionButtons = new HBox(15, clearBtn, placeOrderBtn);
        actionButtons.setAlignment(Pos.CENTER_RIGHT); // Align buttons to the right
        actionButtons.setPadding(new Insets(10,0,0,0));

        // Cart section container
        VBox cartSection = new VBox(15, new Label("Your Order"), cartScroll, totalLabel, actionButtons);
        cartSection.setPadding(new Insets(20));
        cartSection.setStyle("-fx-background-color: #f0f9ff; -fx-background-radius: 8px;"); // Light blue background for cart section
        ((Label)cartSection.getChildren().get(0)).setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e40af;"); // Style "Your Order" label
        VBox.setVgrow(cartScroll, Priority.ALWAYS); // Make cartScroll take available vertical space

        // --- Main Layout: SplitPane for Menu and Cart ---
        SplitPane splitPane = new SplitPane(menuScroll, cartSection);
        splitPane.setDividerPositions(0.65); // Menu takes 65% of width, Cart 35%
        // Ensure SplitPane itself grows
        HBox.setHgrow(splitPane, Priority.ALWAYS);
        VBox.setVgrow(splitPane, Priority.ALWAYS);


        // Overall layout container
        VBox mainLayout = new VBox(topBar, splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS); // Ensure splitPane (and thus its content) fills vertical space
        mainLayout.setStyle("-fx-background-color: #cbd5e1;"); // A neutral background for the whole view

        // Set the screen
        AppLauncher.setScreen(mainLayout); // Use mainLayout directly
    }

    /**
     * Helper method to style control buttons (Expand/Collapse All).
     */
    private static void styleControlButton(Button button) {
        String baseStyle = "-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6px; -fx-text-fill: white;";
        String normalColor = "-fx-background-color: #64748b;"; // Slate
        String hoverColor = "-fx-background-color: #475569;";
        button.setStyle(baseStyle + normalColor);
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + hoverColor));
        button.setOnMouseExited(e -> button.setStyle(baseStyle + normalColor));
    }

    /**
     * Helper method to style action buttons (Clear Cart, Place Order).
     */
    private static void styleActionButton(Button button, String normalHex, String hoverHex) {
        String baseStyle = "-fx-font-size: 16px; -fx-padding: 10 20; -fx-background-radius: 8px; -fx-text-fill: white; -fx-font-weight: bold;";
        button.setStyle(baseStyle + "-fx-background-color: " + normalHex + ";");
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + "-fx-background-color: " + hoverHex + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0.2, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(baseStyle + "-fx-background-color: " + normalHex + ";"));
        button.setMinWidth(120); // Ensure buttons have a decent minimum width
    }


    /**
     * Populates the menuVBox with TitledPanes for each category,
     * containing cards for each ice cream flavor.
     * Filters flavors based on the search filter.
     * @param filter The text to filter ice cream flavors by (case-insensitive).
     */
    private static void populateMenu(String filter) {
        menuVBox.getChildren().clear(); // Clear previous items except search field
        menuVBox.getChildren().add(searchField); // Re-add search field at the top
        categoryPanes = new java.util.ArrayList<>(); // Reset list of panes

        for (Map.Entry<String, List<String>> categoryEntry : categories.entrySet()) {
            String categoryName = categoryEntry.getKey();
            List<String> flavorsInCategory = categoryEntry.getValue();

            List<String> filteredFlavors = flavorsInCategory.stream()
                    .filter(flavor -> filter == null || filter.isEmpty() || flavor.toLowerCase().contains(filter.toLowerCase()))
                    .collect(Collectors.toList());

            if (filteredFlavors.isEmpty() && !(filter == null || filter.isEmpty())) { // Only skip if filtering and no results
                continue;
            }

            FlowPane itemsPane = new FlowPane(15, 15); // Gap between items
            itemsPane.setPadding(new Insets(15));
            itemsPane.setAlignment(Pos.TOP_LEFT); // Align items from top-left

            for (String flavor : filteredFlavors) {
                itemsPane.getChildren().add(createFlavorCard(flavor));
            }

            TitledPane categoryPane = new TitledPane(categoryName + " (" + filteredFlavors.size() + ")", itemsPane);
            categoryPane.setAnimated(true); // Smooth expand/collapse
            categoryPane.setExpanded(true); // Default to expanded
            categoryPane.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #334155;"); // Style TitledPane title
            menuVBox.getChildren().add(categoryPane);
            categoryPanes.add(categoryPane);
        }
    }

    /**
     * Creates a VBox card for a given ice cream flavor.
     * @param flavor The name of the ice cream flavor.
     * @return A VBox representing the flavor card.
     */
    private static VBox createFlavorCard(String flavor) {
        int price = prices.getOrDefault(flavor, 0);

        ImageView imgView = new ImageView();
        try {
            String imagePath = "/images/" + flavor.toLowerCase().replace(" ", "_") + ".png";
            Image img = new Image(NewOrderView.class.getResourceAsStream(imagePath));
            if (img.isError()) { // Check if image loaded correctly
                throw new NullPointerException("Image load error for " + imagePath);
            }
            imgView.setImage(img);
        } catch (Exception e) {
            System.err.println("Error loading image for " + flavor + ": " + e.getMessage());
            // Fallback: Placeholder if image is missing
            Label placeholder = new Label(flavor.substring(0,1)); // First letter as placeholder
            placeholder.setStyle("-fx-font-size: 30px; -fx-alignment: center; -fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-background-color: #e2e8f0;");
            placeholder.setPrefSize(100,100);
            placeholder.setAlignment(Pos.CENTER);
            imgView.setImage(null); // Ensure no broken image icon
            // In a real app, you might set a default placeholder image to imgView
        }
        imgView.setFitWidth(100);
        imgView.setFitHeight(100);
        imgView.setPreserveRatio(true);

        Label nameLabel = new Label(flavor);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label priceLabel = new Label("₹" + price);
        priceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        VBox card = new VBox(8, imgView.getImage() != null ? imgView : createPlaceholderGraphic(flavor) , nameLabel, priceLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        String baseCardStyle = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-width: 1px;";
        String hoverCardStyle = "-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #94a3b8; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.3, 0, 2);";

        card.setStyle(baseCardStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverCardStyle));
        card.setOnMouseExited(e -> card.setStyle(baseCardStyle));
        card.setOnMouseClicked(e -> addToCart(flavor));
        card.setMinWidth(150); // Ensure cards have a minimum width
        card.setMaxWidth(150); // And a maximum width for consistency in FlowPane

        return card;
    }

    private static Pane createPlaceholderGraphic(String flavor) {
        Label placeholderText = new Label(flavor.length() > 0 ? flavor.substring(0, 1).toUpperCase() : "?");
        placeholderText.setFont(Font.font("System", FontWeight.BOLD, 40));
        placeholderText.setTextFill(Color.SLATEGRAY);
        StackPane placeholderPane = new StackPane(placeholderText);
        placeholderPane.setPrefSize(100, 100);
        placeholderPane.setMinSize(100,100);
        placeholderPane.setMaxSize(100,100);
        placeholderPane.setStyle("-fx-background-color: #e2e8f0; -fx-border-color: #cbd5e1; -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        placeholderPane.setAlignment(Pos.CENTER);
        return placeholderPane;
    }


    /**
     * Adds an item to the cart or increments its quantity.
     * @param flavor The flavor to add.
     */
    private static void addToCart(String flavor) {
        cartItems.put(flavor, cartItems.getOrDefault(flavor, 0) + 1);
        refreshCart();
    }

    /**
     * Refreshes the cart display with current items and total.
     */
    private static void refreshCart() {
        cartBox.getChildren().clear();
        if (cartItems.isEmpty()) {
            Label emptyCartLabel = new Label("Your cart is empty.");
            emptyCartLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            emptyCartLabel.setPadding(new Insets(20));
            cartBox.getChildren().add(emptyCartLabel);
            cartBox.setAlignment(Pos.CENTER); // Center the empty message
        } else {
            cartBox.setAlignment(Pos.TOP_LEFT); // Align items to top when not empty
            double currentTotal = 0;
            for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
                String flavor = entry.getKey();
                int quantity = entry.getValue();
                int pricePerItem = prices.getOrDefault(flavor, 0);
                double subtotal = quantity * pricePerItem;
                currentTotal += subtotal;

                cartBox.getChildren().add(createCartItemBox(flavor, quantity, pricePerItem, subtotal));
            }
            totalLabel.setText(String.format("Total: ₹%.2f", currentTotal));
        }
        // If cartItems became empty after an operation (e.g. removing last item)
        if (cartItems.isEmpty()) {
            totalLabel.setText("Total: ₹0.00");
        }
    }

    /**
     * Creates an HBox representing an item in the cart.
     */
    private static HBox createCartItemBox(String flavor, int quantity, int pricePerItem, double subtotal) {
        ImageView imgView = new ImageView();
        try {
            String imagePath = "/images/" + flavor.toLowerCase().replace(" ", "_") + ".png";
            Image img = new Image(NewOrderView.class.getResourceAsStream(imagePath));
            if (img.isError()) {
                throw new NullPointerException("Image load error for " + imagePath);
            }
            imgView.setImage(img);
        } catch (Exception e) {
            // Silently ignore, placeholder will be used by createPlaceholderGraphic
            imgView.setImage(null);
        }
        imgView.setFitWidth(50);
        imgView.setFitHeight(50);
        imgView.setPreserveRatio(true);

        Label nameLabel = new Label(flavor);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label priceInfoLabel = new Label(String.format("₹%d x %d", pricePerItem, quantity));
        priceInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
        Label subtotalLabel = new Label(String.format("Sub: ₹%.2f", subtotal));
        subtotalLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");


        Button plusButton = new Button("+");
        styleCartControlButton(plusButton);
        plusButton.setOnAction(e -> {
            cartItems.put(flavor, cartItems.get(flavor) + 1);
            refreshCart();
        });

        Label quantityLabel = new Label(String.valueOf(quantity));
        quantityLabel.setStyle("-fx-font-size: 14px; -fx-padding: 0 5px;");


        Button minusButton = new Button("-");
        styleCartControlButton(minusButton);
        minusButton.setOnAction(e -> {
            if (cartItems.get(flavor) > 1) {
                cartItems.put(flavor, cartItems.get(flavor) - 1);
            } else {
                cartItems.remove(flavor);
            }
            refreshCart();
        });

        HBox quantityControls = new HBox(5, minusButton, quantityLabel, plusButton);
        quantityControls.setAlignment(Pos.CENTER_LEFT);

        VBox itemDetails = new VBox(5, nameLabel, priceInfoLabel, quantityControls);
        itemDetails.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(itemDetails, Priority.ALWAYS); // Allow itemDetails to take available space

        VBox subtotalAndRemove = new VBox(5, subtotalLabel);
        subtotalAndRemove.setAlignment(Pos.CENTER_RIGHT);


        HBox cartItemBox = new HBox(10, (imgView.getImage() != null ? imgView : createPlaceholderGraphic(flavor)), itemDetails, subtotalAndRemove);
        cartItemBox.setPadding(new Insets(10));
        cartItemBox.setStyle("-fx-background-color: #f0f9ff; -fx-background-radius: 8px; -fx-border-color: #e0f2fe; -fx-border-width: 1px;");
        cartItemBox.setAlignment(Pos.CENTER_LEFT);

        // Ensure placeholder also has a fixed size in cart
        if(imgView.getImage() == null) {
            Pane placeholder = (Pane) cartItemBox.getChildren().get(0);
            placeholder.setPrefSize(50,50);
            placeholder.setMinSize(50,50);
            placeholder.setMaxSize(50,50);
            ((Label)((StackPane)placeholder).getChildren().get(0)).setFont(Font.font("System", FontWeight.BOLD, 20));
        }


        return cartItemBox;
    }

    private static void styleCartControlButton(Button button) {
        button.setStyle("-fx-font-size: 12px; -fx-padding: 3 7; -fx-background-radius: 4px; -fx-background-color: #d1d5db; -fx-text-fill: #1f2937;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-font-size: 12px; -fx-padding: 3 7; -fx-background-radius: 4px; -fx-background-color: #9ca3af; -fx-text-fill: #1f2937;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-font-size: 12px; -fx-padding: 3 7; -fx-background-radius: 4px; -fx-background-color: #d1d5db; -fx-text-fill: #1f2937;"));
    }


    /**
     * Expands or collapses all category TitledPanes in the menu.
     * @param expand True to expand all, false to collapse all.
     */
    private static void toggleAllCategoryPanes(boolean expand) {
        if (categoryPanes != null) {
            for (TitledPane pane : categoryPanes) {
                pane.setExpanded(expand);
            }
        }
    }
}
