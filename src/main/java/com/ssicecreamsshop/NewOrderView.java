package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigManager;
import com.ssicecreamsshop.model.Order;
import com.ssicecreamsshop.model.OrderItem;
import com.ssicecreamsshop.utils.NetworkStatusIndicator;
import com.ssicecreamsshop.utils.OrderExcelUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow; // For material shadow effect
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class NewOrderView {

    // --- Blue Material Theme Colors ---
    private static final String PRIMARY_BLUE = "#1E88E5"; // Blue 600 (a bit richer than 500)
    private static final String PRIMARY_BLUE_DARK = "#1565C0"; // Blue 700/800 (for hover, darker elements)
    private static final String PRIMARY_BLUE_LIGHT = "#90CAF9"; // Blue 200 (for light backgrounds/accents)
    private static final String ACCENT_BLUE = "#42A5F5"; // Blue 400 (for important actions, slightly lighter)
    private static final String ACCENT_BLUE_DARK = "#1976D2"; // Blue 700 (darker accent for hover)

    private static final String TEXT_ON_BLUE = "white";
    private static final String TEXT_ON_WHITE_PRIMARY = "#212121"; // Primary text on light bg
    private static final String TEXT_ON_WHITE_SECONDARY = "#757575"; // Secondary text on light bg
    private static final String BORDER_COLOR_LIGHT = "#BBDEFB"; // Blue 100 (very light border)
    private static final String BACKGROUND_MAIN = "#E3F2FD"; // Blue 50 (very light blue for overall background)
    private static final String BACKGROUND_CONTENT_AREA = "#FFFFFF"; // White for content cards/areas
    private static final String BACKGROUND_ACCENT_LIGHT = "#E1F5FE"; // Light Cyan A100 (subtle accent bg)
    private static final String SHADOW_COLOR = "rgba(0,0,0,0.15)"; // Softer shadow
    private static final String BUTTON_ACTION_GREEN = "#4CAF50"; // Green for confirm
    private static final String BUTTON_ACTION_GREEN_HOVER = "#388E3C";
    private static final String BUTTON_ACTION_RED = "#F44336"; // Red for clear/cancel
    private static final String BUTTON_ACTION_RED_HOVER = "#D32F2F";


    // MenuItem inner class remains the same
    private static class MenuItem {
        String name;
        String imageName;
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
    private static VBox menuVBox;
    private static TextField searchField;
    private static Map<String, Integer> cartItems = new HashMap<>();

    private static final Map<String, List<MenuItem>> categorizedMenuItems = new LinkedHashMap<>();
    private static final Map<String, MenuItem> allMenuItems = new HashMap<>();
    private static List<TitledPane> categoryPanesList = new ArrayList<>();

    private static NetworkStatusIndicator networkIndicator;

    static {
        loadMenuItemsFromJson();
    }

    public static void loadMenuItemsFromJson() {
        String menuJsonPathString = ConfigManager.getMenuItemsJsonPath();
        Path menuJsonPath = Paths.get(menuJsonPathString);

        categorizedMenuItems.clear();
        allMenuItems.clear();

        if (!Files.exists(menuJsonPath)) {
            System.err.println("Menu JSON file not found at configured path: " + menuJsonPathString);
            showErrorDialog("Menu Configuration Error",
                    "Menu file not found: " + menuJsonPathString +
                            "\nPlease check Configuration or add items via Manage Inventory.");
            if (menuVBox != null) refreshMenuView();
            return;
        }

        try (InputStream inputStream = Files.newInputStream(menuJsonPath)) {
            String jsonText = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            JSONObject jsonObject = new JSONObject(jsonText);
            JSONArray categoriesArray = jsonObject.getJSONArray("categories");

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
                    allMenuItems.put(itemName, menuItem);
                }
                if (!itemList.isEmpty()) {
                    categorizedMenuItems.put(categoryName, itemList);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading menu JSON from '" + menuJsonPathString + "': " + e.getMessage());
            showErrorDialog("File Read Error", "Error reading menu configuration from '" + menuJsonPathString + "': " + e.getMessage());
        } catch (JSONException e) {
            System.err.println("Error parsing menu JSON from '" + menuJsonPathString + "': " + e.getMessage());
            showErrorDialog("JSON Parsing Error", "Error parsing menu configuration from '" + menuJsonPathString + "': " + e.getMessage() + ". Please check the JSON file format.");
        } catch (Exception e) {
            System.err.println("An unexpected error occurred while loading menu items: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Unexpected Error", "An unexpected error occurred while loading menu items: " + e.getMessage());
        }
        if (menuVBox != null) refreshMenuView();
    }

    private static void showErrorDialog(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-background-color: " + BACKGROUND_MAIN +";");
            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            if(okButton != null) okButton.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_ON_BLUE + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;");
            alert.showAndWait();
        });
    }


    public static void show() {
        if (networkIndicator != null) {
            networkIndicator.stopMonitoring();
        }
        networkIndicator = new NetworkStatusIndicator();

        Button backButton = new Button("← Back");
        String backBtnBase = "-fx-font-size: 14px; -fx-text-fill: " + TEXT_ON_BLUE + "; -fx-padding: 8 18; -fx-background-radius: 20px; -fx-font-weight: bold;"; // Adjusted padding
        backButton.setStyle(backBtnBase + "-fx-background-color: " + PRIMARY_BLUE_DARK + ";");
        backButton.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        backButton.setOnMouseEntered(e -> backButton.setStyle(backBtnBase + "-fx-background-color: " + ACCENT_BLUE_DARK + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 7, 0.2, 0, 2);"));
        backButton.setOnMouseExited(e -> backButton.setStyle(backBtnBase + "-fx-background-color: " + PRIMARY_BLUE_DARK + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 3, 0, 0, 0);"));
        backButton.setOnAction(e -> {
            if (networkIndicator != null) networkIndicator.stopMonitoring();
            MainView.show();
        });

        Button expandAllBtn = new Button("Expand All");
        styleControlButton(expandAllBtn, PRIMARY_BLUE, PRIMARY_BLUE_DARK);
        expandAllBtn.setOnAction(e -> toggleAllCategoryPanes(true));

        Button viewOrdersButton = new Button("📜 Orders");
        styleControlButton(viewOrdersButton, PRIMARY_BLUE, PRIMARY_BLUE_DARK);
        viewOrdersButton.setOnAction(e -> {
            ViewOrdersView.show();
        });

        Button collapseAllBtn = new Button("Collapse All");
        styleControlButton(collapseAllBtn, PRIMARY_BLUE, PRIMARY_BLUE_DARK);
        collapseAllBtn.setOnAction(e -> toggleAllCategoryPanes(false));

        Button refreshMenuBtn = new Button("🔄 Refresh");
        styleControlButton(refreshMenuBtn, PRIMARY_BLUE, PRIMARY_BLUE_DARK);
        refreshMenuBtn.setOnAction(e -> {
            loadMenuItemsFromJson();
        });

        HBox controlButtons = new HBox(10, viewOrdersButton, expandAllBtn, collapseAllBtn, refreshMenuBtn);
        controlButtons.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(15, networkIndicator, backButton, controlButtons, spacer);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 25, 15, 25)); // Adjusted padding
        topBar.setStyle("-fx-background-color: " + PRIMARY_BLUE_LIGHT + "; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 0 0 1.5px 0;");
        topBar.setEffect(new DropShadow(5, 0, 2, Color.web(SHADOW_COLOR))); // Subtle shadow for top bar

        searchField = new TextField();
        searchField.setPromptText("Search Ice Cream...");
        searchField.setStyle("-fx-font-size: 14px; -fx-padding: 10px 15px; -fx-background-radius: 25px; -fx-border-radius: 25px; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-background-color: white;");
        searchField.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> populateMenu(newVal));

        menuVBox = new VBox(18); // Increased spacing
        menuVBox.setPadding(new Insets(20));
        menuVBox.setStyle("-fx-background-color: " + BACKGROUND_CONTENT_AREA + ";");

        ScrollPane menuScroll = new ScrollPane(menuVBox);
        menuScroll.setFitToWidth(true);
        menuScroll.setFitToHeight(true);
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        menuScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        populateMenu("");

        cartBox = new VBox(12);
        cartBox.setPadding(new Insets(20));
        cartBox.setStyle("-fx-background-color: " + BACKGROUND_ACCENT_LIGHT + "; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 1px; -fx-border-radius: 15px; -fx-background-radius: 15px;");
        cartBox.setEffect(new DropShadow(8, 2, 2, Color.web(SHADOW_COLOR)));


        ScrollPane cartScroll = new ScrollPane(cartBox);
        cartScroll.setFitToWidth(true);
        cartScroll.setFitToHeight(true);
        cartScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cartScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        totalLabel = new Label("Total: ₹0.00");
        totalLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_BLUE_DARK + ";");
        totalLabel.setPadding(new Insets(15, 0, 5, 0));

        Button clearBtn = new Button("Clear Cart");
        styleActionButton(clearBtn, BUTTON_ACTION_RED, BUTTON_ACTION_RED_HOVER);
        clearBtn.setOnAction(e -> {
            Alert confirmClear = new Alert(Alert.AlertType.CONFIRMATION);
            confirmClear.setTitle("Confirm Clear Cart");
            confirmClear.setHeaderText("Are you sure you want to empty your cart?");
            confirmClear.setContentText("This action cannot be undone.");
            DialogPane dialogPane = confirmClear.getDialogPane();
            dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-background-color: " + BACKGROUND_MAIN +";");
            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            if(okButton != null) okButton.setStyle("-fx-background-color: " + BUTTON_ACTION_RED + "; -fx-text-fill: " + TEXT_ON_BLUE + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;");
            Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
            if(cancelButton != null) cancelButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;");
            Optional<ButtonType> result = confirmClear.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                cartItems.clear();
                refreshCart();
            }
        });

        Button placeOrderBtn = new Button("Place Order 🛍️");
        styleActionButton(placeOrderBtn, BUTTON_ACTION_GREEN, BUTTON_ACTION_GREEN_HOVER);
        placeOrderBtn.setOnAction(e -> {
            if (cartItems.isEmpty()) {
                Alert emptyCartAlert = new Alert(Alert.AlertType.WARNING, "Your cart is empty. Please add items to place an order.", ButtonType.OK);
                emptyCartAlert.setHeaderText(null);
                emptyCartAlert.setTitle("Empty Cart");
                DialogPane dialogPane = emptyCartAlert.getDialogPane();
                dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-background-color: " + BACKGROUND_MAIN +";");
                dialogPane.lookup(".button").setStyle("-fx-background-color: " + PRIMARY_BLUE + "; -fx-text-fill: " + TEXT_ON_BLUE + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;");
                emptyCartAlert.showAndWait();
                return;
            }

            List<OrderItem> currentOrderItems = new ArrayList<>();
            for (Map.Entry<String, Integer> cartEntry : cartItems.entrySet()) {
                String itemName = cartEntry.getKey();
                Integer quantity = cartEntry.getValue();
                MenuItem menuItemDetails = allMenuItems.get(itemName);

                if (menuItemDetails != null) {
                    currentOrderItems.add(new OrderItem(itemName, quantity, menuItemDetails.getPrice()));
                } else {
                    System.err.println("Error: Could not find details for item in cart: " + itemName);
                }
            }

            if (!currentOrderItems.isEmpty()) {
                Order newOrder = new Order(currentOrderItems);
                OrderExcelUtil.saveOrderToExcel(newOrder);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Order placed successfully and saved!\nOrder ID: " + newOrder.getOrderId(), ButtonType.OK);
                alert.setHeaderText(null);
                alert.setTitle("Order Confirmation");
                DialogPane dialogPane = alert.getDialogPane();
                dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-background-color: " + BACKGROUND_MAIN +";");
                dialogPane.lookup(".button").setStyle("-fx-background-color: " + BUTTON_ACTION_GREEN + "; -fx-text-fill: " + TEXT_ON_BLUE + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;");
                alert.showAndWait();
            } else {
                showErrorDialog("Order Processing Error", "Could not process order. No valid items found in cart.");
                return;
            }

            cartItems.clear();
            refreshCart();
            if (networkIndicator != null) networkIndicator.stopMonitoring();
            MainView.show();
        });

        HBox actionButtons = new HBox(20, clearBtn, placeOrderBtn);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.setPadding(new Insets(15,0,5,0));

        Label cartTitleLabel = new Label("Your Order 🛒");
        cartTitleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_BLUE_DARK + ";");

        VBox cartSection = new VBox(15, cartTitleLabel, cartScroll, totalLabel, actionButtons);
        cartSection.setPadding(new Insets(20));
        cartSection.setStyle("-fx-background-color: " + BACKGROUND_CONTENT_AREA + "; -fx-background-radius: 15px;"); // White background for cart area
        VBox.setVgrow(cartScroll, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(menuScroll, cartSection);
        splitPane.setDividerPositions(0.65);
        splitPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"); // Make SplitPane transparent

        VBox mainLayout = new VBox(topBar, splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        mainLayout.setStyle("-fx-background-color: " + BACKGROUND_MAIN + ";"); // Overall background

        AppLauncher.setScreen(mainLayout);
        refreshCart();
    }

    private static void styleControlButton(Button button, String baseColor, String hoverColor) {
        String baseStyle = "-fx-font-size: 13px; -fx-padding: 7 14; -fx-background-radius: 18px; -fx-text-fill: " + TEXT_ON_BLUE + "; -fx-font-weight: bold;"; // Adjusted padding & font
        button.setStyle(baseStyle + "-fx-background-color: " + baseColor + ";");
        button.setEffect(new DropShadow(2, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + "-fx-background-color: " + hoverColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 5, 0.15, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(baseStyle + "-fx-background-color: " + baseColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 2, 0, 0, 0);"));
    }

    private static void styleActionButton(Button button, String normalHex, String hoverHex) {
        String baseStyle = "-fx-font-size: 17px; -fx-padding: 12 25; -fx-background-radius: 25px; -fx-text-fill: " + TEXT_ON_BLUE + "; -fx-font-weight: bold;"; // Slightly larger
        button.setStyle(baseStyle + "-fx-background-color: " + normalHex + ";");
        button.setEffect(new DropShadow(4, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + "-fx-background-color: " + hoverHex + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 8, 0.25, 0, 2);"));
        button.setOnMouseExited(e -> button.setStyle(baseStyle + "-fx-background-color: " + normalHex + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 4, 0, 0, 0);"));
        button.setMinWidth(150);
    }

    private static void updateMenuDisplay(String filter) {
        if (menuVBox == null || searchField == null) {
            System.err.println("Menu UI components not ready for display update.");
            return;
        }
        menuVBox.getChildren().clear();
        menuVBox.getChildren().add(searchField);
        categoryPanesList.clear();

        if (categorizedMenuItems.isEmpty()) {
            Label infoLabel = new Label("🍦 No menu items loaded or available.\nCheck configuration or add items via Manage Inventory.");
            infoLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXT_ON_WHITE_SECONDARY + "; -fx-padding: 20px; -fx-alignment: center;");
            infoLabel.setWrapText(true);
            menuVBox.getChildren().add(infoLabel);
            return;
        }

        String lowerCaseFilter = (filter == null) ? "" : filter.toLowerCase().trim();

        for (Map.Entry<String, List<MenuItem>> categoryEntry : categorizedMenuItems.entrySet()) {
            String categoryName = categoryEntry.getKey();
            List<MenuItem> itemsInCategory = categoryEntry.getValue();

            List<MenuItem> filteredItems = itemsInCategory.stream()
                    .filter(item -> {
                        if (lowerCaseFilter.isEmpty()) return true;
                        boolean nameMatches = item.getName().toLowerCase().contains(lowerCaseFilter);
                        boolean priceMatches = String.valueOf(item.getPrice()).contains(lowerCaseFilter);
                        return nameMatches || priceMatches;
                    })
                    .collect(Collectors.toList());

            if (filteredItems.isEmpty() && !lowerCaseFilter.isEmpty()) continue;

            FlowPane itemsPane = new FlowPane(18, 18);
            itemsPane.setPadding(new Insets(15));
            itemsPane.setAlignment(Pos.TOP_LEFT);

            for (MenuItem item : filteredItems) {
                itemsPane.getChildren().add(createFlavorCard(item));
            }

            if (!itemsPane.getChildren().isEmpty() || lowerCaseFilter.isEmpty()) {
                TitledPane categoryPane = new TitledPane(categoryName + " (" + filteredItems.size() + ")", itemsPane);
                categoryPane.setAnimated(true);
                categoryPane.setExpanded(true);
                // Themed TitledPane
                categoryPane.setStyle(
                        "-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_BLUE_DARK + ";" +
                                "-fx-base: " + PRIMARY_BLUE_LIGHT + ";" + // Affects arrow and background on hover
                                "-fx-body-color: " + BACKGROUND_CONTENT_AREA + ";" + // Background of content
                                "-fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 1px; " +
                                "-fx-border-radius: 8px; -fx-background-radius: 8px;"
                );
                categoryPane.setEffect(new DropShadow(2, Color.web(SHADOW_COLOR)));
                menuVBox.getChildren().add(categoryPane);
                categoryPanesList.add(categoryPane);
            }
        }
        if (menuVBox.getChildren().size() == 1 && !lowerCaseFilter.isEmpty()) {
            Label noResultsLabel = new Label("😢 No items match your search: '" + filter + "'");
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + TEXT_ON_WHITE_SECONDARY + "; -fx-padding: 10px;");
            menuVBox.getChildren().add(noResultsLabel);
        }
    }

    private static void populateMenu(String filter) {
        updateMenuDisplay(filter);
    }

    public static void refreshMenuView() {
        if (menuVBox != null && searchField != null) {
            updateMenuDisplay(searchField.getText());
        } else {
            System.out.println("NewOrderView UI not fully initialized. Menu display will update when view is shown.");
        }
    }


    private static VBox createFlavorCard(MenuItem item) {
        ImageView imgView = new ImageView();
        String imageBasePath = ConfigManager.getImagePath();
        Path imageFilePath = Paths.get(imageBasePath, item.getImageName());

        try {
            if (Files.exists(imageFilePath) && !Files.isDirectory(imageFilePath)) {
                try (InputStream imageStream = Files.newInputStream(imageFilePath)) {
                    Image img = new Image(imageStream);
                    if (img.isError()) {
                        System.err.println("JavaFX Image error for " + imageFilePath + ": " + (img.getException() != null ? img.getException().getMessage() : "Unknown error"));
                        imgView.setImage(null);
                    } else {
                        imgView.setImage(img);
                    }
                }
            } else {
                System.err.println("Image file not found or is a directory: " + imageFilePath.toString());
                imgView.setImage(null);
            }
        } catch (IOException e) {
            System.err.println("IOException loading image " + imageFilePath + " for " + item.getName() + ": " + e.getMessage());
            imgView.setImage(null);
        } catch (Exception e) {
            System.err.println("Unexpected error loading image " + imageFilePath + " for " + item.getName() + ": " + e.getMessage());
            e.printStackTrace();
            imgView.setImage(null);
        }

        imgView.setFitWidth(110);
        imgView.setFitHeight(110);
        imgView.setPreserveRatio(true);
        // Add rounded corners to images
        if (imgView.getImage() != null) {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(
                    imgView.getFitWidth(), imgView.getFitHeight()
            );
            clip.setArcWidth(15); clip.setArcHeight(15); // More rounding
            imgView.setClip(clip);
        }


        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_ON_WHITE_PRIMARY + ";");
        Label priceLabel = new Label("₹" + item.getPrice());
        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ACCENT_BLUE + "; -fx-font-weight: bold;");

        Node imageNode = imgView.getImage() != null ? imgView : createPlaceholderGraphic(item.getName(), 110);

        VBox card = new VBox(10, imageNode, nameLabel, priceLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        String baseCardStyle = "-fx-background-color: " + BACKGROUND_CONTENT_AREA + "; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 1px;";
        String hoverCardStyle = "-fx-background-color: #f0f8ff; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: " + PRIMARY_BLUE + "; -fx-border-width: 1.5px; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 12, 0.3, 0, 3);";

        card.setStyle(baseCardStyle + " -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 6, 0.1, 0, 1);");
        card.setOnMouseEntered(e -> card.setStyle(hoverCardStyle));
        card.setOnMouseExited(e -> card.setStyle(baseCardStyle + " -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 6, 0.1, 0, 1);"));
        card.setOnMouseClicked(e -> addToCart(item.getName()));
        card.setMinWidth(160);
        card.setMaxWidth(160);

        return card;
    }

    private static Pane createPlaceholderGraphic(String text, double size) {
        Label placeholderText = new Label(text.length() > 0 ? text.substring(0, 1).toUpperCase() : "🍦");
        placeholderText.setFont(Font.font("Arial", FontWeight.BOLD, size * 0.5));
        placeholderText.setTextFill(Color.web(PRIMARY_BLUE_DARK));
        StackPane placeholderPane = new StackPane(placeholderText);
        placeholderPane.setPrefSize(size, size);
        placeholderPane.setMinSize(size,size);
        placeholderPane.setMaxSize(size,size);
        placeholderPane.setStyle("-fx-background-color: " + PRIMARY_BLUE_LIGHT + "; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 1.5px; -fx-background-radius: " + (size/2) + "px; -fx-border-radius: " + (size/2) + "px;"); // Make placeholder circular
        return placeholderPane;
    }

    private static void addToCart(String itemName) {
        if (!allMenuItems.containsKey(itemName)) {
            System.err.println("Attempted to add unknown item to cart: " + itemName);
            return;
        }
        cartItems.put(itemName, cartItems.getOrDefault(itemName, 0) + 1);
        refreshCart();
    }

    private static void refreshCart() {
        cartBox.getChildren().clear();
        if (cartItems.isEmpty()) {
            Label emptyCartLabel = new Label("Your cart is empty... Add some treats! 😋");
            emptyCartLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: " + TEXT_ON_WHITE_SECONDARY + "; -fx-font-weight: bold;");
            emptyCartLabel.setPadding(new Insets(25));
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
            MenuItem item = allMenuItems.get(itemName);

            if (item == null) {
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
        String imageBasePath = ConfigManager.getImagePath();
        Path imageFilePath = Paths.get(imageBasePath, item.getImageName());

        try {
            if (Files.exists(imageFilePath) && !Files.isDirectory(imageFilePath)) {
                try (InputStream imageStream = Files.newInputStream(imageFilePath)) {
                    Image img = new Image(imageStream);
                    if (img.isError()) {
                        System.err.println("JavaFX Image error for cart item " + imageFilePath + ": " + (img.getException() != null ? img.getException().getMessage() : "Unknown error"));
                        imgView.setImage(null);
                    } else {
                        imgView.setImage(img);
                    }
                }
            } else {
                System.err.println("Cart image file not found or is directory: " + imageFilePath.toString());
                imgView.setImage(null);
            }
        } catch (IOException e) {
            System.err.println("IOException loading cart image " + imageFilePath + " for " + item.getName() + ": " + e.getMessage());
            imgView.setImage(null);
        } catch (Exception e) {
            System.err.println("Unexpected error loading cart image " + imageFilePath + " for " + item.getName() + ": " + e.getMessage());
            imgView.setImage(null);
        }
        imgView.setFitWidth(60);
        imgView.setFitHeight(60);
        imgView.setPreserveRatio(true);
        if (imgView.getImage() != null) {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(
                    imgView.getFitWidth(), imgView.getFitHeight()
            );
            clip.setArcWidth(10); clip.setArcHeight(10);
            imgView.setClip(clip);
        }


        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_BLUE_DARK + ";");
        Label priceInfoLabel = new Label(String.format("₹%d x %d", item.getPrice(), quantity));
        priceInfoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_ON_WHITE_SECONDARY + ";");
        Label subtotalLabelText = new Label(String.format("Sub: ₹%.2f", subtotal));
        subtotalLabelText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_BLUE + ";");

        Button plusButton = new Button("➕");
        styleCartControlButton(plusButton);
        plusButton.setOnAction(e -> {
            cartItems.put(item.getName(), cartItems.get(item.getName()) + 1);
            refreshCart();
        });

        Label quantityLabel = new Label(String.valueOf(quantity));
        quantityLabel.setStyle("-fx-font-size: 16px; -fx-padding: 0 10px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_ON_WHITE_PRIMARY + ";");

        Button minusButton = new Button("➖");
        styleCartControlButton(minusButton);
        minusButton.setOnAction(e -> {
            if (cartItems.get(item.getName()) > 1) {
                cartItems.put(item.getName(), cartItems.get(item.getName()) - 1);
            } else {
                cartItems.remove(item.getName());
            }
            refreshCart();
        });

        HBox quantityControls = new HBox(10, minusButton, quantityLabel, plusButton);
        quantityControls.setAlignment(Pos.CENTER_LEFT);

        VBox itemDetails = new VBox(6, nameLabel, priceInfoLabel, quantityControls);
        itemDetails.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(itemDetails, Priority.ALWAYS);

        VBox subtotalAndRemove = new VBox(5, subtotalLabelText);
        subtotalAndRemove.setAlignment(Pos.CENTER_RIGHT);

        Node imageNodeCart = imgView.getImage() != null ? imgView : createPlaceholderGraphic(item.getName(), 60);

        HBox cartItemBox = new HBox(15, imageNodeCart, itemDetails, subtotalAndRemove);
        cartItemBox.setPadding(new Insets(12));
        cartItemBox.setStyle("-fx-background-color: " + BACKGROUND_CONTENT_AREA + "; -fx-background-radius: 10px; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 1px; -fx-border-radius: 10px;");
        cartItemBox.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        cartItemBox.setAlignment(Pos.CENTER_LEFT);

        return cartItemBox;
    }

    private static void styleCartControlButton(Button button) {
        String baseCartBtnStyle = "-fx-font-size: 18px; -fx-padding: 6 12; -fx-background-radius: 50px; -fx-text-fill: " + TEXT_ON_BLUE + "; -fx-font-weight: bold;";
        button.setStyle(baseCartBtnStyle + "-fx-background-color: " + ACCENT_BLUE + ";");
        button.setEffect(new DropShadow(2, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(baseCartBtnStyle + "-fx-background-color: " + ACCENT_BLUE_DARK + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 4, 0.15, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(baseCartBtnStyle + "-fx-background-color: " + ACCENT_BLUE + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 2, 0, 0, 0);"));
    }


    private static void toggleAllCategoryPanes(boolean expand) {
        if (categoryPanesList != null) {
            for (TitledPane pane : categoryPanesList) {
                pane.setExpanded(expand);
            }
        }
    }
    public static void stopNetworkIndicator() {
        if (networkIndicator != null) {
            networkIndicator.stopMonitoring();
            networkIndicator = null;
        }
    }
}
