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
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
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

    public static EstablishedRoute ruteoCoreUnico(Graph<Integer, Link> graph, Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength) {
        int k = 0;

        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        Integer fsIndexBegin = null;
        Integer selectedIndex;
        // Iteramos los KSP elegidos

        KShortestSimplePaths<Integer, Link> kspFinder = new KShortestSimplePaths<>(graph);
        List<GraphPath<Integer, Link>> kspaths = kspFinder.getPaths(demand.getSource(), demand.getDestination(), 5);
        while (k < kspaths.size() && kspaths.get(k) != null) {
            fsIndexBegin = null;
            GraphPath<Integer, Link> ksp = kspaths.get(k);
            // Recorremos los FS
            for (int i = 0; i < capacity - demand.getFs(); i++) {
                for (int core = 0; core < cores; core++) {
                    List<Link> enlacesLibres = new ArrayList<>();
                    List<Integer> kspCores = new ArrayList<>();
                    List<BigDecimal> crosstalkFSList = new ArrayList<>();
                    // Se inicializa la lista de valores de crosstalk para cada slot de frecuencia del bloque
                    for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {
                        crosstalkFSList.add(BigDecimal.ZERO);
                    }
                    // Se recorre la ruta
                    for (Link link : ksp.getEdgeList()) {
                        if (core < cores) {
                            // Se obtiene los slots de frecuencia a verificar
                            List<FrequencySlot> bloqueFS = link.getCores().get(core).getFrequencySlots().subList(i, i + demand.getFs());

                            // Controla si está ocupado por una demanda
                            if (isFSBlockFree(bloqueFS)) {

                                // Control de crosstalk
                                for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {
                                    // Control de crosstalk en la ruta elegida
                                    BigDecimal crosstalkRuta = crosstalkFSList.get(fsCrosstalkIndex);
                                    if (isCrosstalkFree(bloqueFS.get(fsCrosstalkIndex), maxCrosstalk, crosstalkRuta)) {
                                        // Control de crosstalk en los cores vecinos
                                        if (isNextToCrosstalkFreeCores(link, maxCrosstalk, core, i, demand.getFs(), crosstalkPerUnitLength)) {
                                            enlacesLibres.add(link);
                                            kspCores.add(core);
                                            fsIndexBegin = i;
                                            selectedIndex = k;
                                            crosstalkRuta = crosstalkRuta.add(Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), crosstalkPerUnitLength, link.getDistance())));
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
            //System.out.println("Bloqueo");
            establisedRoute = null;
        }
        return establisedRoute;

    }

    public static EstablishedRoute ruteoCoreMultiple(Graph<Integer, Link> graph, Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength) {
        int k = 0;

        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        Integer fsIndexBegin = null;
        Integer selectedIndex;
        // Iteramos los KSP elegidos
        //k caminos más cortos entre source y destination de la demanda actual

        KShortestSimplePaths<Integer, Link> kspFinder = new KShortestSimplePaths<>(graph);
        List<GraphPath<Integer, Link>> kspaths = kspFinder.getPaths(demand.getSource(), demand.getDestination(), 5);
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
                                    BigDecimal crosstalkRuta = crosstalkFSList.get(fsCrosstalkIndex);
                                    if (isCrosstalkFree(bloqueFS.get(fsCrosstalkIndex), maxCrosstalk, crosstalkRuta)) {
                                        if (isNextToCrosstalkFreeCores(link, maxCrosstalk, core, i, demand.getFs(), crosstalkPerUnitLength)) {
                                            enlacesLibres.add(link);
                                            kspCores.add(core);
                                            fsIndexBegin = i;
                                            selectedIndex = k;
                                            crosstalkRuta = crosstalkRuta.add(Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), crosstalkPerUnitLength, link.getDistance())));
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
            //System.out.println("Bloqueo");
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
        return crosstalkActual.compareTo(maxCrosstalk) <= 0;
    }

    private static Boolean isNextToCrosstalkFreeCores(Link link, BigDecimal maxCrosstalk, Integer core, Integer fsIndexBegin, Integer fsWidth, Double crosstalkPerUnitLength) {
        List<Integer> vecinos = Utils.getCoreVecinos(core);
        for (Integer coreVecino : vecinos) {
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                if(!fsVecino.isFree()) {
                    BigDecimal crosstalkASumar = Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), crosstalkPerUnitLength, link.getDistance()));
                    BigDecimal crosstalk = fsVecino.getCrosstalk().add(crosstalkASumar);
                    //BigDecimal crosstalkDB = Utils.toDB(crosstalk.doubleValue());
                    if (crosstalk.compareTo(maxCrosstalk) >= 0) {
                        return false;
                    }
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
            List<HashMap<String, Integer>> espectralBlocks = new ArrayList<>();
            for (int core = 0; core < cores; core++) {
                for (int i = 0; i < capacity; i++) {
                    if (!so[i][core]) {
                        begin = i;
                        while (i < capacity && !so[i][core]) {
                            i++;
                        }
                        end = i - 1;
                        if (end - begin + 1 >= demand.getFs()) { //bloque que puede utilizarse
                            HashMap<String, Integer> block = new HashMap<>();
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
}
