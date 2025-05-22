package py.una.pol.simulador.eon;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.Main;
import org.jgrapht.Graph;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.models.enums.RSAEnum;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;
import py.una.pol.simulador.eon.rsa.Algorithms;
import py.una.pol.simulador.eon.utils.Constants;
import py.una.pol.simulador.eon.utils.MathUtils;
import py.una.pol.simulador.eon.utils.Utils;
import py.una.pol.simulador.eon.func.MainFunctions;

/**
 * @author Néstor E. Reinoso Wood
 */
public class SimulatorTest {
    /**
     * Simulador
     *
     * @param args Argumentos de entrada (Vacío)
     */

    public static void main(String[] args) {
        try {
            MainFunctions.createTableBloqueos();
            Input input = MainFunctions.getTestingInput();
            String topologia = Constants.TOPOLOGIA_NSFNET;
            Integer tiempoSimulacion = input.getSimulationTime();
            int desf = 0;

            for (TopologiesEnum topology : input.getTopologies()) {
                if (topology.toString().equals(topologia)) {
                    Graph<Integer, Link> graph = Utils.createTopology(topology, input.getCores(), input.getFsWidth(), input.getCapacity());

                    // Generación de demandas a ser utilizadas en la simulación
                    Integer demandsQ = 1;
                    List<List<Demand>> listaDemandas = new ArrayList<>();
                    for (int i = 0; i < tiempoSimulacion; i++) {
                        List<Demand> demands = Utils.generateDemands(input.getLambda(), tiempoSimulacion, input.getFsRangeMin(),
                                input.getFsRangeMax(), graph.vertexSet().size(), input.getErlang() / input.getLambda(), demandsQ, i);
                        demandsQ += demands.size();
                        listaDemandas.add(demands);
                    }

                    for (Double crosstalkPerUnitLength : input.getCrosstalkPerUnitLenghtList()) {
                        for (RSAEnum algorithm : input.getAlgorithms()) {
                            // Lista de rutas establecidas durante la simulacióndem
                            List<EstablishedRoute> establishedRoutes = new ArrayList<>();
                            System.out.println("Inicializando simulación del RSA " + algorithm.label() + " para erlang: "
                                    + (input.getErlang()) + " para la topología " + topology.label() + " y H = "
                                    + crosstalkPerUnitLength.toString());
                            int demandaNumero = 0;
                            int bloqueos = 0;
                            int rutasProcesadas = 0;
                            establishedRoutes = new ArrayList<>();

                                for (int i = 0; i < tiempoSimulacion; i++) {

                                    //  Demandas a ser transmitidas en el intervalo de tiempo i
                                    List<Demand> demands = listaDemandas.get(i);
                                    for (Demand demand : demands) {
                                        demandaNumero++;
                                        EstablishedRoute establishedRoute;
                                        // ALGORITMO RSA CON CONMUTACION DE NUCLEOS
                                        establishedRoute = Algorithms.ruteoCoreMultiple(graph, demand, input.getCapacity(),
                                                input.getCores(), input.getMaxCrosstalk(), crosstalkPerUnitLength);

                                        if (establishedRoute == null || establishedRoute.getFsIndexBegin() == -1) {
                                            // BLOQUEO
                                            System.out.println("Insertando demanda Nro : " + demandaNumero + " en el tiempo t= " + i + " ---------------------->" + " Bloqueado");
                                            demand.setBlocked(true);
                                            MainFunctions.insertDataBloqueo(algorithm.label(), topology.label(), "" + i, "" + demand.getId(),
                                                    "" + input.getErlang(), crosstalkPerUnitLength.toString());
                                            bloqueos++;
                                        } else {
                                            // RUTA ESTABLECIDA
                                            System.out.println("Insertando demanda Nro : " + demandaNumero + " en el tiempo t= " + i + " ---->" + " Ejecutado");
                                            AssignFsResponse response = Utils.assignFs(graph, establishedRoute,
                                                    crosstalkPerUnitLength);
                                            establishedRoute = response.getRoute();
                                            graph = response.getGraph();
                                            establishedRoutes.add(establishedRoute);
                                            rutasProcesadas++;
                                        }
                                    }
                                    for (EstablishedRoute route : establishedRoutes) {
                                        route.subLifeTime();
                                    }
                                    for (int ri = 0; ri < establishedRoutes.size(); ri++) {
                                        EstablishedRoute route = establishedRoutes.get(ri);
                                        if (route.getLifetime().equals(0)) {
                                            Utils.deallocateFs(graph, route, crosstalkPerUnitLength);
                                            establishedRoutes.remove(ri);
                                            ri--;
                                        }
                                    }

                                }
                                System.out.println("Topología utilizada: " + topologia);
                                System.out.println("Erlangs : " + input.getErlang());
                                System.out.println("TOTAL DE BLOQUEOS: " + bloqueos);
                                System.out.println("Cantidad de demandas asignadas: " + rutasProcesadas);
                                System.out.println("Cantidad de demandas total: " + demandaNumero);
                                System.out.println("--------------------------------------------------");
                                System.out.println("Cantidad de desfragmentaciones: " + desf);
                                System.out.println(System.lineSeparator());
                                desf = 0;

                        }
                    }
                }
            }

        } catch (IOException | IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }


}