package com.ssicecreamsshop.config;

import com.ssicecreamsshop.ManageInventoryView;
import com.ssicecreamsshop.NewOrderView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image; // Import Image class
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationDialog {

    // Theme Colors
    private static final String PRIMARY_NAVY = "#1A237E";
    private static final String PRIMARY_NAVY_DARK = "#283593";
    private static final String ACCENT_YELLOW = "#FFC107";
    private static final String ACCENT_YELLOW_DARK = "#FFA000";
    private static final String TEXT_ON_DARK = "white";
    private static final String TEXT_ON_YELLOW = "#212121";
    private static final String BACKGROUND_MAIN = "#E8EAF6";
    private static final String BORDER_COLOR_LIGHT = "#CFD8DC";
    private static final String SHADOW_COLOR = "rgba(26, 35, 126, 0.2)";
    private static final String BUTTON_ACTION_GREEN = "#4CAF50";
    private static final String BUTTON_ACTION_GREEN_HOVER = "#388E3C";
    private static final String BUTTON_ACTION_RED = "#F44336";
    private static final String BUTTON_ACTION_RED_HOVER = "#D32F2F";


    private static TextField imagePathField;
    private static TextField menuJsonPathField;
    private static Stage dialogStage;

    public static void show() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("⚙️ Application Configuration");

        // --- ADDED: Set Window Icon ---
        try {
            Image appIcon = new Image(ConfigurationDialog.class.getResourceAsStream("/images/app_icon.png"));
            dialogStage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Error loading icon for Configuration dialog: " + e.getMessage());
        }

        VBox layout = new VBox(25);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: " + BACKGROUND_MAIN + "; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        Label titleLabel = new Label("Configure File Paths");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY + ";");
        titleLabel.setEffect(new DropShadow(5, Color.web(SHADOW_COLOR)));


        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(18);
        grid.setAlignment(Pos.CENTER);

        String labelStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY + ";";
        String fieldStyle = "-fx-font-size: 14px; -fx-background-radius: 18px; -fx-border-radius: 18px; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-padding: 8px 12px;";


        // Image Path
        grid.add(new Label("Images Directory Path:") {{ setStyle(labelStyle); }}, 0, 0);
        imagePathField = new TextField(ConfigManager.getImagePath());
        imagePathField.setPrefWidth(380);
        imagePathField.setPromptText("Path to your images folder");
        imagePathField.setStyle(fieldStyle);
        Button browseImageDirButton = new Button("Browse...");
        styleDialogButton(browseImageDirButton, PRIMARY_NAVY, PRIMARY_NAVY_DARK, false, TEXT_ON_DARK);
        browseImageDirButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Images Directory");
            File currentDir = new File(imagePathField.getText());
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            } else {
                File defaultDir = new File(ConfigManager.getImagePath()).getParentFile();
                if (defaultDir!=null && defaultDir.exists() && defaultDir.isDirectory()) {
                    directoryChooser.setInitialDirectory(defaultDir);
                }
            }
            File selectedDirectory = directoryChooser.showDialog(dialogStage);
            if (selectedDirectory != null) {
                imagePathField.setText(selectedDirectory.getAbsolutePath());
            }
        });
        HBox imagePathBox = new HBox(10, imagePathField, browseImageDirButton);
        imagePathBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(imagePathBox, 1, 0);

        // Menu JSON Path
        grid.add(new Label("Menu JSON File Path:") {{ setStyle(labelStyle); }}, 0, 1);
        menuJsonPathField = new TextField(ConfigManager.getMenuItemsJsonPath());
        menuJsonPathField.setPromptText("Path to your menu_items.json file");
        menuJsonPathField.setStyle(fieldStyle);
        Button browseMenuJsonButton = new Button("Browse...");
        styleDialogButton(browseMenuJsonButton, PRIMARY_NAVY, PRIMARY_NAVY_DARK, false, TEXT_ON_DARK);
        browseMenuJsonButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select menu_items.json");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"));

            File currentJsonFile = new File(menuJsonPathField.getText());
            File parentDir = currentJsonFile.getParentFile();
            if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
                fileChooser.setInitialDirectory(parentDir);
            } else {
                Path defaultJsonPath = Paths.get(ConfigManager.getMenuItemsJsonPath());
                File defaultParentDir = defaultJsonPath.getParent() != null ? defaultJsonPath.getParent().toFile() : null;
                if (defaultParentDir != null && defaultParentDir.exists() && defaultParentDir.isDirectory()) {
                    fileChooser.setInitialDirectory(defaultParentDir);
                }
            }
            File selectedFile = fileChooser.showOpenDialog(dialogStage);
            if (selectedFile != null) {
                menuJsonPathField.setText(selectedFile.getAbsolutePath());
            }
        });
        HBox menuJsonPathBox = new HBox(10, menuJsonPathField, browseMenuJsonButton);
        menuJsonPathBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(menuJsonPathBox, 1, 1);

        // Save and Cancel Buttons
        Button saveButton = new Button("Save Configuration");
        styleDialogButton(saveButton, BUTTON_ACTION_GREEN, BUTTON_ACTION_GREEN_HOVER, true, TEXT_ON_DARK);
        saveButton.setOnAction(e -> saveConfiguration());

        Button cancelButton = new Button("Cancel");
        styleDialogButton(cancelButton, BUTTON_ACTION_RED, BUTTON_ACTION_RED_HOVER, true, TEXT_ON_DARK);
        cancelButton.setOnAction(e -> dialogStage.close());

        HBox buttonBar = new HBox(20, saveButton, cancelButton);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(25, 0, 0, 0));

        layout.getChildren().addAll(titleLabel, grid, buttonBar);

        Scene scene = new Scene(layout);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialogStage.close();
            }
        });

        dialogStage.setScene(scene);
        dialogStage.sizeToScene();
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    private static void styleDialogButton(Button button, String baseColor, String hoverColor, boolean isPrimary, String textColor) {
        String padding = isPrimary ? "10 22" : "8 15";
        String fontSize = isPrimary ? "14px" : "13px";
        String style = "-fx-font-size: " + fontSize + "; -fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-padding: " + padding + "; -fx-background-radius: 20px;";
        button.setStyle(style + "-fx-background-color: " + baseColor + ";");
        button.setEffect(new DropShadow(3, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(style + "-fx-background-color: " + hoverColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 7, 0.2, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(style + "-fx-background-color: " + baseColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 3, 0, 0, 0);"));
    }


    private static void saveConfiguration() {
        String imagePathStr = imagePathField.getText().trim();
        String menuJsonPathStr = menuJsonPathField.getText().trim();

        if (imagePathStr.isEmpty() || menuJsonPathStr.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Both paths must be specified.");
            return;
        }

        try {
            Path imgP = Paths.get(imagePathStr);
            if (Files.exists(imgP) && !Files.isDirectory(imgP)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Image path exists but is not a directory: " + imagePathStr);
                return;
            }
        } catch (InvalidPathException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid image directory path: " + e.getMessage());
            return;
        }

        try {
            Path menuP = Paths.get(menuJsonPathStr);
            if (Files.exists(menuP) && Files.isDirectory(menuP)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Menu JSON path must be a file, not a directory: " + menuJsonPathStr);
                return;
            }
            if (!menuJsonPathStr.toLowerCase().endsWith(".json")) {
                showAlert(Alert.AlertType.WARNING, "Validation Warning", "Menu file path does not end with .json. Ensure it's a valid JSON file.");
            }
        } catch (InvalidPathException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid menu JSON file path: " + e.getMessage());
            return;
        }

        ConfigManager.setImagePath(imagePathStr);
        ConfigManager.setMenuItemsJsonPath(menuJsonPathStr);

        showAlert(Alert.AlertType.INFORMATION, "Configuration Saved", "Paths have been saved successfully.");

        try {
            ManageInventoryView.loadInventoryData();
            NewOrderView.loadMenuItemsFromJson();
            NewOrderView.refreshMenuView();
        } catch (Exception ex) {
            System.err.println("Error refreshing views after config change: " + ex.getMessage());
            showAlert(Alert.AlertType.WARNING, "Refresh Issue",
                    "Configuration saved, but there was an issue refreshing some views immediately: " + ex.getMessage() +
                            "\nChanges will apply fully on next view or application restart.");
        }
        dialogStage.close();
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
                String buttonBaseColor = alertType == Alert.AlertType.ERROR || alertType == Alert.AlertType.WARNING ? BUTTON_ACTION_RED : PRIMARY_NAVY;
                String buttonHoverColor = alertType == Alert.AlertType.ERROR || alertType == Alert.AlertType.WARNING ? BUTTON_ACTION_RED_HOVER : PRIMARY_NAVY_DARK;
                String btnStyle = "-fx-text-fill: " + TEXT_ON_DARK + "; -fx-font-weight: bold; -fx-padding: 6 12px; -fx-background-radius: 4px;";
                button.setStyle(btnStyle + "-fx-background-color: " + buttonBaseColor + ";");
                button.setOnMouseEntered(e -> button.setStyle(btnStyle + "-fx-background-color: " + buttonHoverColor + ";"));
                button.setOnMouseExited(e -> button.setStyle(btnStyle + "-fx-background-color: " + buttonBaseColor + ";"));
            }

            alert.initOwner(dialogStage);
            alert.showAndWait();
        });
    }
}
