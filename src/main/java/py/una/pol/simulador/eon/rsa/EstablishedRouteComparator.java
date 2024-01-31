package py.una.pol.simulador.eon.rsa;
import java.util.Comparator;

import py.una.pol.simulador.eon.models.EstablishedRoute;

public class EstablishedRouteComparator implements Comparator<EstablishedRoute> {
    @Override
   
    public int compare(EstablishedRoute route1, EstablishedRoute route2) {
        // Comparar por fsWidth de forma descendente
        int fsWidthComparison = route2.getFsWidth().compareTo(route1.getFsWidth());

        if (fsWidthComparison != 0) {
            return fsWidthComparison;
        } else {
            // Si fsWidth es igual, comparar por weight de forma ascendente
            return Double.compare(route1.getDijkstra().getWeight(), route2.getDijkstra().getWeight());
        }
    }
}