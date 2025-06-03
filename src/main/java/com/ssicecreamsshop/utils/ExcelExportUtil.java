package com.ssicecreamsshop.utils;

import com.ssicecreamsshop.config.ConfigManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;


public class ExcelExportUtil {

    private static final String[] HEADERS = {"Category", "Item Name", "Price", "Image Filename"};

    public static void exportToExcel(Stage ownerStage) {
        String menuJsonPathString = ConfigManager.getMenuItemsJsonPath();
        Path menuJsonPath = Paths.get(menuJsonPathString);

        if (!Files.exists(menuJsonPath)) {
            showAlert(Alert.AlertType.ERROR, "Export Error", "Menu items file (menu_items.json) not found at:\n" + menuJsonPathString + "\nCannot export.", ownerStage);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Menu Export As");
        // Suggest a filename with a timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileChooser.setInitialFileName("menu_export_" + timestamp + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));

        File file = fileChooser.showSaveDialog(ownerStage);
        if (file == null) {
            // User cancelled the save dialog
            return;
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Menu Items");

        // Create Header Row
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Populate Data Rows
        int rowIndex = 1;
        try (InputStream inputStream = Files.newInputStream(menuJsonPath)) {
            String jsonText = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            JSONObject rootJson = new JSONObject(jsonText);
            JSONArray categoriesArray = rootJson.optJSONArray("categories");

            if (categoriesArray == null || categoriesArray.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Export Info", "No categories or items found in menu_items.json to export.", ownerStage);
                workbook.close(); // Close workbook if nothing to write
                return;
            }

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                String categoryName = categoryObj.getString("name");
                JSONArray itemsArray = categoryObj.getJSONArray("items");

                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject itemObj = itemsArray.getJSONObject(j);
                    Row dataRow = sheet.createRow(rowIndex++);

                    dataRow.createCell(0).setCellValue(categoryName);
                    dataRow.createCell(1).setCellValue(itemObj.getString("name"));
                    dataRow.createCell(2).setCellValue(itemObj.getInt("price"));
                    dataRow.createCell(3).setCellValue(itemObj.optString("imageName", "")); // Use optString for optional field
                }
            }

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write the workbook to the selected file
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Menu items successfully exported to:\n" + file.getAbsolutePath(), ownerStage);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Export Error", "Error reading menu_items.json or writing Excel file:\n" + e.getMessage(), ownerStage);
        } catch (JSONException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Export Error", "Error parsing menu_items.json content:\n" + e.getMessage(), ownerStage);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace(); // Log error closing workbook
            }
        }
    }

    private static void showAlert(Alert.AlertType alertType, String title, String message, Stage owner) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (owner != null && owner.isShowing()) {
                alert.initOwner(owner);
            }
            alert.showAndWait();
        });
    }
}
