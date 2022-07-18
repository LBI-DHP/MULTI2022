# MULTI2022

<h2 align="center">Towards Integration-Preserving Customization of Just-in-Time Adaptive Interventions with Composite Clabjects in RDF and SHACL</h2>
<h3 align="center">-- Demonstration --</h3>
  
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

![github](https://user-images.githubusercontent.com/26625992/179472082-fd3b9168-412c-472c-816e-e109501ffda8.jpg)

The <em>main menu</em> offers the following functionality:
1. Model
    1. Open: Open a model
    2. Save: Save current model
    3. Validate: Validation of the current model with regard to the SHACL shapes of the model
2. Edit
    1. Undo: Undo the last assertion (up to 5 times).
    2. Redo: Redo the last undone assertion.
    3. Globals: View and maintain things outside the hierarchy.
4. View
    1. SPARQL Query: Perform a SPARQL query.

The <em>search field</em> allows navigation within the hierarchy with auto complete support.

The <em>tree view</em> also allows for navigation within the hierarchy, with clabjects being represented by their local name and specialization indicated by ">". Objects are represented by a file symbol, classes by folders and metaclasses by double folders.

Using the arrows, you can navigate <em>back and forth</em> (up to 10 times).

The <em>model size</em> is displayed as the number of triples.

The <em>derived clabject description</em> displays (not modifiable) asserted and derived facts of the current selected clabject. The blue coloured things are resources within the model that can be navigated to by clicking on them. For this demonstration, properties of the ddo: and n: namespaces are irrelevant, however, you may want to use them for navigation.

The <em>asserted clabject description</em> displays (modifiable) asserted facts of the current selected clabject. You can add additional facts about the clabject by asserting triples and clicking the <em>synchronization</em> button. Do not change properties of the ddo: and n: namespaces!

The difference between the <em>derived clabject description</em> and the <em>asserted clabject description</em> can be seen in the above figure by the fact that the derived SHACL property shape is shown in the former but not in the latter.

### Examples

By default, all examples from the paper are loaded into the model. We also provide the standard version and an empty version (without SHACL shapes) as files that can be loaded by the tool for experimentation.
