package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigManager;
import com.ssicecreamsshop.utils.ExcelExportUtil;
import com.ssicecreamsshop.utils.ExcelImportDialog;
import com.ssicecreamsshop.utils.NetworkStatusIndicator; // Added import
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.*;
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

    // Theme Colors
    private static final String PRIMARY_RED = "#E53935";
    private static final String PRIMARY_RED_DARK = "#C62828";
    private static final String PRIMARY_RED_LIGHT = "#FFCDD2";
    private static final String ACCENT_RED = "#D32F2F";
    private static final String TEXT_ON_RED = "white";
    private static final String TEXT_ON_WHITE = "#212121";
    private static final String BORDER_COLOR_LIGHT = "#FFAB91";
    private static final String BACKGROUND_LIGHT_NEUTRAL = "#fce4ec";
    private static final String BACKGROUND_FORM = "#ffebee"; // Slightly darker than light neutral for form
    private static final String SHADOW_COLOR = "rgba(0,0,0,0.15)";
    private static final String BUTTON_GREEN = "#4CAF50";
    private static final String BUTTON_GREEN_HOVER = "#388E3C";
    private static final String BUTTON_BLUE = "#2196F3";
    private static final String BUTTON_BLUE_HOVER = "#1976D2";


    private static TableView<DisplayMenuItem> tableView;
    private static final ObservableList<DisplayMenuItem> menuItemsList = FXCollections.observableArrayList();
    private static File selectedImageFile;
    private static Label selectedImageLabel;
    private static ComboBox<String> categoryComboBox;
    private static final ObservableList<String> categoriesList = FXCollections.observableArrayList();

    private static TextField itemNameField;
    private static TextField itemPriceField;
    private static Stage inventoryStage;
    private static NetworkStatusIndicator networkIndicator; // Added


    public static void show() {
        inventoryStage = new Stage();
        inventoryStage.initModality(Modality.APPLICATION_MODAL);
        inventoryStage.setTitle("🍦 Manage Inventory");
        inventoryStage.setMinWidth(950);
        inventoryStage.setMinHeight(750);

        if (networkIndicator != null) networkIndicator.stopMonitoring(); // Stop if existing
        networkIndicator = new NetworkStatusIndicator();


        // --- Input Form ---
        GridPane formPane = new GridPane();
        formPane.setHgap(15); // Increased gap
        formPane.setVgap(15); // Increased gap
        formPane.setPadding(new Insets(30)); // Increased padding
        formPane.setStyle("-fx-background-color: " + BACKGROUND_FORM + "; -fx-background-radius: 10px;");
        formPane.setEffect(new DropShadow(10, Color.web(SHADOW_COLOR)));

        String labelStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_RED_DARK + ";";
        String textFieldStyle = "-fx-font-size: 14px; -fx-background-radius: 15px; -fx-border-radius: 15px; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-padding: 7px;";

        formPane.add(new Label("Item Name:") {{ setStyle(labelStyle); }}, 0, 0);
        itemNameField = new TextField();
        itemNameField.setPromptText("e.g., Classic Vanilla");
        itemNameField.setStyle(textFieldStyle);
        formPane.add(itemNameField, 1, 0);
        GridPane.setHgrow(itemNameField, Priority.ALWAYS);

        formPane.add(new Label("Item Price (₹):") {{ setStyle(labelStyle); }}, 0, 1);
        itemPriceField = new TextField();
        itemPriceField.setPromptText("e.g., 150");
        itemPriceField.setStyle(textFieldStyle);
        formPane.add(itemPriceField, 1, 1);
        itemPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                itemPriceField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        formPane.add(new Label("Category:") {{ setStyle(labelStyle); }}, 0, 2);
        categoryComboBox = new ComboBox<>(categoriesList);
        categoryComboBox.setEditable(true);
        categoryComboBox.setPromptText("Select or type new category");
        categoryComboBox.setStyle(textFieldStyle + "-fx-background-color: white;"); // Ensure combobox background is white
        categoryComboBox.setMaxWidth(Double.MAX_VALUE);
        formPane.add(categoryComboBox, 1, 2);

        formPane.add(new Label("Item Image:") {{ setStyle(labelStyle); }}, 0, 3);
        Button browseButton = new Button("Browse...");
        styleDialogButton(browseButton, BUTTON_BLUE, BUTTON_BLUE_HOVER); // Themed browse button
        selectedImageLabel = new Label("No image selected");
        selectedImageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        selectedImageLabel.setWrapText(true);
        HBox imageSelectionBox = new HBox(10, browseButton, selectedImageLabel);
        imageSelectionBox.setAlignment(Pos.CENTER_LEFT);
        formPane.add(imageSelectionBox, 1, 3);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Item Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        browseButton.setOnAction(e -> {
            File initialImageDir = new File(ConfigManager.getImagePath());
            if (initialImageDir.exists() && initialImageDir.isDirectory()) {
                fileChooser.setInitialDirectory(initialImageDir);
            }
            selectedImageFile = fileChooser.showOpenDialog(inventoryStage);
            if (selectedImageFile != null) {
                selectedImageLabel.setText(selectedImageFile.getName());
            } else {
                selectedImageLabel.setText("No image selected");
            }
        });

        Button addItemButton = new Button("➕ Add Item to Inventory");
        styleDialogButton(addItemButton, BUTTON_GREEN, BUTTON_GREEN_HOVER); // Themed add button
        addItemButton.setMaxWidth(Double.MAX_VALUE);
        addItemButton.setOnAction(e -> handleAddItem());
        formPane.add(addItemButton, 0, 4, 2, 1); // Span 2 columns
        GridPane.setHalignment(addItemButton, HPos.CENTER);


        // --- Table View for Displaying Items ---
        tableView = new TableView<>();
        tableView.setStyle("-fx-font-size: 13px; -fx-selection-bar: " + PRIMARY_RED_LIGHT + "; -fx-selection-bar-text: " + PRIMARY_RED_DARK + ";");
        tableView.setEffect(new DropShadow(5, Color.web(SHADOW_COLOR)));

        TableColumn<DisplayMenuItem, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(160);
        categoryCol.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 6px;");

        TableColumn<DisplayMenuItem, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);
        nameCol.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 6px;");

        TableColumn<DisplayMenuItem, String> imageCol = new TableColumn<>("Image File");
        imageCol.setCellValueFactory(new PropertyValueFactory<>("imageName"));
        imageCol.setPrefWidth(160);
        imageCol.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 6px;");

        TableColumn<DisplayMenuItem, Integer> priceCol = new TableColumn<>("Price (₹)");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(110);
        priceCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-padding: 6px;");

        TableColumn<DisplayMenuItem, Void> deleteCol = new TableColumn<>("Actions");
        deleteCol.setPrefWidth(100);
        deleteCol.setSortable(false);
        deleteCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete 🗑️");
            {
                deleteButton.setStyle("-fx-background-color: " + ACCENT_RED + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 8; -fx-background-radius: 15px; -fx-font-weight: bold;");
                deleteButton.setOnMouseEntered(e -> deleteButton.setStyle("-fx-background-color: " + PRIMARY_RED_DARK + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 8; -fx-background-radius: 15px; -fx-font-weight: bold;"));
                deleteButton.setOnMouseExited(e -> deleteButton.setStyle("-fx-background-color: " + ACCENT_RED + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 8; -fx-background-radius: 15px; -fx-font-weight: bold;"));
                deleteButton.setOnAction(event -> {
                    DisplayMenuItem item = getTableView().getItems().get(getIndex());
                    handleDeleteItem(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
                setAlignment(Pos.CENTER);
                setPadding(new Insets(3));
            }
        });

        tableView.getColumns().addAll(categoryCol, nameCol, imageCol, priceCol, deleteCol);
        tableView.setItems(menuItemsList);
        tableView.setPlaceholder(new Label("No items in inventory. Add some goodies! 🍨"));

        // --- Control Buttons for Table ---
        Button refreshButton = new Button("🔄 Refresh");
        styleTableControlButton(refreshButton, BUTTON_BLUE, BUTTON_BLUE_HOVER);
        refreshButton.setOnAction(e -> loadInventoryData());

        Button exportButton = new Button("📤 Export");
        styleTableControlButton(exportButton, BUTTON_GREEN, BUTTON_GREEN_HOVER);
        exportButton.setOnAction(e -> ExcelExportUtil.exportToExcel(inventoryStage));

        Button importButton = new Button("📥 Import");
        styleTableControlButton(importButton, PRIMARY_RED, PRIMARY_RED_DARK);
        importButton.setOnAction(e -> ExcelImportDialog.show());

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox tableControlsBox = new HBox(10, networkIndicator, spacer, refreshButton, exportButton, importButton);
        tableControlsBox.setAlignment(Pos.CENTER_LEFT);
        tableControlsBox.setPadding(new Insets(10,0,10,0));


        Label currentInventoryLabel = new Label("Current Inventory Stock:");
        currentInventoryLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_RED_DARK + "; -fx-padding: 10 0;");
        VBox bottomPane = new VBox(15, new Separator(), currentInventoryLabel, tableControlsBox, tableView); // Increased spacing
        bottomPane.setPadding(new Insets(20, 25, 25, 25)); // Increased padding
        VBox.setVgrow(tableView, Priority.ALWAYS);
        bottomPane.setStyle("-fx-background-color: white;");

        BorderPane rootLayout = new BorderPane();
        rootLayout.setTop(formPane);
        rootLayout.setCenter(bottomPane);
        rootLayout.setStyle("-fx-background-color: " + BACKGROUND_LIGHT_NEUTRAL + ";"); // Overall background
        BorderPane.setMargin(formPane, new Insets(20));


        Scene scene = new Scene(rootLayout);
        inventoryStage.setScene(scene);
        inventoryStage.setOnHidden(event -> {
            if (networkIndicator != null) {
                networkIndicator.stopMonitoring();
            }
        });

        loadInventoryData();
        inventoryStage.showAndWait();
    }

    private static void styleDialogButton(Button button, String baseColor, String hoverColor) {
        String style = "-fx-font-size: 14px; -fx-text-fill: " + TEXT_ON_RED + "; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 20px;";
        button.setStyle(style + "-fx-background-color: " + baseColor + ";");
        button.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(style + "-fx-background-color: " + hoverColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 7, 0.2, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(style + "-fx-background-color: " + baseColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 3, 0, 0, 0);"));
    }

    private static void styleTableControlButton(Button button, String baseColor, String hoverColor) {
        String style = "-fx-font-size: 13px; -fx-text-fill: " + TEXT_ON_RED + "; -fx-font-weight: bold; -fx-padding: 7 15; -fx-background-radius: 18px;";
        button.setStyle(style + "-fx-background-color: " + baseColor + ";");
        button.setEffect(new DropShadow(2, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(style + "-fx-background-color: " + hoverColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 5, 0.15, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(style + "-fx-background-color: " + baseColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 2, 0, 0, 0);"));
    }

    // --- handleAddItem, handleDeleteItem, removeItemFromJson, addItemToJson, loadInventoryData methods remain largely the same ---
    // --- Only showAlert and showAlertWithConfirmation need theming updates ---

    private static void handleAddItem() {
        String itemName = itemNameField.getText().trim();
        String priceStr = itemPriceField.getText().trim();
        String category = categoryComboBox.getEditor().getText().trim();

        if (itemName.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Item Name, Price, and Category cannot be empty.");
            return;
        }
        if (selectedImageFile == null) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please select an image for the item.");
            return;
        }

        int price;
        try {
            price = Integer.parseInt(priceStr);
            if (price <= 0) {
                showAlert(Alert.AlertType.ERROR, "Input Error", "Price must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Invalid price format. Please enter a whole number.");
            return;
        }

        Path targetImageDir = Paths.get(ConfigManager.getImagePath());
        try {
            if (!Files.exists(targetImageDir)) {
                Files.createDirectories(targetImageDir);
            } else if (!Files.isDirectory(targetImageDir)) {
                showAlert(Alert.AlertType.ERROR, "Configuration Error", "Configured image path is not a directory: " + targetImageDir.toString());
                return;
            }
            Path targetImagePath = targetImageDir.resolve(selectedImageFile.getName());
            Files.copy(selectedImageFile.toPath(), targetImagePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Image copied to: " + targetImagePath.toString());
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "File Error", "Could not copy image file: " + e.getMessage() +
                    "\nEnsure the path '" + targetImageDir.toString() + "' is writable and accessible.");
            return;
        }

        String imageFileNameInJson = selectedImageFile.getName();
        addItemToJson(category, itemName, price, imageFileNameInJson);

        itemNameField.clear();
        itemPriceField.clear();
        categoryComboBox.getEditor().clear();
        categoryComboBox.setValue(null);
        selectedImageFile = null;
        selectedImageLabel.setText("No image selected");

        loadInventoryData();
        NewOrderView.loadMenuItemsFromJson();
        NewOrderView.refreshMenuView();

        showAlert(Alert.AlertType.INFORMATION, "Success", "Item '" + itemName + "' added to inventory!");
    }

    private static void handleDeleteItem(DisplayMenuItem itemToDelete) {
        Optional<ButtonType> result = showAlertWithConfirmation("Confirm Delete",
                "Are you sure you want to delete the item '" + itemToDelete.getName() + "' from category '" + itemToDelete.getCategory() + "'?\n" +
                        "The item record will be removed from the inventory. The image file itself will NOT be deleted from the disk.");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean jsonUpdated = removeItemFromJson(itemToDelete);
            if (jsonUpdated) {
                loadInventoryData();
                NewOrderView.loadMenuItemsFromJson();
                NewOrderView.refreshMenuView();
                showAlert(Alert.AlertType.INFORMATION, "Delete Successful", "Item '" + itemToDelete.getName() + "' has been removed from the inventory records.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Delete Failed", "Could not delete item '" + itemToDelete.getName() + "' from the inventory file.");
            }
        }
    }

    private static boolean removeItemFromJson(DisplayMenuItem itemToDelete) {
        String menuJsonPathStr = ConfigManager.getMenuItemsJsonPath();
        File menuFile = new File(menuJsonPathStr);
        JSONObject rootJson;
        boolean itemRemoved = false;

        if (!menuFile.exists()) {
            System.err.println("Cannot remove item: Menu JSON file not found at " + menuJsonPathStr);
            return false;
        }

        try {
            String content = new String(Files.readAllBytes(menuFile.toPath()), StandardCharsets.UTF_8);
            rootJson = new JSONObject(content);
            JSONArray categoriesArray = rootJson.getJSONArray("categories");
            JSONArray updatedCategoriesArray = new JSONArray();

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                if (categoryObj.getString("name").equalsIgnoreCase(itemToDelete.getCategory())) {
                    JSONArray itemsArray = categoryObj.getJSONArray("items");
                    JSONArray updatedItemsArray = new JSONArray();
                    boolean itemFoundInCategory = false;

                    for (int j = 0; j < itemsArray.length(); j++) {
                        JSONObject itemObj = itemsArray.getJSONObject(j);
                        if (itemObj.getString("name").equalsIgnoreCase(itemToDelete.getName())) {
                            itemRemoved = true;
                            itemFoundInCategory = true;
                            System.out.println("Removing item '" + itemToDelete.getName() + "' from category '" + itemToDelete.getCategory() + "' in JSON.");
                        } else {
                            updatedItemsArray.put(itemObj);
                        }
                    }
                    if (updatedItemsArray.length() > 0) {
                        categoryObj.put("items", updatedItemsArray);
                        updatedCategoriesArray.put(categoryObj);
                    } else if (!itemFoundInCategory) {
                        updatedCategoriesArray.put(categoryObj);
                    } else {
                        System.out.println("Category '" + itemToDelete.getCategory() + "' is now empty after item deletion and will be removed from JSON.");
                    }
                } else {
                    updatedCategoriesArray.put(categoryObj);
                }
            }
            rootJson.put("categories", updatedCategoriesArray);

            if (itemRemoved) {
                try (FileWriter writer = new FileWriter(menuFile)) {
                    writer.write(rootJson.toString(4));
                    System.out.println("Successfully updated JSON after item deletion.");
                }
            } else {
                System.out.println("Item '" + itemToDelete.getName() + "' not found in JSON for deletion.");
            }
            return itemRemoved;

        } catch (IOException e) {
            System.err.println("Error reading/writing '" + menuJsonPathStr + "' for deletion: " + e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            System.err.println("Error parsing JSON from '" + menuJsonPathStr + "' for deletion: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


    private static void addItemToJson(String categoryName, String itemName, int price, String imageName) {
        String menuJsonPathStr = ConfigManager.getMenuItemsJsonPath();
        File menuFile = new File(menuJsonPathStr);
        JSONObject rootJson;

        try {
            if (menuFile.getParentFile() != null && !menuFile.getParentFile().exists()) {
                menuFile.getParentFile().mkdirs();
            }

            if (menuFile.exists() && menuFile.length() > 0) {
                String content = new String(Files.readAllBytes(menuFile.toPath()), StandardCharsets.UTF_8);
                rootJson = new JSONObject(content);
            } else {
                rootJson = new JSONObject();
                rootJson.put("categories", new JSONArray());
            }

            JSONArray categoriesArray = rootJson.getJSONArray("categories");
            JSONObject targetCategory = null;

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject cat = categoriesArray.getJSONObject(i);
                if (cat.getString("name").equalsIgnoreCase(categoryName)) {
                    targetCategory = cat;
                    break;
                }
            }

            if (targetCategory == null) {
                targetCategory = new JSONObject();
                targetCategory.put("name", categoryName);
                targetCategory.put("items", new JSONArray());
                categoriesArray.put(targetCategory);
            }

            JSONArray itemsArray = targetCategory.getJSONArray("items");
            boolean itemUpdated = false;
            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject existingItem = itemsArray.getJSONObject(i);
                if (existingItem.getString("name").equalsIgnoreCase(itemName)) {
                    Optional<ButtonType> result = showAlertWithConfirmation("Item Exists",
                            "An item named '" + itemName + "' already exists in category '" + categoryName + "'.\n" +
                                    "Do you want to update its price to ₹" + price + " and image to '" + imageName + "'?");
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        existingItem.put("price", price);
                        existingItem.put("imageName", imageName);
                        itemUpdated = true;
                    } else {
                        return;
                    }
                    break;
                }
            }

            if (!itemUpdated) {
                JSONObject newItem = new JSONObject();
                newItem.put("name", itemName);
                newItem.put("imageName", imageName);
                newItem.put("price", price);
                itemsArray.put(newItem);
            }

            try (FileWriter writer = new FileWriter(menuFile)) {
                writer.write(rootJson.toString(4));
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "File Error", "Could not read/write '" + menuJsonPathStr + "': " + e.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "JSON Error", "Error processing '" + menuJsonPathStr + "': " + e.getMessage());
        }
    }

    public static void loadInventoryData() {
        menuItemsList.clear();
        categoriesList.clear();

        String menuItemsPathString = ConfigManager.getMenuItemsJsonPath();
        Path menuItemsPathObj = Paths.get(menuItemsPathString);

        File menuFile = menuItemsPathObj.toFile();

        if (menuItemsPathObj.getParent() != null && !Files.exists(menuItemsPathObj.getParent())) {
            try {
                Files.createDirectories(menuItemsPathObj.getParent());
            } catch (IOException e) {
                System.err.println("Could not create parent directory for '" + menuItemsPathString + "': " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "File System Error", "Could not create parent directory for menu JSON: " + e.getMessage());
                if (tableView != null) tableView.setPlaceholder(new Label("Error: Cannot create directory for menu file."));
                return;
            }
        }

        if (!menuFile.exists() || menuFile.length() == 0) {
            System.err.println("'" + menuItemsPathString + "' not found or is empty. Cannot load inventory.");
            if (tableView != null) {
                tableView.setPlaceholder(new Label("'" + menuItemsPathString + "' not found or is empty.\nAdd items to create it, or check Configuration."));
            }
            if (!menuFile.exists()) {
                try {
                    JSONObject root = new JSONObject();
                    root.put("categories", new JSONArray());
                    try (FileWriter writer = new FileWriter(menuFile)) {
                        writer.write(root.toString(2));
                    }
                } catch (IOException | JSONException e) {
                    System.err.println("Could not create placeholder '" + menuItemsPathString + "': " + e.getMessage());
                }
            }
            return;
        }
        if (tableView != null) {
            tableView.setPlaceholder(new Label("No items in inventory. Add some!"));
        }

        try (InputStream inputStream = Files.newInputStream(menuItemsPathObj)) {
            String jsonText = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            JSONObject jsonObject = new JSONObject(jsonText);
            JSONArray categoriesArray = jsonObject.getJSONArray("categories");

            List<String> tempCategories = new ArrayList<>();
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                String categoryName = categoryObj.getString("name");
                if (!tempCategories.contains(categoryName)) {
                    tempCategories.add(categoryName);
                }

                JSONArray itemsArray = categoryObj.getJSONArray("items");
                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject itemObj = itemsArray.getJSONObject(j);
                    menuItemsList.add(new DisplayMenuItem(
                            categoryName,
                            itemObj.getString("name"),
                            itemObj.optString("imageName", ""),
                            itemObj.getInt("price")
                    ));
                }
            }
            tempCategories.sort(String.CASE_INSENSITIVE_ORDER);
            categoriesList.setAll(tempCategories);

        } catch (IOException e) {
            System.err.println("Error reading '" + menuItemsPathString + "': " + e.getMessage());
            showAlert(Alert.AlertType.WARNING, "Load Error", "Could not load inventory from '" + menuItemsPathString + "': " + e.getMessage());
        } catch (JSONException e) {
            System.err.println("Error parsing '" + menuItemsPathString + "': " + e.getMessage());
            showAlert(Alert.AlertType.WARNING, "JSON Parse Error", "Error parsing '" + menuItemsPathString + "'. File might be corrupted: " + e.getMessage());
        }
    }

    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-background-color: " + BACKGROUND_LIGHT_NEUTRAL +";");
            // Generic button styling for alerts
            dialogPane.lookupAll(".button").forEach(node -> {
                if (node instanceof Button) {
                    Button button = (Button) node;
                    button.setStyle("-fx-background-color: " + PRIMARY_RED + "; -fx-text-fill: " + TEXT_ON_RED + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;");
                    button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: " + PRIMARY_RED_DARK + "; -fx-text-fill: " + TEXT_ON_RED + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;"));
                    button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + PRIMARY_RED + "; -fx-text-fill: " + TEXT_ON_RED + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;"));
                }
            });

            if (inventoryStage != null && inventoryStage.isShowing()) {
                alert.initOwner(inventoryStage);
            }
            alert.showAndWait();
        });
    }

    private static Optional<ButtonType> showAlertWithConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-background-color: " + BACKGROUND_LIGHT_NEUTRAL +";");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle("-fx-background-color: " + BUTTON_GREEN + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;");
            okButton.setOnMouseEntered(e -> okButton.setStyle("-fx-background-color: " + BUTTON_GREEN_HOVER + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;"));
            okButton.setOnMouseExited(e -> okButton.setStyle("-fx-background-color: " + BUTTON_GREEN + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;"));
        }
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setStyle("-fx-background-color: " + ACCENT_RED + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;");
            cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: " + PRIMARY_RED_DARK + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;"));
            cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: " + ACCENT_RED + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;"));
        }


        if (inventoryStage != null && inventoryStage.isShowing()) {
            alert.initOwner(inventoryStage);
        }
        return alert.showAndWait();
    }

    public static class DisplayMenuItem {
        private final String category;
        private final String name;
        private final String imageName;
        private final int price;

        public DisplayMenuItem(String category, String name, String imageName, int price) {
            this.category = category;
            this.name = name;
            this.imageName = imageName;
            this.price = price;
        }

        public String getCategory() { return category; }
        public String getName() { return name; }
        public String getImageName() { return imageName; }
        public int getPrice() { return price; }
    }
}
