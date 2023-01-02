package py.una.pol.simulador.eon.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Core implements Serializable {

    private BigDecimal bandwidth;
    private List<FrequencySlot> frequencySlots;

    /**
     * Constructor with quantity of Frequency Slots
     *
     * @param bandwidth Bandwidth of the core
     * @param frequencySlotQuantity Number of frequency slots available
     */
    public Core(BigDecimal bandwidth, Integer frequencySlotQuantity) {
        this.bandwidth = bandwidth;
        this.frequencySlots = new ArrayList<>();
        for (int i = 0; i < frequencySlotQuantity; i++) {
            this.frequencySlots.add(new FrequencySlot(bandwidth.divide(new BigDecimal(frequencySlotQuantity), RoundingMode.HALF_UP)));
        }
    }

    /**
     * Constructor with list of Frequency Slots
     *
     * @param bandwidth Bandwidth of the core
     * @param frequencySlots Number of frequency slots available
     */
    public Core(BigDecimal bandwidth, List<FrequencySlot> frequencySlots) {
        this.bandwidth = bandwidth;
        this.frequencySlots = frequencySlots;
    }

    @Override
    public String toString() {
        return "Core{"
                + "bandwidth=" + bandwidth
                + ", fs=" + frequencySlots
                + "}";
    }

}
