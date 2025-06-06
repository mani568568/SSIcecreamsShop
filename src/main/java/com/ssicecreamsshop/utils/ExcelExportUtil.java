package com.ssicecreamsshop.utils;

import com.ssicecreamsshop.ViewOrdersView;
import com.ssicecreamsshop.config.ConfigManager;
import com.ssicecreamsshop.model.Order;
import com.ssicecreamsshop.model.OrderItem;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;


public class ExcelExportUtil {

    // --- Navy & Yellow Theme Colors for Alerts ---
    private static final String PRIMARY_NAVY = "#1A237E";
    private static final String PRIMARY_NAVY_DARK = "#283593";
    private static final String BUTTON_ACTION_RED = "#F44336";
    private static final String BUTTON_ACTION_RED_HOVER = "#D32F2F";
    private static final String TEXT_ON_DARK = "white";
    private static final String BACKGROUND_MAIN = "#E8EAF6";

    private static final String[] MENU_HEADERS = {"Category", "Item Name", "Price", "Image Filename", "Quantity"};

    // This method remains for exporting the menu from ManageInventoryView
    public static void exportToExcel(Stage ownerStage) {
        String menuJsonPathString = ConfigManager.getMenuItemsJsonPath();
        Path menuJsonPath = Paths.get(menuJsonPathString);

        if (!Files.exists(menuJsonPath)) {
            showAlert(Alert.AlertType.ERROR, "Export Error", "Menu items file (menu_items.json) not found.", ownerStage);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Menu Export As");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileChooser.setInitialFileName("menu_export_" + timestamp + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));

        File file = fileChooser.showSaveDialog(ownerStage);
        if (file == null) {
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Menu Items");
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < MENU_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(MENU_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            try (InputStream inputStream = Files.newInputStream(menuJsonPath)) {
                String jsonText = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));
                JSONObject rootJson = new JSONObject(jsonText);
                JSONArray categoriesArray = rootJson.optJSONArray("categories");

                if (categoriesArray != null) {
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
                            dataRow.createCell(3).setCellValue(itemObj.optString("imageName", ""));
                            if (itemObj.has("quantity")) {
                                dataRow.createCell(4).setCellValue(itemObj.getInt("quantity"));
                            } else {
                                dataRow.createCell(4).setCellValue("Unlimited");
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < MENU_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Menu items successfully exported.", ownerStage);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to export menu items: " + e.getMessage(), ownerStage);
        }
    }

    // --- UPDATED METHOD TO EXPORT ORDERS ---
    public static void exportOrdersToExcel(List<ViewOrdersView.DisplayableOrder> ordersToExport, Stage ownerStage) {
        if (ordersToExport == null || ordersToExport.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Export Info", "There are no orders in the current view to export.", ownerStage);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Orders Report As");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileChooser.setInitialFileName("orders_report_" + timestamp + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));

        File file = fileChooser.showSaveDialog(ownerStage);
        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Orders Report");

            String[] headers = {"Order ID", "Created Date & Time", "Item Name", "Quantity", "Unit Price (₹)", "Total Item Price (₹)", "Complete Order Total (₹)"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (ViewOrdersView.DisplayableOrder orderView : ordersToExport) {
                Order order = orderView.getOriginalOrder(); // Get the underlying Order object
                for (OrderItem item : order.getOrderItems()) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(order.getOrderId());
                    row.createCell(1).setCellValue(order.getCreatedDateTime().format(formatter));
                    row.createCell(2).setCellValue(item.getItemName());
                    row.createCell(3).setCellValue(item.getQuantity());
                    row.createCell(4).setCellValue(item.getUnitPrice());
                    row.createCell(5).setCellValue(item.getTotalItemPrice());
                    row.createCell(6).setCellValue(order.getOrderTotalAmount());
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Orders report successfully exported to:\n" + file.getAbsolutePath(), ownerStage);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to create or write to the Excel file:\n" + e.getMessage(), ownerStage);
        }
    }

    // --- NEW METHOD TO IMPORT ORDERS ---
    public static void importOrdersFromExcel(Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Orders Excel File to Import");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));
        File fileToImport = fileChooser.showOpenDialog(ownerStage);

        if (fileToImport == null) {
            return; // User cancelled
        }

        try {
            List<Order> existingOrders = OrderExcelUtil.loadOrdersFromExcel();
            Set<String> existingOrderIds = existingOrders.stream().map(Order::getOrderId).collect(Collectors.toSet());

            List<Order> newOrdersToImport = parseOrdersFromImportFile(fileToImport, existingOrderIds);

            if (newOrdersToImport.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Import Complete", "No new, unique orders were found in the selected file to import.", ownerStage);
                return;
            }

            for (Order order : newOrdersToImport) {
                OrderExcelUtil.saveOrderToExcel(order); // This method correctly calculates incremental totals
            }

            int skippedCount = countTotalOrdersInFile(fileToImport) - newOrdersToImport.size();

            showAlert(Alert.AlertType.INFORMATION, "Import Successful",
                    "Successfully imported " + newOrdersToImport.size() + " new orders.\n" +
                            "Skipped " + skippedCount + " orders that already existed.",
                    ownerStage);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Import Error", "An error occurred during the import process:\n" + e.getMessage(), ownerStage);
        }
    }

    private static List<Order> parseOrdersFromImportFile(File file, Set<String> existingIds) throws IOException {
        Map<String, List<OrderItem>> orderItemsMap = new LinkedHashMap<>();
        Map<String, LocalDateTime> orderCreationTimeMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (InputStream fis = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String orderId = getCellStringValue(row.getCell(0));
                if (orderId.isEmpty() || existingIds.contains(orderId)) {
                    continue; // Skip empty or duplicate orders
                }

                LocalDateTime createdDateTime = LocalDateTime.parse(getCellStringValue(row.getCell(1)), formatter);
                String itemName = getCellStringValue(row.getCell(2));
                int quantity = (int) Double.parseDouble(getCellStringValue(row.getCell(3)));
                double unitPrice = Double.parseDouble(getCellStringValue(row.getCell(4)));

                orderCreationTimeMap.putIfAbsent(orderId, createdDateTime);
                orderItemsMap.computeIfAbsent(orderId, k -> new ArrayList<>()).add(new OrderItem(itemName, quantity, unitPrice));
            }
        }

        List<Order> newOrders = new ArrayList<>();
        for (Map.Entry<String, List<OrderItem>> entry : orderItemsMap.entrySet()) {
            newOrders.add(new Order(entry.getKey(), orderCreationTimeMap.get(entry.getKey()), entry.getValue()));
        }

        newOrders.sort((o1, o2) -> o1.getCreatedDateTime().compareTo(o2.getCreatedDateTime())); // Sort chronologically before saving
        return newOrders;
    }

