package py.una.pol.simulador.eon.metrics;

import org.jgrapht.Graph;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.utils.MetricsUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

public class MetricsCalculator {

    private static List<MetricasInstantaneas> historialMetricas = new ArrayList<>();

    public static class MetricasInstantaneas {
        private int tiempo;
        private int demandasProcesadas;
        private int bloqueos;
        private int demandasActivas;
        private double tasaBloqueoInstantaneo;
        private double compactacionPromedio;
        private double fragmentacionExternaPromedio;
        private double entropiaPromedio;
        private double bfrMejorado;

        public MetricasInstantaneas(int tiempo, int demandasProcesadas, int bloqueos,
                                    int demandasActivas, double compactacionPromedio,
                                    double fragmentacionExternaPromedio,
                                    double entropiaPromedio, double bfrMejorado) {
            this.tiempo = tiempo;
            this.demandasProcesadas = demandasProcesadas;
            this.bloqueos = bloqueos;
            this.demandasActivas = demandasActivas;
            this.tasaBloqueoInstantaneo = demandasProcesadas > 0 ? (double) bloqueos / demandasProcesadas : 0.0;
            this.compactacionPromedio = compactacionPromedio;
            this.fragmentacionExternaPromedio = fragmentacionExternaPromedio;
            this.entropiaPromedio = entropiaPromedio;
            this.bfrMejorado = bfrMejorado;
        }

        // Getters
        public int getTiempo() { return tiempo; }
        public int getDemandasProcesadas() { return demandasProcesadas; }
        public int getBloqueos() { return bloqueos; }
        public int getDemandasActivas() { return demandasActivas; }
        public double getTasaBloqueoInstantaneo() { return tasaBloqueoInstantaneo; }
        public double getCompactacionPromedio() { return compactacionPromedio; }
        public double getFragmentacionExternaPromedio() { return fragmentacionExternaPromedio; }
        public double getEntropiaPromedio() { return entropiaPromedio; }
        public double getBfrMejorado() { return bfrMejorado; }
    }

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

    public static void registrarMetricasInstantaneas(int tiempo, int demandasProcesadas,
                                                     int bloqueos, int demandasActivas,
                                                     Graph<Integer, Link> graph, int capacidad, int cores) {
        double compactacion = MetricsUtils.getCompactacionPromedio(graph);
        double fragmentacion = MetricsUtils.getFragmentacionExternaPromedio(graph);
        double entropia = MetricsUtils.getEntropiaPromedio(graph);
        double bfr = MetricsUtils.getBFRMejorado(graph, capacidad, cores);

        MetricasInstantaneas metricas = new MetricasInstantaneas(tiempo, demandasProcesadas,
                bloqueos, demandasActivas, compactacion, fragmentacion, entropia, bfr);

        historialMetricas.add(metricas);
    }

    public static void reportarMetricasCompletas(
            int tiempoActual, int demandasActivas) {

        // Obtener métricas del tiempo actual desde el historial
        MetricasInstantaneas metricas = null;
        for (MetricasInstantaneas m : historialMetricas) {
            if (m.getTiempo() == tiempoActual) {
                metricas = m;
                break;
            }
        }

        if (metricas == null) return;

        System.out.println("MÉTRICAS COMPLETAS EN TIEMPO t=" + tiempoActual);
        System.out.println("- Compactación promedio: " +
                String.format("%.2f", metricas.getCompactacionPromedio()));
        System.out.println("- Fragmentación externa: " +
                String.format("%.2f", metricas.getFragmentacionExternaPromedio()) + "%");
        System.out.println("- Entropía: " +
                String.format("%.4f", metricas.getEntropiaPromedio()));
        System.out.println("- BFR: " +
                String.format("%.4f", metricas.getBfrMejorado()));
        // ... resto de métricas ...
    }

    public static double calcularTasaBloqueoInstantaneoHaciaAtras(int tiempoActual) {
        // Buscar métricas del tiempo siguiente (x+1)
        for (MetricasInstantaneas metricas : historialMetricas) {
            if (metricas.getTiempo() == tiempoActual + 1) {
                return metricas.getTasaBloqueoInstantaneo();
            }
        }
        return -1.0; // No hay datos disponibles
    }

