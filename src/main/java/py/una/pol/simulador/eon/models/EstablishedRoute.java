package py.una.pol.simulador.eon.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EstablishedRoute {

    private Integer fsIndexBegin;
    private Integer fsWidth;
    private Integer lifetime;
    private Integer from;
    private Integer to;
    private List<Link> path;
    private List<Integer> pathCores;

    public EstablishedRoute() {
    }

    public EstablishedRoute(List<Link> path, Integer fsIndexBegin, Integer fsWidth, Integer lifetime, Integer from, Integer to, List<Integer> pathCores) {
        this.path = path;
        this.fsIndexBegin = fsIndexBegin;
        this.fsWidth = fsWidth;
        this.lifetime = lifetime;
        this.from = from;
        this.to = to;
        this.pathCores = pathCores;
    }

    @Override
    public String toString() {
        return "EstablisedRoute{"
                + "path=" + path
                + ", fsIndexBegin=" + fsIndexBegin
                + ", fsWidth=" + fsWidth
                + ", tl=" + lifetime
                + ", from=" + from
                + ", to=" + to
                + "}";
    }

}
