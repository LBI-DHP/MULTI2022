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

public class DomainObjectDialog extends TextInputDialog {

    private TextField nameTextField;
    private Text errorText;
    private VBox classesVBox;
    private DatasetService datasetService;
    private Resource element;

    public DomainObjectDialog(DatasetService datasetService, Resource element){
        super();

        this.datasetService = datasetService;
        this.element = element;

        setTitle("Add Domain Object");
        setHeaderText("Enter details for the new domain object.");
        setGraphic(null);

        ((Stage) getDialogPane().getScene().getWindow()).getIcons().add(new Image(DomainObjectDialog.class.getResourceAsStream("/images/icon.png")));
        getDialogPane().getStylesheets().add(DomainObjectDialog.class.getResource("/css/style.css").toExternalForm());

        GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(10);

        gridPane.add(new Label(element.getURI() + "/"), 0,0);

        this.nameTextField = new TextField();
        gridPane.add(this.nameTextField, 1, 0, 2, 1);

        gridPane.add(new Label("rdf:type"),1,1);
        gridPane.add(new Label("ddo:DomainObject"),2,1);

        gridPane.add(new Label("ddo:parent"),1,2);
        gridPane.add(new Label(element.getURI()),2,2);

        gridPane.add(new Label("ddo:instanceOf"), 1,3);

        this.classesVBox = new VBox();
        this.classesVBox.setSpacing(5);
        gridPane.add(this.classesVBox, 2,4);

        MenuButton options = new MenuButton("more options");
        for (Resource resource : datasetService.getDomainObjectClassPathsOfContext(element)) {
            CheckMenuItem checkMenuItem = new CheckMenuItem("n:" + resource.getLocalName());
            Label label = new Label("n:" + resource.getLocalName());
            checkMenuItem.setOnAction(event -> {
                if (checkMenuItem.isSelected()) {
                    this.classesVBox.getChildren().add(label);
                } else {
                    this.classesVBox.getChildren().remove(label);
                }
                getDialogPane().getScene().getWindow().sizeToScene();
            });
            options.getItems().add(checkMenuItem);
        }
        gridPane.add(options, 2,3);

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

    public boolean validateInput(){
        if (this.nameTextField.getText().matches("[a-zA-Z][a-zA-Z0-9-]*")
                && !this.datasetService.getNamesUnderLevel(this.element).contains(this.nameTextField.getText())) {
            return true;
        }
        this.errorText.setText("ERROR: invalid local name entered, please try again.");
        return false;
    }

    public List<Resource> getClasses(){
        List<Resource> classes = new LinkedList<>();
        this.classesVBox.getChildren().forEach(x -> classes.add(ResourceFactory.createResource(((Label) x).getText().replaceFirst("n:", DDO.getN()))));
        return classes;
    }

    public String getName() {
        return this.nameTextField.getText();
    }
}
