package py.una.pol.simulador.eon.models;

import lombok.Data;

@Data
public class Demand {

    private Integer id;
    private Integer source;
    private Integer destination;
    private Integer fs;
    private Integer lifetime;
    private Boolean blocked;
    private final Integer insertionTime;

    /**
     * Constructor with all parameters
     *
     * @param id ID of the demmand
     * @param source Origin of the demand
     * @param destination Destination of the demand
     * @param fs Number of frequency slots required
     * @param lifetime Lifetime of the connection
     * @param blocked Indicates if the demand has been blocked or not
     * @param insertionTime
     */
    public Demand(Integer id, Integer source, Integer destination, Integer fs,
            Integer lifetime, Boolean blocked, Integer insertionTime) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.fs = fs;
        this.lifetime = lifetime;
        this.blocked = blocked;
        this.insertionTime = insertionTime;
    }

    @Override
    public String toString() {
        return "Demand{"
                + "Origen=" + source
                + ", Destino=" + destination
                + ", FS=" + fs
                + ", Tiempo de vida=" + lifetime
                + ", Bloqueado=" + (blocked ? "Si" : "No")
                + '}';
    }
}
