package py.una.pol.simulador.eon;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.Main;
import org.jgrapht.Graph;
import py.una.pol.simulador.eon.metrics.MetricsCalculator;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.models.enums.RSAEnum;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;
import py.una.pol.simulador.eon.rsa.Algorithms;
import py.una.pol.simulador.eon.utils.Constants;
import py.una.pol.simulador.eon.utils.MathUtils;
import py.una.pol.simulador.eon.utils.Utils;
import py.una.pol.simulador.eon.func.MainFunctions;


public class SimulatorTest {

    public static void main(String[] args) {
        try {
            MainFunctions.createTableBloqueos();
            Input input = MainFunctions.getTestingInput();
            String topologia = Constants.TOPOLOGIA_NSFNET;
            Integer tiempoSimulacion = input.getSimulationTime();

            System.out.println("Input: " + input);
            System.out.println("Topologia: " + topologia);
            System.out.println("Simulacion de Tiempo: " + tiempoSimulacion);

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
                            List<EstablishedRoute> establishedRoutes;
                            System.out.println("Inicializando simulación del RSA " + algorithm.label() + " para erlang: "
                                    + (input.getErlang()) + " para la topología " + topology.label() + " y H = "
                                    + crosstalkPerUnitLength.toString());
                            int demandaNumero = 0;
                            int bloqueos = 0;
                            int rutasProcesadas = 0;
                            establishedRoutes = new ArrayList<>();

                            MetricsCalculator.limpiarHistorial();

                            for (int i = 0; i < tiempoSimulacion; i++) {

                                // Contadores para este tiempo específico
                                int demandasProcesadasEnTiempo = 0;
                                int bloqueosEnTiempo = 0;

                                // Demandas a ser transmitidas en el intervalo de tiempo i
                                List<Demand> demands = listaDemandas.get(i);

                                for (Demand demand : demands) {
                                    demandaNumero++;
                                    demandasProcesadasEnTiempo++;
                                    EstablishedRoute establishedRoute;

                                    // ALGORITMO RSA CON CONMUTACION DE NUCLEOS
                                    establishedRoute = Algorithms.ruteoCoreMultiple(graph, demand, input.getCapacity(),
                                            input.getCores(), input.getMaxCrosstalk(), crosstalkPerUnitLength);

                                    if (establishedRoute == null || establishedRoute.getFsIndexBegin() == -1) {
                                        // BLOQUEO
                                        System.out.println("Insertando demanda Nro : " + demandaNumero + " en el tiempo t= " + i +
                                                " ---------------------->" + " Bloqueado");
                                        demand.setBlocked(true);
                                        MainFunctions.insertDataBloqueo(algorithm.label(), topology.label(), "" + i, "" + demand.getId(),
                                                "" + input.getErlang(), crosstalkPerUnitLength.toString());
                                        bloqueos++;
                                        bloqueosEnTiempo++;
                                    } else {
                                        // RUTA ESTABLECIDA
                                        //System.out.println("Insertando demanda Nro : " + demandaNumero + " en el tiempo t= " + i +
                                        //                 " ---->" + " Ejecutado");
                                        AssignFsResponse response = Utils.assignFs(graph, establishedRoute,
                                                crosstalkPerUnitLength);
                                        establishedRoute = response.getRoute();
                                        graph = response.getGraph();
                                        establishedRoutes.add(establishedRoute);
                                        rutasProcesadas++;
                                    }
                                }

                                // Decrementar tiempo de vida de las rutas establecidas
                                for (EstablishedRoute route : establishedRoutes) {
                                    route.subLifeTime();
                                }

                                // Remover rutas que han expirado
                                for (int ri = 0; ri < establishedRoutes.size(); ri++) {
                                    EstablishedRoute route = establishedRoutes.get(ri);
                                    if (route.getLifetime().equals(0)) {
                                        Utils.deallocateFs(graph, route, crosstalkPerUnitLength);
                                        establishedRoutes.remove(ri);
                                        ri--;
                                    }
                                }

                                // Registrar métricas instantáneas para este tiempo
                                int demandasActivas = establishedRoutes.size();
                                MetricsCalculator.registrarMetricasInstantaneas(i, demandasProcesadasEnTiempo,
                                        bloqueosEnTiempo, demandasActivas);

                                // Reportar métricas cada cierto intervalo (opcional)
                                if (i % 10 == 0 || i == tiempoSimulacion - 1) {
                                    MetricsCalculator.reportarMetricasCompletas(graph, input.getCapacity(),
                                            input.getCores(), i, demandasActivas);
                                }

                                // Si es el último tiempo, mostrar métricas finales detalladas
                                if (i == tiempoSimulacion - 1) {
                                    System.out.println("\n" + "=".repeat(60));
                                    System.out.println("MÉTRICAS FINALES DE LA SIMULACIÓN");
                                    System.out.println("=".repeat(60));

                                    // Métricas tradicionales detalladas
                                    MetricsCalculator.calcularMetricas(graph, input.getCapacity(), input.getCores());

                                    // Resumen estadístico completo
                                    MetricsCalculator.generarResumenEstadistico();
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