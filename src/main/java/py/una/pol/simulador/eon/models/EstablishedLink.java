package py.una.pol.simulador.eon.models;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstablishedLink implements Serializable {

    private Integer distance;
    private Integer core;
    private Integer from;
    private Integer to;

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
