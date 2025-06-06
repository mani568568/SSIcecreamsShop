package com.ssicecreamsshop.utils; // Or a dedicated 'bot' package

import javafx.application.Platform;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramBotService {

    private final String botToken;
    private final String botUsername;
    private AppBot appBotInstance;
    private TelegramBotsApi botsApi;
    private GlobalStatusManager globalStatusManager; // To update Telegram icon status

    private final long authorizedUserId = 0; // <<<--- REPLACE THIS WITH YOUR TELEGRAM USER ID

    public TelegramBotService(String botToken, String botUsername, GlobalStatusManager statusManager) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.globalStatusManager = statusManager;
    }

    public void startBot() {
        if (globalStatusManager != null) {
            globalStatusManager.telegramBotStarting(); // Indicate attempt to start
        }
        try {
            appBotInstance = new AppBot();
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(appBotInstance);
            System.out.println("Telegram Bot Service started successfully. Bot username: @" + botUsername);
            if (globalStatusManager != null) {
                globalStatusManager.setTelegramBotStatus(true); // Bot is active
            }
        } catch (TelegramApiException e) {
            System.err.println("Error starting Telegram Bot Service: " + e.getMessage());
            e.printStackTrace();
            if (globalStatusManager != null) {
                globalStatusManager.setTelegramBotStatus(false); // Bot failed to start
            }
        }
    }

    public void stopBot() {
        System.out.println("Telegram Bot Service stopping...");
        if (globalStatusManager != null) {
            globalStatusManager.setTelegramBotStatus(false); // Bot is inactive
        }
        // The TelegramBotsApi library typically manages its own thread shutdown (daemon threads).
        // If botsApi.close() or similar existed and was non-blocking, it could be called.
        // For DefaultBotSession, explicit closing of the session is often not required.
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
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText().toLowerCase().trim();
                long chatId = update.getMessage().getChatId();
                long userId = update.getMessage().getFrom().getId();

                System.out.println("Received message: '" + messageText + "' from chat ID: " + chatId + ", user ID: " + userId);

                // --- SECURITY CHECK ---
                // IMPORTANT: Uncomment and use this to restrict commands to authorized users only!
                // if (userId != authorizedUserId) {
                //     System.out.println("Unauthorized command attempt from user ID: " + userId);
                //     sendMessage(chatId, "Sorry, you are not authorized to issue commands to this bot.");
                //     return;
                // }

                if (messageText.equals("/start")) {
                    sendMessage(chatId, "Welcome to the Ice Cream Shop Application Control Bot!");
                    if (authorizedUserId == 0) {
                        sendMessage(chatId, "WARNING: This bot is not yet secured. Please configure an authorized user ID in the code.");
                    } else if (userId == authorizedUserId) {
                        sendMessage(chatId, "You are an authorized user.");
                    }
                } else if (messageText.equals("shut") || messageText.equals("/shutdown")) {
                    // Apply security check specifically for sensitive commands if not globally done
                    if (authorizedUserId != 0 && userId != authorizedUserId) {
                        System.out.println("Unauthorized shutdown attempt from user ID: " + userId);
                        sendMessage(chatId, "Sorry, you are not authorized to issue this command.");
                        return;
                    }
                    System.out.println("Shutdown command received from chat ID: " + chatId);
                    sendMessage(chatId, "Understood. Shutting down the Ice Cream Shop application...");

                    Platform.runLater(() -> {
                        System.out.println("Executing Platform.exit()...");
                        Platform.exit();
                        System.exit(0);
                    });
                } else {
                    // sendMessage(chatId, "I received: " + update.getMessage().getText());
                }
            }
        }

        public void sendMessage(long chatId, String text) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            try {
                execute(message);
                System.out.println("Sent message: '" + text + "' to chat ID: " + chatId);
            } catch (TelegramApiException e) {
                System.err.println("Error sending message: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
