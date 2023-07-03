package py.una.pol.simulador.eon.models.enums;

/**
 * Enumerator for the RSAs supported on the simulator
 *
 * @author Néstor E. Reinoso Wood
 */
public enum RSAEnum {

    CORE_UNICO("Core único", "ruteoCoreUnico"),
    MULTIPLES_CORES("Múltiples Cores", "ruteoCoreMultiple");

    private final String label;

    private final String method;

    public String label() {
        return label;
    }

    public String method() {
        return method;
    }

    /**
     * Enum constructor
     *
     * @param label Label of the algorithm
     * @param method Name of the RSA method
     */
    private RSAEnum(String label, String method) {
        this.label = label;
        this.method = method;
    }
}
