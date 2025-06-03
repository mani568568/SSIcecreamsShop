package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigManager; // Import ConfigManager
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

    // Paths are now retrieved from ConfigManager, no longer static final here.
    // private static final String MENU_ITEMS_FILE_PATH = "src/main/resources/menu_items.json"; // OLD
    // private static final String IMAGES_DIRECTORY_PATH = "src/main/resources/images/"; // OLD

    private static TableView<DisplayMenuItem> tableView;
    private static final ObservableList<DisplayMenuItem> menuItemsList = FXCollections.observableArrayList();
    private static File selectedImageFile;
    private static Label selectedImageLabel;
    private static ComboBox<String> categoryComboBox;
    private static final ObservableList<String> categoriesList = FXCollections.observableArrayList();

    private static TextField itemNameField;
    private static TextField itemPriceField;
    private static Stage inventoryStage; // Keep a reference to the stage for alerts


    public static void show() {
        inventoryStage = new Stage(); // Initialize here
        inventoryStage.initModality(Modality.APPLICATION_MODAL);
        inventoryStage.setTitle("🍦 Manage Inventory");

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
            // Set initial directory for FileChooser based on configured images path
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

        tableView.getColumns().addAll(categoryCol, nameCol, imageCol, priceCol);
        tableView.setItems(menuItemsList);
        tableView.setPlaceholder(new Label("Loading inventory... or inventory file not found."));

        Button refreshButton = new Button("🔄 Refresh View");
        refreshButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        refreshButton.setOnAction(e -> loadInventoryData());
        HBox refreshButtonBox = new HBox(refreshButton);
        refreshButtonBox.setAlignment(Pos.CENTER_RIGHT);
        refreshButtonBox.setPadding(new Insets(0,0,10,0));

        Label currentInventoryLabel = new Label("Current Inventory:");
        currentInventoryLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        VBox bottomPane = new VBox(10, new Separator(), currentInventoryLabel, refreshButtonBox, tableView);
        bottomPane.setPadding(new Insets(10, 25, 25, 25));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        bottomPane.setStyle("-fx-background-color: #ffffff;");

        BorderPane rootLayout = new BorderPane();
        rootLayout.setTop(formPane);
        rootLayout.setCenter(bottomPane);

        Scene scene = new Scene(rootLayout, 800, 700);
        inventoryStage.setScene(scene);

        loadInventoryData(); // Initial load

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
                Files.createDirectories(targetImageDir); // Ensure configured image directory exists
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
        NewOrderView.loadMenuItemsFromJson(); // Refresh data in NewOrderView
        NewOrderView.refreshMenuView(); // Also refresh its UI if it's active

        showAlert(Alert.AlertType.INFORMATION, "Success", "Item '" + itemName + "' added to inventory!");
    }

    private static void addItemToJson(String categoryName, String itemName, int price, String imageName) {
        String menuJsonPathStr = ConfigManager.getMenuItemsJsonPath();
        File menuFile = new File(menuJsonPathStr);
        JSONObject rootJson;

        try {
            // Ensure parent directory for JSON file exists
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
                        return; // User chose not to update
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

            try (FileWriter writer = new FileWriter(menuFile)) { // Use menuFile (derived from configured path)
                writer.write(rootJson.toString(4));
                System.out.println("Item '" + itemName + (itemUpdated ? "' updated" : "' added") + " in JSON in category '" + categoryName + "'.");
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "File Error", "Could not read/write '" + menuJsonPathStr + "': " + e.getMessage() +
                    "\nPlease ensure the path is correct and the application has write permissions.");
        } catch (JSONException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "JSON Error", "Error processing '" + menuJsonPathStr + "': " + e.getMessage() +
                    "\nThe file might be corrupted. Please check its format.");
        }
    }

    // Make this public static so ConfigurationDialog can call it
    public static void loadInventoryData() {
        menuItemsList.clear();
        categoriesList.clear();

        String menuItemsPathString = ConfigManager.getMenuItemsJsonPath();
        String imagesDirPathString = ConfigManager.getImagePath(); // Get configured images path

        Path menuItemsPathObj = Paths.get(menuItemsPathString);
        Path imagesDirPathObj = Paths.get(imagesDirPathString);

        // Ensure images directory exists (primarily for context, actual image use is in NewOrderView)
        try {
            if (!Files.exists(imagesDirPathObj)) {
                Files.createDirectories(imagesDirPathObj);
                System.out.println("Created images directory (from ManageInventoryView): " + imagesDirPathString);
            } else if (!Files.isDirectory(imagesDirPathObj)) {
                System.err.println("Configured images path is not a directory: " + imagesDirPathString);
                showAlert(Alert.AlertType.ERROR, "Configuration Error", "Images path (" + imagesDirPathString + ") is not a directory. Please correct it in Configuration.");
                if (tableView != null) tableView.setPlaceholder(new Label("Error: Images path is not a directory. Check Configuration."));
                return;
            }
        } catch (IOException e) {
            System.err.println("Error ensuring images directory '" + imagesDirPathString + "': " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "File System Error", "Could not create/access images directory: " + e.getMessage());
            if (tableView != null) tableView.setPlaceholder(new Label("Error with images directory. Check logs and Configuration."));
            return;
        }

        File menuFile = menuItemsPathObj.toFile();

        if (!Files.exists(menuItemsPathObj.getParent())) {
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
            // Attempt to create a placeholder file if it doesn't exist
            if (!menuFile.exists()) {
                try {
                    JSONObject root = new JSONObject();
                    root.put("categories", new JSONArray());
                    try (FileWriter writer = new FileWriter(menuFile)) {
                        writer.write(root.toString(2));
                        System.out.println("Created empty placeholder: " + menuItemsPathString);
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

        try (InputStream inputStream = Files.newInputStream(menuItemsPathObj)) { // Use Path object
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
                            itemObj.getString("imageName"), // This is just the filename
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
            if (inventoryStage != null && inventoryStage.isShowing()) { // Check if stage is available
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
            this.imageName = imageName; // Store just the filename
            this.price = price;
        }

        public String getCategory() { return category; }
        public String getName() { return name; }
        public String getImageName() { return imageName; }
        public int getPrice() { return price; }
    }
}
