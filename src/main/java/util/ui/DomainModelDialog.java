package util.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class DomainModelDialog extends TextInputDialog {

    private TextField uriTextField;
    private Text errorText;

    public DomainModelDialog() {
        super();

        setTitle("New Domain Model");
        setHeaderText("Enter a URI for the new domain model.");
        setGraphic(null);

        ((Stage) getDialogPane().getScene().getWindow()).getIcons().add(new Image(DomainModelDialog.class.getResourceAsStream("/images/icon.png")));
        getDialogPane().getStylesheets().add(DomainModelDialog.class.getResource("/css/style.css").toExternalForm());

        VBox vBox = new VBox();
        vBox.setSpacing(10);

        this.uriTextField = new TextField();
        this.uriTextField.setPrefWidth(400);
        vBox.getChildren().add(this.uriTextField);

        vBox.getChildren().add(new Label("""
                The URI must start with "http://".
                The URI must end with a letter or digit.
                """));

        this.errorText = new Text();
        this.errorText.setFill(Color.RED);
        vBox.getChildren().add(this.errorText);

        getDialogPane().setContent(vBox);
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    public boolean validateInput() {
        if(this.uriTextField.getText().matches("^(http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9]")){
            return true;
        }

        this.errorText.setText("ERROR: invalid URI entered, please try again.");
        return false;
    }

    public String getURI() {
        return uriTextField.getText();
    }
}
