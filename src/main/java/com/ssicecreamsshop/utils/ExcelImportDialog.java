package com.ssicecreamsshop.utils; // Or com.ssicecreamsshop.config

import com.ssicecreamsshop.ManageInventoryView;
import com.ssicecreamsshop.NewOrderView;
import com.ssicecreamsshop.config.ConfigManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
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

public class ExcelImportDialog {

    private static Stage dialogStage;
    private static File selectedExcelFile;
    private static Label selectedFileLabel;
    private static TextArea logArea;

    // Define expected column indices (0-based)
    private static final int COL_CATEGORY = 0;
    private static final int COL_ITEM_NAME = 1;
    private static final int COL_PRICE = 2;
    private static final int COL_IMAGE_FILENAME = 3;

    public static void show() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("📥 Import Menu from Excel");
        dialogStage.setMinWidth(550);
        dialogStage.setMinHeight(450);

        BorderPane rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: #f4f6f8;");

        // --- Top: File Selection ---
        VBox topPane = new VBox(15);
        topPane.setPadding(new Insets(20));
        topPane.setAlignment(Pos.CENTER_LEFT);

        Label instructionLabel = new Label(
                "Select an Excel file (.xlsx or .xls) to import menu items.\n" +
                        "Expected columns: Category, Item Name, Price, Image Filename (in that order).\n" +
                        "The first row will be skipped (assumed to be headers)."
        );
        instructionLabel.setWrapText(true);
        instructionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        selectedFileLabel = new Label("No file selected.");
        selectedFileLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #555;");

        Button browseButton = new Button("Browse Excel File...");
        browseButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5px;");
        browseButton.setOnMouseEntered(e -> browseButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5px;"));
        browseButton.setOnMouseExited(e -> browseButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5px;"));

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel Menu File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );
        browseButton.setOnAction(e -> {
            selectedExcelFile = fileChooser.showOpenDialog(dialogStage);
            if (selectedExcelFile != null) {
                selectedFileLabel.setText("Selected: " + selectedExcelFile.getName());
                logArea.setText("File selected: " + selectedExcelFile.getAbsolutePath() + "\nReady to import.");
            } else {
                selectedFileLabel.setText("No file selected.");
            }
        });
        topPane.getChildren().addAll(instructionLabel, new HBox(10, browseButton, selectedFileLabel));
        rootLayout.setTop(topPane);

        // --- Center: Log Area ---
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPromptText("Import logs will appear here...");
        logArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");
        VBox.setVgrow(logArea, Priority.ALWAYS);
        BorderPane.setMargin(logArea, new Insets(0, 20, 10, 20));
        rootLayout.setCenter(logArea);


        // --- Bottom: Action Buttons ---
        Button importButton = new Button("🚀 Import Data");
        importButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-font-size:14px; -fx-background-radius: 5px;");
        importButton.setOnAction(e -> processExcelImport());

        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 10 20;-fx-font-size:14px; -fx-background-radius: 5px;");
        closeButton.setOnAction(e -> dialogStage.close());

        HBox bottomBar = new HBox(20, importButton, closeButton);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(15, 20, 20, 20));
        rootLayout.setBottom(bottomBar);

