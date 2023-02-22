/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package py.una.pol.simulador.eon.rsa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import py.una.pol.simulador.eon.models.Core;
import py.una.pol.simulador.eon.models.Demand;
import py.una.pol.simulador.eon.models.EstablishedRoute;
import py.una.pol.simulador.eon.models.FrequencySlot;
import py.una.pol.simulador.eon.models.Link;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class Algorithms {

    public static EstablishedRoute asd(Graph<Integer, Link> graph,
            List<GraphPath<Integer, Link>> kspaths, Demand demand,
            Integer capacity, Integer cores) {
        int k = 0;

        // Iteramos los KSPaths
        while (k < kspaths.size() && kspaths.get(k) != null) {
            GraphPath<Integer, Link> ksp = kspaths.get(k);
            // Recorremos los slots de frecuencia
            for (int i = 0; i < capacity; i++) {
                for (Link link : ksp.getEdgeList()) {
                    for (int core = 0; core < cores; core++) {
                        FrequencySlot fs = link.getCores().get(core).getFrequencySlots().get(i);
                        if (fs.isFree()) {

                        }
                    }
                }
            }
        }

        return null;
    }

    public static EstablishedRoute fa(Graph<Integer, Link> graph,
            List<GraphPath<Integer, Link>> kspaths, Demand demand,
            Integer capacity, Integer cores) {

        // Representa la ocupación del espectro de todos los enlaces.
        Boolean so[][] = new Boolean[capacity][cores];
        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        Map<String, Integer> bestKspSlot;
        int k = 0;

        while (k < kspaths.size() && kspaths.get(k) != null) {
            // Se inicializa todo el espectro como libre
            for (int i = 0; i < capacity; i++) {
                Arrays.fill(so[i], false);
            }
            GraphPath<Integer, Link> ksp = kspaths.get(k);

            // Se setean los slots libres
            for (int i = 0; i < capacity; i++) {
                for (Link link : ksp.getEdgeList()) {
                    for (int core = 0; core < cores; core++) {
                        FrequencySlot fs = link.getCores().get(core).getFrequencySlots().get(i);
                        if (!fs.isFree()) {
                            //TODO: agregar control de crosstalk acá
                            so[i][core] = true;
                        }
                    }
                }
            }
            int count, j;
            List<Integer> kspCores = new ArrayList<>();
            capacity:
            for (int i = 0; i < capacity; i++) {
                count = 0;
                if (!isFullyOccupied(so[i])) {
                    for (j = i; j < capacity; j++) {
                        if (!isFullyOccupied(so[j])) {
                            count++;
                        } else {
                            i = j;
                            break;
                        }
                        if (count == demand.getFs()) {
                            // TODO: Agregar control de cuando no hay core libre
                            for (Link link : kspaths.get(k).getEdgeList()) {
                                kspCores.add(getFreeCore(so[j]));
                            }
                            kspPlaced.add(kspaths.get(k));
                            kspPlacedCores.add(kspCores);
                            break capacity;
                        }
                    }
                    if (j == capacity) {
                        break;
                    }
                }
            }
            k++;
        }
        if (kspPlaced.isEmpty()) {
            return null;
        }
        //Ksp ubidados ahora se debe elegir el mejor
        bestKspSlot = countCuts(graph, kspPlaced, capacity, demand.getFs(), kspPlacedCores);
        EstablishedRoute establisedRoute = new EstablishedRoute((kspPlaced.get(bestKspSlot.get("ksp")).getEdgeList()),
                bestKspSlot.get("slot"), demand.getFs(), demand.getLifetime(),
                demand.getSource(), demand.getDestination(), kspPlacedCores.get(bestKspSlot.get("ksp")));
        return establisedRoute;
    }

    public static Map countCuts(Graph<Integer, Link> graph, List<GraphPath<Integer, Link>> ksp, int capacity, int fs, List<List<Integer>> kspCores) {
        Map<String, Integer> slotCuts;
        ArrayList<Map<String, Integer>> bestKspSlot = new ArrayList<>();

        for (int k = 0; k < ksp.size(); k++) {
            slotCuts = numCuts(ksp.get(k), graph, capacity, fs, kspCores.get(k));
            // Primera vez o si encuentra encuentra un resultado mejor (menos cuts)
            if (bestKspSlot.isEmpty() || slotCuts.get("cuts") < bestKspSlot.get(0).get("cuts")) {
                // Limpiamos el array porque pueden haber más de un resultado guardado
                bestKspSlot.clear();
                // Guardamos el indice del mejor ksp
                slotCuts.put("ksp", k);
                bestKspSlot.add(slotCuts);
                // Si tienen igual cantidad de cortes guardamos
            } else if (Objects.equals(slotCuts.get("cuts"), bestKspSlot.get(0).get("cuts"))) {
                slotCuts.put("ksp", k);
                bestKspSlot.add(slotCuts);
            }
        }
//        if (slotCuts.size() == 1) //Solo un resultado
//            return bestKspSlot.get(0);

        int finalPath;
        finalPath = alignmentCalc(ksp, graph, bestKspSlot, kspCores);
        return bestKspSlot.get(finalPath);
    }

    private static Boolean isFullyOccupied(Boolean[] so) {
        for (Boolean isOccupied : so) {
            if (!isOccupied) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    private static Integer getFreeCore(Boolean[] so) {
        for (int core = 0; core < so.length; core++) {
            if (!so[core]) {
                return core;
            }
        }
        return -1;
    }

    public static Map numCuts(GraphPath<Integer, Link> ksp, Graph<Integer, Link> graph, int capacity, int fs, List<Integer> kspCores) {
        int cuts = -1;
        int slot = -1;
        Map<String, Integer> slotCuts = new HashMap<>();

        ArrayList<Integer> cIndexes;
        int cutAux = 0;
        cIndexes = searchIndexes(ksp, graph, capacity, fs);

        for (int slotIndex : cIndexes) {
            if (slotIndex != 0) {
                for (int i = 0; i < ksp.getEdgeList().size(); i++) {
                    Link link = ksp.getEdgeList().get(i);
                    Core core = link.getCores().get(kspCores.get(i));
                    // Si se encuentra un lugar vacio en el slot i - 1 del ksp actual
                    if (core.getFrequencySlots().get(slotIndex - 1).isFree()) {
                        cutAux++;
                    }

                }
            }

            if (cuts == -1 || cutAux < cuts) {
                cuts = cutAux;
                slot = slotIndex;
            }
            cutAux = 0;
        }

        slotCuts.put("cuts", cuts);
        slotCuts.put("slot", slot);
        if(slot == -1) {
            System.out.println("asd");
        }
        return slotCuts;

    }

    public static ArrayList<Integer> searchIndexes(GraphPath<Integer, Link> ksp,
            Graph<Integer, Link> graph, Integer capacity, Integer fsQ) {

        // Inicialmente el primer slot puede ser candidato
        boolean canBeCandidate = true;
        ArrayList<Integer> indexes = new ArrayList<>();
        boolean free;
        int slots = 0;

        for (int i = 0; i < capacity; i++) {
            free = true;
            for (Link link : ksp.getEdgeList()) {
                for (Core core : link.getCores()) {
                    FrequencySlot fs = core.getFrequencySlots().get(i);
                    // Se verifica que todo el camino este libre en el slot i
                    if (!fs.isFree()) {
                        // Cuando encuentra un slot ocupado entonces el siguiente puede ser candidato
                        canBeCandidate = true;
                        free = false;
                        slots = 0;
                        break;
                    }
                }
            }
            // Si esta libre se aumenta el contador
            if (free) {
                slots++;
            }

            // Si puede contener la cantidad de fs y es candidadto valido entonces se agrega
            if (slots == fsQ && canBeCandidate) {
                indexes.add(i - fsQ + 1);
                slots = 0;
                canBeCandidate = false;
            }
        }
        return indexes;
    }

    public static int alignmentCalc(List<GraphPath<Integer, Link>> ksp,
            Graph<Integer, Link> graph, ArrayList<Map<String, Integer>> kspSlot, List<List<Integer>> kspCores) {
        int lessMisalign = -1;
        int lessMisalignAux;
        int bestIndex = 0;
        int c = 0;
        for (Map<String, Integer> k : kspSlot) {
            lessMisalignAux = countMisalignment(ksp.get(k.get("ksp")), graph, k.get("slot"), kspCores.get(k.get("ksp")));
            if (lessMisalign == -1 || lessMisalignAux < lessMisalign) {
                lessMisalign = lessMisalignAux;
                bestIndex = c;//Tengo que guardar el indice en kspSlot, no el indice en ksp
            }
            c++;
        }
        return bestIndex;
    }

    public static int countMisalignment(GraphPath<Integer, Link> ksp, Graph<Integer, Link> graph, int slot, List<Integer> kspCores) {
        int missalign = 0;
        // Por cada enlace
        for (int i = 0; i < ksp.getEdgeList().size(); i++) {
            Link link = ksp.getEdgeList().get(i);
            // Vecinos por el nodo origen
            for (Link fromNeighbour : graph.outgoingEdgesOf(link.getFrom())) {
                // Verificamos que el vecino no este en el camino
                if (!ksp.getEdgeList().contains(fromNeighbour)) {
                    //Si el slot elegido esta ocupado ocurre desalineación
                    try {
                        //System.out.println("i = " + i + "; slot = " + slot);
                        //System.out.println("link: origen: " + link.getFrom() + " ; destino: " + link.getTo());
                        //System.out.println("link-vecino: origen: " + fromNeighbour.getFrom() + " ; destino: " + fromNeighbour.getTo());
                        if (fromNeighbour.getCores().get(kspCores.get(i)).getFrequencySlots().get(slot).isFree()) {
                            missalign++;
                        }
                    } catch (IndexOutOfBoundsException ex) {
                        System.out.println("asd");
                    }
                }
            }
            // Vecinos por el nodo destino
            for (Link toNeighbour : graph.outgoingEdgesOf(link.getTo())) {
                // Verificamos que el vecino no este en el camino
                if (!ksp.getEdgeList().contains(toNeighbour)) {
                    try {
                        if (toNeighbour.getCores().get(kspCores.get(i)).getFrequencySlots().get(slot).isFree())//Si el slot elegido esta ocupado ocurre desalineación
                        {
                            missalign++;
                        }
                    } catch (IndexOutOfBoundsException ex) {
                        System.out.println("asd");
                    }
                }
            }
        }
        return missalign;
    }
}
