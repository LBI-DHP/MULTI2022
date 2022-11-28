package util.ui;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import service.DatasetService;
import vocabulary.DDO;

import java.util.LinkedList;
import java.util.List;


public class SpecializationDialog extends TextInputDialog {

    private TextField nameTextField;
    private Text errorText;
    private ComboBox<String> abstractTypeComboBox;
    private VBox specializationsVBox;
    private Resource element;
    private Resource parent;
    private DatasetService datasetService;

    public SpecializationDialog(DatasetService datasetService, Resource element) {
        super();

        this.datasetService = datasetService;
        this.element = element;

        setTitle("Add Specialization");
        setHeaderText("Enter details for the new specialization.");
        setGraphic(null);

        ((Stage) getDialogPane().getScene().getWindow()).getIcons().add(new Image(SpecializationDialog.class.getResourceAsStream("/images/icon.png")));
        getDialogPane().getStylesheets().add(SpecializationDialog.class.getResource("/css/style.css").toExternalForm());

        GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(10);

        this.parent = this.datasetService.getParent(element);
        gridPane.add(new Label(this.parent.getURI() + "/"), 0, 0);

        this.nameTextField = new TextField();
        gridPane.add(this.nameTextField, 1, 0, 2, 1);

        gridPane.add(new Label("rdf:type"), 1, 1);

        List<Resource> linguisticTypes = datasetService.getLinguisticTypes(element);
        Resource individualType = linguisticTypes.contains(DDO.DomainObjectClass) ? DDO.DomainObjectClass : DDO.OccurrenceClass;
        gridPane.add(new Label("ddo:" + individualType.getLocalName()), 2, 1);

        gridPane.add(new Label("ddo:ModeledClass"), 2, 2);

        gridPane.add(new Label("ddo:leafs"), 1, 3);
        this.abstractTypeComboBox = new ComboBox<>();
        this.abstractTypeComboBox.getItems().add("ddo:AbstractClass");
        this.abstractTypeComboBox.getItems().add("ddo:ConcreteClass");
        this.abstractTypeComboBox.getSelectionModel().selectFirst();
        gridPane.add(this.abstractTypeComboBox, 2, 3);
        if (individualType.equals(DDO.DomainObjectClass)) {
            this.abstractTypeComboBox.setDisable(true);
            this.abstractTypeComboBox.getSelectionModel().clearSelection();
        }

        gridPane.add(new Label("ddo:specializationOf"), 1, 4);

        this.specializationsVBox = new VBox();
        this.specializationsVBox.setSpacing(5);
        this.specializationsVBox.getChildren().add(new Label("n:" + element.getLocalName()));
        gridPane.add(this.specializationsVBox, 2, 5);

        MenuButton menuButton = new MenuButton("more options");
        for (Resource name : this.datasetService.getIntraContextPathsOfSpecializationHierarchy(element)) {
            CheckMenuItem checkMenuItem = new CheckMenuItem("n:" + name.getLocalName());
            Label label = new Label("n:" + name.getLocalName());
            checkMenuItem.setOnAction(event -> {
                if (checkMenuItem.isSelected()) {
                    this.specializationsVBox.getChildren().add(label);
                } else {
                    this.specializationsVBox.getChildren().remove(label);
                }
                getDialogPane().getScene().getWindow().sizeToScene();
            });
            menuButton.getItems().add(checkMenuItem);
        }
        if (menuButton.getItems().size() == 0) {
            menuButton.setDisable(true);
        }
        gridPane.add(menuButton, 2, 4);

        gridPane.add(new Label("""
                The name must start with a letter.
                The name must contain only letters and digits.
                The name must be unique within a level.
                """), 0, 6, 3, 1);

        this.errorText = new Text();
        this.errorText.setFill(Color.RED);
        gridPane.add(this.errorText, 0, 7, 3, 1);

        getDialogPane().setContent(gridPane);
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    public boolean validateInput() {
        if (this.nameTextField.getText().matches("[a-zA-Z][a-zA-Z0-9-]*")
                && !this.datasetService.getNamesUnderLevel(this.parent).contains(this.nameTextField.getText())) {
            return true;
        }
        this.errorText.setText("ERROR: invalid local name entered, please try again.");
        return false;
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

    public List<Resource> getSpecializations() {
        List<Resource> list = new LinkedList<>();
        this.specializationsVBox.getChildren().forEach(x ->
                list.add(ResourceFactory.createResource(((Label) x).getText().replaceFirst("n:", DDO.getN()))));
        list.add(ResourceFactory.createResource(DDO.getN() + this.element.getLocalName()));
        return list;
    }

    public String getName() {
        return nameTextField.getText();
    }
}
