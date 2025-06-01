package com.ssicecreamsshop;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

// Required for JSON parsing. Ensure org.json.jar is in your classpath
// or added as a dependency in your build tool (e.g., Maven, Gradle).
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap; // Preserves category order
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class NewOrderView {

    // Inner class to represent a menu item
    private static class MenuItem {
        String name;
        String imageName; // e.g., "vanilla.png"
        int price;

        MenuItem(String name, String imageName, int price) {
            this.name = name;
            this.imageName = imageName;
            this.price = price;
        }

        public String getName() { return name; }
        public String getImageName() { return imageName; }
        public int getPrice() { return price; }
    }

    private static VBox cartBox;
    private static Label totalLabel;
    private static VBox menuVBox; // Displays menu items (TitledPanes)
    private static TextField searchField;
    private static Map<String, Integer> cartItems = new HashMap<>(); // Flavor Name (String) -> Quantity (Integer)

    // Data structures to hold loaded menu items
    // Map: Category Name -> List of MenuItems in that category
    private static final Map<String, List<MenuItem>> categorizedMenuItems = new LinkedHashMap<>();
    // Map: Flavor Name -> MenuItem object (for quick lookup of details)
    private static final Map<String, MenuItem> allMenuItems = new HashMap<>();

    // Store TitledPane references for expand/collapse all
    private static List<TitledPane> categoryPanesList; // Renamed to avoid conflict

    // Static initializer block to load menu items from JSON when the class is loaded
    static {
        loadMenuItemsFromJson();
    }

    /**
     * Loads menu items from the menu_items.json file.
     * Populates categorizedMenuItems and allMenuItems.
     */
    private static void loadMenuItemsFromJson() {
        String jsonFilePath = "/menu_items.json"; // Path within resources
        try (InputStream inputStream = NewOrderView.class.getResourceAsStream(jsonFilePath)) {
            if (inputStream == null) {
                System.err.println("Cannot find " + jsonFilePath + ". Please ensure it's in the resources folder.");
                showErrorDialog("Menu Configuration Error", "Could not load menu items (" + jsonFilePath + " not found). Please contact support.");
                return;
            }

            // Read the JSON file content
            String jsonText = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            JSONObject jsonObject = new JSONObject(jsonText);
            JSONArray categoriesArray = jsonObject.getJSONArray("categories");

            categorizedMenuItems.clear();
            allMenuItems.clear();

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                String categoryName = categoryObj.getString("name");
                JSONArray itemsArray = categoryObj.getJSONArray("items");
                List<MenuItem> itemList = new ArrayList<>();

                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject itemObj = itemsArray.getJSONObject(j);
                    String itemName = itemObj.getString("name");
                    String itemImageName = itemObj.getString("imageName");
                    int itemPrice = itemObj.getInt("price");

                    MenuItem menuItem = new MenuItem(itemName, itemImageName, itemPrice);
                    itemList.add(menuItem);
                    allMenuItems.put(itemName, menuItem); // For quick lookup by name
                }
                if (!itemList.isEmpty()) {
                    categorizedMenuItems.put(categoryName, itemList);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading " + jsonFilePath + ": " + e.getMessage());
            showErrorDialog("File Read Error", "Error reading menu configuration: " + e.getMessage());
        } catch (JSONException e) {
            System.err.println("Error parsing " + jsonFilePath + ": " + e.getMessage());
            showErrorDialog("JSON Parsing Error", "Error parsing menu configuration: " + e.getMessage() + ". Please check the JSON file format.");
        } catch (Exception e) {
            System.err.println("An unexpected error occurred while loading menu items: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed debugging
            showErrorDialog("Unexpected Error", "An unexpected error occurred while loading menu items: " + e.getMessage());
        }
    }

    /**
     * Displays an error dialog to the user.
     * Ensures the dialog is shown on the JavaFX Application Thread.
     * @param title Title of the error dialog.
     * @param content Content message of the error dialog.
     */
    private static void showErrorDialog(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
            // Consider disabling UI elements or navigating away if the error is critical
        });
    }


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

        HBox topBar = new HBox(30, backButton, controlButtons);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 30, 15, 30));
        topBar.setStyle("-fx-background-color: #e2e8f0;");

        // --- Left Section: Menu ---
        searchField = new TextField();
        searchField.setPromptText("Search Ice Cream (Name or Price)..."); // Updated prompt
        searchField.setStyle("-fx-font-size: 14px; -fx-padding: 8px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> populateMenu(newVal));

        menuVBox = new VBox(15);
        menuVBox.setPadding(new Insets(20));
        menuVBox.setStyle("-fx-background-color: #f7fafc;");
        populateMenu(""); // Initial population

        ScrollPane menuScroll = new ScrollPane(menuVBox);
        menuScroll.setFitToWidth(true);
        menuScroll.setFitToHeight(true);
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        menuScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // --- Right Section: Cart ---
        cartBox = new VBox(10);
        cartBox.setPadding(new Insets(15));
        cartBox.setStyle("-fx-background-color: #fffff0; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        ScrollPane cartScroll = new ScrollPane(cartBox);
        cartScroll.setFitToWidth(true);
        cartScroll.setFitToHeight(true);
        cartScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cartScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        totalLabel = new Label("Total: ₹0.00");
        totalLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c5282;");
        totalLabel.setPadding(new Insets(10, 0, 0, 0));

        Button clearBtn = new Button("Clear Cart");
        styleActionButton(clearBtn, "#ef4444", "#dc2626");
        clearBtn.setOnAction(e -> {
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
        styleActionButton(placeOrderBtn, "#10b981", "#059669");
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
            MainView.show();
        });

        HBox actionButtons = new HBox(15, clearBtn, placeOrderBtn);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.setPadding(new Insets(10,0,0,0));

        Label cartTitleLabel = new Label("Your Order");
        cartTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e40af;");

        VBox cartSection = new VBox(15, cartTitleLabel, cartScroll, totalLabel, actionButtons);
        cartSection.setPadding(new Insets(20));
        cartSection.setStyle("-fx-background-color: #f0f9ff; -fx-background-radius: 8px;");
        VBox.setVgrow(cartScroll, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(menuScroll, cartSection);
        splitPane.setDividerPositions(0.65);
        HBox.setHgrow(splitPane, Priority.ALWAYS);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        VBox mainLayout = new VBox(topBar, splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        mainLayout.setStyle("-fx-background-color: #cbd5e1;");

        AppLauncher.setScreen(mainLayout);
        refreshCart(); // Refresh cart display on initial show
    }

    private static void styleControlButton(Button button) {
        String baseStyle = "-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6px; -fx-text-fill: white;";
        String normalColor = "-fx-background-color: #64748b;";
        String hoverColor = "-fx-background-color: #475569;";
        button.setStyle(baseStyle + normalColor);
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + hoverColor));
        button.setOnMouseExited(e -> button.setStyle(baseStyle + normalColor));
    }

    private static void styleActionButton(Button button, String normalHex, String hoverHex) {
        String baseStyle = "-fx-font-size: 16px; -fx-padding: 10 20; -fx-background-radius: 8px; -fx-text-fill: white; -fx-font-weight: bold;";
        button.setStyle(baseStyle + "-fx-background-color: " + normalHex + ";");
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + "-fx-background-color: " + hoverHex + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0.2, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(baseStyle + "-fx-background-color: " + normalHex + ";"));
        button.setMinWidth(120);
    }

    private static void populateMenu(String filter) {
        menuVBox.getChildren().clear();
        menuVBox.getChildren().add(searchField);
        categoryPanesList = new ArrayList<>();

        if (categorizedMenuItems.isEmpty() && allMenuItems.isEmpty()) {
            Label errorLabel = new Label("Menu items could not be loaded.\nPlease check the configuration or contact support.");
            errorLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: red; -fx-padding: 20px;");
            errorLabel.setAlignment(Pos.CENTER);
            menuVBox.getChildren().add(errorLabel);
            return;
        }

        String lowerCaseFilter = (filter == null) ? "" : filter.toLowerCase().trim();

        for (Map.Entry<String, List<MenuItem>> categoryEntry : categorizedMenuItems.entrySet()) {
            String categoryName = categoryEntry.getKey();
            List<MenuItem> itemsInCategory = categoryEntry.getValue();

            List<MenuItem> filteredItems = itemsInCategory.stream()
                    .filter(item -> {
                        if (lowerCaseFilter.isEmpty()) {
                            return true; // No filter, show all items
                        }
                        // Check if name contains the filter
                        boolean nameMatches = item.getName().toLowerCase().contains(lowerCaseFilter);
                        // Check if price (as string) contains the filter
                        boolean priceMatches = String.valueOf(item.getPrice()).contains(lowerCaseFilter);
                        return nameMatches || priceMatches;
                    })
                    .collect(Collectors.toList());

            if (filteredItems.isEmpty() && !lowerCaseFilter.isEmpty()) {
                continue; // Skip category if filter applied and no items match in this category
            }

            FlowPane itemsPane = new FlowPane(15, 15);
            itemsPane.setPadding(new Insets(15));
            itemsPane.setAlignment(Pos.TOP_LEFT);

            for (MenuItem item : filteredItems) {
                itemsPane.getChildren().add(createFlavorCard(item));
            }

            if (!itemsPane.getChildren().isEmpty() || lowerCaseFilter.isEmpty()) {
                TitledPane categoryPane = new TitledPane(categoryName + " (" + filteredItems.size() + ")", itemsPane);
                categoryPane.setAnimated(true);
                categoryPane.setExpanded(true); // Default to expanded, or based on filter
                categoryPane.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #334155;");
                menuVBox.getChildren().add(categoryPane);
                categoryPanesList.add(categoryPane);
            }
        }
    }

    private static VBox createFlavorCard(MenuItem item) {
        ImageView imgView = new ImageView();
        String imagePath = "/images/" + item.getImageName(); // Image path from MenuItem
        try {
            Image img = new Image(NewOrderView.class.getResourceAsStream(imagePath));
            if (img.isError()) {
                throw new NullPointerException("Image load error for " + imagePath + ": " + img.getException());
            }
            imgView.setImage(img);
        } catch (Exception e) {
            System.err.println("Error loading image " + imagePath + " for " + item.getName() + ": " + e.getMessage());
            imgView.setImage(null); // Important to set to null if error
        }
        imgView.setFitWidth(100);
        imgView.setFitHeight(100);
        imgView.setPreserveRatio(true);

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label priceLabel = new Label("₹" + item.getPrice());
        priceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        Node imageNode = imgView.getImage() != null ? imgView : createPlaceholderGraphic(item.getName(), 100);

        VBox card = new VBox(8, imageNode, nameLabel, priceLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        String baseCardStyle = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-width: 1px;";
        String hoverCardStyle = "-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #94a3b8; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.3, 0, 2);";

        card.setStyle(baseCardStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverCardStyle));
        card.setOnMouseExited(e -> card.setStyle(baseCardStyle));
        card.setOnMouseClicked(e -> addToCart(item.getName())); // Add by name
        card.setMinWidth(150);
        card.setMaxWidth(150);

        return card;
    }

    private static Pane createPlaceholderGraphic(String text, double size) {
        Label placeholderText = new Label(text.length() > 0 ? text.substring(0, 1).toUpperCase() : "?");
        placeholderText.setFont(Font.font("System", FontWeight.BOLD, size * 0.4)); // Scale font size
        placeholderText.setTextFill(Color.SLATEGRAY);
        StackPane placeholderPane = new StackPane(placeholderText);
        placeholderPane.setPrefSize(size, size);
        placeholderPane.setMinSize(size,size);
        placeholderPane.setMaxSize(size,size);
        placeholderPane.setStyle("-fx-background-color: #e2e8f0; -fx-border-color: #cbd5e1; -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        placeholderPane.setAlignment(Pos.CENTER);
        return placeholderPane;
    }

    private static void addToCart(String itemName) {
        if (!allMenuItems.containsKey(itemName)) {
            System.err.println("Attempted to add unknown item to cart: " + itemName);
            return; // Do not add if item is not recognized
        }
        cartItems.put(itemName, cartItems.getOrDefault(itemName, 0) + 1);
        refreshCart();
    }

    private static void refreshCart() {
        cartBox.getChildren().clear();
        if (cartItems.isEmpty()) {
            Label emptyCartLabel = new Label("Your cart is empty.");
            emptyCartLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            emptyCartLabel.setPadding(new Insets(20));
            cartBox.getChildren().add(emptyCartLabel);
            cartBox.setAlignment(Pos.CENTER);
            totalLabel.setText("Total: ₹0.00");
            return;
        }

        cartBox.setAlignment(Pos.TOP_LEFT);
        double currentTotal = 0;
        for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
            String itemName = entry.getKey();
            int quantity = entry.getValue();
            MenuItem item = allMenuItems.get(itemName); // Get MenuItem from its name

            if (item == null) { // Should not happen if addToCart is robust
                System.err.println("Item " + itemName + " not found in allMenuItems during cart refresh.");
                continue;
            }

            double subtotal = quantity * item.getPrice();
            currentTotal += subtotal;
            cartBox.getChildren().add(createCartItemBox(item, quantity, subtotal));
        }
        totalLabel.setText(String.format("Total: ₹%.2f", currentTotal));
    }

    private static HBox createCartItemBox(MenuItem item, int quantity, double subtotal) {
        ImageView imgView = new ImageView();
        String imagePath = "/images/" + item.getImageName();
        try {
            Image img = new Image(NewOrderView.class.getResourceAsStream(imagePath));
            if (img.isError()) {
                throw new NullPointerException("Image load error for " + imagePath + ": " + img.getException());
            }
            imgView.setImage(img);
        } catch (Exception e) {
            System.err.println("Error loading cart image " + imagePath + " for " + item.getName() + ": " + e.getMessage());
            imgView.setImage(null);
        }
        imgView.setFitWidth(50);
        imgView.setFitHeight(50);
        imgView.setPreserveRatio(true);

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label priceInfoLabel = new Label(String.format("₹%d x %d", item.getPrice(), quantity));
        priceInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
        Label subtotalLabelText = new Label(String.format("Sub: ₹%.2f", subtotal));
        subtotalLabelText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        Button plusButton = new Button("+");
        styleCartControlButton(plusButton);
        plusButton.setOnAction(e -> {
            cartItems.put(item.getName(), cartItems.get(item.getName()) + 1);
            refreshCart();
        });

        Label quantityLabel = new Label(String.valueOf(quantity));
        quantityLabel.setStyle("-fx-font-size: 14px; -fx-padding: 0 5px;");

        Button minusButton = new Button("-");
        styleCartControlButton(minusButton);
        minusButton.setOnAction(e -> {
            if (cartItems.get(item.getName()) > 1) {
                cartItems.put(item.getName(), cartItems.get(item.getName()) - 1);
            } else {
                cartItems.remove(item.getName());
            }
            refreshCart();
        });

        HBox quantityControls = new HBox(5, minusButton, quantityLabel, plusButton);
        quantityControls.setAlignment(Pos.CENTER_LEFT);

        VBox itemDetails = new VBox(5, nameLabel, priceInfoLabel, quantityControls);
        itemDetails.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(itemDetails, Priority.ALWAYS);

        VBox subtotalAndRemove = new VBox(5, subtotalLabelText);
        subtotalAndRemove.setAlignment(Pos.CENTER_RIGHT);

        Node imageNodeCart = imgView.getImage() != null ? imgView : createPlaceholderGraphic(item.getName(), 50);

        HBox cartItemBox = new HBox(10, imageNodeCart, itemDetails, subtotalAndRemove);
        cartItemBox.setPadding(new Insets(10));
        cartItemBox.setStyle("-fx-background-color: #f0f9ff; -fx-background-radius: 8px; -fx-border-color: #e0f2fe; -fx-border-width: 1px;");
        cartItemBox.setAlignment(Pos.CENTER_LEFT);

        return cartItemBox;
    }

    private static void styleCartControlButton(Button button) {
        button.setStyle("-fx-font-size: 12px; -fx-padding: 3 7; -fx-background-radius: 4px; -fx-background-color: #d1d5db; -fx-text-fill: #1f2937;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-font-size: 12px; -fx-padding: 3 7; -fx-background-radius: 4px; -fx-background-color: #9ca3af; -fx-text-fill: #1f2937;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-font-size: 12px; -fx-padding: 3 7; -fx-background-radius: 4px; -fx-background-color: #d1d5db; -fx-text-fill: #1f2937;"));
    }

    private static void toggleAllCategoryPanes(boolean expand) {
        if (categoryPanesList != null) {
            for (TitledPane pane : categoryPanesList) {
                pane.setExpanded(expand);
            }
        }
    }
}
