package util.ui;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.SimpleSelector;
import org.topbraid.shacl.util.ModelPrinter;
import org.topbraid.shacl.vocabulary.SH;

public class ValidationDialog extends TextInputDialog {

    public ValidationDialog(Model report){
        super();

        setTitle("Domain Model Validation");
        setGraphic(null);
        setResizable(true);

        ((Stage) getDialogPane().getScene().getWindow()).getIcons().add(new Image(ValidationDialog.class.getResourceAsStream("/images/icon.png")));
        getDialogPane().getStylesheets().add(ValidationDialog.class.getResource("/css/style.css").toExternalForm());

        if (report.listStatements(new SimpleSelector(null, SH.conforms, (RDFNode) null)).next().getObject().asLiteral().getBoolean()) {
            setHeaderText("Validation successful!");
        } else {
            setHeaderText("Validation failed!");
            getDialogPane().setExpanded(true);
            getDialogPane().setMinSize(1200, 800);
        }

        TextArea textArea = new TextArea();
        textArea.setText(ModelPrinter.get().print(report));
        getDialogPane().setContent(null);
        getDialogPane().setExpandableContent(textArea);
        getDialogPane().getScene().getWindow().sizeToScene();
    }
}
