module com.algosys {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    exports com.algosys;
    exports com.algosys.model;
    exports com.algosys.util;
    opens com.algosys.controller to javafx.fxml;
}
