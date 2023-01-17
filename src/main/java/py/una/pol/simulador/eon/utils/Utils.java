/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package py.una.pol.simulador.eon.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleWeightedGraph;
import py.una.pol.simulador.eon.models.Core;
import py.una.pol.simulador.eon.models.Demand;
import py.una.pol.simulador.eon.models.Link;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class Utils {

    /**
     * Creates the graph that represents the optical network
     *
     * @param topology Topology selected for the network
     * @param numberOfCores Quantity of cores in each link
     * @param fsWidth Width of the frequency slots
     * @param capacity Quantity of frequency slots in a core
     * @return Graph that represents the optical network
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static Graph<Integer, Link> createTopology(TopologiesEnum topology, int numberOfCores,
            BigDecimal fsWidth, Integer capacity)
            throws IOException, IllegalArgumentException {

        ObjectMapper objectMapper = new ObjectMapper();
        Graph<Integer, Link> g = new SimpleWeightedGraph<>(Link.class);
        InputStream is = ResourceReader.getFileFromResourceAsStream(topology.filePath());
        JsonNode object = objectMapper.readTree(is);

        for (int i = 0; i < object.get("network").size(); i++) {
            g.addVertex(i);
        }
        int vertex = 0;
        for (JsonNode node : object.get("network")) {
            for (int i = 0; i < node.get("connections").size(); i++) {
                int connection = node.get("connections").get(i).intValue();
                int distance = node.get("distance").get(i).intValue();
                List<Core> cores = new ArrayList<>();

                for (int j = 0; j < numberOfCores; j++) {
                    Core core = new Core(fsWidth, capacity);
                    cores.add(core);
                }

                Link link = new Link(distance, cores, vertex, connection);
                g.addEdge(vertex, connection, link);
                g.setEdgeWeight(link, distance);
            }
            vertex++;
        }
        return g;
    }

    /**
     * Genera una lista de demandas en base a los argumentos de entrada
     *
     * @param lambda
     * @param totalTime
     * @param fsMin
     * @param fsMax
     * @param cantNodos
     * @param HT
     * @param demandId Identificador de la última demanda generada
     * @return
     */
    public static List<Demand> generateDemands(Integer lambda, Integer totalTime,
            Integer fsMin, Integer fsMax, Integer cantNodos, Integer HT, Integer demandId) {
        List<Demand> demands = new ArrayList<>();
        Random rand;
        Integer demandasQuantity = MathUtils.poisson(lambda);
        for (Integer j = demandId; j < demandasQuantity + demandId; j++) {
            rand = new Random();
            Integer source = rand.nextInt(cantNodos);
            Integer destination = rand.nextInt(cantNodos);
            Integer fs = (int) (Math.random() * (fsMax - fsMin + 1)) + fsMin;
            while (source.equals(destination)) {
                destination = rand.nextInt(cantNodos);
            }
            Integer tLife = MathUtils.getLifetime(HT);
            demands.add(new Demand(j, source, destination, fs, tLife, false));
        }
        return demands;
    }

    public static double crosstalkPerUnitLenght() {
        double h;
        h = (2 * Math.pow(0.06, 2) * 0.05) / (4000000 * 0.00003);
        return h;
    }

    /**
     * 
     * @param n Número de cores vecinos
     * @param h Crosstalk por Unidad de Longitud
     * @param L Longitud del enlace
     * @return Crosstalk
     */
    public static double XT(int n, double h, int L) {
        double XT, e;
        e = -(n + 1) * h * L;
        XT = (n - (n * Math.exp(e))) / (1 + (n * Math.exp(e)));
        return XT;
    }

}
