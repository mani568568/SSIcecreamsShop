package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image; // Added import
import javafx.scene.input.KeyCode; // Added import
import javafx.scene.input.KeyEvent; // Added import
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ManageInventoryView {

    // --- Navy Blue & Yellow Theme Colors ---
    private static final String PRIMARY_NAVY = "#1A237E";
    private static final String TEXT_ON_DARK = "white";
    private static final String BACKGROUND_MAIN = "#E8EAF6";
    private static final String BACKGROUND_CONTENT = "#FFFFFF";
    private static final String SHADOW_COLOR = "rgba(26, 35, 126, 0.2)";
    private static final String BUTTON_ACTION_GREEN = "#4CAF50";
    private static final String BUTTON_ACTION_GREEN_HOVER = "#388E3C";
    private static final String BUTTON_ACTION_BLUE = "#2196F3";
    private static final String BUTTON_ACTION_BLUE_HOVER = "#1976D2";
    private static final String TEXT_ON_LIGHT_SECONDARY = "#757575";

    private static File selectedImageFile;
    private static Label selectedImageLabel;
    private static ComboBox<String> categoryComboBox;
    private static final ObservableList<String> categoriesList = FXCollections.observableArrayList();
    private static TextField itemNameField, itemPriceField, quantityField;
    private static Stage stage;

    public static void show() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("➕ Add New Item to Menu");
        stage.setMinWidth(600);
        stage.setMinHeight(550);

        try {
            Image appIcon = new Image(ManageInventoryView.class.getResourceAsStream("/images/app_icon.png"));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Error loading icon for Add Item window: " + e.getMessage());
        }

        GridPane formPane = new GridPane();
        formPane.setHgap(15);
        formPane.setVgap(18);
        formPane.setPadding(new Insets(30));
        formPane.setStyle("-fx-background-color: " + BACKGROUND_CONTENT + "; -fx-background-radius: 12px;");
        formPane.setEffect(new DropShadow(10, Color.web(SHADOW_COLOR)));
        formPane.setAlignment(Pos.CENTER);

        String labelStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY + ";";
        String textFieldStyle = "-fx-font-size: 14px; -fx-background-radius: 18px; -fx-border-radius: 18px; -fx-border-color: #CFD8DC; -fx-padding: 8px 12px;";

        formPane.add(new Label("Item Name:") {{ setStyle(labelStyle); }}, 0, 0);
        itemNameField = new TextField();
        itemNameField.setPromptText("e.g., Classic Vanilla");
        itemNameField.setStyle(textFieldStyle);
        formPane.add(itemNameField, 1, 0);

        formPane.add(new Label("Item Price (₹):") {{ setStyle(labelStyle); }}, 0, 1);
        itemPriceField = new TextField();
        itemPriceField.setPromptText("e.g., 150");
        itemPriceField.setStyle(textFieldStyle);
        formPane.add(itemPriceField, 1, 1);
        itemPriceField.textProperty().addListener((obs, ov, nv) -> { if (!nv.matches("\\d*")) itemPriceField.setText(nv.replaceAll("[^\\d]", "")); });

        formPane.add(new Label("Category:") {{ setStyle(labelStyle); }}, 0, 2);
        categoryComboBox = new ComboBox<>(categoriesList);
        categoryComboBox.setEditable(true);
        categoryComboBox.setPromptText("Select or type new category");
        categoryComboBox.setStyle(textFieldStyle + "-fx-background-color: white;");
        categoryComboBox.setMaxWidth(Double.MAX_VALUE);
        formPane.add(categoryComboBox, 1, 2);

        formPane.add(new Label("Initial Stock Quantity:") {{ setStyle(labelStyle); }}, 0, 3);
        quantityField = new TextField();
        quantityField.setPromptText("e.g., 50 (leave empty for unlimited)");
        quantityField.setStyle(textFieldStyle);
        formPane.add(quantityField, 1, 3);
        quantityField.textProperty().addListener((obs, ov, nv) -> { if (!nv.matches("\\d*")) quantityField.setText(nv.replaceAll("[^\\d]", "")); });


        formPane.add(new Label("Item Image:") {{ setStyle(labelStyle); }}, 0, 4);
        Button browseButton = new Button("Browse...");
        styleDialogButton(browseButton, BUTTON_ACTION_BLUE, BUTTON_ACTION_BLUE_HOVER, false);
        selectedImageLabel = new Label("No image selected");
        selectedImageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_ON_LIGHT_SECONDARY + ";");
        HBox imageSelectionBox = new HBox(10, browseButton, selectedImageLabel);
        imageSelectionBox.setAlignment(Pos.CENTER_LEFT);
        formPane.add(imageSelectionBox, 1, 4);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Item Image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        browseButton.setOnAction(e -> {
            File initialImageDir = new File(ConfigManager.getImagePath());
            if (initialImageDir.exists() && initialImageDir.isDirectory()) fileChooser.setInitialDirectory(initialImageDir);
            selectedImageFile = fileChooser.showOpenDialog(stage);
            if (selectedImageFile != null) selectedImageLabel.setText(selectedImageFile.getName());
            else selectedImageLabel.setText("No image selected");
        });

        Button addItemButton = new Button("➕ Add New Item to Menu");
        styleDialogButton(addItemButton, BUTTON_ACTION_GREEN, BUTTON_ACTION_GREEN_HOVER, true);
        addItemButton.setMaxWidth(Double.MAX_VALUE);
        addItemButton.setOnAction(e -> handleAddItem());
        formPane.add(addItemButton, 0, 5, 2, 1);
        GridPane.setHalignment(addItemButton, HPos.CENTER);

        VBox rootLayout = new VBox(formPane);
        rootLayout.setAlignment(Pos.CENTER);
        rootLayout.setPadding(new Insets(30));
        rootLayout.setStyle("-fx-background-color: " + BACKGROUND_MAIN + ";");

        Scene scene = new Scene(rootLayout);

        // --- ADDED: ESC key listener to close the window ---
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });

        stage.setScene(scene);

        loadInventoryData();

        stage.showAndWait();
    }

    private static void handleAddItem() {
        String itemName = itemNameField.getText().trim();
        String priceStr = itemPriceField.getText().trim();
        String category = categoryComboBox.getEditor().getText().trim();
        String quantityStr = quantityField.getText().trim();

        if (itemName.isEmpty() || priceStr.isEmpty() || category.isEmpty() || selectedImageFile == null) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Name, Price, Category, and Image are all required.");
            return;
        }

        int price;
        try { price = Integer.parseInt(priceStr); if (price <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { showAlert(Alert.AlertType.ERROR, "Input Error", "Price must be a valid positive number."); return; }

        Integer quantity = null;
        if (!quantityStr.isEmpty()) {
            try { quantity = Integer.parseInt(quantityStr); }
            catch (NumberFormatException e) { showAlert(Alert.AlertType.ERROR, "Input Error", "Quantity must be a valid number."); return; }
        }

        try {
            Path targetImageDir = Paths.get(ConfigManager.getImagePath());
            if (!Files.exists(targetImageDir)) Files.createDirectories(targetImageDir);
            Files.copy(selectedImageFile.toPath(), targetImageDir.resolve(selectedImageFile.getName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "File Error", "Could not copy image file: " + e.getMessage()); return;
        }

        addItemToJson(category, itemName, price, selectedImageFile.getName(), quantity);
        itemNameField.clear(); itemPriceField.clear(); categoryComboBox.getEditor().clear(); categoryComboBox.setValue(null);
        quantityField.clear(); selectedImageFile = null; selectedImageLabel.setText("No image selected");
        showAlert(Alert.AlertType.INFORMATION, "Success", "Item '" + itemName + "' added to menu!");
    }

    private static void addItemToJson(String categoryName, String itemName, int price, String imageName, Integer quantity) {
        String menuJsonPathStr = ConfigManager.getMenuItemsJsonPath();
        File menuFile = new File(menuJsonPathStr);
        JSONObject rootJson;
        try {
            if (menuFile.getParentFile() != null && !menuFile.getParentFile().exists()) menuFile.getParentFile().mkdirs();
            if (menuFile.exists() && menuFile.length() > 0) rootJson = new JSONObject(new String(Files.readAllBytes(menuFile.toPath()), StandardCharsets.UTF_8));
            else { rootJson = new JSONObject(); rootJson.put("categories", new JSONArray()); }

            JSONArray categoriesArray = rootJson.getJSONArray("categories");
            JSONObject targetCategory = null;
            for (int i = 0; i < categoriesArray.length(); i++) if (categoriesArray.getJSONObject(i).getString("name").equalsIgnoreCase(categoryName)) targetCategory = categoriesArray.getJSONObject(i);

            if (targetCategory == null) { targetCategory = new JSONObject(); targetCategory.put("name", categoryName); targetCategory.put("items", new JSONArray()); categoriesArray.put(targetCategory); }

            JSONArray itemsArray = targetCategory.getJSONArray("items");
            for (int i = 0; i < itemsArray.length(); i++) {
                if (itemsArray.getJSONObject(i).getString("name").equalsIgnoreCase(itemName)) {
                    showAlert(Alert.AlertType.ERROR, "Item Exists", "An item with this name already exists. Please use the 'Update Stock' view to change quantity.");
                    return;
                }
            }

            JSONObject newItem = new JSONObject();
            newItem.put("name", itemName);
            newItem.put("imageName", imageName);
            newItem.put("price", price);
            if (quantity != null) {
                newItem.put("quantity", quantity.intValue());
            }
            itemsArray.put(newItem);

            try (FileWriter writer = new FileWriter(menuFile)) { writer.write(rootJson.toString(4)); }

            NewOrderView.loadMenuItemsFromJson();
            NewOrderView.refreshMenuView();
            loadInventoryData();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "File Error", "Could not read/write to menu file: " + e.getMessage());
        }
    }

    public static void loadInventoryData() {
        categoriesList.clear();
        Path menuItemsPathObj = Paths.get(ConfigManager.getMenuItemsJsonPath());
        if (!Files.exists(menuItemsPathObj)) return;

        try (java.io.InputStream inputStream = Files.newInputStream(menuItemsPathObj)) {
            String jsonText = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            JSONObject jsonObject = new JSONObject(jsonText);
            JSONArray categoriesArray = jsonObject.optJSONArray("categories");
            if (categoriesArray == null) return;

            List<String> tempCategories = new ArrayList<>();
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                String categoryName = categoryObj.getString("name");
                if (!tempCategories.contains(categoryName)) {
                    tempCategories.add(categoryName);
                }
            }
            tempCategories.sort(String.CASE_INSENSITIVE_ORDER);
            categoriesList.setAll(tempCategories);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.WARNING, "Load Error", "Could not load categories from file.");
        }
    }


    // Utility methods
    private static void styleDialogButton(Button button, String baseColor, String hoverColor, boolean isPrimary) {
        String padding = isPrimary ? "12 25" : "8 15";
        String fontSize = isPrimary ? "15px" : "13px";
        String style = "-fx-font-size: " + fontSize + "; -fx-text-fill: " + TEXT_ON_DARK + "; -fx-font-weight: bold; -fx-padding: " + padding + "; -fx-background-radius: 20px;";
        button.setStyle(style + "-fx-background-color: " + baseColor + ";");
        button.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(style + "-fx-background-color: " + hoverColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 7, 0.2, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(style + "-fx-background-color: " + baseColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 3, 0, 0, 0);"));
    }

    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (stage != null && stage.isShowing()) alert.initOwner(stage);
            alert.showAndWait();
        });
    }
}
