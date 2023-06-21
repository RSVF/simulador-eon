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
import py.una.pol.simulador.eon.utils.Utils;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class Algorithms {

    public static EstablishedRoute ruteoCoreUnico(Graph<Integer, Link> graph, List<GraphPath<Integer, Link>> kspaths, Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk) {
        int k = 0;

        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        Integer fsIndexBegin = null;
        Integer selectedIndex = 0;
        Integer selectedFS = 0;
        // Iteramos los KSP elegidos
        while (k < kspaths.size() && kspaths.get(k) != null) {
            fsIndexBegin = null;
            GraphPath<Integer, Link> ksp = kspaths.get(k);
            // Recorremos los FS
            for (int i = 0; i < capacity - demand.getFs(); i++) {
                for (int core = 0; core < cores; core++) {
                    List<Link> enlacesLibres = new ArrayList<>();
                    List<Integer> kspCores = new ArrayList<>();
                    List<BigDecimal> crosstalkFSList = new ArrayList<>();
                    for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {
                        crosstalkFSList.add(BigDecimal.ZERO);
                    }
                    for (Link link : ksp.getEdgeList()) {
                        if (core < cores) {
                            List<FrequencySlot> bloqueFS = link.getCores().get(core).getFrequencySlots().subList(i, i + demand.getFs());

                            // Controla si está ocupado por una demanda
                            if (isFSBlockFree(bloqueFS)) {

                                // Control de crosstalk
                                for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {
                                    BigDecimal crosstalkRuta = crosstalkFSList.get(0);
                                    if (isCrosstalkFree(bloqueFS.get(fsCrosstalkIndex), maxCrosstalk, crosstalkRuta)) {
                                        if (isNextToCrosstalkFreeCores(link, maxCrosstalk, core, i, demand.getFs())) {
                                            enlacesLibres.add(link);
                                            kspCores.add(core);
                                            fsIndexBegin = i;
                                            selectedIndex = k;
                                            selectedFS = fsCrosstalkIndex;
                                            crosstalkRuta = crosstalkRuta.add(Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), Utils.crosstalkPerUnitLenght(), link.getDistance())));
                                            crosstalkFSList.set(fsCrosstalkIndex, crosstalkRuta);
                                            fsCrosstalkIndex = demand.getFs();
                                            // Si todos los enlaces tienen el mismo bloque de FS libre, se agrega la ruta a la lista de rutas establecidas.
                                            if (enlacesLibres.size() == ksp.getEdgeList().size()) {
                                                kspPlaced.add(kspaths.get(selectedIndex));
                                                kspPlacedCores.add(kspCores);
                                                k = kspaths.size();
                                                i = capacity;
                                                core = cores;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            k++;
        }
        EstablishedRoute establisedRoute;
        if (fsIndexBegin != null && !kspPlaced.isEmpty()) {
            establisedRoute = new EstablishedRoute(kspPlaced.get(0).getEdgeList(),
                    fsIndexBegin, demand.getFs(), demand.getLifetime(),
                    demand.getSource(), demand.getDestination(), kspPlacedCores.get(0));
        } else {
            System.out.println("Bloqueo");
            establisedRoute = null;
        }
        return establisedRoute;

    }

    public static EstablishedRoute ruteoCoreMultiple(Graph<Integer, Link> graph, List<GraphPath<Integer, Link>> kspaths, Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk) {
        int k = 0;

        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        Integer fsIndexBegin = null;
        Integer selectedIndex = 0;
        Integer selectedFS = 0;
        // Iteramos los KSP elegidos
        while (k < kspaths.size() && kspaths.get(k) != null) {
            fsIndexBegin = null;
            GraphPath<Integer, Link> ksp = kspaths.get(k);
            // Recorremos los FS
            for (int i = 0; i < capacity - demand.getFs(); i++) {
                List<Link> enlacesLibres = new ArrayList<>();
                List<Integer> kspCores = new ArrayList<>();
                List<BigDecimal> crosstalkFSList = new ArrayList<>();
                for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {
                    crosstalkFSList.add(BigDecimal.ZERO);
                }
                for (Link link : ksp.getEdgeList()) {
                    for (int core = 0; core < cores; core++) {
                        if (i < capacity - demand.getFs()) {
                            List<FrequencySlot> bloqueFS = link.getCores().get(core).getFrequencySlots().subList(i, i + demand.getFs());

                            // Controla si está ocupado por una demanda
                            if (isFSBlockFree(bloqueFS)) {

                                // Control de crosstalk
                                for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {
                                    BigDecimal crosstalkRuta = crosstalkFSList.get(0);
                                    if (isCrosstalkFree(bloqueFS.get(fsCrosstalkIndex), maxCrosstalk, crosstalkRuta)) {
                                        if (isNextToCrosstalkFreeCores(link, maxCrosstalk, core, i, demand.getFs())) {
                                            enlacesLibres.add(link);
                                            kspCores.add(core);
                                            fsIndexBegin = i;
                                            selectedIndex = k;
                                            selectedFS = fsCrosstalkIndex;
                                            crosstalkRuta = crosstalkRuta.add(Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), Utils.crosstalkPerUnitLenght(), link.getDistance())));
                                            crosstalkFSList.set(fsCrosstalkIndex, crosstalkRuta);
                                            fsCrosstalkIndex = demand.getFs();
                                            // Si todos los enlaces tienen el mismo bloque de FS libre, se agrega la ruta a la lista de rutas establecidas.
                                            if (enlacesLibres.size() == ksp.getEdgeList().size()) {
                                                kspPlaced.add(kspaths.get(selectedIndex));
                                                kspPlacedCores.add(kspCores);
                                                k = kspaths.size();
                                                i = capacity;
                                                core = cores;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            k++;
        }
        EstablishedRoute establisedRoute;
        if (fsIndexBegin != null && !kspPlaced.isEmpty()) {
            establisedRoute = new EstablishedRoute(kspPlaced.get(0).getEdgeList(),
                    fsIndexBegin, demand.getFs(), demand.getLifetime(),
                    demand.getSource(), demand.getDestination(), kspPlacedCores.get(0));
        } else {
            System.out.println("Bloqueo");
            establisedRoute = null;
        }
        return establisedRoute;

    }

    private static Boolean isFSBlockFree(List<FrequencySlot> bloqueFS) {
        for (FrequencySlot fs : bloqueFS) {
            if (!fs.isFree()) {
                return false;
            }
        }
        return true;
    }

    private static Boolean isCrosstalkFree(FrequencySlot fs, BigDecimal maxCrosstalk, BigDecimal crosstalkRuta) {
        BigDecimal crosstalkActual = crosstalkRuta.add(fs.getCrosstalk());
        return crosstalkActual.compareTo(maxCrosstalk) < 0;
    }

    private static Boolean isNextToCrosstalkFreeCores(Link link, BigDecimal maxCrosstalk, Integer core, Integer fsIndexBegin, Integer fsWidth) {
        List<Integer> vecinos = Utils.getCoreVecinos(core);
        for (Integer coreVecino : vecinos) {
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                BigDecimal crosstalkASumar = Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), Utils.crosstalkPerUnitLenght(), link.getDistance()));
                BigDecimal crosstalk = fsVecino.getCrosstalk().add(crosstalkASumar);
                //BigDecimal crosstalkDB = Utils.toDB(crosstalk.doubleValue());
                if (crosstalk.compareTo(maxCrosstalk) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public static EstablishedRoute genericRouting2(Graph<Integer, Link> graph,
            List<GraphPath<Integer, Link>> kspaths, Demand demand,
            Integer capacity, Integer cores) {

        // Iteramos los KSPaths
        GraphPath<Integer, Link> ksp = kspaths.get(0);

        // Recorremos los slots de frecuencia
        for (int i = 0; i < capacity - demand.getFs(); i++) {
            for (Link link : ksp.getEdgeList()) {
                for (int core = 0; core < cores; core++) {
                    FrequencySlot fs = link.getCores().get(core).getFrequencySlots().get(i);
                    if (fs.isFree()) {

                    }
                }
            }
        }

        return null;
    }

    public static EstablishedRoute genericRouting(Graph<Integer, Link> graph, List<GraphPath<Integer, Link>> kspaths, Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk) {
        int k = 0;
        int begin;
        int end;
        int beginSlot = -1;
        int selectedPath = -1;
        float kspMaxSC = -1;
        Boolean so[][] = new Boolean[capacity][cores];

        EstablishedRoute establisedRoute = new EstablishedRoute();
        while (k < kspaths.size() && kspaths.get(k) != null) {
            float maxSC = -1;
            // Se inicializa todo el espectro como libre
            for (int i = 0; i < capacity; i++) {
                Arrays.fill(so[i], false);
            }

            // Se carga la matriz de ocupación del espectro
            GraphPath<Integer, Link> ksp = kspaths.get(k);
            for (int i = 0; i < capacity; i++) {
                BigDecimal crosstalkEnEnlace = BigDecimal.ZERO;
                for (Link link : ksp.getEdgeList()) {
                    for (int core = 0; core < cores; core++) {
                        FrequencySlot fs = link.getCores().get(core).getFrequencySlots().get(i);
                        crosstalkEnEnlace = crosstalkEnEnlace.add(fs.getCrosstalk());
                        if (!fs.isFree()) {
                            so[i][core] = true;
                        }
                        if (crosstalkEnEnlace.compareTo(maxCrosstalk) > 0) {
                            so[i][core] = true;
                        }
                    }
                }
            }

            // Se cargan los bloques de FS utilizables en la red
            List<HashMap> espectralBlocks = new ArrayList<>();
            for (int core = 0; core < cores; core++) {
                for (int i = 0; i < capacity; i++) {
                    if (!so[i][core]) {
                        begin = i;
                        while (i < capacity && !so[i][core]) {
                            i++;
                        }
                        end = i - 1;
                        if (end - begin + 1 >= demand.getFs()) { //bloque que puede utilizarse
                            HashMap<String, Integer> block = new HashMap();
                            block.put("begin", begin);
                            block.put("end", end);
                            block.put("core", core);
                            espectralBlocks.add(block);
                        }
                    }
                }
            }

            // Por cada bloque de espectro libre en la red, se trata asignar a enlaces de la ruta.
            for (HashMap<String, Integer> espectralBlock : espectralBlocks) {
                float spectrumConsecutiveness = 0;
                int blockBegin = (int) espectralBlock.get("begin");
                for (Link link : ksp.getEdgeList()) {

                    int linkBlocks = 0;
                    for (int i = 0; i < capacity; i++) {
                        for (int core = 0; core < cores; core++) {
                            if (link.getCores().get(core).getFrequencySlots().get(i).isFree()) {
                                while (i < capacity && !link.getCores().get(core).getFrequencySlots().get(i).isFree()) {  //calculamos la cantidad de bloques del Link
                                    i++;
                                }
                                linkBlocks++;
                            }
                        }
                    }

                    float sum = 0;
                    float fsCount = 0;

                    /*for (int c = 0; c < capacity - 1; c++) {
                        if (c < blockBegin || c > blockBegin + demand.getFs() - 1) {
                            int slot = link.getCores().get(blockCore).getFrequencySlots().get(c).isFree() ? 1 : 0;
                            int nextSlot = link.getCores().get(blockCore).getFrequencySlots().get(c + 1).isFree() ? 1 : 0;
                            sum += slot * nextSlot;
                            fsCount += slot;
                        }
                    }
                    fsCount += link.getCores().get(blockCore).getFrequencySlots().get(capacity - 1).isFree() ? 1 : 0; //para el ultimo slot
                     */
                    spectrumConsecutiveness += (sum / linkBlocks) * (fsCount / capacity);  //acumulamos el cl de los links

                }

                if (spectrumConsecutiveness > maxSC) {
                    maxSC = spectrumConsecutiveness;
                    selectedPath = k;
                    beginSlot = blockBegin;

                }
            }
            if (maxSC > kspMaxSC) {
                kspMaxSC = maxSC;
                establisedRoute.setPath(kspaths.get(selectedPath).getEdgeList());
                establisedRoute.setFsIndexBegin(beginSlot);
            }
            k++;
        }
        if (establisedRoute.getPath() != null) {
            establisedRoute.setFsWidth(demand.getFs());
            establisedRoute.setLifetime(demand.getLifetime());
            establisedRoute.setFrom(demand.getSource());
            establisedRoute.setTo(demand.getDestination());
            System.out.println("RUTA ESTABLECIDA: " + establisedRoute);
            return establisedRoute;
        }

        return null;

    }

    public static EstablishedRoute fa(Graph<Integer, Link> graph,
            List<GraphPath<Integer, Link>> kspaths, Demand demand,
            Integer capacity, Integer cores, BigDecimal maxCrosstalk) {

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
                BigDecimal crosstalkEnEnlace = BigDecimal.ZERO;
                for (Link link : ksp.getEdgeList()) {
                    for (int core = 0; core < cores; core++) {
                        FrequencySlot fs = link.getCores().get(core).getFrequencySlots().get(i);
                        crosstalkEnEnlace = crosstalkEnEnlace.add(fs.getCrosstalk());
                        // Si el slot ya está ocupado, o el crosstalk supera el límite máximo, se marca como ocupado
                        if (!fs.isFree()) {
                            so[i][core] = true;
                        }
                        //BigDecimal db = Utils.toDB(crosstalkEnEnlace.doubleValue());
                        //BigDecimal maxCTdb = Utils.toDB(maxCrosstalk.doubleValue());
                        //System.out.println("Crosstalk en enlace = " + db.toPlainString());
                        //System.out.println("Crosstalk máximo = " + maxCTdb.toPlainString());
                        if (crosstalkEnEnlace.compareTo(maxCrosstalk) > 0) {
                            so[i][core] = true;
                        }

                        for (int coreVecino = 0; coreVecino < cores; coreVecino++) {
                            if (coreVecino != core) {
                                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                                BigDecimal crosstalkASumar = Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), Utils.crosstalkPerUnitLenght(), link.getDistance()));
                                BigDecimal crosstalk = fsVecino.getCrosstalk().add(crosstalkASumar);
                                //BigDecimal crosstalkDB = Utils.toDB(crosstalk.doubleValue());
                                if (crosstalk.compareTo(maxCrosstalk) > 0) {
                                    so[i][core] = true;
                                }
                            }
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
                            for (Link link : kspaths.get(k).getEdgeList()) {
                                if (getFreeCore(so[j]) > -1) {
                                    kspCores.add(getFreeCore(so[j]));
                                }
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
            //System.out.println("Bloqueado por crosstalk");
            return null;
        }
        //Ksp ubidados ahora se debe elegir el mejor
        bestKspSlot = countCuts(graph, kspPlaced, capacity, demand.getFs(), kspPlacedCores);
        EstablishedRoute establisedRoute = new EstablishedRoute((kspPlaced.get(bestKspSlot.get("ksp")).getEdgeList()),
                bestKspSlot.get("slot"), demand.getFs(), demand.getLifetime(),
                demand.getSource(), demand.getDestination(), kspPlacedCores.get(bestKspSlot.get("ksp")));
        return establisedRoute;
    }

    public static EstablishedRoute faca(Graph<Integer, Link> graph, List<GraphPath<Integer, Link>> kspaths,
            Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk) {
        int count;
        Boolean so[][] = new Boolean[capacity][cores]; //Representa la ocupación del espectro de todos los enlaces.
        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        List<List<Integer>> kspCoresList = new ArrayList<>();
        int k = 0;
        while (k < kspaths.size() && kspaths.get(k) != null) {
            // Se inicializa todo el espectro como libre
            for (int i = 0; i < capacity; i++) {
                Arrays.fill(so[i], false);
            }
            GraphPath<Integer, Link> ksp = kspaths.get(k);

            // Se setean los slots libres
            for (int i = 0; i < capacity; i++) {
                BigDecimal crosstalkEnEnlace = BigDecimal.ZERO;
                for (Link link : ksp.getEdgeList()) {
                    for (int core = 0; core < cores; core++) {
                        FrequencySlot fs = link.getCores().get(core).getFrequencySlots().get(i);
                        crosstalkEnEnlace = crosstalkEnEnlace.add(fs.getCrosstalk());
                        // Si el slot ya está ocupado, o el crosstalk supera el límite máximo, se marca como ocupado
                        if (!fs.isFree()) {
                            so[i][core] = true;
                        }
                        if (crosstalkEnEnlace.compareTo(maxCrosstalk) < 0) {
                            so[i][core] = true;
                        }
                    }
                }
            }

            int j;
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
                            for (Link link : kspaths.get(k).getEdgeList()) {
                                if (getFreeCore(so[j]) > -1) {
                                    kspCores.add(getFreeCore(so[j]));
                                }
                            }
                            kspPlaced.add(kspaths.get(k));
                            break capacity;
                        }
                    }
                    if (j == capacity) {
                        break;
                    }
                }
            }
            kspCoresList.add(kspCores);
            k++;

        }

        System.out.println("CANTIDAD DE KSP UBICADOS: " + kspPlaced.size());
        if (kspPlaced.size() == 0) {
            return null;
        }
        Map<String, Integer> slotCuts = new HashMap<>();
        ArrayList<Integer> cIndexes;
        double Fcmt = 9999999;
        double FcmtAux;
        int selectedPath = -1;
        int slot = -1;
        for (k = 0; k < kspPlaced.size(); k++) {
            slotCuts = numCuts(kspPlaced.get(k), graph, capacity, demand.getFs(), kspCoresList.get(k));
            System.out.println("Mejor slot: " + slotCuts.get("slot") + " con " + slotCuts.get("cuts") + " cuts");
            if (slotCuts != null) {

                double misalignement = countMisalignment(kspPlaced.get(k), graph, slotCuts.get("slot"), kspCoresList.get(k));
                double jumps = kspPlaced.get(k).getLength();
                double freeCapacity = countFreeCapacity(kspPlaced.get(k), graph, capacity, kspCoresList.get(k));

                // CONVERTIR A MULTICORE
                double neighbours = countNeighbour(kspPlaced.get(k), graph);

                FcmtAux = slotCuts.get("cuts") + (misalignement / (demand.getFs() * neighbours)) + (jumps * (demand.getFs() / freeCapacity));

                if (FcmtAux < Fcmt) {
                    Fcmt = FcmtAux;
                    selectedPath = k;
                    slot = slotCuts.get("slot");
                    System.out.println("FCMT " + Fcmt);
                }
            }
        }

        if (selectedPath == -1) {
            return null;
        }
        List<Link> bestKsp = kspPlaced.get(selectedPath).getEdgeList();
        List<Integer> bestKspCores = kspCoresList.get(selectedPath);
        EstablishedRoute establisedRoute = new EstablishedRoute(bestKsp, slot, demand.getFs(), demand.getLifetime(), demand.getSource(), demand.getDestination(), bestKspCores);
        //System.out.println("RUTA ESTABLECIDA: " + establisedRoute);
        return establisedRoute;

    }

    public static int countNeighbour(GraphPath<Integer, Link> ksp, Graph<Integer, Link> graph) {
        List neighbours = new ArrayList();
        for (Link link : ksp.getEdgeList()) {
            for (Link fromNeighbour : graph.outgoingEdgesOf(link.getFrom())) {
                if (!ksp.getEdgeList().contains(fromNeighbour) && !neighbours.contains(fromNeighbour)) {
                    neighbours.add(fromNeighbour);
                }
            }
            for (Link toNeighbour : graph.outgoingEdgesOf(link.getTo())) {
                if (!ksp.getEdgeList().contains(toNeighbour) && !neighbours.contains(toNeighbour)) {

                    neighbours.add(toNeighbour);
                }
            }
        }
        System.out.println("Vecinos: " + neighbours);
        return neighbours.size();
    }

    public static int countFreeCapacity(GraphPath<Integer, Link> ksp, Graph<Integer, Link> graph, int capacity, List<Integer> kspCores) {

        int frees = 0;
        for (int i = 0; i < capacity; i++) {
            for (int k = 0; k < ksp.getEdgeList().size(); k++) {
                Link link = ksp.getEdgeList().get(k);
                FrequencySlot fs = link.getCores().get(kspCores.get(k)).getFrequencySlots().get(i);
                if (fs.isFree()) {
                    frees++;
                }
            }
        }

        return frees;
    }

    public static Map<String, Integer> countCuts(Graph<Integer, Link> graph, List<GraphPath<Integer, Link>> ksp, int capacity, int fs, List<List<Integer>> kspCores) {
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

    public static Map<String, Integer> numCuts(GraphPath<Integer, Link> ksp, Graph<Integer, Link> graph, int capacity, int fs, List<Integer> kspCores) {
        int cuts = -1;
        int slot = -1;
        Map<String, Integer> slotCuts = new HashMap<>();

        ArrayList<Integer> cIndexes;
        int cutAux = 0;
        cIndexes = searchIndexes(ksp, graph, capacity, fs, kspCores);
        //System.out.println("cIndexes: " + cIndexes.size());    
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
        if (slot == -1) {
            //System.out.println("asd");
            //System.out.println("asdfg");
        }
        return slotCuts;

    }

    public static ArrayList<Integer> searchIndexes(GraphPath<Integer, Link> ksp,
            Graph<Integer, Link> graph, Integer capacity, Integer fsQ, List<Integer> kspCores) {

        // Inicialmente el primer slot puede ser candidato
        boolean canBeCandidate = true;
        ArrayList<Integer> indexes = new ArrayList<>();
        boolean free;
        int slots = 0;

        for (int i = 0; i < capacity; i++) {
            free = true;
            // System.out.println("Tamaño KSP: " + ksp.getEdgeList().size());
            //System.out.println("Tamaño KSPCores: " + kspCores.size());
            for (int j = 0; j < ksp.getEdgeList().size(); j++) {
                Link link = ksp.getEdgeList().get(j);
                Core core = link.getCores().get(kspCores.get(j));
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
        if (indexes.isEmpty()) {
            //System.out.println("testSearchIndexes");
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
                        //System.out.println("qwe");
                        // System.out.println("qwe");
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
                        //System.out.println("qwe");
                    }
                }
            }
        }
        return missalign;
    }
}
