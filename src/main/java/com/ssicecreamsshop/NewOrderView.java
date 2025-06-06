package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigManager;
import com.ssicecreamsshop.model.Order;
import com.ssicecreamsshop.model.OrderItem;
import com.ssicecreamsshop.utils.OrderExcelUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
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

    // --- Navy Blue & Yellow Theme Colors (Complete Set) ---
    private static final String PRIMARY_NAVY = "#1A237E";
    private static final String PRIMARY_NAVY_DARK = "#283593";
    private static final String PRIMARY_NAVY_LIGHT = "#C5CAE9";
    private static final String ACCENT_YELLOW = "#FFC107";
    private static final String ACCENT_YELLOW_DARK = "#FFA000";
    private static final String ACCENT_BLUE = "#42A5F5";
    private static final String ACCENT_BLUE_DARK = "#1976D2";

    private static final String TEXT_ON_DARK = "white";
    private static final String TEXT_ON_LIGHT_PRIMARY = "#212121";
    private static final String TEXT_ON_LIGHT_SECONDARY = "#757575";
    private static final String BORDER_COLOR_LIGHT = "#CFD8DC";
    private static final String BACKGROUND_MAIN = "#E8EAF6";
    private static final String BACKGROUND_CONTENT = "#FFFFFF";
    private static final String BACKGROUND_ACCENT_AREA = "#FFFDE7";
    private static final String SHADOW_COLOR = "rgba(26, 35, 126, 0.2)";

    private static final String BUTTON_ACTION_GREEN = "#4CAF50";
    private static final String BUTTON_ACTION_GREEN_HOVER = "#388E3C";
    private static final String BUTTON_ACTION_RED = "#F44336";
    private static final String BUTTON_ACTION_RED_HOVER = "#D32F2F";

    private static class MenuItem {
        String name;
        String imageName;
        int price;
        Integer quantity;

        MenuItem(String name, String imageName, int price, Integer quantity) {
            this.name = name;
            this.imageName = imageName;
            this.price = price;
            this.quantity = quantity;
        }
        public String getName() { return name; }
        public String getImageName() { return imageName; }
        public int getPrice() { return price; }
        public Integer getQuantity() { return quantity; }
        public boolean hasLimitedStock() { return quantity != null; }
    }

    private static VBox cartBox;
    private static Label totalLabel;
    private static VBox menuVBox;
    private static TextField searchField;
    private static Map<String, Integer> cartItems = new HashMap<>();

    private static final Map<String, List<MenuItem>> categorizedMenuItems = new LinkedHashMap<>();
    private static final Map<String, MenuItem> allMenuItems = new HashMap<>();
    private static List<TitledPane> categoryPanesList = new ArrayList<>();

    static {
        loadMenuItemsFromJson();
    }

    public static void loadMenuItemsFromJson() {
        String menuJsonPathString = ConfigManager.getMenuItemsJsonPath();
        Path menuJsonPath = Paths.get(menuJsonPathString);
        categorizedMenuItems.clear();
        allMenuItems.clear();
        if (!Files.exists(menuJsonPath)) {
            showErrorDialog("Menu Configuration Error", "Menu file not found.");
            return;
        }
        try (InputStream inputStream = Files.newInputStream(menuJsonPath)) {
            String jsonText = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            JSONObject jsonObject = new JSONObject(jsonText);
            JSONArray categoriesArray = jsonObject.optJSONArray("categories");
            if (categoriesArray == null) return;

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                String categoryName = categoryObj.getString("name");
                JSONArray itemsArray = categoryObj.getJSONArray("items");
                List<MenuItem> itemList = new ArrayList<>();
                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject itemObj = itemsArray.getJSONObject(j);
                    Integer quantity = itemObj.has("quantity") ? itemObj.getInt("quantity") : null;
                    MenuItem menuItem = new MenuItem(itemObj.getString("name"), itemObj.getString("imageName"), itemObj.getInt("price"), quantity);
                    itemList.add(menuItem);
                    allMenuItems.put(menuItem.getName(), menuItem);
                }
                if (!itemList.isEmpty()) categorizedMenuItems.put(categoryName, itemList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("Menu Load Error", "Error loading menu: " + e.getMessage());
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
            if(okButton != null) okButton.setStyle("-fx-background-color: " + BUTTON_ACTION_RED + "; -fx-text-fill: " + TEXT_ON_DARK + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;");
            alert.showAndWait();
        });
    }

    public static void show() {
        Button backButton = new Button("← Back");
        String backBtnBase = "-fx-font-size: 14px; -fx-text-fill: " + TEXT_ON_LIGHT_PRIMARY + "; -fx-padding: 8 18; -fx-background-radius: 20px; -fx-font-weight: bold;";
        backButton.setStyle(backBtnBase + "-fx-background-color: " + PRIMARY_NAVY_LIGHT + ";");
        backButton.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        backButton.setOnMouseEntered(e -> backButton.setStyle(backBtnBase + "-fx-background-color: " + BORDER_COLOR_LIGHT + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 7, 0.2, 0, 2);"));
        backButton.setOnMouseExited(e -> backButton.setStyle(backBtnBase + "-fx-background-color: " + PRIMARY_NAVY_LIGHT + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 3, 0, 0, 0);"));
        backButton.setOnAction(e -> MainView.show());

        Button expandAllBtn = new Button("Expand All");
        styleControlButton(expandAllBtn, PRIMARY_NAVY, PRIMARY_NAVY_DARK);
        expandAllBtn.setOnAction(e -> toggleAllCategoryPanes(true));

        Button viewOrdersButton = new Button("📜 Orders");
        styleControlButton(viewOrdersButton, PRIMARY_NAVY, PRIMARY_NAVY_DARK);
        viewOrdersButton.setOnAction(e -> ViewOrdersView.show());

        Button collapseAllBtn = new Button("Collapse All");
        styleControlButton(collapseAllBtn, PRIMARY_NAVY, PRIMARY_NAVY_DARK);
        collapseAllBtn.setOnAction(e -> toggleAllCategoryPanes(false));

        Button refreshMenuBtn = new Button("🔄 Refresh");
        styleControlButton(refreshMenuBtn, PRIMARY_NAVY, PRIMARY_NAVY_DARK);
        refreshMenuBtn.setOnAction(e -> loadMenuItemsFromJson());

        HBox controlButtons = new HBox(10, viewOrdersButton, expandAllBtn, collapseAllBtn, refreshMenuBtn);
        controlButtons.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(15, backButton, controlButtons, spacer);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setStyle("-fx-background-color: " + BACKGROUND_CONTENT + "; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 0 0 1.5px 0;");
        topBar.setEffect(new DropShadow(5, 0, 2, Color.web(SHADOW_COLOR)));

        searchField = new TextField();
        searchField.setPromptText("Search Ice Cream...");
        searchField.setStyle("-fx-font-size: 14px; -fx-padding: 10px 15px; -fx-background-radius: 25px; -fx-border-radius: 25px; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-background-color: white;");
        searchField.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> populateMenu(newVal));

        menuVBox = new VBox(18);
        menuVBox.setPadding(new Insets(20));
        menuVBox.setStyle("-fx-background-color: transparent;");

        ScrollPane menuScroll = new ScrollPane(menuVBox);
        menuScroll.setFitToWidth(true);
        menuScroll.setFitToHeight(true);
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        menuScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        cartBox = new VBox(12);
        cartBox.setPadding(new Insets(20));
        cartBox.setStyle("-fx-background-color: " + BACKGROUND_ACCENT_AREA + "; -fx-border-color: #FDD835; -fx-border-width: 1px; -fx-border-radius: 15px; -fx-background-radius: 15px;");
        cartBox.setEffect(new DropShadow(8, 2, 2, Color.web(SHADOW_COLOR)));

        ScrollPane cartScroll = new ScrollPane(cartBox);
        cartScroll.setFitToWidth(true);
        cartScroll.setFitToHeight(true);
        cartScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cartScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        totalLabel = new Label("Total: ₹0.00");
        totalLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY + ";");
        totalLabel.setPadding(new Insets(15, 0, 5, 0));

        Button clearBtn = new Button("Clear Cart");
        styleActionButton(clearBtn, BUTTON_ACTION_RED, BUTTON_ACTION_RED_HOVER);
        clearBtn.setOnAction(e -> {
            Alert confirmClear = new Alert(Alert.AlertType.CONFIRMATION);
            confirmClear.setTitle("Confirm Clear Cart");
            confirmClear.setHeaderText("Are you sure you want to empty your cart?");
            Optional<ButtonType> result = confirmClear.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                cartItems.clear();
                refreshCart();
            }
        });

        Button placeOrderBtn = new Button("Place Order 🛍️");
        styleActionButton(placeOrderBtn, BUTTON_ACTION_GREEN, BUTTON_ACTION_GREEN_HOVER);
        placeOrderBtn.setOnAction(e -> handlePlaceOrder());

        HBox actionButtons = new HBox(20, clearBtn, placeOrderBtn);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.setPadding(new Insets(15,0,5,0));

        Label cartTitleLabel = new Label("Your Order 🛒");
        cartTitleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY_DARK + ";");

        VBox cartSection = new VBox(15, cartTitleLabel, cartScroll, totalLabel, actionButtons);
        cartSection.setPadding(new Insets(20));
        cartSection.setStyle("-fx-background-color: " + BACKGROUND_CONTENT + "; -fx-background-radius: 15px;");
        VBox.setVgrow(cartScroll, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(menuScroll, cartSection);
        splitPane.setDividerPositions(0.65);
        splitPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        VBox mainLayout = new VBox(topBar, splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        mainLayout.setStyle("-fx-background-color: " + BACKGROUND_MAIN + ";");

        populateMenu("");
        AppLauncher.setScreen(mainLayout);
    }

    // --- Helper Methods Start Here ---

    private static void populateMenu(String filter) {
        updateMenuDisplay(filter);
    }

    private static void updateMenuDisplay(String filter) {
        if (menuVBox == null || searchField == null) return;

        menuVBox.getChildren().clear();
        menuVBox.getChildren().add(searchField);
        categoryPanesList.clear();

        if (categorizedMenuItems.isEmpty()) {
            Label infoLabel = new Label("🍦 No menu items loaded or available.\nCheck configuration or add items via Manage Inventory.");
            infoLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXT_ON_LIGHT_SECONDARY + "; -fx-padding: 20px; -fx-alignment: center;");
            infoLabel.setWrapText(true);
            menuVBox.getChildren().add(infoLabel);
            return;
        }

        String lowerCaseFilter = (filter == null) ? "" : filter.toLowerCase().trim();

        for (Map.Entry<String, List<MenuItem>> categoryEntry : categorizedMenuItems.entrySet()) {
            String categoryName = categoryEntry.getKey();
            List<MenuItem> filteredItems = categoryEntry.getValue().stream()
                    .filter(item -> (lowerCaseFilter.isEmpty() || item.getName().toLowerCase().contains(lowerCaseFilter) || String.valueOf(item.getPrice()).contains(lowerCaseFilter)))
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
                categoryPane.setStyle(
                        "-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY_DARK + ";" +
                                "-fx-base: " + PRIMARY_NAVY_LIGHT + ";" +
                                "-fx-body-color: " + BACKGROUND_CONTENT + ";" +
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
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + TEXT_ON_LIGHT_SECONDARY + "; -fx-padding: 10px;");
            menuVBox.getChildren().add(noResultsLabel);
        }
    }

    private static void styleControlButton(Button button, String baseColor, String hoverColor) {
        String baseStyle = "-fx-font-size: 13px; -fx-padding: 7 14; -fx-background-radius: 18px; -fx-text-fill: " + TEXT_ON_DARK + "; -fx-font-weight: bold;";
        button.setStyle(baseStyle + "-fx-background-color: " + baseColor + ";");
        button.setEffect(new DropShadow(2, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + "-fx-background-color: " + hoverColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 5, 0.15, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(baseStyle + "-fx-background-color: " + baseColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 2, 0, 0, 0);"));
    }

    private static void styleActionButton(Button button, String normalHex, String hoverHex) {
        String baseStyle = "-fx-font-size: 17px; -fx-padding: 12 25; -fx-background-radius: 25px; -fx-text-fill: " + TEXT_ON_DARK + "; -fx-font-weight: bold;";
        button.setStyle(baseStyle + "-fx-background-color: " + normalHex + ";");
        button.setEffect(new DropShadow(4, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + "-fx-background-color: " + hoverHex + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 8, 0.25, 0, 2);"));
        button.setOnMouseExited(e -> button.setStyle(baseStyle + "-fx-background-color: " + normalHex + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 4, 0, 0, 0);"));
        button.setMinWidth(150);
    }

    private static void refreshCart() {
        cartBox.getChildren().clear();
        if (cartItems.isEmpty()) {
            Label emptyCartLabel = new Label("Your cart is empty... Add some treats! 😋");
            emptyCartLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: " + TEXT_ON_LIGHT_SECONDARY + "; -fx-font-weight: bold;");
            emptyCartLabel.setPadding(new Insets(25));
            cartBox.getChildren().add(emptyCartLabel);
            cartBox.setAlignment(Pos.CENTER);
            totalLabel.setText("Total: ₹0.00");
            return;
        }
        cartBox.setAlignment(Pos.TOP_LEFT);
        double currentTotal = 0;
        for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
            MenuItem item = allMenuItems.get(entry.getKey());
            if (item == null) continue;
            double subtotal = entry.getValue() * item.getPrice();
            currentTotal += subtotal;
            cartBox.getChildren().add(createCartItemBox(item, entry.getValue(), subtotal));
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
                    if (img.isError()) imgView.setImage(null); else imgView.setImage(img);
                }
            } else imgView.setImage(null);
        } catch (IOException e) { imgView.setImage(null); }
        imgView.setFitWidth(60);
        imgView.setFitHeight(60);
        imgView.setPreserveRatio(true);
        if (imgView.getImage() != null) {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(imgView.getFitWidth(), imgView.getFitHeight());
            clip.setArcWidth(10); clip.setArcHeight(10);
            imgView.setClip(clip);
        }

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY_DARK + ";");
        Label priceInfoLabel = new Label(String.format("₹%d x %d", item.getPrice(), quantity));
        priceInfoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_ON_LIGHT_SECONDARY + ";");
        Label subtotalLabelText = new Label(String.format("Sub: ₹%.2f", subtotal));
        subtotalLabelText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_BLUE + ";");

        Button plusButton = new Button("➕");
        styleCartControlButton(plusButton);
        plusButton.setOnAction(e -> { addToCart(item.getName()); });

        Label quantityLabel = new Label(String.valueOf(quantity));
        quantityLabel.setStyle("-fx-font-size: 16px; -fx-padding: 0 10px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_ON_LIGHT_PRIMARY + ";");

        Button minusButton = new Button("➖");
        styleCartControlButton(minusButton);
        minusButton.setOnAction(e -> {
            if (cartItems.get(item.getName()) > 1) cartItems.put(item.getName(), cartItems.get(item.getName()) - 1);
            else cartItems.remove(item.getName());
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
        cartItemBox.setStyle("-fx-background-color: " + BACKGROUND_CONTENT + "; -fx-background-radius: 10px; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 1px; -fx-border-radius: 10px;");
        cartItemBox.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        cartItemBox.setAlignment(Pos.CENTER_LEFT);

        return cartItemBox;
    }

    private static void styleCartControlButton(Button button) {
        String baseCartBtnStyle = "-fx-font-size: 18px; -fx-padding: 6 12; -fx-background-radius: 50px; -fx-text-fill: " + TEXT_ON_LIGHT_PRIMARY + "; -fx-font-weight: bold;";
        button.setStyle(baseCartBtnStyle + "-fx-background-color: " + ACCENT_YELLOW + ";");
        button.setEffect(new DropShadow(2, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(baseCartBtnStyle + "-fx-background-color: " + ACCENT_YELLOW_DARK + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 4, 0.15, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(baseCartBtnStyle + "-fx-background-color: " + ACCENT_YELLOW + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 2, 0, 0, 0);"));
    }

    private static void toggleAllCategoryPanes(boolean expand) {
        if (categoryPanesList != null) {
            for (TitledPane pane : categoryPanesList) {
                pane.setExpanded(expand);
            }
        }
    }

    private static VBox createFlavorCard(MenuItem item) {
        ImageView imgView = new ImageView();
        String imageBasePath = ConfigManager.getImagePath();
        Path imageFilePath = Paths.get(imageBasePath, item.getImageName());
        try {
            if (Files.exists(imageFilePath) && !Files.isDirectory(imageFilePath)) {
                try (InputStream imageStream = Files.newInputStream(imageFilePath)) {
                    imgView.setImage(new Image(imageStream));
                }
            }
        } catch (Exception e) { imgView.setImage(null); }
        imgView.setFitWidth(110); imgView.setFitHeight(110); imgView.setPreserveRatio(true);
        if (imgView.getImage() != null) {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(imgView.getFitWidth(), imgView.getFitHeight());
            clip.setArcWidth(15); clip.setArcHeight(15);
            imgView.setClip(clip);
        }

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_ON_LIGHT_PRIMARY + ";");
        Label priceLabel = new Label("₹" + item.getPrice());
        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ACCENT_YELLOW_DARK + "; -fx-font-weight: bold;");

        Label stockLabel = new Label();
        boolean outOfStock = false;
        if (item.hasLimitedStock()) {
            if (item.getQuantity() <= 0) {
                stockLabel.setText("Out of Stock");
                stockLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + BUTTON_ACTION_RED + ";");
                outOfStock = true;
            } else {
                stockLabel.setText("Stock: " + item.getQuantity());
                stockLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_ON_LIGHT_SECONDARY + ";");
            }
        } else {
            stockLabel.setText("Unlimited Stock");
            stockLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4CAF50;");
        }

        Node imageNode = imgView.getImage() != null ? imgView : createPlaceholderGraphic(item.getName(), 110);

        VBox card = new VBox(10, imageNode, nameLabel, priceLabel, stockLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        String baseCardStyle = "-fx-background-color: " + BACKGROUND_CONTENT + "; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 1px;";
        String hoverCardStyle = "-fx-background-color: #E3F2FD; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: " + PRIMARY_NAVY + "; -fx-border-width: 1.5px; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 12, 0.3, 0, 3);";

        card.setStyle(baseCardStyle + " -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 6, 0.1, 0, 1);");

        if (outOfStock) {
            card.setOpacity(0.6);
            card.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 12px; -fx-border-color: #e0e0e0;");
            card.setOnMouseClicked(event -> {
                if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 3) {
                    System.out.println("Triple-click detected on out-of-stock item: " + item.getName());
                    String itemCategory = findCategoryOfItem(item.getName());
                    if (itemCategory != null) {
                        boolean success = UpdateInventoryView.setItemStockToUnlimited(item.getName(), itemCategory);
                        if (success) {
                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.INFORMATION, "Stock Updated", item.getName() + " is now set to Unlimited stock.");
                                loadMenuItemsFromJson();
                                refreshMenuView();
                            });
                        }
                    }
                }
            });
        } else {
            card.setOnMouseEntered(e -> card.setStyle(hoverCardStyle));
            card.setOnMouseExited(e -> card.setStyle(baseCardStyle + " -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 6, 0.1, 0, 1);"));
            card.setOnMouseClicked(e -> addToCart(item.getName()));
        }

        card.setMinWidth(160);
        card.setMaxWidth(160);
        return card;
    }

    private static void addToCart(String itemName) {
        MenuItem item = allMenuItems.get(itemName);
        if (item == null) return;
        if (item.hasLimitedStock()) {
            int currentInCart = cartItems.getOrDefault(itemName, 0);
            if (currentInCart >= item.getQuantity()) {
                showAlert(Alert.AlertType.WARNING, "Out of Stock", "No more stock available for " + itemName + ".");
                return;
            }
        }
        cartItems.put(itemName, cartItems.getOrDefault(itemName, 0) + 1);
        refreshCart();
    }

    private static Pane createPlaceholderGraphic(String text, double size) {
        Label placeholderText = new Label(text.length() > 0 ? text.substring(0, 1).toUpperCase() : "🍦");
        placeholderText.setFont(Font.font("Arial", FontWeight.BOLD, size * 0.5));
        placeholderText.setTextFill(Color.web(PRIMARY_NAVY_DARK));
        StackPane placeholderPane = new StackPane(placeholderText);
        placeholderPane.setPrefSize(size, size);
        placeholderPane.setMinSize(size,size);
        placeholderPane.setMaxSize(size,size);
        placeholderPane.setStyle("-fx-background-color: " + PRIMARY_NAVY_LIGHT + "; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 1.5px; -fx-background-radius: " + (size/2) + "px; -fx-border-radius: " + (size/2) + "px;");
        return placeholderPane;
    }

    public static void refreshMenuView() {
        if (menuVBox != null && searchField != null) {
            populateMenu(searchField.getText());
        }
    }

    private static void handlePlaceOrder() {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Cart", "Your cart is empty.");
            return;
        }
        for (Map.Entry<String, Integer> cartEntry : cartItems.entrySet()) {
            MenuItem item = allMenuItems.get(cartEntry.getKey());
            if (item != null && item.hasLimitedStock()) {
                if (cartEntry.getValue() > item.getQuantity()) {
                    showAlert(Alert.AlertType.ERROR, "Stock Issue", "Not enough stock for " + item.getName() + ". Required: " + cartEntry.getValue() + ", Available: " + item.getQuantity());
                    return;
                }
            }
        }

        List<OrderItem> currentOrderItems = new ArrayList<>();
        for (Map.Entry<String, Integer> cartEntry : cartItems.entrySet()) {
            MenuItem menuItemDetails = allMenuItems.get(cartEntry.getKey());
            if (menuItemDetails != null) currentOrderItems.add(new OrderItem(menuItemDetails.getName(), cartEntry.getValue(), menuItemDetails.getPrice()));
        }
        Order newOrder = new Order(currentOrderItems);

        if (!updateStockQuantities()) {
            showAlert(Alert.AlertType.ERROR, "Critical Error", "Could not update stock levels. Order has been cancelled.");
            return;
        }
        OrderExcelUtil.saveOrderToExcel(newOrder);
        showAlert(Alert.AlertType.INFORMATION, "Order Placed", "Order placed successfully!");
        cartItems.clear();
        refreshCart();
        loadMenuItemsFromJson();
        refreshMenuView();
    }

    private static boolean updateStockQuantities() {
        Path menuItemsPath = Paths.get(ConfigManager.getMenuItemsJsonPath());
        File menuFile = menuItemsPath.toFile();
        if (!menuFile.exists()) return false;
        try {
            String content = new String(Files.readAllBytes(menuItemsPath), StandardCharsets.UTF_8);
            JSONObject rootJson = new JSONObject(content);
            JSONArray categoriesArray = rootJson.getJSONArray("categories");
            for (Map.Entry<String, Integer> cartEntry : cartItems.entrySet()) {
                MenuItem menuItem = allMenuItems.get(cartEntry.getKey());
                if (menuItem != null && menuItem.hasLimitedStock()) {
                    boolean found = false;
                    for (int i = 0; i < categoriesArray.length(); i++) {
                        JSONArray itemsArray = categoriesArray.getJSONObject(i).getJSONArray("items");
                        for (int j = 0; j < itemsArray.length(); j++) {
                            JSONObject itemObj = itemsArray.getJSONObject(j);
                            if (itemObj.getString("name").equalsIgnoreCase(menuItem.getName())) {
                                int currentStock = itemObj.optInt("quantity");
                                itemObj.put("quantity", currentStock - cartEntry.getValue());
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                }
            }
            try (FileWriter writer = new FileWriter(menuFile)) { writer.write(rootJson.toString(4)); }
            return true;
        } catch (IOException | JSONException e) { e.printStackTrace(); return false; }
    }

    private static String findCategoryOfItem(String itemName) {
        for (Map.Entry<String, List<MenuItem>> entry : categorizedMenuItems.entrySet()) {
            for (MenuItem item : entry.getValue()) {
                if (item.getName().equals(itemName)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-background-color: " + BACKGROUND_MAIN + ";");
            Button okButton = (Button) dialogPane.lookupButton(alert.getButtonTypes().get(0));
            if (okButton != null) {
                String baseColor = alertType == Alert.AlertType.ERROR || alertType == Alert.AlertType.WARNING ? BUTTON_ACTION_RED : PRIMARY_NAVY;
                String hoverColor = alertType == Alert.AlertType.ERROR || alertType == Alert.AlertType.WARNING ? BUTTON_ACTION_RED_HOVER : PRIMARY_NAVY_DARK;
                String btnStyle = "-fx-text-fill: " + TEXT_ON_DARK + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;";
                okButton.setStyle(btnStyle + "-fx-background-color: " + baseColor + ";");
                okButton.setOnMouseEntered(e -> okButton.setStyle(btnStyle + "-fx-background-color: " + hoverColor + ";"));
                okButton.setOnMouseExited(e -> okButton.setStyle(btnStyle + "-fx-background-color: " + baseColor + ";"));
            }
            alert.showAndWait();
        });
    }


}
