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
    private List<EstablishedLink> path;

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
