package py.una.pol.simulador.eon.utils;

import py.una.pol.simulador.eon.models.Core;
import py.una.pol.simulador.eon.models.EstablishedRoute;
import py.una.pol.simulador.eon.models.FrequencySlot;
import py.una.pol.simulador.eon.models.Link;

import org.jgrapht.Graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MetricsUtils - Utilidades para calcular métricas de fragmentación y utilización en redes EON
 *
 * @author Claude
 */
public class MetricsUtils {

    /**
     * 1. COMPACTACIÓN
     *
     * Calcula el espacio aprovechado después de una compactación teórica
     * que eliminaría todos los espacios vacíos entre bloques ocupados
     *
     * @param link Enlace a evaluar
     * //* @param core Núcleo específico del enlace
     * @return Número de bloques ocupados que quedarían contiguos después de compactar
     */
    public static int getCompactacion(Link link, int coreIndex) {
        Core core = link.getCores().get(coreIndex);
        List<FrequencySlot> slots = core.getFrequencySlots();

        int bloqueOcupados = 0;
        for (FrequencySlot slot : slots) {
            if (!slot.isFree()) {
                bloqueOcupados++;
            }
        }

        return bloqueOcupados;
    }

    /**
     * Calcula la compactación promedio para toda la red
     *
     * @param graph Grafo de la red
     * @return Valor promedio de compactación
     */
    public static double getCompactacionPromedio(Graph<Integer, Link> graph) {
        double sumCompactacion = 0;
        int totalEvaluaciones = 0;

        for (Link link : graph.edgeSet()) {
            for (int i = 0; i < link.getCores().size(); i++) {
                sumCompactacion += getCompactacion(link, i);
                totalEvaluaciones++;
            }
        }

        return totalEvaluaciones > 0 ? sumCompactacion / totalEvaluaciones : 0;
    }

    /**
     * 2. FRAGMENTACIÓN EXTERNA
     *
     * Calcula el porcentaje de fragmentación externa en un núcleo específico
     * Formula: (1 - (Tamaño del bloque libre más grande / Memoria total libre)) × 100
     *
     * @param link Enlace a evaluar
     * @param coreIndex Índice del núcleo a evaluar
     * @return Porcentaje de fragmentación externa (0-100)
     */
    public static double getFragmentacionExterna(Link link, int coreIndex) {
        Core core = link.getCores().get(coreIndex);
        List<FrequencySlot> slots = core.getFrequencySlots();

        int totalLibre = 0;
        int bloqueLibreMasGrande = 0;
        int bloqueLibreActual = 0;

        // Calcula el total libre y el bloque libre más grande
        for (FrequencySlot slot : slots) {
            if (slot.isFree()) {
                totalLibre++;
                bloqueLibreActual++;
                if (bloqueLibreActual > bloqueLibreMasGrande) {
                    bloqueLibreMasGrande = bloqueLibreActual;
                }
            } else {
                bloqueLibreActual = 0;
            }
        }

        // Si no hay espacio libre, no hay fragmentación externa
        if (totalLibre == 0) {
            return 0;
        }

        // Calcula el porcentaje de fragmentación externa
        return (1 - ((double) bloqueLibreMasGrande / totalLibre)) * 100;
    }

    /**
     * Calcula la fragmentación externa promedio para toda la red
     *
     * @param graph Grafo de la red
     * @return Promedio de fragmentación externa en la red
     */
    public static double getFragmentacionExternaPromedio(Graph<Integer, Link> graph) {
        double sumFragmentacion = 0;
        int totalEvaluaciones = 0;

        for (Link link : graph.edgeSet()) {
            for (int i = 0; i < link.getCores().size(); i++) {
                sumFragmentacion += getFragmentacionExterna(link, i);
                totalEvaluaciones++;
            }
        }

        return totalEvaluaciones > 0 ? sumFragmentacion / totalEvaluaciones : 0;
    }

    /**
     * 3. ENTROPÍA
     *
     * Calcula la entropía de Shannon para la distribución de espacios
     * libres/ocupados en un núcleo específico
     *
     * @param link Enlace a evaluar
     * @param coreIndex Índice del núcleo a evaluar
     * @return Valor de entropía
     */
    public static double getEntropia(Link link, int coreIndex) {
        Core core = link.getCores().get(coreIndex);
        List<FrequencySlot> slots = core.getFrequencySlots();

        int totalSlots = slots.size();
        int slotsOcupados = 0;

        // Contar slots ocupados
        for (FrequencySlot slot : slots) {
            if (!slot.isFree()) {
                slotsOcupados++;
            }
        }

        int slotsLibres = totalSlots - slotsOcupados;

        // Calcular probabilidades
        double probOcupado = (double) slotsOcupados / totalSlots;
        double probLibre = (double) slotsLibres / totalSlots;

        // Calcular entropía (evitando log(0))
        double entropia = 0;
        if (probOcupado > 0) {
            entropia -= probOcupado * Math.log(probOcupado) / Math.log(2);
        }
        if (probLibre > 0) {
            entropia -= probLibre * Math.log(probLibre) / Math.log(2);
        }

        return entropia;
    }

