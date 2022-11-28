package util.ui;

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
import org.apache.jena.rdf.model.ResourceFactory;
import service.DatasetService;
import vocabulary.DDO;

public class OccurrenceDialog extends TextInputDialog {

    private TextField nameTextField;
    private Text errorText;
    private ComboBox<String> instanceOfComboBox;
    private DatasetService datasetService;
    private Resource element;

    public OccurrenceDialog(DatasetService datasetService, Resource element){
        super();

        this.datasetService = datasetService;
        this.element = element;

        setTitle("Add Occurrence");
        setHeaderText("Enter details for the new occurrence.");
        setGraphic(null);

        ((Stage) getDialogPane().getScene().getWindow()).getIcons().add(new Image(OccurrenceDialog.class.getResourceAsStream("/images/icon.png")));
        getDialogPane().getStylesheets().add(OccurrenceDialog.class.getResource("/css/style.css").toExternalForm());

        GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(10);

        gridPane.add(new Label(element.getURI() + "/"), 0,0);

        this.nameTextField = new TextField();
        gridPane.add(this.nameTextField, 1, 0, 2, 1);

        gridPane.add(new Label("rdf:type"),1,1);
        gridPane.add(new Label("ddo:Occurrence"),2,1);

        gridPane.add(new Label("ddo:parent"),1,2);
        gridPane.add(new Label(element.getURI()),2,2);

        gridPane.add(new Label("ddo:instanceOf"), 1,3);

        this.instanceOfComboBox = new ComboBox<>();
        for (Resource occurrenceClass : datasetService.getOccurrenceClassPathsOfContext(element)) {
            this.instanceOfComboBox.getItems().add("n:" + occurrenceClass.getLocalName());
        }
        this.instanceOfComboBox.getSelectionModel().selectFirst();
        gridPane.add(this.instanceOfComboBox, 2, 3);

        gridPane.add(new Label("""
                The name must start with a letter.
                The name must contain only letters and digits.
                The name must be unique within a level.
                """), 0, 4, 3, 1);

        this.errorText = new Text();
        this.errorText.setFill(Color.RED);
        gridPane.add(this.errorText, 0, 5, 3, 1);

        getDialogPane().setContent(gridPane);
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    public boolean validateInput(){
        if (this.nameTextField.getText().matches("[a-zA-Z][a-zA-Z0-9-]*")
                && !this.datasetService.getNamesUnderLevel(this.element).contains(this.nameTextField.getText())) {
            return true;
        }
        this.errorText.setText("ERROR: invalid local name entered, please try again.");
        return false;
    }

    public Resource getInstanceOf(){
        return ResourceFactory.createResource(this.instanceOfComboBox.getSelectionModel().getSelectedItem().replaceFirst("n:", DDO.getN()));
    }

    public String getName() {
        return this.nameTextField.getText();
    }
}
