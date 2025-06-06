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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class OrderExcelUtil {

    private static final String[] HEADERS = {"Order ID", "Item Name", "Quantity", "Unit Price", "Created Date", "Created Time", "Total Item Price", "Daily Incremental Total"};
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private static Path getOrdersFilePath() {
        return Paths.get(ConfigManager.getOrdersExcelPath());
    }

    public static synchronized void saveOrderToExcel(Order order) {
        Path filePath = getOrdersFilePath();
        File file = filePath.toFile();
        Workbook workbook;
        Sheet sheet;

        double currentOrderTotal = order.getOrderTotalAmount();
        double dailyIncrementalTotalForThisOrder = currentOrderTotal;

        try {
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }

            if (file.exists() && file.length() > 0) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    workbook = WorkbookFactory.create(fis);
                }
                sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    sheet = workbook.createSheet("Orders");
                    createHeaderRow(sheet, workbook);
                }

                LocalDate today = order.getCreatedDateTime().toLocalDate();
                for (int i = sheet.getLastRowNum(); i >= 1; i--) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    Cell dateCell = row.getCell(4);
                    if (dateCell != null && dateCell.getCellType() == CellType.STRING) {
                        try {
                            LocalDate rowDate = LocalDate.parse(dateCell.getStringCellValue(), DATE_FORMATTER);
                            if (rowDate.isEqual(today)) {
                                Cell incrementalTotalCell = row.getCell(7);
                                if (incrementalTotalCell != null && incrementalTotalCell.getCellType() == CellType.NUMERIC) {
                                    dailyIncrementalTotalForThisOrder += incrementalTotalCell.getNumericCellValue();
                                }
                                break;
                            } else if (rowDate.isBefore(today)) {
                                break;
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing date from Excel row " + i + " for incremental total: " + e.getMessage());
                        }
                    }
                }

            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Orders");
                createHeaderRow(sheet, workbook);
            }

            int lastRowNum = sheet.getLastRowNum();
            if (sheet.getPhysicalNumberOfRows() <= 1) {
                lastRowNum = 0;
            }


            for (OrderItem item : order.getOrderItems()) {
                Row dataRow = sheet.createRow(++lastRowNum);
                dataRow.createCell(0).setCellValue(order.getOrderId());
                dataRow.createCell(1).setCellValue(item.getItemName());
                dataRow.createCell(2).setCellValue(item.getQuantity());
                dataRow.createCell(3).setCellValue(item.getUnitPrice());
                dataRow.createCell(4).setCellValue(order.getCreatedDateTime().toLocalDate().format(DATE_FORMATTER));
                dataRow.createCell(5).setCellValue(order.getCreatedDateTime().toLocalTime().format(TIME_FORMATTER));
                dataRow.createCell(6).setCellValue(item.getTotalItemPrice());
                dataRow.createCell(7).setCellValue(dailyIncrementalTotalForThisOrder);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            workbook.close();
            System.out.println("Order " + order.getOrderId() + " saved to Excel.");

        } catch (Exception e) {
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

    public static List<Order> loadOrdersFromExcel() {
        List<Order> orders = new ArrayList<>();
        Path filePath = getOrdersFilePath();

        // --- FIX: Check if the file exists AND is not empty before trying to read ---
        try {
            if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                System.out.println("Orders file does not exist or is empty. Nothing to load.");
                return orders; // Return empty list
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "File System Error", "Could not check the size of the orders file.");
            return orders;
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile()); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return orders;

            Map<String, List<OrderItem>> orderItemsMap = new LinkedHashMap<>();
            Map<String, LocalDateTime> orderCreationTimeMap = new HashMap<>();
            DataFormatter dataFormatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String orderId = dataFormatter.formatCellValue(row.getCell(0)).trim();
                if (orderId.isEmpty()) continue;

                String itemName = dataFormatter.formatCellValue(row.getCell(1));
                int quantity = (int) Double.parseDouble(dataFormatter.formatCellValue(row.getCell(2)));
                double unitPrice = Double.parseDouble(dataFormatter.formatCellValue(row.getCell(3)));

                OrderItem orderItem = new OrderItem(itemName, quantity, unitPrice);
                orderItemsMap.computeIfAbsent(orderId, k -> new ArrayList<>()).add(orderItem);

                if (!orderCreationTimeMap.containsKey(orderId)) {
                    String dateStr = dataFormatter.formatCellValue(row.getCell(4));
                    String timeStr = dataFormatter.formatCellValue(row.getCell(5));
                    try {
                        LocalDateTime createdDateTime = LocalDateTime.parse(dateStr + " " + timeStr, DATETIME_FORMATTER_DISPLAY);
                        orderCreationTimeMap.put(orderId, createdDateTime);
                    } catch (Exception e) {
                        orderCreationTimeMap.put(orderId, LocalDateTime.MIN);
                    }
                }
            }

            for (Map.Entry<String, List<OrderItem>> entry : orderItemsMap.entrySet()) {
                String orderId = entry.getKey();
                orders.add(new Order(orderId, orderCreationTimeMap.getOrDefault(orderId, LocalDateTime.MIN), entry.getValue()));
            }

            orders.sort((o1, o2) -> o2.getCreatedDateTime().compareTo(o1.getCreatedDateTime()));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Excel Load Error", "Could not load orders from Excel: " + e.getMessage());
        }
        return orders;
    }

    public static synchronized boolean deleteOrderFromExcel(String orderIdToDelete, LocalDate orderDateToDelete) {
        Path filePath = getOrdersFilePath();
        if (!Files.exists(filePath)) return false;

        List<Order> allOrders = loadOrdersFromExcel();
        List<Order> ordersToKeep = allOrders.stream()
                .filter(order -> !order.getOrderId().equals(orderIdToDelete))
                .collect(Collectors.toList());

        if (allOrders.size() == ordersToKeep.size()) {
            return false;
        }

        try {
            Files.deleteIfExists(filePath);

            // Re-save the remaining orders. This will naturally recalculate totals.
            // Sorting by date before saving ensures incremental totals are correct.
            ordersToKeep.sort((o1, o2) -> o1.getCreatedDateTime().compareTo(o2.getCreatedDateTime()));
            for (Order order : ordersToKeep) {
                saveOrderToExcel(order);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Excel Write Error", "Failed to rewrite orders file after deletion.");
            return false;
        }
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