    /**
     * Calcula la entropía promedio para toda la red
     *
     * @param graph Grafo de la red
     * @return Promedio de entropía en la red
     */
    public static double getEntropiaPromedio(Graph<Integer, Link> graph) {
        double sumEntropia = 0;
        int totalEvaluaciones = 0;

        for (Link link : graph.edgeSet()) {
            for (int i = 0; i < link.getCores().size(); i++) {
                sumEntropia += getEntropia(link, i);
                totalEvaluaciones++;
            }
        }

        return totalEvaluaciones > 0 ? sumEntropia / totalEvaluaciones : 0;
    }

    /**
     * 4. BFR (Blocking Factor Ratio)
     *
     * Basado en las funciones existentes en Utils.bfrRuta y Utils.bfrRed,
     * pero adaptado para calcular el BFR específico para un enlace y núcleo.
     *
     * @param link Enlace a evaluar
     * @param coreIndex Índice del núcleo
     * @param capacidad Capacidad total de slots
     * @return Valor de BFR para el núcleo específico
     */
    public static double getBFR(Link link, int coreIndex, int capacidad) {
        Core core = link.getCores().get(coreIndex);
        List<FrequencySlot> slots = core.getFrequencySlots();

        // Identificar bloques libres y su tamaño
        List<Integer> bloqueLibre = new ArrayList<>();
        int tamañoBloqueActual = 0;

        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isFree()) {
                tamañoBloqueActual++;
            } else {
                if (tamañoBloqueActual > 0) {
                    bloqueLibre.add(tamañoBloqueActual);
                    tamañoBloqueActual = 0;
                }
            }
        }

        // Añadir el último bloque libre si existe
        if (tamañoBloqueActual > 0) {
            bloqueLibre.add(tamañoBloqueActual);
        }

        // Calcular la suma de los cuadrados de los bloques libres
        double sumaCuadrados = 0;
        for (Integer tamaño : bloqueLibre) {
            sumaCuadrados += Math.pow(tamaño, 2);
        }

        // Calcular el total de slots libres
        int totalLibres = bloqueLibre.stream().mapToInt(Integer::intValue).sum();

        // Fórmula BFR: suma de cuadrados / (capacidad * total libres)
        // Si no hay slots libres, retornar 0
        return totalLibres > 0 ? sumaCuadrados / (capacidad * totalLibres) : 0;
    }

    /**
     * Calcula el BFR promedio para toda la red
     *
     * @param graph Grafo de la red
     * @param capacidad Capacidad total de slots
     * @return Promedio de BFR en la red
     */
    public static double getBFRPromedio(Graph<Integer, Link> graph, int capacidad) {
        double sumBFR = 0;
        int totalEvaluaciones = 0;

        for (Link link : graph.edgeSet()) {
            for (int i = 0; i < link.getCores().size(); i++) {
                sumBFR += getBFR(link, i, capacidad);
                totalEvaluaciones++;
            }
        }

        return totalEvaluaciones > 0 ? sumBFR / totalEvaluaciones : 0;
    }

    /**
     * Versión mejorada del BFR específicamente para redes EON
     * que considera la relación entre slots libres y totales
     *
     * @param graph Grafo de la red
     * @param capacidad Capacidad total del enlace en slots
     * @param cores Número total de núcleos
     * @return Valor de BFR para la red
     */
    public static double getBFRMejorado(Graph<Integer, Link> graph, int capacidad, int cores) {
        double totalFS = 0;
        double totalFSLibres = 0;
        double cantidadBloquesLibres = 0;

        for (Link link : graph.edgeSet()) {
            for (int coreIndex = 0; coreIndex < cores; coreIndex++) {
                if (coreIndex < link.getCores().size()) {
                    Core core = link.getCores().get(coreIndex);
                    List<FrequencySlot> slots = core.getFrequencySlots();

                    totalFS += slots.size();

                    // Contar slots libres y bloques libres
                    boolean enBloqueLibre = false;
                    for (FrequencySlot slot : slots) {
                        if (slot.isFree()) {
                            totalFSLibres++;
                            if (!enBloqueLibre) {
                                enBloqueLibre = true;
                                cantidadBloquesLibres++;
                            }
                        } else {
                            enBloqueLibre = false;
                        }
                    }
                }
            }
        }

        // Si no hay slots libres, no hay fragmentación
        if (totalFSLibres == 0) {
            return 0;
        }

        // BFR mejorado: 1 - (número de bloques libres / número total de slots libres)
        return 1 - (cantidadBloquesLibres / totalFSLibres);
    }
}