package com.ssicecreamsshop;

import com.ssicecreamsshop.model.Order;
import com.ssicecreamsshop.model.OrderItem;
import com.ssicecreamsshop.utils.NetworkStatusIndicator;
import com.ssicecreamsshop.utils.OrderExcelUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ViewOrdersView {

    private static TableView<DisplayableOrder> ordersTable;
    private static final ObservableList<DisplayableOrder> displayableOrdersList = FXCollections.observableArrayList();
    private static NetworkStatusIndicator networkIndicator;
    private static Stage viewOrdersStage;

    private static DatePicker startDatePicker;
    private static DatePicker endDatePicker;
    private static Label totalAmountLabel;

    private static final DateTimeFormatter TABLE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FILTER_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DIALOG_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d,ގައި 'at' hh:mm a");


    public static void show() {
        viewOrdersStage = new Stage();
        viewOrdersStage.initModality(Modality.APPLICATION_MODAL);
        viewOrdersStage.setTitle("📜 View Past Orders");
        viewOrdersStage.setMinWidth(1000);
        viewOrdersStage.setMinHeight(750);

        if (networkIndicator != null) {
            networkIndicator.stopMonitoring();
        }
        networkIndicator = new NetworkStatusIndicator();

        // --- Top Bar ---
        Button backButton = new Button("← Back to Home");
        backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 6px; -fx-font-weight: bold;");
        backButton.setOnMouseEntered(e -> backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #6c7a7d; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 6px; -fx-font-weight: bold;"));
        backButton.setOnMouseExited(e -> backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 6px; -fx-font-weight: bold;"));
        backButton.setOnAction(e -> {
            if (networkIndicator != null) networkIndicator.stopMonitoring();
            viewOrdersStage.close();
        });

        Button refreshButton = new Button("🔄 Refresh Orders");
        refreshButton.setStyle("-fx-font-size: 13px; -fx-padding: 7 12; -fx-background-radius: 6px; -fx-text-fill: white; -fx-background-color: #5dade2; -fx-font-weight: bold;");
        refreshButton.setOnMouseEntered(e -> refreshButton.setStyle("-fx-font-size: 13px; -fx-padding: 7 12; -fx-background-radius: 6px; -fx-text-fill: white; -fx-background-color: #5499c7; -fx-font-weight: bold;"));
        refreshButton.setOnMouseExited(e -> refreshButton.setStyle("-fx-font-size: 13px; -fx-padding: 7 12; -fx-background-radius: 6px; -fx-text-fill: white; -fx-background-color: #5dade2; -fx-font-weight: bold;"));
        refreshButton.setOnAction(e -> loadOrders());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(15, networkIndicator, backButton, refreshButton, spacer);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setStyle("-fx-background-color: #f4f6f8; -fx-border-color: #d1d9e0; -fx-border-width: 0 0 1 0;");

        // --- Date Filter Section ---
        Label startDateLabel = new Label("Start Date:");
        startDateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("From");
        startDatePicker.setStyle("-fx-font-size: 13px;");
        ImageView calendarIconStart = createCalendarIcon();

        Label endDateLabel = new Label("End Date:");
        endDateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        endDatePicker = new DatePicker();
        endDatePicker.setPromptText("To");
        endDatePicker.setStyle("-fx-font-size: 13px;");
        ImageView calendarIconEnd = createCalendarIcon();

        Button calculateTotalButton = new Button("📊 Calculate Range Total");
        calculateTotalButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 12; -fx-background-radius: 6px;");
        calculateTotalButton.setOnMouseEntered(e -> calculateTotalButton.setStyle("-fx-background-color: #28b463; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 12; -fx-background-radius: 6px;"));
        calculateTotalButton.setOnMouseExited(e -> calculateTotalButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 12; -fx-background-radius: 6px;"));
        calculateTotalButton.setOnAction(e -> calculateAndShowTotalForDateRange());

        totalAmountLabel = new Label("Total for range: ₹0.00");
        totalAmountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 0 0 15px; -fx-text-fill: #16a085;");

        HBox dateFilterBox = new HBox(10,
                calendarIconStart, startDateLabel, startDatePicker,
                new Region() {{ HBox.setHgrow(this, Priority.SOMETIMES); setMinWidth(25);}},
                calendarIconEnd, endDateLabel, endDatePicker,
                new Region() {{ HBox.setHgrow(this, Priority.SOMETIMES); setMinWidth(25);}},
                calculateTotalButton, totalAmountLabel
        );
        dateFilterBox.setAlignment(Pos.CENTER_LEFT);
        dateFilterBox.setPadding(new Insets(15, 25, 15, 25));
        dateFilterBox.setStyle("-fx-background-color: #eaf2f8; -fx-border-color: #d1d9e0; -fx-border-width: 0 0 1 0;");


        // --- Orders Table ---
        ordersTable = new TableView<>();
        ordersTable.setStyle("-fx-font-size: 13.5px; -fx-selection-bar: #aed6f1; -fx-selection-bar-text: #212f3c;");
        setupTableColumns();
        ordersTable.setItems(displayableOrdersList);
        ordersTable.setPlaceholder(new Label("No orders found. Please check the date range or load orders."));

        VBox mainLayout = new VBox(topBar, dateFilterBox, ordersTable);
        VBox.setVgrow(ordersTable, Priority.ALWAYS);
        mainLayout.setStyle("-fx-background-color: #ffffff;");

        Scene scene = new Scene(mainLayout);
        viewOrdersStage.setScene(scene);
        viewOrdersStage.setOnHidden(e -> {
            if(networkIndicator != null) networkIndicator.stopMonitoring();
        });

        loadOrders();
        viewOrdersStage.showAndWait();
    }

    private static void setupTableColumns() {
        ordersTable.getColumns().clear();

        TableColumn<DisplayableOrder, String> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        idCol.setPrefWidth(280);
        idCol.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 8px;");

        TableColumn<DisplayableOrder, String> createdCol = new TableColumn<>("Created On");
        createdCol.setCellValueFactory(new PropertyValueFactory<>("createdDateTime"));
        createdCol.setPrefWidth(160);
        createdCol.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 8px;");

        TableColumn<DisplayableOrder, String> totalCol = new TableColumn<>("Order Total (₹)");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("orderTotalAmount"));
        totalCol.setPrefWidth(140);
        totalCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-padding: 8px;");

        TableColumn<DisplayableOrder, Void> viewCol = new TableColumn<>("Details");
        viewCol.setPrefWidth(100);
        viewCol.setSortable(false);
        viewCol.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("View ✨");
            {
                viewButton.setStyle("-fx-background-color: #5dade2; -fx-text-fill: white; -fx-font-size:12px; -fx-padding: 6 10; -fx-background-radius: 4px; -fx-font-weight: bold;");
                viewButton.setOnAction(event -> {
                    DisplayableOrder order = getTableView().getItems().get(getIndex());
                    if (order != null) {
                        showOrderDetailsDialog(order.getOriginalOrder());
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
                setAlignment(Pos.CENTER);
                setPadding(new Insets(4));
            }
        });

        TableColumn<DisplayableOrder, Void> deleteCol = new TableColumn<>("Action");
        deleteCol.setPrefWidth(100);
        deleteCol.setSortable(false);
        deleteCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete 🗑️");
            {
                deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size:12px; -fx-padding: 6 10; -fx-background-radius: 4px; -fx-font-weight: bold;");
                deleteButton.setOnAction(event -> {
                    DisplayableOrder order = getTableView().getItems().get(getIndex());
                    if (order != null) {
                        handleDeleteOrderLogic(order);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    DisplayableOrder order = getTableView().getItems().get(getIndex());
                    LocalDate orderDate = order.getOriginalCreatedDateTime().toLocalDate();
                    deleteButton.setDisable(!orderDate.isEqual(LocalDate.now()));
                    setGraphic(deleteButton);
                    setAlignment(Pos.CENTER);
                    setPadding(new Insets(4));
                }
            }
        });
        ordersTable.getColumns().addAll(idCol, createdCol, totalCol, viewCol, deleteCol);
    }


    private static ImageView createCalendarIcon() {
        try {
            Image calIcon = new Image(ViewOrdersView.class.getResourceAsStream("/images/calendar_icon.png"));
            ImageView iconView = new ImageView(calIcon);
            iconView.setFitHeight(18);
            iconView.setFitWidth(18);
            return iconView;
        } catch (Exception e) {
            System.err.println("Calendar icon not found, using text placeholder: " + e.getMessage());
            Label iconLabel = new Label("🗓️");
            iconLabel.setStyle("-fx-font-size: 16px; -fx-padding: 0 3 0 0;");
            return new ImageView(iconLabel.snapshot(null,null));
        }
    }


    private static void loadOrders() {
        displayableOrdersList.clear();
        List<Order> ordersFromExcel = OrderExcelUtil.loadOrdersFromExcel();

        for (Order order : ordersFromExcel) {
            displayableOrdersList.add(new DisplayableOrder(order));
        }
        if (displayableOrdersList.isEmpty()) {
            ordersTable.setPlaceholder(new Label("No orders found in the selected period or orders.xlsx is empty."));
        }
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

        String totalText = String.format("Total for selected range (%s to %s): ₹%.2f (%d transactions)",
                startDate.format(DATE_FILTER_FORMATTER),
                endDate.format(DATE_FILTER_FORMATTER),
                totalForRange,
                transactionsInRange);
        totalAmountLabel.setText(totalText);

        showAlert(Alert.AlertType.INFORMATION, "Date Range Total", totalText, viewOrdersStage);
    }

    private static void showOrderDetailsDialog(Order order) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(viewOrdersStage);
        dialogStage.setTitle("🛍️ Order Details");
        dialogStage.setMinWidth(600);
        dialogStage.setMinHeight(450);


        VBox dialogLayout = new VBox(20);
        dialogLayout.setPadding(new Insets(25));
        dialogLayout.setStyle("-fx-background-color: #fdfefe; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // Header Section
        Label idLabel = new Label("Order ID: " + order.getOrderId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2c3e50;");
        Label createdLabel = new Label("Placed on: " + order.getCreatedDateTime().format(DIALOG_DATETIME_FORMATTER));
        createdLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #566573;");

        VBox headerBox = new VBox(5, idLabel, createdLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Separator separator1 = new Separator();
        separator1.setStyle("-fx-background-color: #bdc3c7; -fx-pref-height: 1px;");


        Label itemsTitle = new Label("Items in this Order:");
        itemsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 0 8 0; -fx-text-fill: #34495e;");

        GridPane itemsGrid = new GridPane();
        itemsGrid.setHgap(20);
        itemsGrid.setVgap(10);
        itemsGrid.setPadding(new Insets(10,0,15,0));

        // Headers for items grid
        itemsGrid.add(new Label("Item Name") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;"); }}, 0, 0);
        itemsGrid.add(new Label("Quantity") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;"); }}, 1, 0);
        itemsGrid.add(new Label("Unit Price (₹)") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;"); }}, 2, 0);
        itemsGrid.add(new Label("Item Total (₹)") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;"); }}, 3, 0);

        int rowIndex = 1;
        for (OrderItem item : order.getOrderItems()) {
            itemsGrid.add(new Label(item.getItemName()) {{ setStyle("-fx-font-size: 14px;"); }}, 0, rowIndex);
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
        itemsScrollPane.setStyle("-fx-background-color: #fdfefe; -fx-border-color: #e5e8e8; -fx-border-radius: 5px; -fx-padding: 10px;");


        Separator separator2 = new Separator();
        separator2.setStyle("-fx-background-color: #bdc3c7; -fx-pref-height: 1px;");

        Label overallTotalLabelText = new Label("Grand Total for this Order:");
        overallTotalLabelText.setStyle("-fx-font-size: 17px; -fx-text-fill: #2c3e50;");
        Label overallTotalValue = new Label(String.format("₹%.2f", order.getOrderTotalAmount()));
        overallTotalValue.setStyle("-fx-font-weight: bold; -fx-font-size: 22px; -fx-text-fill: #1abc9c;");

        HBox totalBox = new HBox(10, overallTotalLabelText, overallTotalValue);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.setPadding(new Insets(15, 0, 0, 0));


        Button closeButton = new Button("Close Window");
        closeButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 10 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 6px;");
        closeButton.setOnMouseEntered(e -> closeButton.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 10 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 6px;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 10 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 6px;"));
        closeButton.setOnAction(e -> dialogStage.close());
        HBox buttonBar = new HBox(closeButton);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(20,0,0,0));

        dialogLayout.getChildren().addAll(headerBox, separator1, itemsTitle, itemsScrollPane, separator2, totalBox, buttonBar);

        Scene dialogScene = new Scene(dialogLayout);
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }


    private static void handleDeleteOrderLogic(DisplayableOrder orderToDelete) {
        LocalDate orderDate = orderToDelete.getOriginalCreatedDateTime().toLocalDate();
        if (!orderDate.isEqual(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Deletion Not Allowed", "This order cannot be deleted as it was not placed today.", viewOrdersStage);
            return;
        }

        Optional<ButtonType> result = showAlertWithConfirmation(
                "Confirm Delete Order",
                "Are you sure you want to delete Order ID: " + orderToDelete.getOrderId() + "?\n" +
                        "This action will remove all items of this order from the records.",
                viewOrdersStage
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = OrderExcelUtil.deleteOrderFromExcel(orderToDelete.getOrderId(), orderDate);
            if (deleted) {
                showAlert(Alert.AlertType.INFORMATION, "Delete Successful", "Order " + orderToDelete.getOrderId() + " has been deleted.", viewOrdersStage);
                loadOrders();
            } else {
                showAlert(Alert.AlertType.ERROR, "Delete Failed", "Could not delete order " + orderToDelete.getOrderId() + ".", viewOrdersStage);
            }
        }
    }


    private static void showAlert(Alert.AlertType alertType, String title, String message, Stage owner) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13.5px; -fx-background-color: #fdfefe;");
            // Attempt to load external CSS for dialogs, if available
            // String cssPath = "/styles/dialogs.css"; // Example path
            // try {
            //     String dialogCss = ViewOrdersView.class.getResource(cssPath).toExternalForm();
            //     dialogPane.getStylesheets().add(dialogCss);
            // } catch (NullPointerException e) {
            //     System.err.println("Dialog CSS not found at: " + cssPath + ". Using inline styles for buttons.");
            // Fallback inline styles if CSS is not found or for simplicity
            dialogPane.lookupAll(".button").forEach(node -> {
                if (node instanceof Button) {
                    Button button = (Button) node;
                    button.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14px; -fx-background-radius: 4px; -fx-font-size: 13px;");
                    button.setOnMouseEntered(eBtn -> button.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14px; -fx-background-radius: 4px; -fx-font-size: 13px;"));
                    button.setOnMouseExited(eBtn -> button.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14px; -fx-background-radius: 4px; -fx-font-size: 13px;"));
                }
            });
            // }


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
        dialogPane.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13.5px; -fx-background-color: #fdfefe;");

        // Using lookupButton for more reliable targeting of OK/Cancel
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14px; -fx-background-radius: 4px; -fx-font-size: 13px;");
            okButton.setOnMouseEntered(e -> okButton.setStyle("-fx-background-color: #28b463; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14px; -fx-background-radius: 4px; -fx-font-size: 13px;"));
            okButton.setOnMouseExited(e -> okButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14px; -fx-background-radius: 4px; -fx-font-size: 13px;"));
        }

        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14px; -fx-background-radius: 4px; -fx-font-size: 13px;");
            cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14px; -fx-background-radius: 4px; -fx-font-size: 13px;"));
            cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14px; -fx-background-radius: 4px; -fx-font-size: 13px;"));
        }


        if (owner != null && owner.isShowing()) {
            alert.initOwner(owner);
        }
        return alert.showAndWait();
    }

    public static class DisplayableOrder {
        private final SimpleStringProperty orderId;
        private final SimpleStringProperty createdDateTime;
        private final SimpleStringProperty orderTotalAmount;
        private final LocalDateTime originalCreatedDateTime;
        private final Order originalOrder;


        public DisplayableOrder(Order order) {
            this.originalOrder = order;
            this.orderId = new SimpleStringProperty(order.getOrderId());
            this.originalCreatedDateTime = order.getCreatedDateTime();
            this.createdDateTime = new SimpleStringProperty(order.getCreatedDateTime().format(TABLE_DATETIME_FORMATTER));
            this.orderTotalAmount = new SimpleStringProperty(String.format("%.2f", order.getOrderTotalAmount()));
        }

        public String getOrderId() { return orderId.get(); }
        public String getCreatedDateTime() { return createdDateTime.get(); }
        public String getOrderTotalAmount() { return orderTotalAmount.get(); }
        public LocalDateTime getOriginalCreatedDateTime() { return originalCreatedDateTime; }
        public Order getOriginalOrder() { return originalOrder; }

    }
}
