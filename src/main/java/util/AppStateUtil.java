package util;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import service.DatasetService;

import java.util.Stack;

public class AppStateUtil {

    private Resource selectedElement;
    private DatasetService datasetService;
    private Stack<Resource> backwardHistory;
    private Stack<Resource> forwardHistory;

    public AppStateUtil(DatasetService datasetService, Resource selectedElement, Stack<Resource> backwardHistory, Stack<Resource> forwardHistory){
        this.datasetService = new DatasetService(datasetService.getDataset());
        this.selectedElement = ResourceFactory.createResource(selectedElement.getURI());
        this.backwardHistory = new Stack<>();
        this.backwardHistory.addAll(backwardHistory);
        this.forwardHistory = new Stack<>();
        this.forwardHistory.addAll(forwardHistory);
    }

    public Resource getSelectedElement() {
        return selectedElement;
    }

    public void setSelectedElement(Resource selectedElement) {
        this.selectedElement = selectedElement;
    }

    public DatasetService getDatasetService() {
        return datasetService;
    }

    public void setDatasetService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    public Stack<Resource> getBackwardHistory() {
        return backwardHistory;
    }

    public void setBackwardHistory(Stack<Resource> backwardHistory) {
        this.backwardHistory = backwardHistory;
    }

    public Stack<Resource> getForwardHistory() {
        return forwardHistory;
    }

    public void setForwardHistory(Stack<Resource> forwardHistory) {
        this.forwardHistory = forwardHistory;
    }
}
