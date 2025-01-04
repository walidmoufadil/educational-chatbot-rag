module com.javaintellij.frontend {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.net.http;
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;

    opens com.javaintellij.frontend to javafx.fxml;
    exports com.javaintellij.frontend;
}