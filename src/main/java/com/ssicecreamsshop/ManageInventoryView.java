package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigManager;
import com.ssicecreamsshop.utils.ExcelExportUtil; // For export functionality
import com.ssicecreamsshop.utils.ExcelImportDialog; // For import functionality
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
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

    private static TableView<DisplayMenuItem> tableView;
    private static final ObservableList<DisplayMenuItem> menuItemsList = FXCollections.observableArrayList();
    private static File selectedImageFile;
    private static Label selectedImageLabel;
    private static ComboBox<String> categoryComboBox;
    private static final ObservableList<String> categoriesList = FXCollections.observableArrayList();

    private static TextField itemNameField;
    private static TextField itemPriceField;
    private static Stage inventoryStage;


    public static void show() {
        inventoryStage = new Stage();
        inventoryStage.initModality(Modality.APPLICATION_MODAL);
        inventoryStage.setTitle("🍦 Manage Inventory");
        inventoryStage.setMinWidth(900); // Increased width for delete column
        inventoryStage.setMinHeight(700);


        // --- Input Form ---
        GridPane formPane = new GridPane();
        formPane.setHgap(10);
        formPane.setVgap(12);
        formPane.setPadding(new Insets(25));
        formPane.setStyle("-fx-background-color: #eef2f9;");

        formPane.add(new Label("Item Name:"), 0, 0);
        itemNameField = new TextField();
        itemNameField.setPromptText("e.g., Classic Vanilla");
        formPane.add(itemNameField, 1, 0);
        GridPane.setHgrow(itemNameField, Priority.ALWAYS);

        formPane.add(new Label("Item Price (₹):"), 0, 1);
        itemPriceField = new TextField();
        itemPriceField.setPromptText("e.g., 150");
        formPane.add(itemPriceField, 1, 1);
        itemPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                itemPriceField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        formPane.add(new Label("Category:"), 0, 2);
        categoryComboBox = new ComboBox<>(categoriesList);
        categoryComboBox.setEditable(true);
        categoryComboBox.setPromptText("Select or type new category");
        categoryComboBox.setMaxWidth(Double.MAX_VALUE);
        formPane.add(categoryComboBox, 1, 2);

        formPane.add(new Label("Item Image:"), 0, 3);
        Button browseButton = new Button("Browse...");
        browseButton.setStyle("-fx-background-color: #5c67f2; -fx-text-fill: white;");
        selectedImageLabel = new Label("No image selected");
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
        addItemButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        addItemButton.setMaxWidth(Double.MAX_VALUE);
        addItemButton.setOnAction(e -> handleAddItem());
        formPane.add(addItemButton, 1, 4);


        // --- Table View for Displaying Items ---
        tableView = new TableView<>();
        TableColumn<DisplayMenuItem, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(150);

        TableColumn<DisplayMenuItem, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<DisplayMenuItem, String> imageCol = new TableColumn<>("Image File");
        imageCol.setCellValueFactory(new PropertyValueFactory<>("imageName"));
        imageCol.setPrefWidth(150);

        TableColumn<DisplayMenuItem, Integer> priceCol = new TableColumn<>("Price (₹)");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);
        priceCol.setStyle("-fx-alignment: CENTER_RIGHT;");

        // --- Delete Action Column ---
        TableColumn<DisplayMenuItem, Void> deleteCol = new TableColumn<>("Actions");
        deleteCol.setPrefWidth(120); // Adjusted width
        deleteCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            {
                deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3 7;");
                deleteButton.setOnAction(event -> {
                    DisplayMenuItem item = getTableView().getItems().get(getIndex());
                    handleDeleteItem(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        tableView.getColumns().addAll(categoryCol, nameCol, imageCol, priceCol, deleteCol); // Added deleteCol
        tableView.setItems(menuItemsList);
        tableView.setPlaceholder(new Label("Loading inventory... or inventory file not found."));

        // --- Control Buttons for Table (Refresh, Export, Import) ---
        Button refreshButton = new Button("🔄 Refresh View");
        refreshButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        refreshButton.setOnAction(e -> loadInventoryData());

        Button exportButton = new Button("📤 Export to Excel");
        exportButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;"); // Green color
        exportButton.setOnAction(e -> ExcelExportUtil.exportToExcel(inventoryStage));

        Button importButton = new Button("📥 Import from Excel");
        // Consistent styling for import button
        String importButtonStyle = "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5px;";
        String importButtonHoverStyle = "-fx-background-color: #a93226; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5px;";
        importButton.setStyle(importButtonStyle);
        importButton.setOnMouseEntered(e -> importButton.setStyle(importButtonHoverStyle));
        importButton.setOnMouseExited(e -> importButton.setStyle(importButtonStyle));
        importButton.setOnAction(e -> ExcelImportDialog.show());


        HBox tableControlsBox = new HBox(10, refreshButton, exportButton, importButton); // Added importButton
        tableControlsBox.setAlignment(Pos.CENTER_RIGHT);
        tableControlsBox.setPadding(new Insets(0,0,10,0));


        Label currentInventoryLabel = new Label("Current Inventory:");
        currentInventoryLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        VBox bottomPane = new VBox(10, new Separator(), currentInventoryLabel, tableControlsBox, tableView);
        bottomPane.setPadding(new Insets(10, 25, 25, 25));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        bottomPane.setStyle("-fx-background-color: #ffffff;");

        BorderPane rootLayout = new BorderPane();
        rootLayout.setTop(formPane);
        rootLayout.setCenter(bottomPane);

        Scene scene = new Scene(rootLayout); // Width and height will be set by stage.setMinWidth/Height
        inventoryStage.setScene(scene);

        loadInventoryData();

        inventoryStage.showAndWait();
    }

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
                        "This will also attempt to delete its image file: " + itemToDelete.getImageName());

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean jsonUpdated = removeItemFromJson(itemToDelete);
            if (jsonUpdated) {
                // Attempt to delete the image file
                if (itemToDelete.getImageName() != null && !itemToDelete.getImageName().isEmpty()) {
                    try {
                        Path imageDirectory = Paths.get(ConfigManager.getImagePath());
                        Path imagePath = imageDirectory.resolve(itemToDelete.getImageName());

                        if (Files.exists(imagePath) && !Files.isDirectory(imagePath)) { // Ensure it's a file
                            Files.delete(imagePath);
                            System.out.println("Deleted image file: " + imagePath.toString());
                        } else if (Files.isDirectory(imagePath)) {
                            System.out.println("Image path is a directory, not deleting: " + imagePath.toString());
                        }
                        else {
                            System.out.println("Image file not found for deletion: " + imagePath.toString());
                        }
                    } catch (IOException e) {
                        System.err.println("Error deleting image file " + itemToDelete.getImageName() + ": " + e.getMessage());
                        showAlert(Alert.AlertType.WARNING, "Image Deletion Warning", "Item removed from inventory, but could not delete image file: " + itemToDelete.getImageName() + "\nError: " + e.getMessage());
                    } catch (SecurityException se) {
                        System.err.println("Security Exception deleting image file " + itemToDelete.getImageName() + ": " + se.getMessage());
                        showAlert(Alert.AlertType.ERROR, "Image Deletion Error", "Permission denied when trying to delete image file: " + itemToDelete.getImageName());
                    }
                }

                loadInventoryData(); // Refresh this view's table
                NewOrderView.loadMenuItemsFromJson(); // Refresh data in NewOrderView
                NewOrderView.refreshMenuView();       // Refresh NewOrderView's UI

                showAlert(Alert.AlertType.INFORMATION, "Delete Successful", "Item '" + itemToDelete.getName() + "' has been deleted.");
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
            JSONArray updatedCategoriesArray = new JSONArray(); // To build a new array without modification issues

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                if (categoryObj.getString("name").equalsIgnoreCase(itemToDelete.getCategory())) {
                    JSONArray itemsArray = categoryObj.getJSONArray("items");
                    JSONArray updatedItemsArray = new JSONArray();
                    boolean itemFoundInCategory = false;

                    for (int j = 0; j < itemsArray.length(); j++) {
                        JSONObject itemObj = itemsArray.getJSONObject(j);
                        if (itemObj.getString("name").equalsIgnoreCase(itemToDelete.getName())) {
                            itemRemoved = true; // Mark as removed
                            itemFoundInCategory = true;
                            System.out.println("Removing item '" + itemToDelete.getName() + "' from category '" + itemToDelete.getCategory() + "' in JSON.");
                        } else {
                            updatedItemsArray.put(itemObj); // Keep other items
                        }
                    }
                    // If items remain in category, or if it wasn't the target item's category
                    if (updatedItemsArray.length() > 0) {
                        categoryObj.put("items", updatedItemsArray);
                        updatedCategoriesArray.put(categoryObj);
                    } else if (!itemFoundInCategory) {
                        // Category was not the one we modified, or it was already empty but not the target. Keep it.
                        updatedCategoriesArray.put(categoryObj);
                    } else {
                        System.out.println("Category '" + itemToDelete.getCategory() + "' is now empty after item deletion and will be removed from JSON.");
                    }
                } else {
                    updatedCategoriesArray.put(categoryObj); // Keep other categories
                }
            }
            rootJson.put("categories", updatedCategoriesArray); // Replace with the potentially modified array

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

        // Ensure parent directory for JSON file exists
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
                            itemObj.optString("imageName", ""), // Handle missing imageName gracefully
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
