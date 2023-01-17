/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package py.una.pol.simulador.eon;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import py.una.pol.simulador.eon.models.Demand;
import py.una.pol.simulador.eon.models.EstablishedLink;
import py.una.pol.simulador.eon.models.EstablishedRoute;
import py.una.pol.simulador.eon.models.FrequencySlot;
import py.una.pol.simulador.eon.models.Input;
import py.una.pol.simulador.eon.models.Link;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;
import py.una.pol.simulador.eon.utils.GraphUtils;
import py.una.pol.simulador.eon.utils.Utils;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class SimulatorTest {

    private Input getTestingInput() {
        Input input = new Input();
        input.setSimulationTime(1000);
        input.setTopology(TopologiesEnum.USNET);
        input.setFsWidth(new BigDecimal("12.5"));
        input.setFsRangeMax(8);
        input.setFsRangeMin(2);
        input.setCapacity(350);
        input.setCores(7);
        input.setLambda(5);
        input.setErlang(400);

        return input;
    }

    public static void main(String[] args) {
        try {
            
            // Lista de rutas establecidas durante la simulación
            List<EstablishedRoute> establishedRoutes = new ArrayList<>();

            // Contador de demandas utilizado para identificación
            Integer demandsQ = 1;

            // Datos de entrada
            Input input = new SimulatorTest().getTestingInput();

            // Se genera la red de acuerdo a los datos de entrada
            Graph<Integer, Link> graph = Utils.createTopology(input.getTopology(),
                    input.getCores(), input.getFsWidth(), input.getCapacity());

            // Iteración de unidades de tiempo
            for (int i = 0; i < input.getSimulationTime(); i++) {

                // Generación de demandas para la unidad de tiempo
                List<Demand> demands = Utils.generateDemands(input.getLambda(),
                        input.getSimulationTime(), input.getFsRangeMin(),
                        input.getFsRangeMax(), graph.vertexSet().size(),
                        input.getErlang() / input.getLambda(), demandsQ);

                demandsQ += demands.size();

                KShortestSimplePaths<Integer, Link> ksp = new KShortestSimplePaths<>(graph);

                for (Demand demand : demands) {
                    //k caminos más cortos entre source y destination de la demanda actual
                    List<GraphPath<Integer, Link>> kspaths = ksp.getPaths(demand.getSource(), demand.getDestination(), 5);
                    
                    for (GraphPath<Integer, Link> kspath : kspaths) {
                        EstablishedRoute establishedRoute = null;
                        List<EstablishedLink> establishedLinks = new ArrayList();
                        // Primero Iterar enlances
                        for (Link enlace : kspath.getEdgeList()) {
                            // Iterar cores dentro de enlaces
                            for (int core = 0; core < input.getCores(); core++) {
                                //Iterar FSs dentro de cores
                                for (int freqSlot = 0; freqSlot < input.getCapacity() - demand.getFs(); freqSlot++) {
                                    Boolean hasSpace = Boolean.TRUE;
                                    List<Integer> freeFsIndexes = new ArrayList<>();
                                    for (int subFsSlot = freqSlot; subFsSlot < freqSlot + demand.getFs(); subFsSlot++) {
                                        // Controlar FSs libres y con bajo Crosstalk
                                        if (!enlace.getCores().get(core).getFrequencySlots().get(subFsSlot).isFree()) {
                                            // está ocupado
                                            hasSpace = Boolean.FALSE;
                                            freqSlot = subFsSlot;
                                            subFsSlot = freqSlot + demand.getFs();
                                            freeFsIndexes = new ArrayList<>();
                                        } else {
                                            // Si no está ocupado, agrega a la lista
                                            freeFsIndexes.add(subFsSlot);
                                        }
                                    }
                                    // Si existe espacio para insertar la demanda en el enlace, se guarda
                                    if(hasSpace) {
                                        EstablishedLink establishedLink = new EstablishedLink();
                                        establishedLink.setDistance(enlace.getDistance());
                                        establishedLink.setCore(core);
                                        establishedLink.setFrom(enlace.getFrom());
                                        establishedLink.setTo(enlace.getTo());
                                    }
                                }
                            }
                        }
                    }

                    // TODO: Ejecutar RSA con crosstalk para insertar la demanda
                    System.out.println(demand.getId());

                }

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private EstablishedLink getFreeLink(Link link, Input input) {
        EstablishedLink establishedLink = null;
        
        return establishedLink;
    }
}
