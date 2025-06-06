package com.ssicecreamsshop.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConfigManager {
    private static final String PREF_NODE_PATH = "com/ssicecreamsshop";
    private static final String KEY_APP_DATA_DIRECTORY = "appDataDirectory"; // Single key for the main folder

    // Default path will be in a ".SSIceCreamShop" folder in the user's home directory
    private static final Path DEFAULT_APP_DATA_DIR = Paths.get(System.getProperty("user.home"), ".SSIceCreamShop");

    private static Preferences getPreferences() {
        return Preferences.userRoot().node(PREF_NODE_PATH);
    }

    /**
     * Gets the single, user-configured base data directory path.
     * @return The path string for the main data directory.
     */
    public static String getDataDirectoryPath() {
        return getPreferences().get(KEY_APP_DATA_DIRECTORY, DEFAULT_APP_DATA_DIR.toString());
    }

    /**
     * Sets the single, user-configured base data directory path.
     * On set, it also ensures the required subdirectories and files are created.
     * @param path The new base data directory path string.
     */
    public static void setDataDirectoryPath(String path) {
        getPreferences().put(KEY_APP_DATA_DIRECTORY, path);
        ensureDefaultPathsExist(); // Create directory structure immediately on change
    }

    /**
     * Constructs the full path for the 'images' subdirectory based on the main data directory.
     * @return The full path string for the images directory.
     */
    public static String getImagePath() {
        return Paths.get(getDataDirectoryPath(), "images").toString();
    }

    /**
     * Constructs the full path for the 'menu_items.json' file based on the main data directory.
     * @return The full path string for the menu JSON file.
     */
    public static String getMenuItemsJsonPath() {
        return Paths.get(getDataDirectoryPath(), "menu_items.json").toString();
    }

    /**
     * Constructs the full path for the 'orders.xlsx' file based on the main data directory.
     * @return The full path string for the orders Excel file.
     */
    public static String getOrdersExcelPath() {
        return Paths.get(getDataDirectoryPath(), "orders.xlsx").toString();
    }

    /**
     * Ensures that the configured base directory, its 'images' subdirectory,
     * and a default 'menu_items.json' file exist.
     * This is typically called at application startup or when the path is changed.
     */
    public static void ensureDefaultPathsExist() {
        try {
            Path dataDir = Paths.get(getDataDirectoryPath());
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                System.out.println("Created application data directory: " + dataDir);
            }
            Path imagesDir = Paths.get(getImagePath());
            if (!Files.exists(imagesDir)) {
                Files.createDirectories(imagesDir);
                System.out.println("Created images subdirectory: " + imagesDir);
            }
            Path menuFile = Paths.get(getMenuItemsJsonPath());
            if (!Files.exists(menuFile)) {
                JSONObject root = new JSONObject();
                root.put("categories", new JSONArray());
                Files.write(menuFile, root.toString(2).getBytes(StandardCharsets.UTF_8));
                System.out.println("Created empty menu_items.json: " + menuFile);
            }
            // Note: orders.xlsx is created on-the-fly by OrderExcelUtil when the first order is saved.
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not create default directories/files: " + e.getMessage());
            // In a real application, you might show a user-facing error here.
        }
    }
}