    public static void reportarMetricasCompletas(Graph<Integer, Link> graph, int capacidad,
                                                 int cores, int tiempoActual, int demandasActivas) {
        System.out.println("============================================================");
        System.out.println("MÉTRICAS COMPLETAS EN TIEMPO t=" + tiempoActual);
        System.out.println("============================================================");

        // Métricas de red tradicionales
        System.out.println("MÉTRICAS DE FRAGMENTACIÓN:");
        System.out.println("- Compactación promedio: " +
                String.format("%.2f", MetricsUtils.getCompactacionPromedio(graph)));
        System.out.println("- Fragmentación externa: " +
                String.format("%.2f", MetricsUtils.getFragmentacionExternaPromedio(graph)) + "%");
        System.out.println("- Entropía: " +
                String.format("%.4f", MetricsUtils.getEntropiaPromedio(graph)));
        System.out.println("- BFR: " +
                String.format("%.4f", MetricsUtils.getBFRMejorado(graph, capacidad, cores)));

        // Estado de la red
        System.out.println("\nESTADO DE LA RED:");
        System.out.println("- Demandas activas: " + demandasActivas);

        // Tasa de bloqueo instantáneo hacia atrás
        double tasaBloqueoHaciaAtras = calcularTasaBloqueoInstantaneoHaciaAtras(tiempoActual);
        System.out.println("\nTASA DE BLOQUEO:");
        if (tasaBloqueoHaciaAtras >= 0) {
            System.out.println("- Tasa de bloqueo instantáneo hacia atrás: " +
                    String.format("%.4f", tasaBloqueoHaciaAtras) + " (" +
                    String.format("%.2f", tasaBloqueoHaciaAtras * 100) + "%)");
        } else {
            System.out.println("- Tasa de bloqueo instantáneo hacia atrás: No disponible (falta t+1)");
        }

        // Métricas del tiempo actual si están disponibles
        for (MetricasInstantaneas metricas : historialMetricas) {
            if (metricas.getTiempo() == tiempoActual) {
                System.out.println("- Tasa de bloqueo instantáneo actual: " +
                        String.format("%.4f", metricas.getTasaBloqueoInstantaneo()) + " (" +
                        String.format("%.2f", metricas.getTasaBloqueoInstantaneo() * 100) + "%)");
                break;
            }
        }

        System.out.println("============================================================");
    }

    /**
     * Genera un resumen estadístico de todas las métricas recolectadas
     */
    public static void generarResumenEstadistico() {
        if (historialMetricas.isEmpty()) {
            System.out.println("No hay métricas históricas disponibles.");
            return;
        }

        System.out.println("============================================================");
        System.out.println("RESUMEN ESTADÍSTICO DE LA SIMULACIÓN");
        System.out.println("============================================================");

        double sumaTasaBloqueo = 0.0;
        double maxTasaBloqueo = 0.0;
        double minTasaBloqueo = Double.MAX_VALUE;
        int totalDemandas = 0;
        int totalBloqueos = 0;
        int maxDemandasActivas = 0;

        for (MetricasInstantaneas metricas : historialMetricas) {
            sumaTasaBloqueo += metricas.getTasaBloqueoInstantaneo();
            maxTasaBloqueo = Math.max(maxTasaBloqueo, metricas.getTasaBloqueoInstantaneo());
            minTasaBloqueo = Math.min(minTasaBloqueo, metricas.getTasaBloqueoInstantaneo());
            totalDemandas += metricas.getDemandasProcesadas();
            totalBloqueos += metricas.getBloqueos();
            maxDemandasActivas = Math.max(maxDemandasActivas, metricas.getDemandasActivas());
        }

        double promedioTasaBloqueo = sumaTasaBloqueo / historialMetricas.size();
        double tasaBloqueoGlobal = totalDemandas > 0 ? (double) totalBloqueos / totalDemandas : 0.0;

        System.out.println("ESTADÍSTICAS DE BLOQUEO:");
        System.out.println("- Tasa de bloqueo global: " + String.format("%.4f", tasaBloqueoGlobal) +
                " (" + String.format("%.2f", tasaBloqueoGlobal * 100) + "%)");
        System.out.println("- Tasa de bloqueo promedio por tiempo: " +
                String.format("%.4f", promedioTasaBloqueo) +
                " (" + String.format("%.2f", promedioTasaBloqueo * 100) + "%)");
        System.out.println("- Tasa de bloqueo máxima: " + String.format("%.4f", maxTasaBloqueo) +
                " (" + String.format("%.2f", maxTasaBloqueo * 100) + "%)");
        System.out.println("- Tasa de bloqueo mínima: " + String.format("%.4f", minTasaBloqueo) +
                " (" + String.format("%.2f", minTasaBloqueo * 100) + "%)");

        System.out.println("\nESTADÍSTICAS GENERALES:");
        System.out.println("- Total de demandas procesadas: " + totalDemandas);
        System.out.println("- Total de bloqueos: " + totalBloqueos);
        System.out.println("- Máximo de demandas activas simultáneas: " + maxDemandasActivas);
        System.out.println("- Tiempos de simulación: " + historialMetricas.size());

        System.out.println("============================================================");
    }

    public static void limpiarHistorial() {
        historialMetricas.clear();
    }

    public static List<MetricasInstantaneas> getHistorialMetricas() {
        return new ArrayList<>(historialMetricas);
    }

    public static void generarCSVMetricas(String nombreArchivo) {
        try (PrintWriter writer = new PrintWriter(new File(nombreArchivo))) {
            // Encabezado del CSV
            writer.println("Tiempo,Demandas Procesadas,Bloqueos,Demandas Activas,"
                    + "Compactacion,Fragmentacion,Entropia,BFR");

            // Datos
            for (MetricasInstantaneas metrica : historialMetricas) {
                writer.println(String.format("%d,%d,%d,%d,%.2f,%.2f,%.4f,%.4f",
                        metrica.getTiempo(),
                        metrica.getDemandasProcesadas(),
                        metrica.getBloqueos(),
                        metrica.getDemandasActivas(),
                        metrica.getCompactacionPromedio(),
                        metrica.getFragmentacionExternaPromedio(),
                        metrica.getEntropiaPromedio(),
                        metrica.getBfrMejorado()));
            }
            System.out.println("Archivo CSV generado: " + nombreArchivo);
        } catch (FileNotFoundException e) {
            System.err.println("Error al generar CSV: " + e.getMessage());
        }
    }
}