module com.algosys {
    requires javafx.controls;
    requires javafx.fxml;

    exports com.algosys;
    exports com.algosys.model;
    exports com.algosys.util;
    opens com.algosys.controller to javafx.fxml;
}
