module com.techcartel.siliconvalley {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.techcartel.siliconvalley to javafx.fxml;
    exports com.techcartel.siliconvalley;
}