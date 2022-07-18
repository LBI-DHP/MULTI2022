# MULTI2022

## Towards Integration-Preserving Customization of Just-in-Time Adaptive Interventions with Composite Clabjects in RDF and SHACL<br/>-- Demonstration --
  
### Foundations

* RDF: https://www.w3.org/TR/rdf11-primer/
* SPARQL: https://www.w3.org/TR/sparql11-query/
* SHACL: https://www.w3.org/TR/shacl/
* SHACL Advanced Features: https://www.w3.org/TR/shacl-af/

### Prerequisites

1. (Oracle) JDK Version 18
2. In case your JDK (e.g. OpenJDK) does not come with the JavaFX SDK, you also need to download it: https://openjfx.io/

### Running

1. Double click or run the tool via console: java -jar mlm-playground.jar
2. In case you downloaded the JavaFX SDK, you have to run the tool via console:<br/>
java --module-path "\<path to your JavaFX SDK lib folder\>" --add-modules javafx.controls,javafx.fxml -jar mlm-playground-basic.jar


### MLM-Playground

By default, all examples from the paper are loaded at the start.

The main menu offers the following functionality:
1. Model
    1. Open: Open a model
    2. Save: Save current model
    3. Validate: Validation of the current model with regard to the SHACL shapes of the model
2. Edit
    1. Undo:
    2. Redo:
    3. Globals:
3. View
    1. SPARQL Query: 

![grafik](https://user-images.githubusercontent.com/26625992/179465820-5b206e90-e062-428b-be9e-8b444c89ec01.png)
