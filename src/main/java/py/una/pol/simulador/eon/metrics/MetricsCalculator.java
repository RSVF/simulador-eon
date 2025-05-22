package py.una.pol.simulador.eon.metrics;

import java.util.ArrayList;
import java.util.List;
import org.jgrapht.Graph;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.utils.MetricsUtils;

/**
 * Clase para calcular y mostrar las métricas de fragmentación
 * solicitadas para la simulación EON
 */
public class MetricsCalculator {

    /**
     * Método principal que calcula todas las métricas solicitadas
     *
     * @param graph El grafo de la red
     * @param capacidad La capacidad de los enlaces en frequency slots
     * @param cores El número de cores por enlace
     */
    public static void calcularMetricas(Graph<Integer, Link> graph, int capacidad, int cores) {
        System.out.println("------------------------------------------------------------");
        System.out.println("CÁLCULO DE MÉTRICAS DE FRAGMENTACIÓN");
        System.out.println("------------------------------------------------------------");

        // 1. Compactación
        double compactacionPromedio = MetricsUtils.getCompactacionPromedio(graph);
        System.out.println("1. COMPACTACIÓN:");
        System.out.println("   - Espacio aprovechado promedio después de compactar: " +
                String.format("%.2f", compactacionPromedio) + " slots");

        // Calcular para cada enlace y core
        System.out.println("   - Detalle por enlace:");
        for (Link link : graph.edgeSet()) {
            System.out.println("     Enlace " + graph.getEdgeSource(link) +
                    " -> " + graph.getEdgeTarget(link) + ":");
            for (int i = 0; i < link.getCores().size(); i++) {
                int compactacion = MetricsUtils.getCompactacion(link, i);
                System.out.println("       Core " + i + ": " + compactacion + " slots");
            }
        }
        System.out.println();

        // 2. Fragmentación externa
        double fragExternaPromedio = MetricsUtils.getFragmentacionExternaPromedio(graph);
        System.out.println("2. FRAGMENTACIÓN EXTERNA:");
        System.out.println("   - Fragmentación externa promedio: " +
                String.format("%.2f", fragExternaPromedio) + "%");

        // Calcular para cada enlace y core
        System.out.println("   - Detalle por enlace:");
        for (Link link : graph.edgeSet()) {
            System.out.println("     Enlace " + graph.getEdgeSource(link) +
                    " -> " + graph.getEdgeTarget(link) + ":");
            for (int i = 0; i < link.getCores().size(); i++) {
                double fragExterna = MetricsUtils.getFragmentacionExterna(link, i);
                System.out.println("       Core " + i + ": " +
                        String.format("%.2f", fragExterna) + "%");
            }
        }
        System.out.println();

        // 3. Entropía
        double entropiaPromedio = MetricsUtils.getEntropiaPromedio(graph);
        System.out.println("3. ENTROPÍA:");
        System.out.println("   - Entropía promedio: " +
                String.format("%.4f", entropiaPromedio));

        // Calcular para cada enlace y core
        System.out.println("   - Detalle por enlace:");
        for (Link link : graph.edgeSet()) {
            System.out.println("     Enlace " + graph.getEdgeSource(link) +
                    " -> " + graph.getEdgeTarget(link) + ":");
            for (int i = 0; i < link.getCores().size(); i++) {
                double entropia = MetricsUtils.getEntropia(link, i);
                System.out.println("       Core " + i + ": " +
                        String.format("%.4f", entropia));
            }
        }
        System.out.println();

        // 4. BFR (Blocking Factor Ratio)
        double bfrPromedio = MetricsUtils.getBFRPromedio(graph, capacidad);
        double bfrMejorado = MetricsUtils.getBFRMejorado(graph, capacidad, cores);
        System.out.println("4. BFR (BLOCKING FACTOR RATIO):");
        System.out.println("   - BFR promedio: " + String.format("%.4f", bfrPromedio));
        System.out.println("   - BFR mejorado para EON: " + String.format("%.4f", bfrMejorado));

        // Calcular para cada enlace y core
        System.out.println("   - Detalle por enlace:");
        for (Link link : graph.edgeSet()) {
            System.out.println("     Enlace " + graph.getEdgeSource(link) +
                    " -> " + graph.getEdgeTarget(link) + ":");
            for (int i = 0; i < link.getCores().size(); i++) {
                double bfr = MetricsUtils.getBFR(link, i, capacidad);
                System.out.println("       Core " + i + ": " +
                        String.format("%.4f", bfr));
            }
        }
        System.out.println("------------------------------------------------------------");
    }

    /**
     * Método para integrarse en la simulación después de procesar demandas
     *
     * @param graph El grafo de la red
     * @param capacidad La capacidad de los enlaces
     * @param cores El número de cores
     * @param tiempoActual El tiempo actual en la simulación
     */
    public static void reportarMetricasEnTiempo(Graph<Integer, Link> graph, int capacidad,
                                                int cores, int tiempoActual) {
        System.out.println("MÉTRICAS EN TIEMPO t=" + tiempoActual);
        System.out.println("- Compactación promedio: " +
                String.format("%.2f", MetricsUtils.getCompactacionPromedio(graph)));
        System.out.println("- Fragmentación externa: " +
                String.format("%.2f", MetricsUtils.getFragmentacionExternaPromedio(graph)) + "%");
        System.out.println("- Entropía: " +
                String.format("%.4f", MetricsUtils.getEntropiaPromedio(graph)));
        System.out.println("- BFR: " +
                String.format("%.4f", MetricsUtils.getBFRMejorado(graph, capacidad, cores)));
    }
}