        Scene scene = new Scene(rootLayout);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private static void processExcelImport() {
        if (selectedExcelFile == null) {
            showAlert(Alert.AlertType.WARNING, "No File Selected", "Please select an Excel file first.");
            logArea.appendText("\nERROR: No Excel file selected for import.");
            return;
        }

        logArea.setText("Starting import from: " + selectedExcelFile.getName() + "...\n");
        List<ManageInventoryView.DisplayMenuItem> importedItems = new ArrayList<>();
        int rowNum = 0;
        int itemsAdded = 0;
        int itemsUpdated = 0;
        int itemsSkipped = 0;

        try (FileInputStream fis = new FileInputStream(selectedExcelFile);
             Workbook workbook = WorkbookFactory.create(fis)) { // WorkbookFactory handles both .xls and .xlsx

            Sheet sheet = workbook.getSheetAt(0); // Get the first sheet
            if (sheet == null) {
                showAlert(Alert.AlertType.ERROR, "Import Error", "No sheet found in the Excel file.");
                logArea.appendText("ERROR: No sheet found in the Excel file.\n");
                return;
            }

            logArea.appendText("Reading sheet: " + sheet.getSheetName() + "\n");

            for (Row row : sheet) {
                rowNum++;
                if (rowNum == 1) { // Skip header row
                    logArea.appendText("Skipping header row (Row 1).\n");
                    continue;
                }

                Cell categoryCell = row.getCell(COL_CATEGORY);
                Cell nameCell = row.getCell(COL_ITEM_NAME);
                Cell priceCell = row.getCell(COL_PRICE);
                Cell imageCell = row.getCell(COL_IMAGE_FILENAME);

                // Basic validation: check if essential cells are present and not blank
                if (isCellEmpty(categoryCell) || isCellEmpty(nameCell) || isCellEmpty(priceCell)) {
                    logArea.appendText("WARNING: Row " + rowNum + ": Skipping due to missing Category, Name, or Price.\n");
                    itemsSkipped++;
                    continue;
                }

                String category = getCellStringValue(categoryCell).trim();
                String itemName = getCellStringValue(nameCell).trim();
                String imageName = isCellEmpty(imageCell) ? "" : getCellStringValue(imageCell).trim(); // Image can be optional
                int price = 0;

                try {
                    if (priceCell.getCellType() == CellType.NUMERIC) {
                        price = (int) priceCell.getNumericCellValue();
                    } else if (priceCell.getCellType() == CellType.STRING) {
                        price = Integer.parseInt(priceCell.getStringCellValue().trim());
                    } else {
                        logArea.appendText("WARNING: Row " + rowNum + " ("+itemName+"): Invalid price format. Skipping item.\n");
                        itemsSkipped++;
                        continue;
                    }
                    if (price <= 0) {
                        logArea.appendText("WARNING: Row " + rowNum + " ("+itemName+"): Price must be positive. Skipping item.\n");
                        itemsSkipped++;
                        continue;
                    }
                } catch (NumberFormatException e) {
                    logArea.appendText("WARNING: Row " + rowNum + " ("+itemName+"): Could not parse price. Skipping item. Error: " + e.getMessage() + "\n");
                    itemsSkipped++;
                    continue;
                }

                if (imageName.isEmpty()){
                    logArea.appendText("INFO: Row " + rowNum + " ("+itemName+"): No image filename provided.\n");
                }


                importedItems.add(new ManageInventoryView.DisplayMenuItem(category, itemName, imageName, price));
                logArea.appendText("Read Row " + rowNum + ": " + category + ", " + itemName + ", " + price + ", " + imageName + "\n");
            }

            if (importedItems.isEmpty() && rowNum > 1) {
                logArea.appendText("No valid items found in the Excel file to import (after skipping header).\n");
                showAlert(Alert.AlertType.INFORMATION, "Import Complete", "No valid items were found in the Excel file to import.");
                return;
            } else if (importedItems.isEmpty()) {
                logArea.appendText("Excel file seems empty or only contains a header.\n");
                showAlert(Alert.AlertType.INFORMATION, "Import Complete", "Excel file seems empty or only contains a header.");
                return;
            }


            // Now update the JSON file
            logArea.appendText("\nUpdating menu_items.json...\n");
            UpdateResult result = batchUpdateItemsInJson(importedItems);
            itemsAdded = result.itemsAdded;
            itemsUpdated = result.itemsUpdated;

            logArea.appendText("JSON update complete. Added: " + itemsAdded + ", Updated: " + itemsUpdated + ".\n");

            // Refresh application views
            ManageInventoryView.loadInventoryData();
            NewOrderView.loadMenuItemsFromJson();
            NewOrderView.refreshMenuView();
            logArea.appendText("Application views refreshed.\n");

            showAlert(Alert.AlertType.INFORMATION, "Import Successful",
                    "Excel import complete!\n\n" +
                            "Items Read from Excel: " + importedItems.size() + "\n" +
                            "Items Added to JSON: " + itemsAdded + "\n" +
                            "Items Updated in JSON: " + itemsUpdated + "\n" +
                            "Items Skipped (due to errors): " + itemsSkipped);

        } catch (IOException e) {
            logArea.appendText("ERROR: Could not read Excel file: " + e.getMessage() + "\n");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Import Error", "Could not read Excel file: " + e.getMessage());
        } catch (Exception e) { // Catch any other unexpected errors
            logArea.appendText("UNEXPECTED ERROR during import: " + e.getMessage() + "\n");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Import Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Handle numeric cells that might represent numbers or text-like numbers
                DataFormatter formatter = new DataFormatter();
                return formatter.formatCellValue(cell); // This handles formatting well
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // For formulas, try to evaluate and get the result as string
                try {
                    return cell.getStringCellValue(); // If formula result is string
                } catch (IllegalStateException e) {
                    DataFormatter formulaFormatter = new DataFormatter();
                    return formulaFormatter.formatCellValue(cell, new FormulaEvaluator() {
                        @Override public void clearAllCachedResultValues() {}
                        @Override public void notifySetFormula(Cell c) {}
                        @Override public void notifyDeleteCell(Cell c) {}
                        @Override public void notifyUpdateCell(Cell c) {}

                        @Override
                        public void evaluateAll() {

                        }

                        @Override public CellValue evaluate(Cell c) { return null; } // Dummy

                        @Override
                        public CellType evaluateFormulaCell(Cell cell) {
                            return null;
                        }

                        @Override public Cell evaluateInCell(Cell c) { return null; } // Dummy
                        @Override public void setupReferencedWorkbooks(java.util.Map<String, FormulaEvaluator> workbooks) {}
                        @Override public void setDebugEvaluationOutputForNextEval(boolean value) {}
                        @Override public void setIgnoreMissingWorkbooks(boolean ignore) {}
                    });
                }
            default:
                return "";
        }
    }

