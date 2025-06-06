package com.ssicecreamsshop.utils;

import com.ssicecreamsshop.ManageInventoryView;
import com.ssicecreamsshop.NewOrderView;
import com.ssicecreamsshop.config.ConfigManager;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TelegramBotService {

    private final String botToken;
    private final String botUsername;
    private AppBot appBotInstance;
    private TelegramBotsApi botsApi;
    private GlobalStatusManager globalStatusManager;

    private final long authorizedUserId = 0; // <<<--- IMPORTANT: REPLACE THIS WITH YOUR TELEGRAM USER ID

    public TelegramBotService(String botToken, String botUsername, GlobalStatusManager statusManager) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.globalStatusManager = statusManager;
    }

    public void startBot() {
        if (globalStatusManager != null) {
            globalStatusManager.telegramBotStarting();
        }
        try {
            appBotInstance = new AppBot();
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(appBotInstance);
            System.out.println("Telegram Bot Service started successfully. Bot username: @" + botUsername);
            if (globalStatusManager != null) {
                globalStatusManager.setTelegramBotStatus(true);
            }
        } catch (TelegramApiException e) {
            System.err.println("Error starting Telegram Bot Service: " + e.getMessage());
            e.printStackTrace();
            if (globalStatusManager != null) {
                globalStatusManager.setTelegramBotStatus(false);
            }
        }
    }

    public void stopBot() {
        System.out.println("Telegram Bot Service stopping...");
        if (globalStatusManager != null) {
            globalStatusManager.setTelegramBotStatus(false);
        }
    }

    private class AppBot extends TelegramLongPollingBot {

        @Override
        public String getBotUsername() {
            return botUsername;
        }

        @Override
        public String getBotToken() {
            return botToken;
        }

        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage()) {
                // --- SECURITY CHECK (Uncomment for production) ---
                // long userId = update.getMessage().getFrom().getId();
                // if (authorizedUserId != 0 && userId != authorizedUserId) {
                //     sendMessage(update.getMessage().getChatId(), "Sorry, you are not authorized to use this bot.");
                //     return;
                // }

                if (update.getMessage().hasText()) {
                    handleTextMessage(update.getMessage());
                } else if (update.getMessage().hasPhoto() && update.getMessage().getCaption() != null) {
                    handlePhotoMessage(update.getMessage());
                }
            }
        }

        private void handleTextMessage(Message message) {
            String messageText = message.getText().trim();
            long chatId = message.getChatId();

            if (messageText.toLowerCase().startsWith("add ")) {
                String[] parts = messageText.split("\\s+", 4);
                if (parts.length == 4) {
                    try {
                        String itemName = parts[1];
                        String category = parts[2];
                        int price = Integer.parseInt(parts[3]);

                        boolean success = addItemToJson(category, itemName, price, "", null);

                        if (success) {
                            sendMessage(chatId, "✅ Item added successfully (without image)!\nName: " + itemName);
                            Platform.runLater(() -> {
                                ManageInventoryView.loadInventoryData();
                                NewOrderView.loadMenuItemsFromJson();
                                NewOrderView.refreshMenuView();
                            });
                        } else {
                            sendMessage(chatId, "❌ Failed to add item. An item with this name might already exist.");
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "Invalid command format. The last word must be a number (price).\nExample: ADD chocolate cone 20");
                    }
                } else {
                    sendMessage(chatId, "Invalid format. To add an item with text, use:\n`ADD <item_name> <category> <price>`");
                }
            } else if (messageText.equalsIgnoreCase("/start") || messageText.equalsIgnoreCase("/help") || messageText.equalsIgnoreCase("help")) {
                String helpText = "Welcome to the *Sri Satyavathi Icecreams Bot*!\n\n"
                        + "*Available Commands:*\n"
                        + "`/menu` or `items` - Lists all available menu items.\n"
                        + "`/todayamt` or `today amt` - Shows total sales for today.\n"
                        + "`/shutdown` or `shut` - Closes the application.\n"
                        + "`/help` - Shows this help message.\n\n"
                        + "*How to Add an Item:*\n"
                        + "1. *Text Only:* `ADD <name> <category> <price>`\n"
                        + "   _Example:_ `ADD ChocoDelight Cones 50`\n\n"
                        + "2. *With Image:* Send a photo with a caption in the same format.";
                sendMessage(chatId, helpText);
            } else if (messageText.equalsIgnoreCase("items") || messageText.equalsIgnoreCase("/items") || messageText.equalsIgnoreCase("/menu")) {
                String itemsList = listAllItems();
                sendMessage(chatId, itemsList);
            } else if (messageText.equalsIgnoreCase("today amt") || messageText.equalsIgnoreCase("/todayamt")) {
                String todaysTotal = calculateTodaysTotal();
                sendMessage(chatId, todaysTotal);
            } else if (messageText.equalsIgnoreCase("shut") || messageText.equalsIgnoreCase("/shutdown")) {
                sendMessage(chatId, "Acknowledged. Shutting down the application...");
                Platform.runLater(() -> {
                    System.out.println("Executing Platform.exit()...");
                    Platform.exit();
                    System.exit(0);
                });
            }
        }

        private String calculateTodaysTotal() {
            String ordersPathStr = ConfigManager.getOrdersExcelPath();
            java.io.File ordersFile = new java.io.File(ordersPathStr);

            if (!ordersFile.exists() || ordersFile.length() == 0) {
                return "No orders have been recorded yet.";
            }

            double dailyTotal = 0;
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            Set<String> processedOrderIds = new HashSet<>();

            try (FileInputStream fis = new FileInputStream(ordersFile);
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) return "Orders sheet not found.";

                DataFormatter dataFormatter = new DataFormatter();

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String orderId = dataFormatter.formatCellValue(row.getCell(0));
                    if (orderId.isEmpty() || processedOrderIds.contains(orderId)) {
                        continue;
                    }

                    Cell dateCell = row.getCell(4);
                    if (dateCell != null) {
                        try {
                            LocalDate rowDate = LocalDate.parse(dataFormatter.formatCellValue(dateCell), formatter);
                            if (rowDate.isEqual(today)) {
                                Cell totalCell = row.getCell(6);
                                if (totalCell != null && totalCell.getCellType() == CellType.NUMERIC) {
                                    dailyTotal += totalCell.getNumericCellValue();
                                    processedOrderIds.add(orderId);
                                }
                            }
                        } catch (Exception e) {
                            // Ignore rows with malformed dates
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "Error calculating today's total. Please check the `orders.xlsx` file.";
            }

            return String.format("Total sales for today (%s): ₹%.2f", today.format(formatter), dailyTotal);
        }

        private String listAllItems() {
            StringBuilder sb = new StringBuilder();
            String menuJsonPathStr = ConfigManager.getMenuItemsJsonPath();
            java.io.File menuFile = new java.io.File(menuJsonPathStr);

            if (!menuFile.exists() || menuFile.length() == 0) {
                return "No menu items found. Add some items first!";
            }

            try {
                String content = new String(Files.readAllBytes(menuFile.toPath()), StandardCharsets.UTF_8);
                JSONObject rootJson = new JSONObject(content);
                JSONArray categoriesArray = rootJson.optJSONArray("categories");
                if (categoriesArray == null || categoriesArray.isEmpty()) {
                    return "No menu items found.";
                }

                sb.append("```\n");
                sb.append(String.format("%-4s | %-20s | %-15s | %s\n", "S.No", "Name", "Category", "Cost (₹)"));
                sb.append("-----------------------------------------------------------\n");

                int sno = 1;
                for (int i = 0; i < categoriesArray.length(); i++) {
                    JSONObject categoryObj = categoriesArray.getJSONObject(i);
                    String categoryName = categoryObj.getString("name");
                    JSONArray itemsArray = categoryObj.getJSONArray("items");
                    for (int j = 0; j < itemsArray.length(); j++) {
                        JSONObject itemObj = itemsArray.getJSONObject(j);
                        String itemName = itemObj.getString("name");
                        int price = itemObj.getInt("price");
                        sb.append(String.format("%-4d | %-20s | %-15s | %d\n", sno++, itemName, categoryName, price));
                    }
                }
                sb.append("```\n");

            } catch (Exception e) {
                e.printStackTrace();
                return "Error reading the menu file.";
            }

            return sb.toString();
        }

        private void handlePhotoMessage(Message message) {
            long chatId = message.getChatId();
            String caption = message.getCaption().trim();

            String[] parts = caption.split("\\s+", 4);
            if (parts.length == 4 && "add".equalsIgnoreCase(parts[0])) {
                try {
                    String itemName = parts[1];
                    String category = parts[2];
                    int price = Integer.parseInt(parts[3]);

                    List<PhotoSize> photos = message.getPhoto();
                    PhotoSize largestPhoto = photos.stream()
                            .max(Comparator.comparing(PhotoSize::getFileSize))
                            .orElse(null);

                    if (largestPhoto != null) {
                        handleAddItemWithPhotoCommand(chatId, itemName, category, price, largestPhoto);
                    } else {
                        sendMessage(chatId, "Error: Could not retrieve photo data.");
                    }

                } catch (NumberFormatException e) {
                    sendMessage(chatId, "Invalid command format. The last word must be a number (price).\nExample: ADD chocolate cone 20");
                }
            } else {
                sendMessage(chatId, "Invalid format. To add an item, send a photo with a caption like:\n`ADD <item_name> <category> <price>`");
            }
        }

        private void handleAddItemWithPhotoCommand(long chatId, String itemName, String category, int price, PhotoSize photo) {
            try {
                GetFile getFileMethod = new GetFile();
                getFileMethod.setFileId(photo.getFileId());
                File telegramFile = execute(getFileMethod);

                String imageFileName = Paths.get(telegramFile.getFilePath()).getFileName().toString();
                java.nio.file.Path localFilePath = Paths.get(ConfigManager.getImagePath(), imageFileName);

                try (InputStream is = new URL(telegramFile.getFileUrl(getBotToken())).openStream()) {
                    Files.copy(is, localFilePath, StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("Successfully downloaded image to: " + localFilePath);

                boolean success = addItemToJson(category, itemName, price, imageFileName, null);

                if (success) {
                    sendMessage(chatId, "✅ Item added successfully!\nName: " + itemName + "\nCategory: " + category + "\nPrice: " + price);
                    Platform.runLater(() -> {
                        ManageInventoryView.loadInventoryData();
                        NewOrderView.loadMenuItemsFromJson();
                        NewOrderView.refreshMenuView();
                    });
                } else {
                    sendMessage(chatId, "❌ Failed to add item. An item with this name might already exist.");
                }

            } catch (TelegramApiException | IOException e) {
                System.err.println("Error processing add item command: " + e.getMessage());
                e.printStackTrace();
                sendMessage(chatId, "An error occurred while processing your request. Please check the application logs.");
            }
        }

        private synchronized boolean addItemToJson(String categoryName, String itemName, int price, String imageName, Integer quantity) {
            String menuJsonPathStr = ConfigManager.getMenuItemsJsonPath();
            java.io.File menuFile = new java.io.File(menuJsonPathStr);
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
                    if (categoriesArray.getJSONObject(i).getString("name").equalsIgnoreCase(categoryName)) {
                        targetCategory = categoriesArray.getJSONObject(i);
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
                for (int i = 0; i < itemsArray.length(); i++) {
                    if (itemsArray.getJSONObject(i).getString("name").equalsIgnoreCase(itemName)) {
                        return false; // Item already exists
                    }
                }

                JSONObject newItem = new JSONObject();
                newItem.put("name", itemName);
                newItem.put("imageName", imageName);
                newItem.put("price", price);
                if (quantity != null) {
                    newItem.put("quantity", quantity);
                }
                itemsArray.put(newItem);

                try (FileWriter writer = new FileWriter(menuFile)) {
                    writer.write(rootJson.toString(4));
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public void sendMessage(long chatId, String text) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            message.setParseMode("Markdown");
            try {
                execute(message);
            } catch (TelegramApiException e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }
    }
}
