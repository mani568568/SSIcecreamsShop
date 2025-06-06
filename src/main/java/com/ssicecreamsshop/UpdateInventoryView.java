package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigManager;
import com.ssicecreamsshop.utils.ExcelExportUtil;
import com.ssicecreamsshop.utils.ExcelImportDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode; // Added import
import javafx.scene.input.KeyEvent; // Added import
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UpdateInventoryView {

    // --- Navy Blue & Yellow Theme Colors ---
    private static final String PRIMARY_NAVY = "#1A237E";
    private static final String PRIMARY_NAVY_DARK = "#283593";
    private static final String TEXT_ON_DARK = "white";
    private static final String BACKGROUND_MAIN = "#E8EAF6";
    private static final String BACKGROUND_CONTENT = "#FFFFFF";
    private static final String SHADOW_COLOR = "rgba(26, 35, 126, 0.2)";
    private static final String BUTTON_ACTION_BLUE = "#2196F3";
    private static final String BUTTON_ACTION_BLUE_HOVER = "#1976D2";
    private static final String BUTTON_ACTION_GREEN = "#4CAF50";
    private static final String BUTTON_ACTION_GREEN_HOVER = "#388E3C";
    private static final String BUTTON_ACTION_YELLOW = "#FFC107";
    private static final String BUTTON_ACTION_YELLOW_HOVER = "#FFA000";
    private static final String BUTTON_ACTION_RED = "#F44336";
    private static final String BUTTON_ACTION_RED_HOVER = "#D32F2F";
    private static final String TEXT_ON_YELLOW = "#212121";


    private static TableView<InventoryItem> tableView;
    private static final ObservableList<InventoryItem> inventoryList = FXCollections.observableArrayList();
    private static Stage stage;

    public static void show() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("📦 Update Item Stock");
        stage.setMinWidth(950);
        stage.setMinHeight(700);

        try {
            Image appIcon = new Image(UpdateInventoryView.class.getResourceAsStream("/images/app_icon.png"));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Error loading icon for Update Stock window: " + e.getMessage());
        }

        Label titleLabel = new Label("Update Inventory Stock");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY + ";");
        titleLabel.setEffect(new DropShadow(5, Color.web(SHADOW_COLOR)));

        Button refreshButton = new Button("🔄 Refresh");
        styleControlButton(refreshButton, BUTTON_ACTION_BLUE, BUTTON_ACTION_BLUE_HOVER);
        refreshButton.setOnAction(e -> loadInventoryData());

        Button exportButton = new Button("📤 Export to Excel");
        styleControlButton(exportButton, BUTTON_ACTION_GREEN, BUTTON_ACTION_GREEN_HOVER);
        exportButton.setOnAction(e -> ExcelExportUtil.exportToExcel(stage));

        Button importButton = new Button("📥 Import from Excel");
        styleControlButton(importButton, BUTTON_ACTION_YELLOW, BUTTON_ACTION_YELLOW_HOVER, TEXT_ON_YELLOW);
        importButton.setOnAction(e -> {
            ExcelImportDialog.show();
            loadInventoryData();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topControls = new HBox(15, titleLabel, spacer, refreshButton, exportButton, importButton);
        topControls.setAlignment(Pos.CENTER_LEFT);
        topControls.setPadding(new Insets(20, 25, 15, 25));

        tableView = new TableView<>();
        tableView.setStyle("-fx-font-size: 13.5px; -fx-selection-bar: #C5CAE9; -fx-selection-bar-text: " + PRIMARY_NAVY + ";");
        tableView.setEffect(new DropShadow(5, Color.web(SHADOW_COLOR)));
        setupTableColumns();
        tableView.setItems(inventoryList);
        tableView.setPlaceholder(new Label("No items found in inventory."));

        VBox mainLayout = new VBox(15, topControls, tableView);
        mainLayout.setPadding(new Insets(0, 25, 25, 25));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        mainLayout.setStyle("-fx-background-color: " + BACKGROUND_MAIN + ";");

        Scene scene = new Scene(mainLayout);

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

    private static void setupTableColumns() {
        tableView.getColumns().clear();
        String cellStyle = "-fx-alignment: CENTER_LEFT; -fx-padding: 8px;";

        TableColumn<InventoryItem, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);
        nameCol.setStyle(cellStyle);

        TableColumn<InventoryItem, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(150);
        categoryCol.setStyle(cellStyle);

        TableColumn<InventoryItem, String> quantityCol = new TableColumn<>("Current Stock");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(120);
        quantityCol.setStyle(cellStyle + "-fx-font-weight: bold;");

        TableColumn<InventoryItem, Void> updateActionCol = new TableColumn<>("Update Stock");
        updateActionCol.setPrefWidth(220);
        updateActionCol.setSortable(false);
        updateActionCol.setCellFactory(param -> new UpdateCell());

        TableColumn<InventoryItem, Void> deleteActionCol = new TableColumn<>("Remove");
        deleteActionCol.setPrefWidth(100);
        deleteActionCol.setSortable(false);
        deleteActionCol.setCellFactory(param -> new DeleteCell());


        tableView.getColumns().addAll(nameCol, categoryCol, quantityCol, updateActionCol, deleteActionCol);
    }

    private static void loadInventoryData() {
        inventoryList.clear();
        Path menuItemsPathObj = Paths.get(ConfigManager.getMenuItemsJsonPath());
        if (!Files.exists(menuItemsPathObj)) return;

        try (java.io.InputStream inputStream = Files.newInputStream(menuItemsPathObj)) {
            String jsonText = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            JSONObject jsonObject = new JSONObject(jsonText);
            JSONArray categoriesArray = jsonObject.optJSONArray("categories");
            if(categoriesArray == null) return;

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                String categoryName = categoryObj.getString("name");
                JSONArray itemsArray = categoryObj.getJSONArray("items");
                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject itemObj = itemsArray.getJSONObject(j);
                    inventoryList.add(new InventoryItem(
                            itemObj.getString("name"),
                            categoryName,
                            itemObj.has("quantity") ? String.valueOf(itemObj.getInt("quantity")) : "Unlimited"
                    ));
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private static void updateItemQuantity(String itemName, String category, int newQuantity) {
        Path menuItemsPath = Paths.get(ConfigManager.getMenuItemsJsonPath());
        File menuFile = menuItemsPath.toFile();
        if (!menuFile.exists()) return;

        try {
            String content = new String(Files.readAllBytes(menuItemsPath), StandardCharsets.UTF_8);
            JSONObject rootJson = new JSONObject(content);
            JSONArray categoriesArray = rootJson.getJSONArray("categories");
            boolean updated = false;

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                if (categoryObj.getString("name").equalsIgnoreCase(category)) {
                    JSONArray itemsArray = categoryObj.getJSONArray("items");
                    for (int j = 0; j < itemsArray.length(); j++) {
                        JSONObject itemObj = itemsArray.getJSONObject(j);
                        if (itemObj.getString("name").equalsIgnoreCase(itemName)) {
                            itemObj.put("quantity", newQuantity);
                            updated = true;
                            break;
                        }
                    }
                }
                if(updated) break;
            }

            if (updated) {
                try (FileWriter writer = new FileWriter(menuFile)) {
                    writer.write(rootJson.toString(4));
                }
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Stock for '" + itemName + "' updated to " + newQuantity + ".");
                    loadInventoryData();
                    NewOrderView.loadMenuItemsFromJson();
                    NewOrderView.refreshMenuView();
                });
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "Failed to update stock quantity."));
        }
    }

    private static void handleDeleteItem(InventoryItem itemToDelete) {
        Optional<ButtonType> result = showAlertWithConfirmation(
                "Confirm Delete",
                "Are you sure you want to permanently delete the item '" + itemToDelete.getName() + "'?\n\nThis action cannot be undone."
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (removeItemFromJson(itemToDelete)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Item '" + itemToDelete.getName() + "' has been deleted.");
                loadInventoryData();
                NewOrderView.loadMenuItemsFromJson();
                NewOrderView.refreshMenuView();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete the item from the menu file.");
            }
        }
    }

    private static boolean removeItemFromJson(InventoryItem itemToDelete) {
        Path menuItemsPath = Paths.get(ConfigManager.getMenuItemsJsonPath());
        File menuFile = menuItemsPath.toFile();
        if (!menuFile.exists()) return false;

        try {
            String content = new String(Files.readAllBytes(menuItemsPath), StandardCharsets.UTF_8);
            JSONObject rootJson = new JSONObject(content);
            JSONArray categoriesArray = rootJson.getJSONArray("categories");
            boolean itemRemoved = false;

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                if (categoryObj.getString("name").equalsIgnoreCase(itemToDelete.getCategory())) {
                    JSONArray itemsArray = categoryObj.getJSONArray("items");
                    JSONArray newItemsArray = new JSONArray();
                    for (int j = 0; j < itemsArray.length(); j++) {
                        if (!itemsArray.getJSONObject(j).getString("name").equalsIgnoreCase(itemToDelete.getName())) {
                            newItemsArray.put(itemsArray.getJSONObject(j));
                        } else {
                            itemRemoved = true;
                        }
                    }
                    categoryObj.put("items", newItemsArray);
                }
            }

            if (itemRemoved) {
                try (FileWriter writer = new FileWriter(menuFile)) {
                    writer.write(rootJson.toString(4));
                }
            }
            return itemRemoved;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void styleControlButton(Button button, String baseColor, String hoverColor) {
        styleControlButton(button, baseColor, hoverColor, TEXT_ON_DARK);
    }

    private static void styleControlButton(Button button, String baseColor, String hoverColor, String textColor) {
        String style = "-fx-font-size: 13px; -fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-padding: 7 15; -fx-background-radius: 18px;";
        button.setStyle(style + "-fx-background-color: " + baseColor + ";");
        button.setEffect(new DropShadow(2, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(style + "-fx-background-color: " + hoverColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 5, 0.1, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(style + "-fx-background-color: " + baseColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 2, 0, 0, 0);"));
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

    private static Optional<ButtonType> showAlertWithConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13.5px; -fx-background-color: " + BACKGROUND_MAIN +";");
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) styleControlButton(okButton, BUTTON_ACTION_GREEN, BUTTON_ACTION_GREEN_HOVER);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) styleControlButton(cancelButton, BUTTON_ACTION_RED, BUTTON_ACTION_RED_HOVER);
        if (stage != null && stage.isShowing()) alert.initOwner(stage);
        return alert.showAndWait();
    }

    public static class InventoryItem {
        private final SimpleStringProperty name, category, quantity;
        public InventoryItem(String name, String category, String quantity) {
            this.name = new SimpleStringProperty(name);
            this.category = new SimpleStringProperty(category);
            this.quantity = new SimpleStringProperty(quantity);
        }
        public String getName() { return name.get(); }
        public String getCategory() { return category.get(); }
        public String getQuantity() { return quantity.get(); }
    }

    private static class UpdateCell extends TableCell<InventoryItem, Void> {
        private final Spinner<Integer> quantitySpinner = new Spinner<>();
        private final Button updateButton = new Button("Update");
        private final HBox pane = new HBox(10, quantitySpinner, updateButton);

        UpdateCell() {
            pane.setAlignment(Pos.CENTER);
            SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 0);
            quantitySpinner.setValueFactory(valueFactory);
            quantitySpinner.setEditable(true);
            quantitySpinner.setPrefWidth(80);
            styleControlButton(updateButton, BUTTON_ACTION_GREEN, BUTTON_ACTION_GREEN_HOVER);

            updateButton.setOnAction(event -> {
                InventoryItem item = getTableView().getItems().get(getIndex());
                int newQuantity = quantitySpinner.getValue();
                updateItemQuantity(item.getName(), item.getCategory(), newQuantity);
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : pane);
        }
    }

    private static class DeleteCell extends TableCell<InventoryItem, Void> {
        private final Button deleteButton = new Button("Delete 🗑️");

        DeleteCell() {
            styleControlButton(deleteButton, BUTTON_ACTION_RED, BUTTON_ACTION_RED_HOVER);
            deleteButton.setOnAction(event -> {
                InventoryItem item = getTableView().getItems().get(getIndex());
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
    }
}
