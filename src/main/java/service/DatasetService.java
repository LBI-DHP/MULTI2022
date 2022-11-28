package service;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.rules.RuleUtil;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.shacl.vocabulary.SH;
import vocabulary.DDO;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DatasetService {

    private static final String DATASET_SERVICE_URL = "http://lbg.dhp.ac.at/ddo/dataset-service";
    private static final String LINGUISTIC_MODEL_VOCABULARY_URI = DATASET_SERVICE_URL + "/linguistic-model-vocabulary";
    private static final String LINGUISTIC_MODEL_RULES_URI = DATASET_SERVICE_URL + "/linguistic-model-rules";
    private static final String ASSERTED_MODEL_URI = DATASET_SERVICE_URL + "/asserted";
    private static final String DERIVED_MODEL_URI = DATASET_SERVICE_URL + "/derived";

    private Resource domainModel;
    private String baseURI;

    private final Dataset dataset;

    public DatasetService() {
        this.dataset = DatasetFactory.create();

        Model temp = ModelFactory.createDefaultModel();
        RDFDataMgr.read(temp, Objects.requireNonNull(DatasetService.class.getResourceAsStream("/core/linguistic-model-vocabulary.ttl")), Lang.TURTLE);
        this.dataset.addNamedModel(LINGUISTIC_MODEL_VOCABULARY_URI, temp);

        this.dataset.getPrefixMapping().setNsPrefixes(temp.getNsPrefixMap());

        temp = ModelFactory.createDefaultModel();
        RDFDataMgr.read(temp, Objects.requireNonNull(DatasetService.class.getResourceAsStream("/core/linguistic-model-rules.ttl")), Lang.TURTLE);
        this.dataset.addNamedModel(LINGUISTIC_MODEL_RULES_URI, temp);

        temp = ModelFactory.createDefaultModel();
        RDFDataMgr.read(temp, Objects.requireNonNull(DatasetService.class.getResourceAsStream("/data/model-full.ttl")), Lang.TURTLE);
        this.dataset.addNamedModel(ASSERTED_MODEL_URI, temp);

        this.dataset.getPrefixMapping().setNsPrefixes(temp.getNsPrefixMap());

        synchronization();

        this.domainModel = getDomainModel();
        this.baseURI = this.domainModel.getURI() + "/";

        updatePrefixes(this.dataset.getPrefixMapping().getNsPrefixMap());

        synchronization();
    }

    public DatasetService(InputStream inputStream) {
        this.dataset = DatasetFactory.create();

        Model temp = ModelFactory.createDefaultModel();
        RDFDataMgr.read(temp, Objects.requireNonNull(DatasetService.class.getResourceAsStream("/core/linguistic-model-vocabulary.ttl")), Lang.TURTLE);
        this.dataset.addNamedModel(LINGUISTIC_MODEL_VOCABULARY_URI, temp);

        this.dataset.getPrefixMapping().setNsPrefixes(temp.getNsPrefixMap());

        temp = ModelFactory.createDefaultModel();
        RDFDataMgr.read(temp, Objects.requireNonNull(DatasetService.class.getResourceAsStream("/core/linguistic-model-rules.ttl")), Lang.TURTLE);
        this.dataset.addNamedModel(LINGUISTIC_MODEL_RULES_URI, temp);

        temp = ModelFactory.createDefaultModel();
        RDFDataMgr.read(temp, Objects.requireNonNull(inputStream), Lang.TURTLE);
        this.dataset.addNamedModel(ASSERTED_MODEL_URI, temp);

        synchronization();

        this.domainModel = getDomainModel();
        this.baseURI = this.domainModel.getURI() + "/";

        updatePrefixes(this.dataset.getPrefixMapping().getNsPrefixMap());

        synchronization();
    }

    public DatasetService(String URI) {
        this.dataset = DatasetFactory.create();

        Model temp = ModelFactory.createDefaultModel();
        RDFDataMgr.read(temp, Objects.requireNonNull(DatasetService.class.getResourceAsStream("/core/linguistic-model-vocabulary.ttl")), Lang.TURTLE);
        this.dataset.addNamedModel(LINGUISTIC_MODEL_VOCABULARY_URI, temp);

        this.dataset.getPrefixMapping().setNsPrefixes(temp.getNsPrefixMap());

        temp = ModelFactory.createDefaultModel();
        RDFDataMgr.read(temp, Objects.requireNonNull(DatasetService.class.getResourceAsStream("/core/linguistic-model-rules.ttl")), Lang.TURTLE);
        this.dataset.addNamedModel(LINGUISTIC_MODEL_RULES_URI, temp);

        temp = ModelFactory.createDefaultModel();
        this.domainModel = ResourceFactory.createResource(URI);
        temp.add(this.domainModel, RDF.type, DDO.DomainObject);
        this.dataset.addNamedModel(ASSERTED_MODEL_URI, temp);

        this.baseURI = URI + "/";

        updatePrefixes(this.dataset.getPrefixMapping().getNsPrefixMap());

        synchronization();
    }

    public DatasetService(Dataset dataset) {
        this.dataset = DatasetFactory.create();
        this.dataset.addNamedModel(LINGUISTIC_MODEL_VOCABULARY_URI, dataset.getNamedModel(LINGUISTIC_MODEL_VOCABULARY_URI));
        this.dataset.addNamedModel(LINGUISTIC_MODEL_RULES_URI, dataset.getNamedModel(LINGUISTIC_MODEL_RULES_URI));
        this.dataset.addNamedModel(ASSERTED_MODEL_URI, dataset.getNamedModel(ASSERTED_MODEL_URI));
        this.dataset.addNamedModel(DERIVED_MODEL_URI, dataset.getNamedModel(DERIVED_MODEL_URI));

        this.dataset.getPrefixMapping().setNsPrefixes(dataset.getPrefixMapping());

        this.domainModel = getDomainModel();
        this.baseURI = this.domainModel.getURI() + "/";
    }

    public ParameterizedSparqlString preprocessQuery(String queryString) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString(queryString);
        pss.setNsPrefixes(this.dataset.getPrefixMapping());
        pss.setBaseUri(this.baseURI);
        return pss;
    }

    private Model applyRules(Model dataGraph, Model shapesGraph) {
        UpdateRequest deactivateRules = UpdateFactory.create(preprocessQuery("""
                DELETE { ?x sh:deactivated ?y }
                INSERT { ?x sh:deactivated true }
                WHERE {
                    ?element sh:rule ?x .
                    OPTIONAL { ?x sh:deactivated ?y }
                }""").toString());

        UpdateRequest deactivateNonForwardChainingRules = UpdateFactory.create(preprocessQuery("""
                DELETE { ?x sh:deactivated ?y }
                INSERT { ?x sh:deactivated true }
                WHERE {
                    ?element sh:rule ?x .
                    FILTER NOT EXISTS { ?x ddo:forwardChaining true }
                    OPTIONAL { ?x sh:deactivated ?y }
                }""").toString());

        UpdateRequest activateNonOrderedRules = UpdateFactory.create(preprocessQuery("""
                DELETE { ?x sh:deactivated ?y }
                INSERT { ?x sh:deactivated false }
                WHERE {
                    ?element sh:rule ?x .
                    ?x sh:deactivated ?y .
                    FILTER NOT EXISTS { ?x ddo:order ?order }
                }""").toString());

        long previousSize;

        UpdateAction.execute(deactivateRules, shapesGraph);
        UpdateAction.execute(activateNonOrderedRules, shapesGraph);
        RuleUtil.executeRules(dataGraph, shapesGraph, dataGraph, null);

        UpdateAction.execute(deactivateNonForwardChainingRules, shapesGraph);
        do {
            previousSize = dataGraph.size();
            RuleUtil.executeRules(dataGraph, shapesGraph, dataGraph, null);
        } while (previousSize < dataGraph.size());
        UpdateAction.execute(deactivateRules, shapesGraph);

        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT DISTINCT ?x WHERE { ?element sh:rule/ddo:order ?x . FILTER isNumeric(?x) } ORDER BY ASC(?x)""");

        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), shapesGraph)) {
            ResultSet resultSet = qexec.execSelect();
            ParameterizedSparqlString activateRulesOfOrderPSS = preprocessQuery("""
                    DELETE { ?x sh:deactivated ?y }
                    INSERT { ?x sh:deactivated false }
                    WHERE {
                        ?element sh:rule ?x .
                        ?x ddo:order ?order ; sh:deactivated ?y .
                    }""");

            while (resultSet.hasNext()) {
                activateRulesOfOrderPSS.setLiteral("order", resultSet.nextSolution().get("x").asLiteral().getInt());
                UpdateRequest activateOrderedRules = UpdateFactory.create(activateRulesOfOrderPSS.toString());
                UpdateAction.execute(activateOrderedRules, shapesGraph);
                RuleUtil.executeRules(dataGraph, shapesGraph, dataGraph, null);

                UpdateAction.execute(deactivateNonForwardChainingRules, shapesGraph);
                do {
                    previousSize = dataGraph.size();
                    RuleUtil.executeRules(dataGraph, shapesGraph, dataGraph, null);
                } while (previousSize < dataGraph.size());
                UpdateAction.execute(deactivateRules, shapesGraph);
            }
        }

        UpdateRequest request = UpdateFactory.create(
                preprocessQuery("""
                        DELETE { ?x sh:deactivated ?y }
                        WHERE { ?element sh:rule ?x . ?x sh:deactivated ?y }"""
                ).toString());
        UpdateAction.execute(request, shapesGraph);

        return dataGraph;
    }

    private void synchronization() {
        Model assertedModel = applyRules(this.dataset.getNamedModel(ASSERTED_MODEL_URI)
                .union(this.dataset.getNamedModel(LINGUISTIC_MODEL_VOCABULARY_URI)), this.dataset.getNamedModel(LINGUISTIC_MODEL_RULES_URI));

        assertedModel.remove(this.dataset.getNamedModel(LINGUISTIC_MODEL_VOCABULARY_URI));
        this.dataset.replaceNamedModel(ASSERTED_MODEL_URI, assertedModel);

        Model derivedModel = applyRules(this.dataset.getNamedModel(ASSERTED_MODEL_URI)
                        .union(this.dataset.getNamedModel(LINGUISTIC_MODEL_VOCABULARY_URI)),
                this.dataset.getNamedModel(ASSERTED_MODEL_URI).union(this.dataset.getNamedModel(LINGUISTIC_MODEL_VOCABULARY_URI)));

        derivedModel.remove(this.dataset.getNamedModel(LINGUISTIC_MODEL_VOCABULARY_URI));
        this.dataset.replaceNamedModel(DERIVED_MODEL_URI, derivedModel);
    }

    private void print(Model model) {
        RDFWriter.create().lang(Lang.TTL).base(this.baseURI).source(model).output(System.out);
    }

    public Dataset getDataset() {
        return this.dataset;
    }

    public String getAssertedModelUri() {
        return ASSERTED_MODEL_URI;
    }

    public String getPrefixes() {
        Model prefixes = ModelFactory.createDefaultModel();
        prefixes.setNsPrefixes(this.dataset.getPrefixMapping());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RDFWriter.create().lang(Lang.TTL).base(this.baseURI).source(prefixes).output(baos);
        return baos.toString();
    }

    private void updatePrefixes(Map<String, String> prefixes) {
        Resource prefixDeclarations = ResourceFactory.createResource(baseURI + "PrefixDeclarations");

        ParameterizedSparqlString pss = preprocessQuery("""
                DESCRIBE ?prefixDeclarations
                """);
        pss.setIri("prefixDeclarations", prefixDeclarations.getURI());

        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), this.dataset.getNamedModel(ASSERTED_MODEL_URI))) {
            this.dataset.getNamedModel(ASSERTED_MODEL_URI).remove(qexec.execDescribe());
        }

        this.dataset.getPrefixMapping().clearNsPrefixMap();
        this.dataset.getPrefixMapping().setNsPrefixes(prefixes);
        this.dataset.getPrefixMapping().setNsPrefix("ddo", DDO.getURI());
        this.dataset.getPrefixMapping().setNsPrefix("n", DDO.getN());

        for (Map.Entry<String, String> entry : this.dataset.getPrefixMapping().getNsPrefixMap().entrySet()) {
            Resource declaration = ResourceFactory.createResource();
            this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(prefixDeclarations, SH.declare, declaration);
            this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(declaration, SH.prefix, entry.getKey());
            this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(declaration, SH.namespace, entry.getValue());
        }

        Resource declaration = ResourceFactory.createResource();
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(prefixDeclarations, SH.declare, declaration);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(declaration, SH.prefix, "");
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(declaration, SH.namespace, this.baseURI);
    }

    public boolean validGlobals(Model model) {
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT ?s WHERE {
                    ?s ?p ?o .
                    FILTER NOT EXISTS { ?s sh:targetClass ?o }
                    FILTER (
                       ( strstarts ( str ( ?s ) , str ( ddo: ) ) )
                       || ( strstarts ( str ( ?s ) , str ( n: ) ) )
                       || ( ?p IN ( ddo:parent , ddo:instanceOf , ddo:specializationOf , ddo:name , ddo:leafs ) )
                       || ( strstarts ( str ( ?p ) , str ( n: ) ) = true )
                       || ( strstarts ( str ( ?o ) , str ( n: ) ) = true )
                       || ( strstarts ( str ( ?o ) , str ( ddo: ) ) = true )
                    )
                }
                """);
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), model)) {
            ResultSet resultSet = qexec.execSelect();
            return !resultSet.hasNext();
        }
    }

    public void updateGlobals(Model model) {
        updatePrefixes(model.getNsPrefixMap());

        ParameterizedSparqlString pss = preprocessQuery("""
                DESCRIBE ?x
                WHERE {
                    ?x ?y ?z .
                    FILTER NOT EXISTS { ?x rdf:type/rdfs:subClassOf* ddo:Element }
                    FILTER ( isIRI ( ?x ) )
                    FILTER ( strstarts ( str ( ?x ) , str ( ddo: ) ) = false )
                }
                """);
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), this.dataset.getUnionModel())) {
            Model temp = qexec.execDescribe();
            this.dataset.getNamedModel(ASSERTED_MODEL_URI).remove(temp);
        }

        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(model);
        synchronization();
    }

    public boolean containsElement(Resource element) {
        ParameterizedSparqlString pss = preprocessQuery("""
                ASK FROM ?derivedModel FROM ?linguisticModel { ?element rdf:type/rdfs:subClassOf* ddo:Element }
                """);
        pss.setIri("derivedModel", DERIVED_MODEL_URI);
        pss.setIri("linguisticModel", LINGUISTIC_MODEL_RULES_URI);
        pss.setIri("element", element.getURI());
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), this.dataset)) {
            return qexec.execAsk();
        }
    }

    public boolean hasConcreteLeafs(Resource element) {
        ParameterizedSparqlString pss = preprocessQuery("""
                ASK FROM ?derivedModel { ?element ddo:modeledClass*/ddo:leafs ddo:ConcreteClass }
                """);
        pss.setIri("derivedModel", DERIVED_MODEL_URI);
        pss.setIri("element", element.getURI());
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), this.dataset)) {
            return qexec.execAsk();
        }
    }

    public void addAssertions(Resource element, Model newAssertions) {
        Model backup = ModelFactory.createDefaultModel();
        backup.add(this.dataset.getNamedModel(ASSERTED_MODEL_URI));

        /*
        UpdateRequest delete = UpdateFactory.create(preprocessQuery("""
                DELETE { ?s ?p ?o }
                WHERE {
                    ?s ?p ?o .
                    FILTER (
                        ( ?p IN ( ddo:parent , ddo:instanceOf , ddo:specializationOf , ddo:name , ddo:leafs ) )
                        || ( strstarts ( str(?p), str(n:) ) = true )
                        || ( strstarts ( str(?o), str(n:) ) = true )
                        #|| ( strstarts ( str(?o), str(ddo:) ) = true )
                    )
                }
                """).toString());
         */

        ParameterizedSparqlString pss = preprocessQuery("DESCRIBE ?x WHERE { ?x ?p ?o. FILTER ( ?x = ?element || strstarts ( str ( ?x ) , concat ( str ( ?element ) , \"#\" ) ) ) }");
        pss.setIri("element", element.getURI());
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), this.dataset.getNamedModel(ASSERTED_MODEL_URI))) {
            Model assertions = qexec.execDescribe();
            //UpdateAction.execute(delete, assertions);
            this.dataset.getNamedModel(ASSERTED_MODEL_URI).remove(assertions);
        }

        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), newAssertions)) {
            newAssertions = qexec.execDescribe();
            //UpdateAction.execute(delete, newAssertions);
            this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(newAssertions);
        }

        updatePrefixes(newAssertions.getNsPrefixMap());

        try {
            synchronization();
        } catch (Exception ex) {
            this.dataset.replaceNamedModel(ASSERTED_MODEL_URI, backup);
            synchronization();
            throw new RuntimeException(ex.getMessage());
        }
    }

    public Resource addComponentClass(String name, Resource parent, Resource individualType, Resource abstractType) {
        Resource component = ResourceFactory.createResource(parent.getURI() + "/" + name);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(component, RDF.type, DDO.ModeledClass);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(component, RDF.type, individualType);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(component, DDO.parent, parent);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(component, DDO.name, name);
        if (abstractType != null)
            this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(component, DDO.leafs, abstractType);
        synchronization();
        return component;
    }

    public Resource addSpecialization(String name, Resource parent, Resource abstractType, List<Resource> specializations) {
        Resource specialization = ResourceFactory.createResource(parent.getURI() + "/" + name);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(specialization, RDF.type, DDO.ModeledClass);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(specialization, DDO.parent, parent);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(specialization, DDO.name, name);
        if (abstractType != null)
            this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(specialization, DDO.leafs, abstractType);
        specializations.forEach(x -> this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(specialization, DDO.specializationOf, x));
        synchronization();
        return specialization;
    }

    public Resource addDomainObject(String name, Resource parent, List<Resource> classes) {
        Resource domainObject = ResourceFactory.createResource(parent.getURI() + "/" + name);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(domainObject, RDF.type, DDO.DomainObject);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(domainObject, DDO.parent, parent);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(domainObject, DDO.name, name);
        classes.forEach(x -> this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(domainObject, DDO.instanceOf, x));
        synchronization();
        return domainObject;
    }

    public Resource addOccurrence(String name, Resource parent, Resource instanceOf) {
        Resource occurrence = ResourceFactory.createResource(parent.getURI() + "/" + name);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(occurrence, RDF.type, DDO.Occurrence);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(occurrence, DDO.parent, parent);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(occurrence, DDO.instanceOf, instanceOf);
        this.dataset.getNamedModel(ASSERTED_MODEL_URI).add(occurrence, DDO.name, name);
        synchronization();
        return occurrence;
    }

    public Resource deleteElement(Resource element) {
        Resource parent = getParent(element);
        UpdateRequest request = UpdateFactory.create();
        ParameterizedSparqlString pss = preprocessQuery("""
                DELETE { ?s ?p ?o . ?x ?y ?s . }
                WHERE {
                    ?s ?p ?o .
                    ?s (rdf:type|rdfs:subClassOf|ddo:parent)* ?element .
                    ?x ?y ?s .
                }
                """);
        pss.setIri("element", element.getURI());
        request.add(pss.toString());
        UpdateAction.execute(request, this.dataset.getNamedModel(ASSERTED_MODEL_URI));
        synchronization();
        return parent;
    }

    public String getBaseURI() {
        return this.baseURI;
    }

    public String getDomainObjectURI() {
        return this.domainModel.getURI();
    }

    public String getResultOfQuery(String theQuery) {
        ParameterizedSparqlString pss = preprocessQuery(theQuery);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), this.dataset.getUnionModel())) {
            if (pss.asQuery().isConstructType()) {
                org.apache.jena.rdf.model.Model results = qexec.execConstruct();
                RDFWriter.create()
                        .base(this.baseURI)
                        .lang(Lang.TURTLE)
                        .source(results)
                        .output(baos);
            } else if (pss.asQuery().isSelectType()) {
                ResultSet results = qexec.execSelect();
                ResultSetFormatter.out(baos, results, pss.asQuery());
            } else if (pss.asQuery().isAskType()) {
                try {
                    if (qexec.execAsk())
                        baos.write("YES".getBytes());
                    else
                        baos.write("NO".getBytes());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (pss.asQuery().isDescribeType()) {
                org.apache.jena.rdf.model.Model results = qexec.execDescribe();
                RDFWriter.create()
                        .base(this.baseURI)
                        .lang(Lang.TURTLE)
                        .source(results)
                        .output(baos);
            }
        }
        return baos.toString();
    }

    public String getURIFragment(Resource element) {
        return element.getURI().replaceFirst(this.domainModel.getURI(), "");
    }

    public String getRelativeURI(Resource element) {
        return element.getURI().replaceFirst(this.baseURI, "");
    }

    public String getView(Resource element, String namedModel, String describe, String filter, boolean showPrefixes) {
        return getView(element, this.dataset.getNamedModel(namedModel), describe, filter, showPrefixes);
    }

    public String getView(Resource element, String describe, String filter, boolean showPrefixes) {
        return getView(element, this.dataset.getUnionModel(), describe, filter, showPrefixes);
    }

    private String getView(Resource element, Model model, String describe, String filter, boolean showPrefixes) {
        ParameterizedSparqlString pss = preprocessQuery(describe);
        if (element != null) pss.setIri("element", element.getURI());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), model)) {
            Model description = qexec.execDescribe();

            String constructs = "";
            String optionals = "";
            String finishOptionals = "";

            AtomicInteger blankNodes = new AtomicInteger(0);
            description.listSubjects().toList().forEach(x -> {
                if (x.isAnon()) blankNodes.getAndIncrement();
            });
            for (int i = 1; i <= blankNodes.get() + 1; i++) {
                constructs = constructs + "?o" + (i - 1) + " ?p" + i + " ?o" + i + " . ";
                optionals = optionals + " OPTIONAL { ?o" + (i - 1) + " ?p" + i + " ?o" + i + " . ";
                if (i == 1) optionals = optionals + " FILTER ( ?s0 != ?o0 ) ";
                finishOptionals = finishOptionals + "} ";
            }
            optionals = optionals + finishOptionals;

            ParameterizedSparqlString pss2 = preprocessQuery(
                    "CONSTRUCT { ?s0 ?p0 ?o0 . " + constructs + " } WHERE { ?s0 ?p0 ?o0 . FILTER (isBlank(?s0) = false) " + filter + optionals + " }");

            try (QueryExecution qexec2 = QueryExecutionFactory.create(pss2.asQuery(), description)) {
                Model constructed = qexec2.execConstruct();
                RDFWriter.create()
                        .base(getBaseURI())
                        .lang(Lang.TURTLE)
                        .source(constructed)
                        .output(baos);
            }
        }
        String view = baos.toString();

        if (!showPrefixes) {
            String temp = "";
            for (String line : view.split("\\n")) {
                if (!(line.startsWith("@prefix") || line.startsWith("@base"))) {
                    temp = temp + line + "\n";
                }
            }
            view = temp.replaceFirst("\\n", "");
        }

        //view = view.replaceAll("\"", "\"\"\"");
        //view = view.replaceAll("\\\\r\\\\n", "\n\t\t");
        //view = view.replaceAll("\\\\n", "\n");
        return view;
    }

    public long getDerivedModelSize() {
        return this.dataset.getNamedModel(DERIVED_MODEL_URI).size();
    }

    public Model getAssertedModel() {
        return ModelFactory.createDefaultModel().add(this.dataset.getNamedModel(ASSERTED_MODEL_URI));
    }

    public Model getValidationReport() {
        return ValidationUtil.validateModel(
                this.dataset.getNamedModel(DERIVED_MODEL_URI),
                this.dataset.getNamedModel(DERIVED_MODEL_URI).union(this.dataset.getNamedModel(LINGUISTIC_MODEL_RULES_URI)),
                false).getModel();
    }

    public Resource getDomainModel() {
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT ?x
                WHERE {
                    ?x rdf:type ddo:DomainObject .
                    FILTER NOT EXISTS { ?x ddo:parent ?y }
                }
                """);
        try (QueryExecution queryExecution = QueryExecutionFactory.create(pss.asQuery(), this.dataset.getNamedModel(DERIVED_MODEL_URI))) {
            ResultSet resultSet = queryExecution.execSelect();
            if (!resultSet.hasNext()) throw new RuntimeException("no root domain object");
            QuerySolution querySolution = resultSet.nextSolution();
            if (resultSet.hasNext()) throw new RuntimeException("more than one root domain object");
            return querySolution.getResource("x");
        }
    }

    public Resource getElementByBaseURI(String fragment) {
        return ResourceFactory.createResource(this.baseURI + fragment);
    }

    public Resource getElementByFragmentURI(String fragment) {
        return ResourceFactory.createResource(this.domainModel.getURI() + fragment);
    }

    public Resource getMetaClass(Resource element) {
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT ?x
                WHERE {
                    GRAPH ?graph {
                        ?x rdf:type ddo:MetaClass ; ddo:parent ?element .
                    }
                }
                """);
        pss.setIri("graph", DERIVED_MODEL_URI);
        pss.setIri("element", element.getURI());
        try (QueryExecution queryExecution = QueryExecutionFactory.create(pss.asQuery(), this.dataset)) {
            ResultSet resultSet = queryExecution.execSelect();
            return resultSet.hasNext() ? resultSet.next().getResource("x") : null;
        }
    }

    public Resource getParent(Resource element) {
        NodeIterator nodeIterator = this.dataset.getNamedModel(DERIVED_MODEL_URI).listObjectsOfProperty(element, DDO.parent);
        return nodeIterator.hasNext() ? nodeIterator.next().asResource() : null;
    }

    public Resource getSingletonClass(Resource element) {
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT ?x
                WHERE {
                    GRAPH ?graph {
                        ?x rdf:type ddo:DomainObjectClass , ddo:InducedClass , ddo:ConcreteClass ; ddo:parent ?element .
                    }
                }
                """);
        pss.setIri("graph", DERIVED_MODEL_URI);
        pss.setIri("element", element.getURI());
        try (QueryExecution queryExecution = QueryExecutionFactory.create(pss.asQuery(), this.dataset)) {
            ResultSet resultSet = queryExecution.execSelect();
            return resultSet.hasNext() ? resultSet.next().getResource("x") : null;
        }
    }

    public List<Resource> getDirectIntraContextSubClasses(Resource element) {
        List<Resource> list = new LinkedList<>();
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT ?x
                FROM ?derivedModel
                WHERE {
                    ?element ddo:parent ?parent .
                    ?x ddo:parent ?parent ; rdfs:subClassOf ?element .
                }
                ORDER BY ?x
                """);
        pss.setIri("derivedModel", DERIVED_MODEL_URI);
        pss.setIri("element", element.getURI());
        try (QueryExecution queryExecution = QueryExecutionFactory.create(pss.asQuery(), this.dataset)) {
            queryExecution.execSelect().forEachRemaining(x -> list.add(x.getResource("x")));
        }
        return list;
    }

    public List<Resource> getIntraContextPathsOfSpecializationHierarchy(Resource element) {
        List<Resource> list = new LinkedList<>();
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT ?x WHERE {
                    ?element ddo:parent ?parent ; rdfs:subClassOf* ?superClass .
                    ?superClass ddo:parent ?parent .
                    FILTER NOT EXISTS { ?superClass rdfs:subClassOf/ddo:parent ?parent }
                    ?y ddo:parent ?parent ; rdfs:subClassOf* ?superClass .
                    ?parent ?x ?y .
                    FILTER ( ?element != ?y )
                }
                ORDER BY ?x
                """);
        pss.setIri("element", element.getURI());
        try (QueryExecution queryExecution = QueryExecutionFactory.create(pss.asQuery(), this.dataset.getNamedModel(DERIVED_MODEL_URI))) {
            queryExecution.execSelect().forEachRemaining(x -> list.add(x.getResource("x")));
        }
        return list;
    }


    public List<Resource> getLinguisticTypes(Resource element) {
        List<Resource> linguisticTypes = new LinkedList<>();
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT DISTINCT ?x
                FROM ?derivedModel
                FROM ?linguisticModel
                WHERE {
                    ?element rdf:type/rdfs:subClassOf* ?x .
                    ?x rdfs:subClassOf* ddo:Element .
                } ORDER BY ?x
                """);
        pss.setIri("derivedModel", DERIVED_MODEL_URI);
        pss.setIri("linguisticModel", LINGUISTIC_MODEL_RULES_URI);
        pss.setIri("element", element.getURI());
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), this.dataset)) {
            qexec.execSelect().forEachRemaining(x -> linguisticTypes.add(x.getResource("x")));
        }
        return linguisticTypes;
    }

    public List<Resource> getDomainObjectClassPathsOfContext(Resource element) {
        List<Resource> list = new LinkedList<>();
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT DISTINCT ?x
                FROM ?derivedModel
                WHERE {
                    ?element n:CLASS ?class .
                    ?y rdf:type ddo:DomainObjectClass ; ddo:parent ?class .
                    ?class ?x ?y .
                    FILTER strstarts(str(?x), str(n:))
                }
                ORDER BY ?x
                """);
        pss.setIri("derivedModel", DERIVED_MODEL_URI);
        pss.setIri("element", element.getURI());
        try (QueryExecution queryExecution = QueryExecutionFactory.create(pss.asQuery(), this.dataset)) {
            queryExecution.execSelect().forEachRemaining(x -> list.add(x.getResource("x")));
        }
        return list;
    }

    public List<Resource> getOccurrenceClassPathsOfContext(Resource element) {
        List<Resource> list = new LinkedList<>();
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT DISTINCT ?x
                FROM ?derivedModel
                WHERE {
                    ?element rdf:type ?class .
                    ?y rdf:type ddo:OccurrenceClass , ddo:ConcreteClass ; ddo:parent ?class .
                    ?class ?x ?y .
                    FILTER strstarts(str(?x), str(n:))
                }
                ORDER BY ?x
                """);
        pss.setIri("derivedModel", DERIVED_MODEL_URI);
        pss.setIri("element", element.getURI());
        try (QueryExecution queryExecution = QueryExecutionFactory.create(pss.asQuery(), this.dataset)) {
            queryExecution.execSelect().forEachRemaining(x -> list.add(x.getResource("x")));
        }
        return list;
    }

    public List<Resource> getRootChildClabjectComponents(Resource element) {
        List<Resource> list = new LinkedList<>();
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT ?x
                WHERE {
                    GRAPH ?graph {
                        ?x ddo:parent ?element .
                        FILTER NOT EXISTS { ?x rdfs:subClassOf/ddo:parent ?element }
                        FILTER NOT EXISTS { ?x rdf:type ddo:MetaClass }
                        FILTER NOT EXISTS { ?x rdf:type ddo:DomainObjectClass , ddo:InducedClass , ddo:ConcreteClass }
                    }
                }
                ORDER BY ?x
                """);
        pss.setIri("graph", DERIVED_MODEL_URI);
        pss.setIri("element", element.getURI());
        try (QueryExecution queryExecution = QueryExecutionFactory.create(pss.asQuery(), this.dataset)) {
            queryExecution.execSelect().forEachRemaining(x -> list.add(x.getResource("x")));
        }
        return list;
    }

    public List<String> getNamesUnderLevel(Resource element) {
        List<String> names = new LinkedList<>();
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT DISTINCT ?name
                FROM ?derivedModel
                WHERE {
                    { ?element rdfs:subClassOf* ?y }
                    UNION
                    { ?y rdfs:subClassOf+ ?element }
                    ?x ddo:parent ?y ; ddo:name ?name .
                }
                """);
        pss.setIri("derivedModel", DERIVED_MODEL_URI);
        pss.setIri("element", element.getURI());
        try (QueryExecution queryExecution = QueryExecutionFactory.create(pss.asQuery(), this.dataset)) {
            queryExecution.execSelect().forEachRemaining(x -> names.add(x.getLiteral("name").getString()));
        }
        return names;
    }

    public SortedSet<String> getSortedDomainURIFragments() {
        SortedSet<String> set = new TreeSet<>();
        ParameterizedSparqlString pss = preprocessQuery("""
                SELECT DISTINCT ?x
                FROM ?derivedModel
                FROM ?linguisticModel
                WHERE { ?x rdf:type/rdfs:subClassOf* ddo:Element }
                """);
        pss.setIri("derivedModel", DERIVED_MODEL_URI);
        pss.setIri("linguisticModel", LINGUISTIC_MODEL_RULES_URI);
        try (QueryExecution queryExecution = QueryExecution.create(pss.asQuery(), this.dataset)) {
            queryExecution.execSelect().forEachRemaining(x -> set.add(getURIFragment(x.getResource("x"))));
        }
        return set;
    }
}
