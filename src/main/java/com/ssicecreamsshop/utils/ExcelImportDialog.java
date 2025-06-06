package com.ssicecreamsshop.utils;

import com.ssicecreamsshop.ManageInventoryView;
import com.ssicecreamsshop.NewOrderView;
import com.ssicecreamsshop.config.ConfigManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExcelImportDialog {

    // --- Navy Blue & Yellow Theme Colors ---
    private static final String PRIMARY_NAVY = "#1A237E";
    private static final String PRIMARY_NAVY_DARK = "#283593";
    private static final String TEXT_ON_DARK = "white";
    private static final String BACKGROUND_MAIN = "#E8EAF6";
    private static final String BORDER_COLOR_LIGHT = "#CFD8DC";
    private static final String SHADOW_COLOR = "rgba(26, 35, 126, 0.2)";
    private static final String BUTTON_ACTION_GREEN = "#4CAF50";
    private static final String BUTTON_ACTION_GREEN_HOVER = "#388E3C";
    private static final String BUTTON_CLOSE_GRAY = "#95a5a6";
    private static final String BUTTON_CLOSE_GRAY_HOVER = "#7f8c8d";
    private static final String BUTTON_ACTION_RED = "#F44336"; // For error alerts
    private static final String BUTTON_ACTION_RED_HOVER = "#D32F2F";


    private static Stage dialogStage;
    private static File selectedExcelFile;
    private static Label selectedFileLabel;
    private static TextArea logArea;

    private static final int COL_CATEGORY = 0;
    private static final int COL_ITEM_NAME = 1;
    private static final int COL_PRICE = 2;
    private static final int COL_IMAGE_FILENAME = 3;
    private static final int COL_QUANTITY = 4;

    private static class ImportedItem {
        String category, name, imageName;
        int price;
        Integer quantity;
        ImportedItem(String category, String name, int price, String imageName, Integer quantity) {
            this.category = category; this.name = name; this.price = price; this.imageName = imageName; this.quantity = quantity;
        }
    }

    public static void show() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("📥 Import Menu from Excel");
        dialogStage.setMinWidth(650);
        dialogStage.setMinHeight(550);

        BorderPane rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: " + BACKGROUND_MAIN + "; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        VBox topPane = new VBox(18);
        topPane.setPadding(new Insets(25));
        topPane.setAlignment(Pos.CENTER_LEFT);
        topPane.setStyle("-fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 0 0 1.5px 0; -fx-background-color: #f1f3f8;");

        Label instructionLabel = new Label(
                "Select an Excel file (.xlsx or .xls) to import/update menu items.\n" +
                        "Expected columns: Category, Item Name, Price, Image Filename, Quantity (optional).\n" +
                        "The first row is assumed to be headers and will be skipped."
        );
        instructionLabel.setWrapText(true);
        instructionLabel.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #424242; -fx-line-spacing: 3px;");

        selectedFileLabel = new Label("No file selected.");
        selectedFileLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY_DARK + ";");

        Button browseButton = new Button("Browse Excel File...");
        styleDialogButton(browseButton, PRIMARY_NAVY, PRIMARY_NAVY_DARK, false);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel Menu File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));
        browseButton.setOnAction(e -> {
            selectedExcelFile = fileChooser.showOpenDialog(dialogStage);
            if (selectedExcelFile != null) {
                selectedFileLabel.setText("Selected: " + selectedExcelFile.getName());
                logArea.setText("File selected: " + selectedExcelFile.getAbsolutePath() + "\nReady to import.");
            } else {
                selectedFileLabel.setText("No file selected.");
            }
        });
        HBox fileSelectionHBox = new HBox(12, browseButton, selectedFileLabel);
        fileSelectionHBox.setAlignment(Pos.CENTER_LEFT);
        topPane.getChildren().addAll(instructionLabel, fileSelectionHBox);
        rootLayout.setTop(topPane);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPromptText("Import logs will appear here...");
        logArea.setStyle("-fx-font-family: 'Monospaced', 'Consolas', monospace; -fx-font-size: 12.5px; -fx-control-inner-background: #fafafa; -fx-text-fill: #333; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        logArea.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        BorderPane.setMargin(logArea, new Insets(15, 25, 15, 25));
        rootLayout.setCenter(logArea);

        Button importButton = new Button("🚀 Import Data");
        styleDialogButton(importButton, BUTTON_ACTION_GREEN, BUTTON_ACTION_GREEN_HOVER, true);
        importButton.setOnAction(e -> processExcelImport());

        Button closeButton = new Button("Close Window");
        styleDialogButton(closeButton, BUTTON_CLOSE_GRAY, BUTTON_CLOSE_GRAY_HOVER, true);
        closeButton.setOnAction(e -> dialogStage.close());

        HBox bottomBar = new HBox(20, importButton, closeButton);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(20, 25, 25, 25));
        rootLayout.setBottom(bottomBar);

        Scene scene = new Scene(rootLayout);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private static void styleDialogButton(Button button, String baseColor, String hoverColor, boolean isPrimary) {
        String padding = isPrimary ? "10 22" : "8 15";
        String fontSize = isPrimary ? "14px" : "13px";
        String style = "-fx-font-size: " + fontSize + "; -fx-text-fill: " + TEXT_ON_DARK + "; -fx-font-weight: bold; -fx-padding: " + padding + "; -fx-background-radius: 20px;";
        button.setStyle(style + "-fx-background-color: " + baseColor + ";");
        button.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(style + "-fx-background-color: " + hoverColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 7, 0.2, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(style + "-fx-background-color: " + baseColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 3, 0, 0, 0);"));
    }

    private static void processExcelImport() {
        if (selectedExcelFile == null) {
            showAlert(Alert.AlertType.WARNING, "No File Selected", "Please select an Excel file first.");
            logArea.appendText("\nERROR: No Excel file selected for import.");
            return;
        }

        logArea.setText("Starting import from: " + selectedExcelFile.getName() + "...\n");
        List<ImportedItem> importedItems = new ArrayList<>();
        int rowNum = 0; int itemsSkipped = 0;

        try (FileInputStream fis = new FileInputStream(selectedExcelFile);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                showAlert(Alert.AlertType.ERROR, "Import Error", "No sheet found in the Excel file.");
                logArea.appendText("ERROR: No sheet found in the Excel file.\n");
                return;
            }

            logArea.appendText("Reading sheet: " + sheet.getSheetName() + "\n");

            for (Row row : sheet) {
                rowNum++;
                if (rowNum == 1) { logArea.appendText("Skipping header row (Row 1).\n"); continue; }

                Cell categoryCell = row.getCell(COL_CATEGORY); Cell nameCell = row.getCell(COL_ITEM_NAME); Cell priceCell = row.getCell(COL_PRICE);
                Cell imageCell = row.getCell(COL_IMAGE_FILENAME); Cell quantityCell = row.getCell(COL_QUANTITY);

                if (isCellEmpty(categoryCell) || isCellEmpty(nameCell) || isCellEmpty(priceCell)) {
                    logArea.appendText("WARNING: Row " + rowNum + ": Skipping due to missing Category, Name, or Price.\n");
                    itemsSkipped++; continue;
                }

                String category = getCellStringValue(categoryCell).trim();
                String itemName = getCellStringValue(nameCell).trim();
                String imageName = isCellEmpty(imageCell) ? "" : getCellStringValue(imageCell).trim();
                int price = 0;
                Integer quantity = null;

                try { price = (int) Double.parseDouble(getCellStringValue(priceCell)); if (price <= 0) throw new NumberFormatException(); }
                catch (NumberFormatException e) { logArea.appendText("WARNING: Row " + rowNum + " ("+itemName+"): Invalid price. Skipping.\n"); itemsSkipped++; continue; }

                try {
                    if (!isCellEmpty(quantityCell)) quantity = (int) Double.parseDouble(getCellStringValue(quantityCell));
                } catch (NumberFormatException e) { logArea.appendText("WARNING: Row " + rowNum + " ("+itemName+"): Invalid quantity. Stock will be unlimited. \n"); }

                importedItems.add(new ImportedItem(category, itemName, price, imageName, quantity));
                logArea.appendText("Read Row " + rowNum + ": " + category + ", " + itemName + ", " + price + ", " + (quantity != null ? quantity : "Unlimited") + "\n");
            }

            if (importedItems.isEmpty()) {
                logArea.appendText("No valid items found to import.\n");
                showAlert(Alert.AlertType.INFORMATION, "Import Complete", "No valid items were found in the Excel file to import.");
                return;
            }

            logArea.appendText("\nUpdating menu_items.json...\n");
            UpdateResult result = batchUpdateItemsInJson(importedItems);

            logArea.appendText("JSON update complete. Added: " + result.itemsAdded + ", Updated: " + result.itemsUpdated + ".\n");

            ManageInventoryView.loadInventoryData();
            NewOrderView.loadMenuItemsFromJson();
            NewOrderView.refreshMenuView();
            logArea.appendText("Application views refreshed.\n");

            showAlert(Alert.AlertType.INFORMATION, "Import Successful", "Excel import complete!\n\n" + "Items Added: " + result.itemsAdded + "\n" + "Items Updated: " + result.itemsUpdated + "\n" + "Items Skipped: " + itemsSkipped);

        } catch (Exception e) {
            logArea.appendText("UNEXPECTED ERROR during import: " + e.getMessage() + "\n");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Import Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private static boolean isCellEmpty(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return true;
        return cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty();
    }


    private static class UpdateResult { int itemsAdded = 0; int itemsUpdated = 0; }

    private static UpdateResult batchUpdateItemsInJson(List<ImportedItem> itemsToImport) {
        UpdateResult result = new UpdateResult();
        String menuJsonPathStr = ConfigManager.getMenuItemsJsonPath();
        File menuFile = new File(menuJsonPathStr);
        JSONObject rootJson;

        try {
            Path menuPathObj = Paths.get(menuJsonPathStr);
            if (menuPathObj.getParent() != null && !Files.exists(menuPathObj.getParent())) Files.createDirectories(menuPathObj.getParent());
            if (menuFile.exists() && menuFile.length() > 0) rootJson = new JSONObject(new String(Files.readAllBytes(menuFile.toPath()), StandardCharsets.UTF_8));
            else { rootJson = new JSONObject(); rootJson.put("categories", new JSONArray()); }

            JSONArray categoriesArray = rootJson.getJSONArray("categories");

            for (ImportedItem itemToImport : itemsToImport) {
                JSONObject targetCategory = null;
                for (int i = 0; i < categoriesArray.length(); i++) {
                    JSONObject cat = categoriesArray.getJSONObject(i);
                    if (cat.getString("name").equalsIgnoreCase(itemToImport.category)) {
                        targetCategory = cat; break;
                    }
                }
                if (targetCategory == null) {
                    targetCategory = new JSONObject(); targetCategory.put("name", itemToImport.category);
                    targetCategory.put("items", new JSONArray()); categoriesArray.put(targetCategory);
                }

                JSONArray itemsArray = targetCategory.getJSONArray("items");
                boolean itemFoundAndUpdated = false;
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject existingItem = itemsArray.getJSONObject(i);
                    if (existingItem.getString("name").equalsIgnoreCase(itemToImport.name)) {
                        existingItem.put("price", itemToImport.price);
                        existingItem.put("imageName", itemToImport.imageName);
                        if (itemToImport.quantity != null) existingItem.put("quantity", itemToImport.quantity);
                        else existingItem.remove("quantity"); // Remove for unlimited
                        itemFoundAndUpdated = true; result.itemsUpdated++;
                        logArea.appendText("Updated item: " + itemToImport.name + "\n");
                        break;
                    }
                }

                if (!itemFoundAndUpdated) {
                    JSONObject newItem = new JSONObject(); newItem.put("name", itemToImport.name);
                    newItem.put("imageName", itemToImport.imageName); newItem.put("price", itemToImport.price);
                    if (itemToImport.quantity != null) newItem.put("quantity", itemToImport.quantity);
                    itemsArray.put(newItem); result.itemsAdded++;
                    logArea.appendText("Added new item: " + itemToImport.name + "\n");
                }
            }
            try (FileWriter writer = new FileWriter(menuFile)) { writer.write(rootJson.toString(4)); }
        } catch (Exception e) {
            logArea.appendText("ERROR writing to JSON file: " + e.getMessage() + "\n");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "JSON Write Error", "Could not write to menu_items.json: " + e.getMessage());
        }
        return result;
    }


    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-background-color: " + BACKGROUND_MAIN +";");
            Button button = (Button) dialogPane.lookupButton(alert.getButtonTypes().get(0));
            if (button != null) {
                String baseColor = alertType == Alert.AlertType.ERROR || alertType == Alert.AlertType.WARNING ? BUTTON_ACTION_RED : PRIMARY_NAVY;
                String hoverColor = alertType == Alert.AlertType.ERROR || alertType == Alert.AlertType.WARNING ? BUTTON_ACTION_RED_HOVER : PRIMARY_NAVY_DARK;
                String btnStyle = "-fx-text-fill: " + TEXT_ON_DARK + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;";
                button.setStyle(btnStyle + "-fx-background-color: " + baseColor + ";");
                button.setOnMouseEntered(e -> button.setStyle(btnStyle + "-fx-background-color: " + hoverColor + ";"));
                button.setOnMouseExited(e -> button.setStyle(btnStyle + "-fx-background-color: " + baseColor + ";"));
            }
            alert.initOwner(dialogStage);
            alert.showAndWait();
        });
    }
}
