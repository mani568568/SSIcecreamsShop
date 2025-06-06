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
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
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
    private static final String TEXT_ON_DARK = "white";
    private static final String BACKGROUND_MAIN = "#E8EAF6";
    private static final String BORDER_COLOR_LIGHT = "#CFD8DC";
    private static final String SHADOW_COLOR = "rgba(26, 35, 126, 0.2)";
    private static final String BUTTON_ACTION_GREEN = "#4CAF50";
    private static final String BUTTON_ACTION_GREEN_HOVER = "#388E3C";
    private static final String BUTTON_ACTION_RED = "#F44336";
    private static final String BUTTON_ACTION_RED_HOVER = "#D32F2F";

    private static TextField dataDirectoryPathField;
    private static Stage dialogStage;

    public static void show() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("⚙️ Application Configuration");

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

        Label titleLabel = new Label("Configure Application Data Folder");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY + ";");
        titleLabel.setEffect(new DropShadow(5, Color.web(SHADOW_COLOR)));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(18);
        grid.setAlignment(Pos.CENTER);

        String labelStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_NAVY + ";";
        String fieldStyle = "-fx-font-size: 14px; -fx-background-radius: 18px; -fx-border-radius: 18px; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-padding: 8px 12px;";

        // --- Single Data Directory Path ---
        grid.add(new Label("Data Folder Location:") {{ setStyle(labelStyle); }}, 0, 0);
        dataDirectoryPathField = new TextField(ConfigManager.getDataDirectoryPath());
        dataDirectoryPathField.setPrefWidth(380);
        dataDirectoryPathField.setPromptText("Path to store all application data");
        dataDirectoryPathField.setStyle(fieldStyle);
        Button browseButton = new Button("Browse...");
        styleDialogButton(browseButton, PRIMARY_NAVY, PRIMARY_NAVY_DARK, false, TEXT_ON_DARK);
        browseButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Application Data Folder");
            File currentDir = new File(dataDirectoryPathField.getText());
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
            File selectedDirectory = directoryChooser.showDialog(dialogStage);
            if (selectedDirectory != null) {
                dataDirectoryPathField.setText(selectedDirectory.getAbsolutePath());
            }
        });
        HBox pathBox = new HBox(10, dataDirectoryPathField, browseButton);
        pathBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(pathBox, 1, 0);

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
        scene.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ESCAPE) dialogStage.close(); });

        dialogStage.setScene(scene);
        dialogStage.sizeToScene();
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    private static void saveConfiguration() {
        String dataDirPath = dataDirectoryPathField.getText().trim();
        if (dataDirPath.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "The data folder path cannot be empty.");
            return;
        }

        try {
            Path p = Paths.get(dataDirPath);
            if (Files.exists(p) && !Files.isDirectory(p)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "The selected path exists but is a file, not a folder.");
                return;
            }
        } catch (InvalidPathException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "The specified path is invalid: " + e.getMessage());
            return;
        }

        ConfigManager.setDataDirectoryPath(dataDirPath);

        showAlert(Alert.AlertType.INFORMATION, "Configuration Saved", "Application data folder has been set to:\n" + dataDirPath);

        try {
            ManageInventoryView.loadInventoryData();
            NewOrderView.loadMenuItemsFromJson();
            NewOrderView.refreshMenuView();
        } catch (Exception ex) {
            System.err.println("Error refreshing views after config change: " + ex.getMessage());
        }
        dialogStage.close();
    }

    // --- Rest of the methods (styleDialogButton, showAlert, etc.) remain unchanged ---
    private static void styleDialogButton(Button button, String baseColor, String hoverColor, boolean isPrimary, String textColor) { /* ... */ }
    private static void showAlert(Alert.AlertType alertType, String title, String message) { /* ... */ }
}
