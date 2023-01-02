package py.una.pol.simulador.eon.models;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class FrequencySlot implements Serializable {

    private int lifetime;
    private boolean free;
    private BigDecimal fsWidh;
    private BigDecimal crosstalk;

    public FrequencySlot(BigDecimal fsWidh) {
        this.fsWidh = fsWidh;
        this.lifetime = 0;
        this.free = true;
        this.crosstalk = BigDecimal.ZERO;
    }

    /**
     * Subtracts a unit of time from the frequency slot, if it's possible
     *
     * @return  <ul>
     * <li><code>true</code> If the frequency slot is occupied after the
     * subtraction</li>
     * <li><code>false</code> If the frequency slot is free after the
     * subtraction</li>
     * </ul>
     */
    public boolean subLifetime() {
        if (this.free) {
            return false;
        }
        this.lifetime--;
        if (this.lifetime == 0) {
            this.free = true;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "FrecuencySlot{"
                + "lifetime=" + lifetime
                + ", free=" + free
                + ", fsWidh=" + fsWidh
                + ", crosstalk=" + crosstalk.toPlainString()
                + "}";
    }

}
