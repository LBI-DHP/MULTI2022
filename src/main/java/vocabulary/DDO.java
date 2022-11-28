package vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class DDO {

    private static final String NAMESPACE = "http://dke.jku.at/ddo#";
    private static final String n = "http://dke.jku.at/ddo-hasNamedChild#";

    public static String getURI() {
        return NAMESPACE;
    }
    public static String getN(){ return n; }

    public static Resource AbstractClass = createResource("AbstractClass");
    public static Resource ConcreteClass = createResource("ConcreteClass");
    public static Resource Individual = createResource("Individual");
    public static Resource DomainObject = createResource("DomainObject");
    public static Resource DomainObjectClass = createResource("DomainObjectClass");
    public static Resource Clabject = createResource("Clabject");
    public static Resource MetaClass = createResource("MetaClass");
    public static Resource ModeledClass = createResource("ModeledClass");
    public static Resource Occurrence = createResource("Occurrence");
    public static Resource OccurrenceClass = createResource("OccurrenceClass");

    public static Property instanceOf = createProperty("instanceOf");
    public static Property leafs = createProperty("leafs");
    public static Property name = createProperty("name");
    public static Property parent = createProperty("parent");
    public static Property specializationOf = createProperty("specializationOf");

    private static Resource createResource(String name) {
        return ResourceFactory.createResource(NAMESPACE + name);
    }

    private static Property createProperty(String name) {
        return ResourceFactory.createProperty(NAMESPACE, name);
    }
}
