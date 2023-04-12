package py.una.pol.simulador.eon.models;

import lombok.Data;
import org.jgrapht.Graph;

@Data
public class AssignFsResponse {

    private EstablishedRoute route;
    private Graph<Integer, Link> graph;

    public AssignFsResponse() {
    }

    public AssignFsResponse(Graph<Integer, Link> graph, EstablishedRoute route) {
        this.graph = graph;
        this.route = route;
    }

}
