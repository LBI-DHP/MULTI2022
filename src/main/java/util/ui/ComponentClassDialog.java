package util.ui;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.jena.rdf.model.Resource;
import service.DatasetService;
import util.AppStateUtil;
import vocabulary.DDO;

import java.util.List;

public class ComponentClassDialog extends TextInputDialog {

    private TextField nameTextField;
    private ComboBox<String> individualTypeComboBox;
    private ComboBox<String> abstractTypeComboBox;
    private Text errorText;
    private Resource element;
    private DatasetService datasetService;

    public ComponentClassDialog(DatasetService datasetService, Resource element) {
        super();

        this.datasetService = datasetService;
        this.element = element;

        setTitle("Add Component Class");
        setHeaderText("Enter details for the new component class.");
        setGraphic(null);

        ((Stage) getDialogPane().getScene().getWindow()).getIcons().add(new Image(ComponentClassDialog.class.getResourceAsStream("/images/icon.png")));
        getDialogPane().getStylesheets().add(ComponentClassDialog.class.getResource("/css/style.css").toExternalForm());

        GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(10);

        gridPane.add(new Label(element.getURI() + "/"), 0, 0);

        this.nameTextField = new TextField();
        gridPane.add(this.nameTextField, 1, 0, 2, 1);

        gridPane.add(new Label("ddo:parent"), 1, 1);
        gridPane.add(new Label(element.getURI()), 2, 1);

        List<Resource> linguisticType = datasetService.getLinguisticTypes(element);

        gridPane.add(new Label("rdf:type"), 1, 2);

        this.individualTypeComboBox = new ComboBox<>();
        if (linguisticType.contains(DDO.DomainObjectClass)) {
            this.individualTypeComboBox.getItems().add("ddo:DomainObjectClass");
        }
        this.individualTypeComboBox.getItems().add("ddo:OccurrenceClass");
        this.individualTypeComboBox.getSelectionModel().selectFirst();
        gridPane.add(this.individualTypeComboBox, 2, 2);

        gridPane.add(new Label("ddo:ModeledClass"), 2, 3);

        gridPane.add(new Label("ddo:leafs"), 1, 4);
        this.abstractTypeComboBox = new ComboBox<>();
        this.abstractTypeComboBox.getItems().add("ddo:AbstractClass");
        this.abstractTypeComboBox.getItems().add("ddo:ConcreteClass");
        this.abstractTypeComboBox.getSelectionModel().selectFirst();
        gridPane.add(this.abstractTypeComboBox, 2, 4);
        if (this.individualTypeComboBox.getSelectionModel().getSelectedItem().equals("ddo:DomainObjectClass")) {
            this.abstractTypeComboBox.setDisable(true);
            this.abstractTypeComboBox.getSelectionModel().clearSelection();
        }

        this.individualTypeComboBox.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue.equals("ddo:DomainObjectClass")) {
                this.abstractTypeComboBox.setDisable(true);
                this.abstractTypeComboBox.getSelectionModel().clearSelection();
            } else {
                this.abstractTypeComboBox.setDisable(false);
                this.abstractTypeComboBox.getSelectionModel().selectFirst();
            }
            getDialogPane().getScene().getWindow().sizeToScene();
        });

        gridPane.add(new Label("""
                The name must start with a letter.
                The name must contain only letters and digits.
                The name must be unique within a level.
                """), 0, 5, 3, 1);

        this.errorText = new Text();
        this.errorText.setFill(Color.RED);
        gridPane.add(this.errorText, 0, 6, 3, 1);

        getDialogPane().setContent(gridPane);
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    public boolean validateInput() {
        if (this.nameTextField.getText().matches("[a-zA-Z][a-zA-Z0-9-]*")
                && !this.datasetService.getNamesUnderLevel(this.element).contains(this.nameTextField.getText())) {
            return true;
        }
        this.errorText.setText("ERROR: invalid name entered, please try again.");
        return false;
    }

    public Resource getIndividualType() {
        if (this.individualTypeComboBox.getSelectionModel().getSelectedItem().equals("ddo:DomainObjectClass"))
            return DDO.DomainObjectClass;
        else return DDO.OccurrenceClass;
    }

    public Resource getAbstractType() {
        if (this.abstractTypeComboBox.isDisabled()) {
            return null;
        } else {
            if (this.abstractTypeComboBox.getSelectionModel().getSelectedItem().equals("ddo:AbstractClass")) {
                return DDO.AbstractClass;
            } else {
                return DDO.ConcreteClass;
            }
        }
    }

    public String getName() {
        return this.nameTextField.getText();
    }
}
