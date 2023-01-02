package py.una.pol.simulador.eon.models.enums;

/**
 * Enumerator for the topologies supported on the simulator
 *
 * @author Néstor E. Reinoso Wood
 */
public enum TopologiesEnum {

    USNET("USNET", "topologies/usnet.json"),
    NSFNET("NSFNET", "topologies/nsfnet.json"),
    EUNET("EUNET", "topologies/eunet.json");

    private final String label;

    private final String filePath;

    public String label() {
        return label;
    }

    public String filePath() {
        return filePath;
    }

    /**
     * Enum constructor
     *
     * @param label Label of the topology
     * @param filePath Path of the resource file used to create the topology
     */
    private TopologiesEnum(String label, String filePath) {
        this.label = label;
        this.filePath = filePath;
    }
}