    private static int countTotalOrdersInFile(File file) {
        Set<String> orderIds = new java.util.HashSet<>();
        try (InputStream fis = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String orderId = getCellStringValue(row.getCell(0));
                if (!orderId.isEmpty()) orderIds.add(orderId);
            }
        } catch (Exception e) { /* ignore */ }
        return orderIds.size();
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }


    private static void showAlert(Alert.AlertType alertType, String title, String message, Stage owner) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-background-color: " + BACKGROUND_MAIN +";");

            Button button = (Button) dialogPane.lookupButton(alert.getButtonTypes().get(0));
            if (button != null) {
                String buttonBaseColor = alertType == Alert.AlertType.ERROR || alertType == Alert.AlertType.WARNING ? BUTTON_ACTION_RED : PRIMARY_NAVY;
                String buttonHoverColor = alertType == Alert.AlertType.ERROR || alertType == Alert.AlertType.WARNING ? BUTTON_ACTION_RED_HOVER : PRIMARY_NAVY_DARK;
                String btnStyle = "-fx-text-fill: " + TEXT_ON_DARK + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;";
                button.setStyle(btnStyle + "-fx-background-color: " + buttonBaseColor + ";");
                button.setOnMouseEntered(e -> button.setStyle(btnStyle + "-fx-background-color: " + buttonHoverColor + ";"));
                button.setOnMouseExited(e -> button.setStyle(btnStyle + "-fx-background-color: " + buttonBaseColor + ";"));
            }

            if (owner != null && owner.isShowing()) {
                alert.initOwner(owner);
            }
            alert.showAndWait();
        });
    }
}
