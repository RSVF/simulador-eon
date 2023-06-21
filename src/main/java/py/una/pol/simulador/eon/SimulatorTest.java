/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package py.una.pol.simulador.eon;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import py.una.pol.simulador.eon.models.AssignFsResponse;
import py.una.pol.simulador.eon.models.Demand;
import py.una.pol.simulador.eon.models.EstablishedRoute;
import py.una.pol.simulador.eon.models.FrequencySlot;
import py.una.pol.simulador.eon.models.Input;
import py.una.pol.simulador.eon.models.Link;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;
import py.una.pol.simulador.eon.rsa.Algorithms;
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
        input.setTopology(TopologiesEnum.NSFNET);
        input.setFsWidth(new BigDecimal("12.5"));
        input.setFsRangeMax(8);
        input.setFsRangeMin(2);
        input.setCapacity(320);
        input.setCores(7);
        input.setLambda(150);
        input.setErlang(2500);
        input.setMaxCrosstalk(new BigDecimal("0.0000031622776601683793"));

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

            List<List<GraphPath<Integer, Link>>> kspList = new ArrayList<>();

            int demandaNumero = 1;
            int bloqueos = 0;
            // Iteración de unidades de tiempo
            for (int i = 0; i < input.getSimulationTime(); i++) {
                System.out.println("Tiempo: " + (i+1));
                // Generación de demandas para la unidad de tiempo
                List<Demand> demands = Utils.generateDemands(input.getLambda(),
                        input.getSimulationTime(), input.getFsRangeMin(),
                        input.getFsRangeMax(), graph.vertexSet().size(),
                        input.getErlang() / input.getLambda(), demandsQ);
                System.out.println("Demandas a insertar: " + demands.size());

                demandsQ += demands.size();

                KShortestSimplePaths<Integer, Link> ksp = new KShortestSimplePaths<>(graph);
                for (Demand demand : demands) {
                    demandaNumero++;
                    //System.out.println("Insertando demanda " + demandaNumero++);
                    //k caminos más cortos entre source y destination de la demanda actual
                    List<GraphPath<Integer, Link>> kspaths = ksp.getPaths(demand.getSource(), demand.getDestination(), 5);

                    EstablishedRoute establishedRoute = Algorithms.fa(graph, kspaths, demand, input.getCapacity(), input.getCores(), input.getMaxCrosstalk());

                    //EstablishedRoute establishedRoute = Algorithms.genericRouting(graph, kspaths, demand, input.getCapacity(), input.getCores(), input.getMaxCrosstalk());

                    if (establishedRoute == null || establishedRoute.getFsIndexBegin() == -1) {
                        //Bloqueo
                        System.out.println("BLOQUEO");
                        demand.setBlocked(true);
                        bloqueos++;
                    } else {
                        //Ruta establecida
                        AssignFsResponse response = Utils.assignFs(graph, establishedRoute);
                        establishedRoute = response.getRoute();
                        graph = response.getGraph();
                        establishedRoutes.add(establishedRoute);
                        kspList.add(kspaths);
                    }

                }

                for (EstablishedRoute route : establishedRoutes) {
                    route.subLifeTime();
                }

                for (int ri = 0; ri < establishedRoutes.size(); ri++) {
                    EstablishedRoute route = establishedRoutes.get(ri);
                    if (route.getLifetime().equals(0)) {
                        Utils.deallocateFs(graph, route);
                        establishedRoutes.remove(ri);
                        kspList.remove(ri);
                        ri--;
                    }
                }
                System.out.println("TOTAL DE BLOQUEOS: " + bloqueos);
            }
            System.out.println("Cantidad de demandas: " + demandaNumero);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
