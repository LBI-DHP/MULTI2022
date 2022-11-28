package controller;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFWriter;
import service.DatasetService;
import util.AppStateUtil;
import util.SVGUtil;
import util.ui.*;
import vocabulary.DDO;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.zip.Deflater;

public class Controller {

    private Resource selectedElement;
    private DatasetService datasetService;

    private SortedSet<String> sortedElements;
    private ContextMenu autoCompleteContextMenu;

    private Stack<Resource> backwardHistory, forwardHistory;
    private Stack<AppStateUtil> undoHistory, redoHistory;

    @FXML
    private ProgressIndicator synchronizationIndicator;
    @FXML
    private BorderPane borderPane;
    @FXML
    private TreeView<String> treeView;
    @FXML
    private TextField explorerTextField;
    @FXML
    private Label domainModelLabel;
    @FXML
    private Menu derivedModelSizeMenu;
    @FXML
    private TextFlow viewTextFlow;
    @FXML
    private TextArea assertionsTextArea;
    @FXML
    private ComboBox<String> selectedViewComboBox;
    @FXML
    private Button forwardButton, backwardButton, deleteElementButton;
    @FXML
    private MenuItem undoMenuItem, redoMenuItem, addComponentClassMenuItem, addSpecializationMenuItem,
            addDomainObjectMenuItem, addOccurrenceMenuItem;

    @FXML
    private void initialize() {
        this.borderPane.setBackground(new Background(new BackgroundImage(
                new Image(Objects.requireNonNull(Controller.class.getResourceAsStream("/images/lbi-background.jpg"))),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1.0, 1.0, true, true, true, false)
        )));

        this.backwardButton.setGraphic(SVGUtil.ARROW_LEFT());
        this.forwardButton.setGraphic(SVGUtil.ARROW_RIGHT());

        this.autoCompleteContextMenu = new ContextMenu();
        this.explorerTextField.textProperty().addListener(this::autoCompleteForExplorerTextField);
        this.explorerTextField.textProperty().addListener(this::selectElementByExplorerTextField);

        this.selectedViewComboBox.getItems().add("Default View");
        this.selectedViewComboBox.getItems().add("DDO-only View");
        this.selectedViewComboBox.getItems().add("SHACL-only View");
        this.selectedViewComboBox.getItems().add("Parent View");
        this.selectedViewComboBox.getItems().add("Super Classes View");
        this.selectedViewComboBox.getItems().add("Classes View");
        this.selectedViewComboBox.getItems().add("Objects View");
        this.selectedViewComboBox.valueProperty().addListener(this::selectView);