    private static boolean isCellEmpty(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        return cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty();
    }


    private static class UpdateResult {
        int itemsAdded = 0;
        int itemsUpdated = 0;
    }

    private static UpdateResult batchUpdateItemsInJson(List<ManageInventoryView.DisplayMenuItem> itemsToImport) {
        UpdateResult result = new UpdateResult();
        String menuJsonPathStr = ConfigManager.getMenuItemsJsonPath();
        File menuFile = new File(menuJsonPathStr);
        JSONObject rootJson;

        try {
            Path menuPathObj = Paths.get(menuJsonPathStr);
            if (menuPathObj.getParent() != null && !Files.exists(menuPathObj.getParent())) {
                Files.createDirectories(menuPathObj.getParent());
                logArea.appendText("Created parent directory for JSON: " + menuPathObj.getParent() + "\n");
            }

            if (menuFile.exists() && menuFile.length() > 0) {
                String content = new String(Files.readAllBytes(menuFile.toPath()), StandardCharsets.UTF_8);
                rootJson = new JSONObject(content);
            } else {
                rootJson = new JSONObject();
                rootJson.put("categories", new JSONArray());
                logArea.appendText("menu_items.json not found or empty. Created new structure.\n");
            }

            JSONArray categoriesArray = rootJson.getJSONArray("categories");

            for (ManageInventoryView.DisplayMenuItem itemToImport : itemsToImport) {
                String categoryName = itemToImport.getCategory();
                String itemName = itemToImport.getName();
                int price = itemToImport.getPrice();
                String imageName = itemToImport.getImageName();

                JSONObject targetCategory = null;
                for (int i = 0; i < categoriesArray.length(); i++) {
                    JSONObject cat = categoriesArray.getJSONObject(i);
                    if (cat.getString("name").equalsIgnoreCase(categoryName)) {
                        targetCategory = cat;
                        break;
                    }
                }

                if (targetCategory == null) { // Category doesn't exist, create it
                    targetCategory = new JSONObject();
                    targetCategory.put("name", categoryName);
                    targetCategory.put("items", new JSONArray());
                    categoriesArray.put(targetCategory);
                    logArea.appendText("Created new category: " + categoryName + "\n");
                }

                JSONArray itemsArray = targetCategory.getJSONArray("items");
                boolean itemFoundAndUpdated = false;
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject existingItem = itemsArray.getJSONObject(i);
                    if (existingItem.getString("name").equalsIgnoreCase(itemName)) {
                        // Item exists, update it
                        existingItem.put("price", price);
                        existingItem.put("imageName", imageName); // Update image name as well
                        itemFoundAndUpdated = true;
                        result.itemsUpdated++;
                        logArea.appendText("Updated item: " + itemName + " in category " + categoryName + "\n");
                        break;
                    }
                }

                if (!itemFoundAndUpdated) { // Item does not exist, add it
                    JSONObject newItem = new JSONObject();
                    newItem.put("name", itemName);
                    newItem.put("imageName", imageName);
                    newItem.put("price", price);
                    itemsArray.put(newItem);
                    result.itemsAdded++;
                    logArea.appendText("Added new item: " + itemName + " to category " + categoryName + "\n");
                }
            }

            // Write the updated JSON back to file
            try (FileWriter writer = new FileWriter(menuFile)) {
                writer.write(rootJson.toString(4)); // 4 for pretty print
                logArea.appendText("Successfully wrote updates to " + menuJsonPathStr + "\n");
            }

        } catch (IOException e) {
            logArea.appendText("ERROR writing to JSON file '" + menuJsonPathStr + "': " + e.getMessage() + "\n");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "JSON Write Error", "Could not write to menu_items.json: " + e.getMessage());
        } catch (JSONException e) {
            logArea.appendText("ERROR processing JSON data: " + e.getMessage() + "\n");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "JSON Processing Error", "Error with JSON structure: " + e.getMessage());
        }
        return result;
    }


    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(dialogStage);
            alert.showAndWait();
        });
    }
}
