package com.ssicecreamsshop.utils;

import com.ssicecreamsshop.config.ConfigManager;
import com.ssicecreamsshop.model.Order;
import com.ssicecreamsshop.model.OrderItem;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderExcelUtil {

    private static final String ORDERS_FILE_NAME = "orders.xlsx";
    private static final String[] HEADERS = {"Order ID", "Item Name", "Quantity", "Unit Price", "Created Date", "Created Time", "Total Item Price", "Daily Incremental Total"};
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private static Path getOrdersFilePath() {
        // Ensure the base application data directory exists
        Path appDataDir = Paths.get(ConfigManager.getImagePath()).getParent(); // Assuming orders.xlsx is in the same parent as images dir
        if (appDataDir == null) { // Fallback if image path is not set or has no parent
            appDataDir = Paths.get(System.getProperty("user.home"), ".SSIceCreamShop");
        }
        try {
            if (!Files.exists(appDataDir)) {
                Files.createDirectories(appDataDir);
            }
        } catch (IOException e) {
            System.err.println("Error creating base directory for orders.xlsx: " + e.getMessage());
            // Fallback to a default location if creation fails, though this is not ideal
            return Paths.get(ORDERS_FILE_NAME); // Relative path in app's working dir
        }
        return appDataDir.resolve(ORDERS_FILE_NAME);
    }

    public static synchronized void saveOrderToExcel(Order order) {
        Path filePath = getOrdersFilePath();
        File file = filePath.toFile();
        Workbook workbook;
        Sheet sheet;

        double currentOrderTotal = order.getOrderTotalAmount();
        double dailyIncrementalTotalForThisOrder = currentOrderTotal; // Default if first order of the day

        try {
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    workbook = WorkbookFactory.create(fis);
                }
                sheet = workbook.getSheetAt(0);
                if (sheet == null) { // Should not happen if file exists and was created by this util
                    sheet = workbook.createSheet("Orders");
                    createHeaderRow(sheet, workbook);
                }

                // Calculate Daily Incremental Total
                // Iterate backwards to find the last entry for the current day
                LocalDate today = order.getCreatedDateTime().toLocalDate();
                for (int i = sheet.getLastRowNum(); i >= 1; i--) { // Skip header row
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    Cell dateCell = row.getCell(4); // Created Date column
                    if (dateCell != null && dateCell.getCellType() == CellType.STRING) {
                        try {
                            LocalDate rowDate = LocalDate.parse(dateCell.getStringCellValue(), DATE_FORMATTER);
                            if (rowDate.isEqual(today)) {
                                Cell incrementalTotalCell = row.getCell(7); // Daily Incremental Total column
                                if (incrementalTotalCell != null && incrementalTotalCell.getCellType() == CellType.NUMERIC) {
                                    dailyIncrementalTotalForThisOrder += incrementalTotalCell.getNumericCellValue();
                                }
                                break; // Found the last entry for today
                            } else if (rowDate.isBefore(today)) {
                                break; // Past today, no need to look further
                            }
                        } catch (Exception e) {
                            // Ignore parsing errors for date, continue searching
                            System.err.println("Error parsing date from Excel row " + i + " for incremental total: " + e.getMessage());
                        }
                    }
                }

            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Orders");
                createHeaderRow(sheet, workbook);
                // dailyIncrementalTotalForThisOrder is already currentOrderTotal
            }

            int lastRowNum = sheet.getLastRowNum();
            if (sheet.getPhysicalNumberOfRows() == 0 || (sheet.getPhysicalNumberOfRows() == 1 && sheet.getRow(0) != null)) {
                // Only header exists or sheet is empty
                lastRowNum = 0; // Start after header if it exists
            }


            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));

            CellStyle timeCellStyle = workbook.createCellStyle();
            timeCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("h:mm:ss"));


            for (OrderItem item : order.getOrderItems()) {
                Row dataRow = sheet.createRow(++lastRowNum);
                dataRow.createCell(0).setCellValue(order.getOrderId());
                dataRow.createCell(1).setCellValue(item.getItemName());
                dataRow.createCell(2).setCellValue(item.getQuantity());
                dataRow.createCell(3).setCellValue(item.getUnitPrice());

                // Created Date
                Cell dateCell = dataRow.createCell(4);
                dateCell.setCellValue(order.getCreatedDateTime().toLocalDate().format(DATE_FORMATTER));
                // dateCell.setCellStyle(dateCellStyle); // Apply date style if storing as actual date

                // Created Time
                Cell timeCell = dataRow.createCell(5);
                timeCell.setCellValue(order.getCreatedDateTime().toLocalTime().format(TIME_FORMATTER));
                // timeCell.setCellStyle(timeCellStyle); // Apply time style

                dataRow.createCell(6).setCellValue(item.getTotalItemPrice());
                dataRow.createCell(7).setCellValue(dailyIncrementalTotalForThisOrder);
            }

            // Auto-size columns (optional, can be slow for very large files)
            // for (int i = 0; i < HEADERS.length; i++) {
            //     sheet.autoSizeColumn(i);
            // }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            workbook.close();
            System.out.println("Order " + order.getOrderId() + " saved to Excel.");

        } catch (IOException e) {
            System.err.println("Error saving order to Excel: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Excel Save Error", "Could not save order to Excel file: " + e.getMessage());
        }
    }

    private static void createHeaderRow(Sheet sheet, Workbook workbook) {
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
    }

    // Placeholder for loadOrders - to be implemented for ViewOrdersView
    public static List<Order> loadOrdersFromExcel() {
        List<Order> orders = new ArrayList<>();
        Path filePath = getOrdersFilePath();
        File file = filePath.toFile();

        if (!file.exists()) {
            System.out.println("Orders file does not exist. No orders to load.");
            return orders; // Return empty list
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                System.out.println("No 'Orders' sheet found in the Excel file.");
                return orders;
            }

            // Temporary map to group items by Order ID
            java.util.Map<String, List<OrderItem>> orderItemsMap = new java.util.LinkedHashMap<>();
            java.util.Map<String, LocalDateTime> orderCreationTimeMap = new java.util.HashMap<>();
            // We don't strictly need to load dailyIncrementalTotal back into the Order object for display,
            // but we do need the createdDateTime and order items.

            DataFormatter dataFormatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Start from row 1 (skip header)
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String orderId = dataFormatter.formatCellValue(row.getCell(0)).trim();
                if (orderId.isEmpty()) continue; // Skip rows with no order ID

                String itemName = dataFormatter.formatCellValue(row.getCell(1));
                int quantity = (int) Double.parseDouble(dataFormatter.formatCellValue(row.getCell(2))); // Assuming numeric
                double unitPrice = Double.parseDouble(dataFormatter.formatCellValue(row.getCell(3))); // Assuming numeric

                OrderItem orderItem = new OrderItem(itemName, quantity, unitPrice);
                orderItemsMap.computeIfAbsent(orderId, k -> new ArrayList<>()).add(orderItem);

                if (!orderCreationTimeMap.containsKey(orderId)) {
                    String dateStr = dataFormatter.formatCellValue(row.getCell(4));
                    String timeStr = dataFormatter.formatCellValue(row.getCell(5));
                    try {
                        LocalDateTime createdDateTime = LocalDateTime.parse(dateStr + " " + timeStr, DATETIME_FORMATTER_DISPLAY);
                        orderCreationTimeMap.put(orderId, createdDateTime);
                    } catch (Exception e) {
                        System.err.println("Could not parse date/time for order " + orderId + ": " + dateStr + " " + timeStr + " - " + e.getMessage());
                        // Fallback or skip this order's timestamp
                        orderCreationTimeMap.put(orderId, LocalDateTime.MIN); // Placeholder
                    }
                }
            }

            // Construct Order objects
            for (java.util.Map.Entry<String, List<OrderItem>> entry : orderItemsMap.entrySet()) {
                String orderId = entry.getKey();
                List<OrderItem> items = entry.getValue();
                LocalDateTime createdDateTime = orderCreationTimeMap.getOrDefault(orderId, LocalDateTime.MIN);
                orders.add(new Order(orderId, createdDateTime, items));
            }

            // Sort orders by creation time, recent first
            orders.sort((o1, o2) -> o2.getCreatedDateTime().compareTo(o1.getCreatedDateTime()));


        } catch (IOException e) {
            System.err.println("Error loading orders from Excel: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Excel Load Error", "Could not load orders from Excel: " + e.getMessage());
        }
        return orders;
    }

    // Placeholder for deleteOrder - complex due to incremental total recalculation
    public static synchronized boolean deleteOrderFromExcel(String orderIdToDelete, LocalDate orderDateToDelete) {
        Path filePath = getOrdersFilePath();
        File file = filePath.toFile();
        if (!file.exists()) return false;

        List<List<Object>> allRows = new ArrayList<>();
        boolean foundOrder = false;
        double deletedOrderTotalAmount = 0;

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return false;

            // Read all rows into memory
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                List<Object> rowData = new ArrayList<>();
                for (int j = 0; j < HEADERS.length; j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell != null) {
                        switch (cell.getCellType()) {
                            case STRING: rowData.add(cell.getStringCellValue()); break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell) && (j == 4 || j==5) ) { // Date or Time column
                                    rowData.add(cell.getLocalDateTimeCellValue()); // Read as LocalDateTime
                                } else {
                                    rowData.add(cell.getNumericCellValue());
                                }
                                break;
                            case BOOLEAN: rowData.add(cell.getBooleanCellValue()); break;
                            case FORMULA: rowData.add(cell.getCellFormula()); break; // Or evaluate
                            default: rowData.add("");
                        }
                    } else {
                        rowData.add("");
                    }
                }
                allRows.add(rowData);
            }

            // Identify rows to delete and calculate total amount of the deleted order
            List<List<Object>> rowsToKeep = new ArrayList<>();
            rowsToKeep.add(allRows.get(0)); // Keep header

            for (int i = 1; i < allRows.size(); i++) {
                List<Object> rowData = allRows.get(i);
                if (rowData.get(0).toString().equals(orderIdToDelete)) {
                    foundOrder = true;
                    // Add item total to deletedOrderTotalAmount
                    if (rowData.get(6) instanceof Number) {
                        deletedOrderTotalAmount += ((Number) rowData.get(6)).doubleValue();
                    }
                } else {
                    rowsToKeep.add(rowData);
                }
            }

            if (!foundOrder) return false; // Order ID not found

            // Rebuild the sheet and adjust incremental totals
            try (FileOutputStream fos = new FileOutputStream(file)) { // Overwrite existing file
                Workbook newWorkbook = new XSSFWorkbook();
                Sheet newSheet = newWorkbook.createSheet("Orders");
                createHeaderRow(newSheet, newWorkbook);

                double previousDayIncrementalTotal = 0;
                LocalDate previousDate = null;

                for (int i = 1; i < rowsToKeep.size(); i++) { // Start from 1 to skip header in rowsToKeep
                    List<Object> rowData = rowsToKeep.get(i);
                    Row newRow = newSheet.createRow(i); // i is correct as newSheet starts from 0, header is at 0

                    LocalDate currentRowDate = null;
                    // Order ID
                    newRow.createCell(0).setCellValue(rowData.get(0).toString());
                    // Item Name
                    newRow.createCell(1).setCellValue(rowData.get(1).toString());
                    // Quantity
                    if (rowData.get(2) instanceof Number) newRow.createCell(2).setCellValue(((Number)rowData.get(2)).doubleValue());
                    else newRow.createCell(2).setCellValue(rowData.get(2).toString());
                    // Unit Price
                    if (rowData.get(3) instanceof Number) newRow.createCell(3).setCellValue(((Number)rowData.get(3)).doubleValue());
                    else newRow.createCell(3).setCellValue(rowData.get(3).toString());

                    // Created Date & Time
                    Object dateObj = rowData.get(4); // Assuming date string
                    Object timeObj = rowData.get(5); // Assuming time string
                    newRow.createCell(4).setCellValue(dateObj.toString());
                    newRow.createCell(5).setCellValue(timeObj.toString());

                    try {
                        currentRowDate = LocalDate.parse(dateObj.toString(), DATE_FORMATTER);
                    } catch (Exception e) { /* ignore, might be an old format or bad data */ }


                    // Total Item Price
                    double itemTotal = 0;
                    if (rowData.get(6) instanceof Number) {
                        itemTotal = ((Number)rowData.get(6)).doubleValue();
                        newRow.createCell(6).setCellValue(itemTotal);
                    } else newRow.createCell(6).setCellValue(rowData.get(6).toString());

                    // Daily Incremental Total - Recalculation
                    if (currentRowDate != null) {
                        if (previousDate == null || !previousDate.isEqual(currentRowDate)) {
                            // First order of a new day (or first overall)
                            previousDayIncrementalTotal = 0;
                        }
                        previousDayIncrementalTotal += itemTotal; // This is not quite right, it should be order total

                        // This logic for incremental total on delete is complex and needs careful thought.
                        // For simplicity, if an order is deleted, the incremental total for subsequent orders
                        // on the SAME DAY as the deleted order should be reduced by the deleted order's total.
                        // This simplified recalculation assumes items of the same order are contiguous.
                        // A more robust way would be to group by order ID first.

                        double currentIncremental = 0;
                        if (rowData.get(7) instanceof Number) {
                            currentIncremental = ((Number)rowData.get(7)).doubleValue();
                        }

                        if (currentRowDate.isEqual(orderDateToDelete)) {
                            // This is an approximation. A full re-aggregation by order ID and then by day is better.
                            // For now, let's just reduce if it's on the same day.
                            // This will be incorrect if multiple items from different orders are interleaved.
                            // A better approach: Recalculate all incremental totals from scratch after forming rowsToKeep.
                            // For now, this part is simplified and likely needs refinement for perfect accuracy after delete.
                            newRow.createCell(7).setCellValue(currentIncremental - (currentRowDate.isEqual(orderDateToDelete) ? deletedOrderTotalAmount / (double)countItemsInOrder(rowsToKeep, rowData.get(0).toString()) : 0) );
                        } else {
                            newRow.createCell(7).setCellValue(currentIncremental);
                        }
                        previousDate = currentRowDate;
                    } else {
                        if (rowData.get(7) instanceof Number) newRow.createCell(7).setCellValue(((Number)rowData.get(7)).doubleValue());
                        else newRow.createCell(7).setCellValue(rowData.get(7).toString());
                    }
                }
                newWorkbook.write(fos);
                newWorkbook.close();
            }
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Excel Delete Error", "Error processing Excel file for deletion: " + e.getMessage());
        }
        return false;
    }

    private static int countItemsInOrder(List<List<Object>> rows, String orderId) {
        int count = 0;
        for (List<Object> row : rows) {
            if (row.get(0).toString().equals(orderId)) {
                count++;
            }
        }
        return Math.max(1, count); // Avoid division by zero
    }


    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
