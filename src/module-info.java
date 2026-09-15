module SmartBank {
	requires javafx.controls;
	requires java.net.http;
	
	opens application to javafx.graphics, javafx.fxml;
}
