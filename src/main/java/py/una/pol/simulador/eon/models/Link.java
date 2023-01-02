package py.una.pol.simulador.eon.models;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Link implements Serializable {

    private int distance;
    private List<Core> cores;
    private int from;
    private int to;

    @Override
    public String toString() {
        /*return "Link{"
                + "distance=" + distance
                + ", cores=" + cores.size()
                + ", from=" + from
                + ", to=" + to
                + "}";*/
        return String.format("%s - %s\n%s", to, from, distance);
    }

}
