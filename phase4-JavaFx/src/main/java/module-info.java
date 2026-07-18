module com.techcartel.siliconvalley {
    requires javafx.controls;
    requires javafx.fxml;

    // 1. Opens these packages to JavaFX so FXML can inject @FXML variables via reflection
    opens com.techcartel.siliconvalley to javafx.fxml;
    opens com.techcartel.siliconvalley.view to javafx.fxml;

    // 2. Exports these packages so JavaFX's application engine can run them
    exports com.techcartel.siliconvalley;
    exports com.techcartel.siliconvalley.view;
}