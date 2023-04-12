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
    public String toString(int core, int fsIndexBegin, int fsWidth) {
        String asd = "Link {"
                + "distance=" + distance
                + ", core=" + core
                + ", from=" + from
                + ", to=" + to
                + "}";
        for(FrequencySlot fs : cores.get(core).getFrequencySlots()) {
            asd = asd + fs.toString();
        }
        return asd;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof Link)) {
            return false;
        }

        Link c = (Link) object;
        return this.hashCode() == c.hashCode();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.from;
        hash = 67 * hash + this.to;
        return hash;
    }

}
