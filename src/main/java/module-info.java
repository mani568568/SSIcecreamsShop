module com.example.ssicecreamsshop {
    requires javafx.controls;
    requires javafx.fxml;
        requires javafx.web;
            
        requires org.controlsfx.controls;
            requires com.dlsc.formsfx;
            requires net.synedra.validatorfx;
            requires org.kordamp.ikonli.javafx;
            requires org.kordamp.bootstrapfx.core;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.json;
    requires java.prefs;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires telegrambots.meta;
    requires telegrambots;

    opens com.example.ssicecreamsshop to javafx.fxml;
    exports com.ssicecreamsshop;
    opens com.ssicecreamsshop to javafx.fxml;
}