        this.datasetService = new DatasetService();
        updatedDataset();
    }

    private void updateDataset() {
        this.undoHistory.push(new AppStateUtil(this.datasetService, this.selectedElement, this.backwardHistory, this.forwardHistory));
        if (this.undoHistory.size() == 5) {
            this.undoHistory.remove(0);
        }

        this.redoHistory = new Stack<>();
    }

    private void updatedDataset() {
        this.backwardHistory = new Stack<>();
        this.forwardHistory = new Stack<>();

        this.redoHistory = new Stack<>();
        this.undoHistory = new Stack<>();

        updatedDataset(this.datasetService.getDomainModel());
    }

    private void updatedDataset(Resource element) {
        this.domainModelLabel.setText(this.datasetService.getDomainObjectURI());
        this.derivedModelSizeMenu.setText(this.datasetService.getDerivedModelSize() + " Triples");

        this.treeView.setRoot(getTreeItem(this.datasetService.getDomainModel()));
        this.treeView.getRoot().setExpanded(true);

        this.sortedElements = this.datasetService.getSortedDomainURIFragments();

        updateSelectedElement(element);
    }

    private void updateSelectedElement(Resource element) {
        if (!(this.backwardHistory.size() == 0 && element.getURI().equals(this.datasetService.getDomainObjectURI())))
            this.backwardHistory.push(this.selectedElement);
        if (this.backwardHistory.size() == 10) {
            this.backwardHistory.remove(0);
        }
        this.forwardHistory = new Stack<>();

        this.selectedElement = element;
        updatedSelectedElement(element);
    }

    private void updatedSelectedElement(Resource element) {
        this.explorerTextField.setText(this.datasetService.getURIFragment(element));
        this.explorerTextField.positionCaret(this.explorerTextField.getText().length());

        this.treeView.getRoot().getChildren().forEach(this::closeTreeItem);
        this.treeView.getSelectionModel().select(openTreeItem(this.treeView.getRoot(), element.getURI()));

        this.selectedViewComboBox.getSelectionModel().clearSelection();
        this.selectedViewComboBox.getSelectionModel().selectFirst();
        this.assertionsTextArea.setText(this.datasetService.getView(element, this.datasetService.getAssertedModelUri(), "DESCRIBE ?x WHERE { ?x ?p ?o. FILTER ( ?x = ?element || strstarts ( str ( ?x ) , concat ( str ( ?element ) , \"#\" ) ) ) }", "", false));

        this.forwardButton.setDisable(this.forwardHistory.isEmpty());
        this.backwardButton.setDisable(this.backwardHistory.isEmpty());
        this.undoMenuItem.setDisable(this.undoHistory.isEmpty());
        this.redoMenuItem.setDisable(this.redoHistory.isEmpty());

        List<Resource> linguisticTypes = this.datasetService.getLinguisticTypes(element);
        this.addComponentClassMenuItem.setDisable(!linguisticTypes.contains(DDO.Clabject));
        this.addSpecializationMenuItem.setDisable(!linguisticTypes.contains(DDO.Clabject)
                || linguisticTypes.contains(DDO.ConcreteClass) || this.datasetService.hasConcreteLeafs(element));
        this.addDomainObjectMenuItem.setDisable(!linguisticTypes.contains(DDO.DomainObject));

        this.addOccurrenceMenuItem.setDisable(!linguisticTypes.contains(DDO.Individual)
                || (linguisticTypes.contains(DDO.Individual) && this.datasetService.getOccurrenceClassPathsOfContext(this.selectedElement).size() == 0));
        this.deleteElementButton.setDisable(this.selectedElement.getURI().equals(this.datasetService.getDomainObjectURI())
                || !(linguisticTypes.contains(DDO.Individual) || linguisticTypes.contains(DDO.ModeledClass)));
    }

    private TreeItem<String> getTreeItem(Resource resource) {
        return getTreeItems(Arrays.asList(resource)).get(0);
    }

    private List<TreeItem<String>> getTreeItems(List<Resource> elements) {
        return getTreeItems(elements, 0);
    }

    private List<TreeItem<String>> getTreeItems(List<Resource> elements, int specializationDepth) {
        List<TreeItem<String>> treeItems = new LinkedList<>();
        elements.sort(Comparator.comparing(Resource::getLocalName));

        AtomicReference<String> specializationDepthIndicator = new AtomicReference<>("");
        IntStream.range(0, specializationDepth).forEach(x -> specializationDepthIndicator.set(specializationDepthIndicator + ">"));

        for (Resource element : elements) {
            TreeItem<String> treeItem = element.getURI().startsWith(this.datasetService.getBaseURI()) ?
                    new TreeItem<>(specializationDepthIndicator + element.getLocalName())
                    : new TreeItem<>(specializationDepthIndicator + element.getURI());
            treeItems.add(treeItem);

            List<Resource> linguisticTypes = this.datasetService.getLinguisticTypes(element);
            if (linguisticTypes.contains(DDO.Individual)) {
                treeItem.setGraphic(SVGUtil.FILE_OUTLINE());
            } else if (linguisticTypes.contains(DDO.Clabject)) {
                treeItem.setGraphic(SVGUtil.FOLDER_OUTLINE());
            } else if (linguisticTypes.contains(DDO.MetaClass)) {
                treeItem.setGraphic(SVGUtil.FOLDER_MULTIPLE_OUTLINE());
            }

            if (linguisticTypes.contains(DDO.DomainObject)) {
                Resource singletonClass = this.datasetService.getSingletonClass(element);
                if (singletonClass != null) treeItem.getChildren().add(getTreeItem(singletonClass));
            }

            if (linguisticTypes.contains(DDO.Clabject)) {
                Resource metaClass = this.datasetService.getMetaClass(element);
                if (metaClass != null) treeItem.getChildren().add(getTreeItem(metaClass));

                treeItems.addAll(getTreeItems(this.datasetService.getDirectIntraContextSubClasses(element), specializationDepth + 1));
            }

            treeItem.getChildren().addAll(getTreeItems(this.datasetService.getRootChildClabjectComponents(element)));
        }

        return treeItems;
    }

    private void closeTreeItem(TreeItem<String> treeItem) {
        treeItem.setExpanded(false);
        treeItem.getChildren().forEach(this::closeTreeItem);
    }

    private TreeItem<String> openTreeItem(TreeItem<String> treeItem, String path) {

        String remainingPath = treeItem.getValue().endsWith("/") ?
                path.replaceFirst(treeItem.getValue(), "")
                : path.replaceFirst(treeItem.getValue().replaceAll(">", "") + "/", "");

        String localName = remainingPath.contains("/") ?
                remainingPath.substring(0, remainingPath.indexOf("/"))
                : remainingPath;

        for (TreeItem<String> child : treeItem.getChildren()) {
            String childName = child.getValue().replaceAll(">", "");
            if (childName.equals(localName)) {
                child.setExpanded(true);
                return childName.equals(remainingPath) ? child : openTreeItem(child, remainingPath);
            }
        }

        return null;
    }

    @FXML
    private void selectModelElementByTreeView(MouseEvent mouseEvent) {
        Node node = mouseEvent.getPickResult().getIntersectedNode();
        if (node instanceof Text || (node instanceof TreeCell && ((TreeCell<?>) node).getText() != null)) {
            StringBuilder uri = new StringBuilder();
            for (TreeItem<String> treeItem = this.treeView.getSelectionModel().getSelectedItem();
                 treeItem != null; treeItem = treeItem.getParent()) {
                if (treeItem == this.treeView.getSelectionModel().getSelectedItem()) {
                    uri.insert(0, treeItem.getValue());
                } else {
                    uri.insert(0, treeItem.getValue() + "/");
                }
            }
            updatedSelectedElement(ResourceFactory.createResource(uri.toString().replaceAll(">", "")));
        }
    }

    private void selectElementByExplorerTextField(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (newValue.equals(oldValue)) return;
        Resource element = this.datasetService.getElementByFragmentURI(newValue);
        if (!this.datasetService.containsElement(element) || this.selectedElement.equals(element)) return;
        updateSelectedElement(element);
    }

    private void autoCompleteForExplorerTextField(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        Resource element = this.datasetService.getElementByFragmentURI(newValue);
        if (this.datasetService.containsElement(element)) {
            this.autoCompleteContextMenu.hide();
        } else {
            this.autoCompleteContextMenu.getItems().clear();
            List<String> subset = this.sortedElements.subSet(newValue, String.valueOf(Character.MAX_VALUE)).stream().toList();
            int maxSuggestions = Math.min(subset.size(), 10);
            for (int i = 0; i < maxSuggestions; i++) {
                MenuItem item = new MenuItem(subset.get(i));
                item.setOnAction(event -> {
                    this.autoCompleteContextMenu.hide();
                    updateSelectedElement(this.datasetService.getElementByFragmentURI(item.getText()));
                });
                this.autoCompleteContextMenu.getItems().add(item);
            }
            this.autoCompleteContextMenu.show(this.explorerTextField, Side.BOTTOM, 0, 0);
        }
    }

    private void selectView(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (newValue != null) {
            switch (newValue) {

                case "DDO-only View" -> {
                    this.viewTextFlow.getChildren().clear();
                    this.viewTextFlow.getChildren().addAll(getTextFlowOfView(
                            this.datasetService.getView(this.selectedElement,
                                    "DESCRIBE ?element",
                                    "FILTER ( strstarts(str(?p0), str(ddo:)) || strstarts(str(?p0), str(n:)) || strstarts(str(?o0), str(ddo:)) )", false)));
                }
                case "SHACL-only View" -> {
                    this.viewTextFlow.getChildren().clear();
                    this.viewTextFlow.getChildren().addAll(getTextFlowOfView(
                            this.datasetService.getView(this.selectedElement,
                                    "DESCRIBE ?element",
                                    "FILTER strstarts(str(?p0), str(sh:))", false)));
                }
                case "Parent View" -> {
                    this.viewTextFlow.getChildren().clear();
                    this.viewTextFlow.getChildren().addAll(getTextFlowOfView(
                            this.datasetService.getView(this.selectedElement,
                                    "DESCRIBE ?x WHERE { ?element ddo:parent ?x}",
                                    "", false)));
                }
                case "Super Classes View" -> {
                    this.viewTextFlow.getChildren().clear();
                    this.viewTextFlow.getChildren().addAll(getTextFlowOfView(
                            this.datasetService.getView(this.selectedElement,
                                    "DESCRIBE ?x WHERE { ?element rdfs:subClassOf+ ?x . ?x rdf:type/rdfs:subClassOf* ddo:Element . }",
                                    "", false)));
                }
                case "Objects View" -> {
                    this.viewTextFlow.getChildren().clear();
                    this.viewTextFlow.getChildren().addAll(getTextFlowOfView(
                            this.datasetService.getView(this.selectedElement,
                                    "DESCRIBE ?x WHERE { ?x rdf:type/rdfs:subClassOf* ?element }",
                                    "", false)));
                }
                case "Classes View" -> {
                    this.viewTextFlow.getChildren().clear();
                    this.viewTextFlow.getChildren().addAll(getTextFlowOfView(
                            this.datasetService.getView(this.selectedElement, """
                                            DESCRIBE ?x WHERE {
                                                { ?element rdf:type/rdfs:subClassOf* ?x . ?x rdf:type/rdfs:subClassOf* ddo:Element . }
                                                UNION
                                                { ?element rdfs:subClassOf*/^sh:targetClass ?x . FILTER NOT EXISTS { ?x rdfs:subClassOf ddo:Element } }
                                            }""",
                                    "", false)));
                }
                default -> {
                    this.viewTextFlow.getChildren().clear();
                    this.viewTextFlow.getChildren().addAll(getTextFlowOfView(
                            this.datasetService.getView(this.selectedElement,
                                    "DESCRIBE ?x WHERE { ?x ?p ?o. FILTER ( ?x = ?element || strstarts ( str ( ?x ) , concat ( str ( ?element ) , \"#\" ) ) ) }",
                                    "", false)));
                }
            }
        }
    }

    private TextFlow getTextFlowOfView(String view) {
        TextFlow textFlow = new TextFlow();
        for (String line : view.split("\\n")) {
            if (line.contains("<")) {
                while (line.length() > 0) {
                    if (line.contains("<") && line.contains(">")) {

                        String text = line.substring(0, line.indexOf("<"));
                        if (text.length() > 0) textFlow.getChildren().add(new Label(text));
                        line = line.replaceFirst(Pattern.quote(text + "<"), "");

                        if (line.contains(">")) {
                            text = line.substring(0, line.indexOf(">"));
                            if (text.equals(this.datasetService.getDomainObjectURI()) || this.datasetService.containsElement(this.datasetService.getElementByBaseURI(text))) {
                                Hyperlink link = new Hyperlink();
                                link.setText("<" + text + ">");
                                link.setTooltip(new Tooltip(link.getText()));
                                link.setOnMouseClicked(event -> {
                                    if (event.getSource() instanceof Hyperlink) {
                                        String path = ((Hyperlink) event.getSource()).getText().replaceFirst("<", "").replaceFirst(">", "");
                                        if (path.equals(this.datasetService.getDomainObjectURI())) {
                                            updateSelectedElement(this.datasetService.getDomainModel());
                                        } else {
                                            updateSelectedElement(this.datasetService.getElementByBaseURI(path));
                                        }
                                    }
                                });
                                textFlow.getChildren().add(link);
                            } else {
                                textFlow.getChildren().add(new Label("<" + text + ">"));
                            }
                            line = line.replaceFirst(text + ">", "");
                        }
                    } else {
                        textFlow.getChildren().add(new Label(line));
                        break;
                    }
                }
            } else {
                textFlow.getChildren().add(new Label(line));
            }
            textFlow.getChildren().add(new Text(System.lineSeparator()));
        }
        return textFlow;
    }

    private void performTransactionalTask(Task<Void> task) {
        task.setOnSucceeded(x -> {
            updatedDataset(this.selectedElement);
            this.synchronizationIndicator.setVisible(false);
        });

        task.setOnFailed(x -> {
            this.synchronizationIndicator.setVisible(false);
            getErrorAlert(task).showAndWait();
        });

        task.run();
    }

    private Alert getErrorAlert(Task<Void> task) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(task.getException().getMessage());
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image(Objects.requireNonNull(Controller.class.getResourceAsStream("/images/icon.png"))));
        return alert;
    }

    @FXML
    private void newDomainModel() {
        DomainModelDialog dialog = new DomainModelDialog();
        while (true) {
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (dialog.validateInput()) {
                    this.synchronizationIndicator.setVisible(true);
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() {
                            datasetService = new DatasetService(dialog.getURI());
                            return null;
                        }
                    };
                    task.setOnSucceeded(x -> {
                        updatedDataset();
                        this.synchronizationIndicator.setVisible(false);
                    });
                    task.setOnFailed(x -> {
                        this.synchronizationIndicator.setVisible(false);
                        getErrorAlert(task).showAndWait();
                    });
                    task.run();
                    break;
                }
            } else {
                break;
            }
        }
    }

    @FXML
    private void openDomainModel(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Turtle Files", "*.ttl"));
        fileChooser.setInitialDirectory(new File("./"));
        File selectedFile = fileChooser.showOpenDialog(((MenuItem) event.getTarget()).getParentPopup().getOwnerWindow());
        if (selectedFile != null) {
            this.synchronizationIndicator.setVisible(true);
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws FileNotFoundException {
                    datasetService = new DatasetService(new FileInputStream(selectedFile.getAbsolutePath()));
                    return null;
                }
            };
            task.setOnSucceeded(x -> {
                updatedDataset();
                this.synchronizationIndicator.setVisible(false);
            });
            task.setOnFailed(x -> {
                this.synchronizationIndicator.setVisible(false);
                getErrorAlert(task).showAndWait();
            });
            task.run();
        }
    }

    @FXML
    private void saveDomainModel(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Turtle Files", "*.ttl"));
        fileChooser.setInitialDirectory(new File("./"));
        File selectedFile = fileChooser.showSaveDialog(((MenuItem) event.getTarget()).getParentPopup().getOwnerWindow());
        if (selectedFile != null) {
            this.synchronizationIndicator.setVisible(true);
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws FileNotFoundException {
                    FileOutputStream fos = new FileOutputStream(selectedFile);
                    RDFWriter.create().source(datasetService.getAssertedModel()).lang(Lang.TTL).base(datasetService.getBaseURI()).output(fos);
                    return null;
                }
            };
            task.setOnSucceeded(x -> {
                this.synchronizationIndicator.setVisible(false);
            });
            task.setOnFailed(x -> {
                this.synchronizationIndicator.setVisible(false);
                getErrorAlert(task).showAndWait();
            });
            task.run();
        }
    }

    @FXML
    private void undo() {
        this.redoHistory.push(new AppStateUtil(this.datasetService, this.selectedElement, this.backwardHistory, this.forwardHistory));
        AppStateUtil appState = this.undoHistory.pop();
        this.datasetService = appState.getDatasetService();
        this.backwardHistory = appState.getBackwardHistory();
        this.forwardHistory = appState.getForwardHistory();
        updatedDataset(appState.getSelectedElement());
    }

    @FXML
    private void redo() {
        this.undoHistory.push(new AppStateUtil(this.datasetService, this.selectedElement, this.backwardHistory, this.forwardHistory));
        AppStateUtil appState = this.redoHistory.pop();
        this.datasetService = appState.getDatasetService();
        this.backwardHistory = appState.getBackwardHistory();
        this.forwardHistory = appState.getForwardHistory();
        updatedDataset(appState.getSelectedElement());
    }

    @FXML
    private void editGlobals() {
        Stage stage = new Stage();
        stage.setTitle("Deep Domain Object Generalization Hierarchies - Globals");
        stage.getIcons().add(new Image(Objects.requireNonNull(Controller.class.getResourceAsStream("/images/icon.png"))));

        TextArea textArea = new TextArea();
        textArea.setPrefSize(800, 600);
        textArea.getStyleClass().add("bordered");
        textArea.setText(this.datasetService.getView(null, """
                DESCRIBE ?x
                WHERE {
                    ?x ?y ?z .
                    FILTER NOT EXISTS { ?x rdf:type/rdfs:subClassOf* ddo:Element }
                    FILTER ( isIRI ( ?x ) )
                    FILTER ( strstarts ( str ( ?x ) , str ( ddo: ) ) = false )
                    FILTER ( ( strstarts ( str ( ?x ) , str ( <> ) ) && contains ( str ( ?x ) , "#" ) ) = false )
                }
                """, "", true));
        VBox.setVgrow(textArea, Priority.ALWAYS);

        Text errorText = new Text();
        errorText.setFill(Color.RED);

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(20, 20);
        progressIndicator.setVisible(false);

        Button saveButton = new Button("Save");
        saveButton.setOnAction(event -> {
            progressIndicator.setVisible(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(textArea.getText().getBytes(StandardCharsets.UTF_8));
                    Model model = ModelFactory.createDefaultModel();
                    RDFDataMgr.read(model, inputStream, Lang.TURTLE);
                    /*if (!datasetService.validGlobals(model))
                        throw new RuntimeException("Invalid use of ddo: and n: namespace");*/
                    updateDataset();
                    datasetService.updateGlobals(model);
                    return null;
                }
            };

            task.setOnSucceeded(x -> {
                progressIndicator.setVisible(false);
                stage.close();
                updatedDataset(this.selectedElement);
            });

            task.setOnFailed(x -> {
                progressIndicator.setVisible(false);
                getErrorAlert(task).showAndWait();
            });

            task.run();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(event -> stage.close());

        HBox hBox = new HBox(progressIndicator, saveButton, cancelButton);
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.getStyleClass().add("non-bordered");

        VBox vBox = new VBox(textArea, errorText, hBox);
        vBox.setSpacing(10);

        Scene scene = new Scene(vBox);
        scene.getStylesheets().add(Objects.requireNonNull(Controller.class.getResource("/css/style.css")).toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void validateDomainModel() {
        this.synchronizationIndicator.setVisible(true);
        final ValidationDialog[] dialog = new ValidationDialog[1];
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                dialog[0] = new ValidationDialog(datasetService.getValidationReport());
                return null;
            }
        };
        task.setOnSucceeded(x -> {
            this.synchronizationIndicator.setVisible(false);
            dialog[0].showAndWait();
        });
        task.setOnFailed(x -> {
            this.synchronizationIndicator.setVisible(false);
            getErrorAlert(task).showAndWait();
        });
        task.run();
    }

    @FXML
    private void goBackward() {
        this.forwardHistory.push(this.selectedElement);
        this.selectedElement = this.backwardHistory.pop();
        updatedSelectedElement(this.selectedElement);
    }

    @FXML
    private void goForward() {
        this.backwardHistory.push(this.selectedElement);
        this.selectedElement = this.forwardHistory.pop();
        updatedSelectedElement(this.selectedElement);
    }

    @FXML
    public void addComponentClass() {
        ComponentClassDialog dialog = new ComponentClassDialog(this.datasetService, this.selectedElement);
        while (true) {
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (dialog.validateInput()) {
                    this.synchronizationIndicator.setVisible(true);
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() {
                            updateDataset();
                            selectedElement = datasetService.addComponentClass(dialog.getName(), selectedElement, dialog.getIndividualType(), dialog.getAbstractType());
                            return null;
                        }
                    };
                    performTransactionalTask(task);
                    break;
                }
            } else {
                break;
            }
        }
    }

    @FXML
    public void addSpecialization() {
        SpecializationDialog dialog = new SpecializationDialog(this.datasetService, this.selectedElement);
        while (true) {
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (dialog.validateInput()) {
                    this.synchronizationIndicator.setVisible(true);
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() {
                            updateDataset();
                            selectedElement = datasetService.addSpecialization(dialog.getName(), datasetService.getParent(selectedElement), dialog.getAbstractType(), dialog.getSpecializations());
                            return null;
                        }
                    };
                    performTransactionalTask(task);
                    break;
                }
            } else {
                break;
            }
        }
    }

    @FXML
    public void addDomainObject() {
        DomainObjectDialog dialog = new DomainObjectDialog(this.datasetService, this.selectedElement);
        while (true) {
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (dialog.validateInput()) {
                    this.synchronizationIndicator.setVisible(true);
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() {
                            updateDataset();
                            selectedElement = datasetService.addDomainObject(dialog.getName(), selectedElement, dialog.getClasses());
                            return null;
                        }
                    };
                    performTransactionalTask(task);
                    break;
                }
            } else {
                break;
            }
        }
    }

    @FXML
    public void addOccurrence() {
        OccurrenceDialog dialog = new OccurrenceDialog(this.datasetService, this.selectedElement);
        while (true) {
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (dialog.validateInput()) {
                    this.synchronizationIndicator.setVisible(true);
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() {
                            updateDataset();
                            selectedElement = datasetService.addOccurrence(dialog.getName(), selectedElement, dialog.getInstanceOf());
                            return null;
                        }
                    };
                    performTransactionalTask(task);
                    break;
                }
            } else {
                break;
            }
        }
    }

    @FXML
    public void deleteElement() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Element");
        alert.setHeaderText("Are you sure you want to delete <" + this.selectedElement.getURI() + ">?");
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image(Objects.requireNonNull(Controller.class.getResourceAsStream("/images/icon.png"))));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().equals(ButtonType.OK)) {
            this.synchronizationIndicator.setVisible(true);
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    updateDataset();
                    selectedElement = datasetService.deleteElement(selectedElement);
                    return null;
                }
            };
            performTransactionalTask(task);
        }
    }

    @FXML
    private void executeSparqlQuery() {
        Stage stage = new Stage();
        stage.setTitle("Deep Domain Object Generalization Hierarchies - SPARQL Query");
        stage.getIcons().add(new Image(Objects.requireNonNull(Controller.class.getResourceAsStream("/images/icon.png"))));

        ParameterizedSparqlString pss = this.datasetService.preprocessQuery("""
                SELECT ?object
                WHERE {
                    ?object rdf:type/rdfs:subClassOf* ?element .
                }
                """);
        pss.setIri("element", this.datasetService.getRelativeURI(this.selectedElement));

        TextArea sparqlTextArea = new TextArea();
        sparqlTextArea.setPrefSize(600, 400);
        sparqlTextArea.getStyleClass().add("bordered");
        sparqlTextArea.setText(pss.toString());

        TextArea resultTextArea = new TextArea();
        resultTextArea.setPrefSize(600, 400);
        resultTextArea.setEditable(false);
        resultTextArea.getStyleClass().add("bordered");

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().add(sparqlTextArea);
        splitPane.getItems().add(resultTextArea);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(20, 20);
        progressIndicator.setVisible(false);

        Button queryButton = new Button("Query");
        final String[] queryResult = {""};
        queryButton.setOnAction(event -> {
            progressIndicator.setVisible(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    queryResult[0] = datasetService.getResultOfQuery(sparqlTextArea.getText());
                    return null;
                }
            };

            task.setOnSucceeded(x -> {
                resultTextArea.setStyle("");
                resultTextArea.setText(queryResult[0]);
                progressIndicator.setVisible(false);
            });

            task.setOnFailed(x -> {
                resultTextArea.setStyle("-fx-text-fill: red");
                resultTextArea.setText("ERROR:\n" + task.getException().getMessage());
                progressIndicator.setVisible(false);
            });

            task.run();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(event -> stage.close());

        HBox hBox = new HBox(progressIndicator, queryButton, cancelButton);
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.getStyleClass().add("non-bordered");

        VBox vBox = new VBox(splitPane, hBox);
        vBox.setSpacing(10);

        Scene scene = new Scene(vBox);
        scene.getStylesheets().add(Objects.requireNonNull(Controller.class.getResource("/css/style.css")).toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void synchronizeAssertions() {
        this.synchronizationIndicator.setVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ByteArrayInputStream prefixIS = new ByteArrayInputStream(datasetService.getPrefixes().getBytes(StandardCharsets.UTF_8));
                ByteArrayInputStream triplesIS = new ByteArrayInputStream(assertionsTextArea.getText().getBytes(StandardCharsets.UTF_8));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                prefixIS.transferTo(baos);
                triplesIS.transferTo(baos);

                Model newAssertions = ModelFactory.createDefaultModel();
                RDFDataMgr.read(newAssertions, new ByteArrayInputStream(baos.toByteArray()), Lang.TURTLE);

                updateDataset();
                datasetService.addAssertions(selectedElement, newAssertions);
                return null;
            }
        };

        performTransactionalTask(task);
    }

    @FXML
    private void showLinguisticModel() throws IOException {
        this.synchronizationIndicator.setVisible(true);
        final String[] mxGraph = {""};
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws IOException {
                mxGraph[0] = new String(Objects.requireNonNull(Controller.class.getResourceAsStream("/images/linguistic-model.xml")).readAllBytes());
                return null;
            }
        };
        task.setOnSucceeded(x -> {
            this.synchronizationIndicator.setVisible(false);
            showDiagram(mxGraph[0]);
        });
        task.setOnFailed(x -> {
            this.synchronizationIndicator.setVisible(false);
            getErrorAlert(task).showAndWait();
        });
        task.run();
    }

    @FXML
    private void showDiagram() {
        this.synchronizationIndicator.setVisible(true);
        final String[] mxGraph = {""};
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                mxGraph[0] = """
                        <mxGraphModel dx="1102" dy="875" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169" math="0" shadow="0">
                          <root>
                            <mxCell value="LABEL" style="rounded=0;whiteSpace=wrap;html=1;sketch=1;" vertex="1" >
                              <mxGeometry x="40" y="40" width="300" height="40" as="geometry" />
                            </mxCell>
                          </root>
                        </mxGraphModel>
                        """.replaceAll("LABEL", selectedElement.getURI());
                return null;
            }
        };
        task.setOnSucceeded(x -> {
            this.synchronizationIndicator.setVisible(false);
            showDiagram(mxGraph[0]);
        });
        task.setOnFailed(x -> {
            this.synchronizationIndicator.setVisible(false);
            getErrorAlert(task).showAndWait();
        });
        task.run();
    }

    private void showDiagram(String mxGraphModel) {
        WebView webView = new WebView();

        webView.getEngine().setCreatePopupHandler(p -> {
            Stage stage = new Stage();
            WebView popup = new WebView();
            stage.setScene(new Scene(popup));
            stage.setTitle("diagrams.net");
            stage.getIcons().add(new Image(Objects.requireNonNull(Controller.class.getResourceAsStream("/images/icon.png"))));
            stage.show();
            return popup.getEngine();
        });

        try {
            String mxGraphModelEncoded = URLEncoder.encode(mxGraphModel, StandardCharsets.UTF_8);
            mxGraphModelEncoded = mxGraphModelEncoded.replace("+", "%20");
            byte[] mxGraphModelEncodedBytes = mxGraphModelEncoded.getBytes(StandardCharsets.UTF_8);

            Deflater deflater = new Deflater(9, true);
            deflater.setInput(mxGraphModelEncodedBytes);
            deflater.finish();
            deflater.deflate(mxGraphModelEncodedBytes);

            String dataMxGraph = "{&quot;highlight&quot;:&quot;#0000ff&quot;,&quot;nav&quot;:true,&quot;resize&quot;:true,&quot;edit&quot;:&quot;_blank&quot;,&quot;xml&quot;:&quot;&lt;mxfile &gt;&lt;diagram &gt;<mxgraph>&lt;/diagram&gt;&lt;/mxfile&gt;&quot;}";
            dataMxGraph = dataMxGraph.replace("<mxgraph>", Base64.getEncoder().encodeToString(mxGraphModelEncodedBytes));

            webView.getEngine().loadContent("""
                    <html>
                        <head>
                        </head>
                        <body>
                            <div id="container" class="mxgraph" style="max-width:100%;border:1px solid transparent;" data-mxgraph="<data-mxgraph>"/>
                            <script type="text/javascript" src="https://viewer.diagrams.net/js/viewer-static.min.js"></script>
                        </body>
                    </html>
                    """.replaceAll("<data-mxgraph>", dataMxGraph));

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("diagrams.net Plugin");
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.setResizable(true);
        alert.getDialogPane().setContent(webView);
        alert.getDialogPane().getScene().getWindow().sizeToScene();
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image(Objects.requireNonNull(Controller.class.getResourceAsStream("/images/icon.png"))));
        alert.showAndWait();
    }
}
