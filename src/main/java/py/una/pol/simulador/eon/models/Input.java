/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package py.una.pol.simulador.eon.models;

import java.math.BigDecimal;
import lombok.Data;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;

/**
 * Input parameters for the simulation
 *
 * @author Néstor E. Reinoso Wood
 */
@Data
public class Input {
    
    private Integer simulationTime;
    private Integer demands;
    private TopologiesEnum topology;
    private BigDecimal fsWidth;
    private Integer capacity;
    private Integer erlang;
    private Integer lambda;
    private Integer fsRangeMin;
    private Integer fsRangeMax;
    private Integer cores;
    private BigDecimal maxCrosstalk;

}
