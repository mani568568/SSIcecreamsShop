package com.ssicecreamsshop;

import com.ssicecreamsshop.model.Order;
import com.ssicecreamsshop.model.OrderItem;
import com.ssicecreamsshop.utils.OrderExcelUtil;
import com.ssicecreamsshop.utils.ExcelExportUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ViewOrdersView {

    // --- Navy Blue & Yellow Theme Colors ---
    private static final String PRIMARY_NAVY = "#1A237E";
    private static final String PRIMARY_NAVY_DARK = "#283593";
    private static final String PRIMARY_NAVY_LIGHT = "#C5CAE9";
    private static final String ACCENT_YELLOW = "#FFC107";
    private static final String ACCENT_YELLOW_DARK = "#FFA000";
    private static final String TEXT_ON_DARK = "white";
    private static final String TEXT_ON_YELLOW = "#212121";
    private static final String TEXT_ON_LIGHT_PRIMARY = "#212121";
    private static final String TEXT_ON_LIGHT_SECONDARY = "#757575";
    private static final String BORDER_COLOR_LIGHT = "#CFD8DC";
    private static final String BACKGROUND_MAIN = "#E8EAF6";
    private static final String BACKGROUND_CONTENT = "#FFFFFF";
    private static final String SHADOW_COLOR = "rgba(26, 35, 126, 0.2)";
    private static final String BUTTON_ACTION_GREEN = "#4CAF50";
    private static final String BUTTON_ACTION_GREEN_HOVER = "#388E3C";
    private static final String BUTTON_ACTION_RED = "#F44336";
    private static final String BUTTON_ACTION_RED_HOVER = "#D32F2F";
    private static final String BUTTON_ACTION_BLUE = "#2196F3";
    private static final String BUTTON_ACTION_BLUE_HOVER = "#1976D2";


    private static TableView<DisplayableOrder> ordersTable;
    private static final ObservableList<DisplayableOrder> masterOrdersList = FXCollections.observableArrayList();
    private static FilteredList<DisplayableOrder> filteredOrdersList;
    private static Stage viewOrdersStage;

    private static DatePicker startDatePicker;
    private static DatePicker endDatePicker;
    private static Label totalAmountLabel;
    private static TextField searchField;

    private static final DateTimeFormatter TABLE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FILTER_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DIALOG_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' hh:mm a");


    public static void show() {
        viewOrdersStage = new Stage();
        viewOrdersStage.initModality(Modality.APPLICATION_MODAL);
        viewOrdersStage.setTitle("📜 View Past Orders");
        viewOrdersStage.setMinWidth(1100);
        viewOrdersStage.setMinHeight(750);

        try {
            Image appIcon = new Image(ViewOrdersView.class.getResourceAsStream("/images/app_icon.png"));
            viewOrdersStage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Error loading icon for View Orders window: " + e.getMessage());
        }

        // --- Top Bar ---
        Button backButton = new Button("← Back");
        styleTopBarButton(backButton, "#78909C", "#546E7A");
        backButton.setOnAction(e -> viewOrdersStage.close());

        Button refreshButton = new Button("🔄 Refresh");
        styleTopBarButton(refreshButton, BUTTON_ACTION_BLUE, BUTTON_ACTION_BLUE_HOVER);
        refreshButton.setOnAction(e -> loadOrders());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(15, backButton, refreshButton, spacer);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setStyle("-fx-background-color: " + BACKGROUND_CONTENT + "; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 0 0 1px 0;");
        topBar.setEffect(new DropShadow(5, 0, 2, Color.web(SHADOW_COLOR)));

        // --- Date Filter Section ---
        Label startDateLabel = new Label("From:");
        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Start Date");

        Label endDateLabel = new Label("To:");
        endDatePicker = new DatePicker();
        endDatePicker.setPromptText("End Date");

        Button calculateTotalButton = new Button("Calculate Total");
        styleTopBarButton(calculateTotalButton, BUTTON_ACTION_GREEN, BUTTON_ACTION_GREEN_HOVER);
        calculateTotalButton.setOnAction(e -> calculateAndShowTotalForDateRange());

        totalAmountLabel = new Label();
        totalAmountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 0 0 15px; -fx-text-fill: " + BUTTON_ACTION_GREEN_HOVER + ";");

        HBox filterBox = new HBox(10,
                startDateLabel, startDatePicker,
                endDateLabel, endDatePicker,
                new Region() {{ HBox.setHgrow(this, Priority.SOMETIMES); setMinWidth(25);}},
                calculateTotalButton, totalAmountLabel
        );
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setPadding(new Insets(15, 25, 15, 25));
        filterBox.setStyle("-fx-background-color: #f1f3f8; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-width: 0 0 1px 0;");


        // --- Orders Table ---
        ordersTable = new TableView<>();
        setupTableColumns();

        filteredOrdersList = new FilteredList<>(masterOrdersList, p -> true);
        SortedList<DisplayableOrder> sortedData = new SortedList<>(filteredOrdersList);
        sortedData.comparatorProperty().bind(ordersTable.comparatorProperty());
        ordersTable.setItems(sortedData);

        ordersTable.setPlaceholder(new Label("No orders found. Please check the date range or load orders."));

        VBox mainLayout = new VBox(topBar, filterBox, ordersTable);
        VBox.setVgrow(ordersTable, Priority.ALWAYS);
        mainLayout.setStyle("-fx-background-color: " + BACKGROUND_MAIN + ";");

        Scene scene = new Scene(mainLayout);

        viewOrdersStage.setScene(scene);

        loadOrders();
        viewOrdersStage.showAndWait();
    }

    private static void setupTableColumns() {
        ordersTable.getColumns().clear();
        String cellStyle = "-fx-alignment: CENTER_LEFT; -fx-padding: 8px;";

        TableColumn<DisplayableOrder, String> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        idCol.setPrefWidth(280);
        idCol.setStyle(cellStyle);

        TableColumn<DisplayableOrder, String> createdCol = new TableColumn<>("Created On");
        createdCol.setCellValueFactory(new PropertyValueFactory<>("createdDateTime"));
        createdCol.setPrefWidth(160);
        createdCol.setStyle(cellStyle);

        TableColumn<DisplayableOrder, String> totalCol = new TableColumn<>("Order Total (₹)");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("orderTotalAmount"));
        totalCol.setPrefWidth(120);
        totalCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-padding: 8px;");

        TableColumn<DisplayableOrder, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(200);
        actionCol.setSortable(false);
        actionCol.setCellFactory(param -> new ActionCell());

        ordersTable.getColumns().addAll(idCol, createdCol, totalCol, actionCol);
    }

    private static void calculateAndShowTotalForDateRange() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showAlert(Alert.AlertType.WARNING, "Date Range Incomplete", "Please select both a start and end date.", viewOrdersStage);
            return;
        }
        if (startDate.isAfter(endDate)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Date Range", "Start date cannot be after end date.", viewOrdersStage);
            return;
        }

        double totalForRange = 0;
        int transactionsInRange = 0;

        List<Order> allOrders = OrderExcelUtil.loadOrdersFromExcel();

        for (Order order : allOrders) {
            LocalDate orderDate = order.getCreatedDateTime().toLocalDate();
            if (!orderDate.isBefore(startDate) && !orderDate.isAfter(endDate)) {
                totalForRange += order.getOrderTotalAmount();
                transactionsInRange++;
            }
        }

        String totalText = String.format("Total for selected range (%s to %s): ₹%.2f",
                startDate.format(DATE_FILTER_FORMATTER),
                endDate.format(DATE_FILTER_FORMATTER),
                totalForRange);

        showAlert(Alert.AlertType.INFORMATION, "Date Range Calculation",
                totalText + "\n(" + transactionsInRange + " transactions found)",
                viewOrdersStage);

        totalAmountLabel.setText(String.format("Range Total: ₹%.2f", totalForRange));
    }


    private static void loadOrders() {
        masterOrdersList.clear();
        List<Order> ordersFromExcel = OrderExcelUtil.loadOrdersFromExcel();
        for (Order order : ordersFromExcel) {
            masterOrdersList.add(new DisplayableOrder(order));
        }
    }

    private static void styleTopBarButton(Button button, String baseColor, String hoverColor) {
        styleTopBarButton(button, baseColor, hoverColor, TEXT_ON_DARK);
    }

    private static void styleTopBarButton(Button button, String baseColor, String hoverColor, String textColor) {
        String style = "-fx-font-size: 13px; -fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 18px;";
        button.setStyle(style + "-fx-background-color: " + baseColor + ";");
        button.setEffect(new DropShadow(2, Color.web(SHADOW_COLOR)));
        button.setOnMouseEntered(e -> button.setStyle(style + "-fx-background-color: " + hoverColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 5, 0.15, 0, 1);"));
        button.setOnMouseExited(e -> button.setStyle(style + "-fx-background-color: " + baseColor + "; -fx-effect: dropshadow(gaussian, " + SHADOW_COLOR + ", 2, 0, 0, 0);"));
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

    private static Optional<ButtonType> showAlertWithConfirmation(String title, String message, Stage owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13.5px; -fx-background-color: " + BACKGROUND_MAIN +";");
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) styleTopBarButton(okButton, BUTTON_ACTION_GREEN, BUTTON_ACTION_GREEN_HOVER);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) styleTopBarButton(cancelButton, BUTTON_ACTION_RED, BUTTON_ACTION_RED_HOVER);
        if (owner != null && owner.isShowing()) {
            alert.initOwner(owner);
        }
        return alert.showAndWait();
    }

    private static void showOrderDetailsDialog(Order order) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(viewOrdersStage);
        dialogStage.setTitle("🛍️ Order Details");
        dialogStage.setMinWidth(700);
        dialogStage.setMinHeight(550);

        try {
            Image appIcon = new Image(ViewOrdersView.class.getResourceAsStream("/images/app_icon.png"));
            dialogStage.getIcons().add(appIcon);
        } catch (Exception e) { System.err.println("Could not load icon for details dialog."); }


        VBox dialogLayout = new VBox(20);
        dialogLayout.setPadding(new Insets(25));
        dialogLayout.setStyle("-fx-background-color: " + BACKGROUND_CONTENT + "; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        Label idLabel = new Label("Order ID: " + order.getOrderId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: " + PRIMARY_NAVY + ";");
        Label createdLabel = new Label("Placed on: " + order.getCreatedDateTime().format(DIALOG_DATETIME_FORMATTER));
        createdLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + TEXT_ON_LIGHT_SECONDARY + ";");

        VBox headerBox = new VBox(5, idLabel, createdLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Separator separator1 = new Separator();
        separator1.setStyle("-fx-background-color: " + BORDER_COLOR_LIGHT + "; -fx-pref-height: 1px;");

        Label itemsTitle = new Label("Items in this Order:");
        itemsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 0 8 0; -fx-text-fill: " + PRIMARY_NAVY_DARK + ";");

        GridPane itemsGrid = new GridPane();
        itemsGrid.setHgap(20); itemsGrid.setVgap(10); itemsGrid.setPadding(new Insets(10,0,15,0));

        String gridHeaderStyle = "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + PRIMARY_NAVY + ";";
        itemsGrid.add(new Label("Item Name") {{ setStyle(gridHeaderStyle); }}, 0, 0);
        itemsGrid.add(new Label("Quantity") {{ setStyle(gridHeaderStyle); }}, 1, 0);
        itemsGrid.add(new Label("Unit Price (₹)") {{ setStyle(gridHeaderStyle); }}, 2, 0);
        itemsGrid.add(new Label("Item Total (₹)") {{ setStyle(gridHeaderStyle); }}, 3, 0);

        int rowIndex = 1;
        for (OrderItem item : order.getOrderItems()) {
            itemsGrid.add(new Label(item.getItemName()) {{ setStyle("-fx-font-size: 14px; -fx-text-fill: " + TEXT_ON_LIGHT_PRIMARY + ";"); }}, 0, rowIndex);
            Label qtyLabel = new Label(String.valueOf(item.getQuantity()));
            qtyLabel.setStyle("-fx-font-size: 14px; -fx-alignment: CENTER;");
            itemsGrid.add(qtyLabel, 1, rowIndex);
            Label unitPriceLabel = new Label(String.format("%.2f", item.getUnitPrice()));
            unitPriceLabel.setStyle("-fx-font-size: 14px; -fx-alignment: CENTER_RIGHT;");
            itemsGrid.add(unitPriceLabel, 2, rowIndex);
            Label itemTotalLabel = new Label(String.format("%.2f", item.getTotalItemPrice()));
            itemTotalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: CENTER_RIGHT;");
            itemsGrid.add(itemTotalLabel, 3, rowIndex);
            rowIndex++;
        }

        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(45); col1.setHalignment(HPos.LEFT);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(15); col2.setHalignment(HPos.CENTER);
        ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(20); col3.setHalignment(HPos.RIGHT);
        ColumnConstraints col4 = new ColumnConstraints(); col4.setPercentWidth(20); col4.setHalignment(HPos.RIGHT);
        itemsGrid.getColumnConstraints().addAll(col1, col2, col3, col4);

        ScrollPane itemsScrollPane = new ScrollPane(itemsGrid);
        itemsScrollPane.setFitToWidth(true);
        itemsScrollPane.setPrefHeight(200);
        itemsScrollPane.setStyle("-fx-background-color: " + BACKGROUND_CONTENT + "; -fx-border-color: " + BORDER_COLOR_LIGHT + "; -fx-border-radius: 5px; -fx-padding: 10px;");

        Separator separator2 = new Separator();
        separator2.setStyle("-fx-background-color: " + BORDER_COLOR_LIGHT + "; -fx-pref-height: 1px;");

        Label overallTotalLabelText = new Label("Grand Total for this Order:");
        overallTotalLabelText.setStyle("-fx-font-size: 17px; -fx-text-fill: " + PRIMARY_NAVY_DARK + ";");
        Label overallTotalValue = new Label(String.format("₹%.2f", order.getOrderTotalAmount()));
        overallTotalValue.setStyle("-fx-font-weight: bold; -fx-font-size: 22px; -fx-text-fill: " + ACCENT_YELLOW_DARK + ";");

        HBox totalBox = new HBox(10, overallTotalLabelText, overallTotalValue);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.setPadding(new Insets(15, 0, 0, 0));

        Button closeButton = new Button("Close Window");
        styleTopBarButton(closeButton, "#78909C", "#546E7A");
        closeButton.setOnAction(e -> dialogStage.close());
        HBox buttonBar = new HBox(closeButton);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(20,0,0,0));

        dialogLayout.getChildren().addAll(headerBox, separator1, itemsTitle, itemsScrollPane, separator2, totalBox, buttonBar);

        Scene dialogScene = new Scene(dialogLayout);

        dialogScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialogStage.close();
            }
        });

        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }

    private static void handleDeleteOrderLogic(DisplayableOrder order) {
        LocalDate orderDate = order.getOriginalCreatedDateTime().toLocalDate();
        if (!orderDate.isEqual(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Deletion Not Allowed", "This order cannot be deleted as it was not placed today.", viewOrdersStage);
            return;
        }

        Optional<ButtonType> result = showAlertWithConfirmation("Confirm Delete Order", "Are you sure you want to delete Order ID: " + order.getOrderId() + "?", viewOrdersStage);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (OrderExcelUtil.deleteOrderFromExcel(order.getOrderId(), orderDate)) {
                showAlert(Alert.AlertType.INFORMATION, "Delete Successful", "Order " + order.getOrderId() + " has been deleted.", viewOrdersStage);
                loadOrders();
            } else {
                showAlert(Alert.AlertType.ERROR, "Delete Failed", "Could not delete order " + order.getOrderId() + ".", viewOrdersStage);
            }
        }
    }

    public static class DisplayableOrder {
        private final SimpleStringProperty orderId, itemsSummary, createdDateTime, orderTotalAmount;
        private final LocalDateTime originalCreatedDateTime;
        private final Order originalOrder;
        public DisplayableOrder(Order order) {
            this.originalOrder = order;
            this.orderId = new SimpleStringProperty(order.getOrderId());
            this.itemsSummary = new SimpleStringProperty(order.getOrderItems().stream().map(item -> item.getItemName() + " (x" + item.getQuantity() + ")").collect(Collectors.joining(", ")));
            this.originalCreatedDateTime = order.getCreatedDateTime();
            this.createdDateTime = new SimpleStringProperty(order.getCreatedDateTime().format(TABLE_DATETIME_FORMATTER));
            this.orderTotalAmount = new SimpleStringProperty(String.format("%.2f", order.getOrderTotalAmount()));
        }
        public String getOrderId() { return orderId.get(); }
        public String getItemsSummary() { return itemsSummary.get(); }
        public String getCreatedDateTime() { return createdDateTime.get(); }
        public String getOrderTotalAmount() { return orderTotalAmount.get(); }
        public LocalDateTime getOriginalCreatedDateTime() { return originalCreatedDateTime; }
        public Order getOriginalOrder() { return originalOrder; }
    }

    // Combined CellFactory for View and Delete buttons
    private static class ActionCell extends TableCell<DisplayableOrder, Void> {
        private final Button viewButton = new Button("View ✨");
        private final Button deleteButton = new Button("Delete 🗑️");
        private final HBox pane = new HBox(10, viewButton, deleteButton);

        ActionCell() {
            pane.setAlignment(Pos.CENTER);
            styleTopBarButton(viewButton, BUTTON_ACTION_BLUE, BUTTON_ACTION_BLUE_HOVER);
            styleTopBarButton(deleteButton, BUTTON_ACTION_RED, BUTTON_ACTION_RED_HOVER);

            viewButton.setOnAction(e -> {
                DisplayableOrder order = getTableView().getItems().get(getIndex());
                if (order != null) showOrderDetailsDialog(order.getOriginalOrder());
            });

            deleteButton.setOnAction(e -> {
                DisplayableOrder order = getTableView().getItems().get(getIndex());
                if (order != null) handleDeleteOrderLogic(order);
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) { setGraphic(null); }
            else {
                DisplayableOrder order = getTableView().getItems().get(getIndex());
                deleteButton.setDisable(!order.getOriginalCreatedDateTime().toLocalDate().isEqual(LocalDate.now()));
                setGraphic(pane);
            }
        }
    }
}
