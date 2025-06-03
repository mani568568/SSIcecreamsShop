package com.ssicecreamsshop.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class ConfigManager {
    private static final String PREF_NODE_PATH = "com/ssicecreamsshop";
    private static final String KEY_IMAGE_PATH = "imagePath";
    private static final String KEY_MENU_JSON_PATH = "menuItemsJsonPath";

    // Default paths will be in a ".SSIceCreamShop" folder in the user's home directory
    private static final Path DEFAULT_APP_DATA_DIR = Paths.get(System.getProperty("user.home"), ".SSIceCreamShop");
    private static final Path DEFAULT_IMAGES_DIR = DEFAULT_APP_DATA_DIR.resolve("images");
    private static final Path DEFAULT_MENU_JSON_FILE = DEFAULT_APP_DATA_DIR.resolve("menu_items.json");

    private static Preferences getPreferences() {
        return Preferences.userRoot().node(PREF_NODE_PATH);
    }

    /**
     * Gets the configured path for the images directory.
     * Returns a default path if no configuration is found.
     * @return The images directory path string.
     */
    public static String getImagePath() {
        return getPreferences().get(KEY_IMAGE_PATH, DEFAULT_IMAGES_DIR.toString());
    }

    /**
     * Sets and saves the path for the images directory.
     * @param path The new images directory path string.
     */
    public static void setImagePath(String path) {
        getPreferences().put(KEY_IMAGE_PATH, path);
        try {
            // Attempt to create the directory if it doesn't exist when set
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            System.err.println("Warning: Could not create image directory on set: " + path + " - " + e.getMessage());
        }
    }

    /**
     * Gets the configured path for the menu_items.json file.
     * Returns a default path if no configuration is found.
     * @return The menu_items.json file path string.
     */
    public static String getMenuItemsJsonPath() {
        return getPreferences().get(KEY_MENU_JSON_PATH, DEFAULT_MENU_JSON_FILE.toString());
    }

    /**
     * Sets and saves the path for the menu_items.json file.
     * @param path The new menu_items.json file path string.
     */
    public static void setMenuItemsJsonPath(String path) {
        getPreferences().put(KEY_MENU_JSON_PATH, path);
        try {
            // Attempt to create parent directories if they don't exist when set
            Path menuPath = Paths.get(path);
            if (menuPath.getParent() != null) {
                Files.createDirectories(menuPath.getParent());
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create parent directory for menu JSON on set: " + path + " - " + e.getMessage());
        }
    }

    /**
     * Ensures that default directories and the default menu JSON file (if empty) exist.
     * This is typically called at application startup.
     */
    public static void ensureDefaultPathsExist() {
        try {
            if (!Files.exists(DEFAULT_APP_DATA_DIR)) {
                Files.createDirectories(DEFAULT_APP_DATA_DIR);
                System.out.println("Created default app data directory: " + DEFAULT_APP_DATA_DIR);
            }
            if (!Files.exists(DEFAULT_IMAGES_DIR)) {
                Files.createDirectories(DEFAULT_IMAGES_DIR);
                System.out.println("Created default images directory: " + DEFAULT_IMAGES_DIR);
            }
            if (!Files.exists(DEFAULT_MENU_JSON_FILE)) {
                String initialJsonContent = "{\"categories\": []}";
                Files.write(DEFAULT_MENU_JSON_FILE, initialJsonContent.getBytes(StandardCharsets.UTF_8));
                System.out.println("Created default menu_items.json: " + DEFAULT_MENU_JSON_FILE);
            }
        } catch (IOException e) {
            System.err.println("Critical Error: Could not create default directories/files: " + e.getMessage());
            // This could be a more user-facing error in a real app
        }
    }

    // Static initializer block to ensure default paths are checked/created when class is loaded,
    // though explicit call in AppLauncher is more controlled.
    // static {
    //     ensureDefaultPathsExist();
    // }
}

