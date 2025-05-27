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

public class Algorithms {

    public static EstablishedRoute ruteoCoreMultiple(Graph<Integer, Link> graph, Demand demand, Integer capacity,
                                                     Integer cores, BigDecimal umbralRuidoMax, Double crosstalkPerUnitLength) {
        int k = 0;
        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        Integer fsIndexBegin = null;
        Integer selectedIndex;
        // Iteramos los KSP elegidos
        // k caminos más cortos entre source y destination de la demanda actual

        KShortestSimplePaths<Integer, Link> kspFinder = new KShortestSimplePaths<>(graph);

        List<GraphPath<Integer, Link>> kspaths = kspFinder.getPaths(demand.getSource(), demand.getDestination(), 5);

        while (k < kspaths.size() && kspaths.get(k) != null) {
            fsIndexBegin = null;
            GraphPath<Integer, Link> ksp = kspaths.get(k);

            // RECORRIDO DE LOS FS
            for (int indiceFS = 0; indiceFS <= capacity - demand.getFs(); indiceFS++) {
                List<Link> enlacesLibres = new ArrayList<>();
                List<Integer> kspCores = new ArrayList<>();
                List<BigDecimal> crosstalkFSList = new ArrayList<>();

                for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {
                    crosstalkFSList.add(BigDecimal.ZERO);
                }

                // RECORRIDO DE LOS ENLACES (RED)
                for (Link link : ksp.getEdgeList()) {
                    // RECORRIDO DE LOS CORES
                    for (int core = 0; core < cores; core++) {

                        // OBTIENE EL BLOQUE DE FS (RED)
                        List<FrequencySlot> bloqueFS = link.getCores().get(core).getFrequencySlots().subList(indiceFS,
                                indiceFS + demand.getFs());

                        // CONTROLA SI ESTA OCUPADO POR UNA DEMANDA
                        if (isFSBlockFree(bloqueFS)) {

                            // CONTROL DE RUIDO

                            if (esBloqueFsMayorUmbralRuido(bloqueFS, umbralRuidoMax, crosstalkFSList)) {

                                // CONTROL DE RUIDO EN LOS NUCLEOS VECINOS
                                if (isNextToCrosstalkFreeCores(link, umbralRuidoMax, core, indiceFS, demand.getFs(),
                                        crosstalkPerUnitLength)) {

                                    enlacesLibres.add(link);
                                    kspCores.add(core);
                                    fsIndexBegin = indiceFS;
                                    selectedIndex = k;
                                    for (int crosstalkFsListIndex = 0; crosstalkFsListIndex < crosstalkFSList.size(); crosstalkFsListIndex++) {

                                        BigDecimal crosstalkRuta = crosstalkFSList.get(crosstalkFsListIndex);
                                        crosstalkRuta = crosstalkRuta.add(Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core),
                                                crosstalkPerUnitLength, link.getDistance())));

                                        crosstalkFSList.set(crosstalkFsListIndex, crosstalkRuta);
                                    }
                                    core = cores;
                                    // Si todos los enlaces tienen el mismo bloque de FS libre, se agrega la ruta a
                                    // la lista de rutas establecidas.
                                    if (enlacesLibres.size() == ksp.getEdgeList().size()) {
                                        kspPlaced.add(kspaths.get(selectedIndex));
                                        kspPlacedCores.add(kspCores);
                                        k = kspaths.size();
                                        indiceFS = capacity;
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

            establisedRoute = new EstablishedRoute(kspPlaced.get(0).getEdgeList(), fsIndexBegin, demand.getFs(), demand.getLifetime(), demand.getSource(),
                    demand.getDestination(), kspPlacedCores.get(0), null, kspPlaced.get(0).getWeight(), null, null);
        } else {
            // System.out.println("Bloqueo");
            establisedRoute = null;
        }
        return establisedRoute;

    }

    public static Boolean esBloqueFsMayorUmbralRuido(List<FrequencySlot> bloqueFS, BigDecimal umbralRuidoMax,
                                                     List<BigDecimal> crosstalkRuta) {
        // ESTA PORCIÓN DE CODIGO CONTROLA SI EL BLOQUE DE FS TIENE RUIDO QUE SUPERE EL
        // UMBRAL DE RUIDO MÁXIMO
        for (int i = 0; i < bloqueFS.size(); i++) {
            BigDecimal crosstalkActual = crosstalkRuta.get(i).add(bloqueFS.get(i).getCrosstalk());
            if (crosstalkActual.compareTo(umbralRuidoMax) > 0) {
                return false;
            }
        }
        return true;
    }

    private static Boolean isFSBlockFree(List<FrequencySlot> bloqueFS) {
        for (FrequencySlot fs : bloqueFS) {
            if (!fs.isFree()) {
                return false;
            }
        }
        return true;
    }

    public static Boolean isNextToCrosstalkFreeCores(Link link, BigDecimal maxCrosstalk, Integer core, Integer fsIndexBegin, Integer fsWidth, Double crosstalkPerUnitLength) {
        List<Integer> vecinos = Utils.getCoreVecinos(core);
        for (Integer coreVecino : vecinos) {
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                if (!fsVecino.isFree()) {
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

    public static EstablishedRoute reRuteoCaminoOriginal(Graph<Integer, Link> graph, Demand demand, Integer capacity,
                                                     Integer cores, BigDecimal umbralRuidoMax, Double crosstalkPerUnitLength) {
        int k = 0;
        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        Integer fsIndexBegin = null;
        Integer selectedIndex;
            List<Link> ksp = demand.getPath();
            // RECORRIDO DE LOS FS
            for (int indiceFS = 0; indiceFS <= capacity - demand.getFs(); indiceFS++) {
                List<Link> enlacesLibres = new ArrayList<>();
                List<Integer> kspCores = new ArrayList<>();
                List<BigDecimal> crosstalkFSList = new ArrayList<>();

                for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {
                    crosstalkFSList.add(BigDecimal.ZERO);
                }

                // RECORRIDO DE LOS ENLACES (RED)
                for (Link link : ksp) {
                    // RECORRIDO DE LOS CORES
                    for (int core = 0; core < cores; core++) {

                        // OBTIENE EL BLOQUE DE FS (RED)
                        List<FrequencySlot> bloqueFS = link.getCores().get(core).getFrequencySlots().subList(indiceFS,
                                indiceFS + demand.getFs());

                        // CONTROLA SI ESTA OCUPADO POR UNA DEMANDA
                        if (isFSBlockFree(bloqueFS)) {

                            // CONTROL DE RUIDO

                            if (esBloqueFsMayorUmbralRuido(bloqueFS, umbralRuidoMax, crosstalkFSList)) {

                                // CONTROL DE RUIDO EN LOS NUCLEOS VECINOS
                                if (isNextToCrosstalkFreeCores(link, umbralRuidoMax, core, indiceFS, demand.getFs(),
                                        crosstalkPerUnitLength)) {

                                    enlacesLibres.add(link);
                                    kspCores.add(core);
                                    fsIndexBegin = indiceFS;
                                    selectedIndex = k;
                                    for (int crosstalkFsListIndex = 0; crosstalkFsListIndex < crosstalkFSList.size(); crosstalkFsListIndex++) {

                                        BigDecimal crosstalkRuta = crosstalkFSList.get(crosstalkFsListIndex);
                                        crosstalkRuta = crosstalkRuta.add(Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core),
                                                crosstalkPerUnitLength, link.getDistance())));

                                        crosstalkFSList.set(crosstalkFsListIndex, crosstalkRuta);
                                    }
                                    core = cores;
                                    // Si todos los enlaces tienen el mismo bloque de FS libre, se agrega la ruta a
                                    // la lista de rutas establecidas.
                                    if (enlacesLibres.size() == ksp.size()) {
                                        kspPlacedCores.add(kspCores);
                                        indiceFS = capacity;
                                    }
                                }
                            }
                        }
                    }
                }
            }


        EstablishedRoute establisedRoute;
        if (fsIndexBegin != null && !kspPlacedCores.isEmpty()) {
            Double distancia = 0.0;
            for (Link link : ksp) {
                distancia = distancia + link.getDistance();
            }
            establisedRoute = new EstablishedRoute(ksp, fsIndexBegin, demand.getFs(),
                    demand.getLifetime(), demand.getSource(), demand.getDestination(), kspPlacedCores.get(0), null, distancia, null, null);
        } else {
            // System.out.println("Bloqueo");
            establisedRoute = null;
        }
        return establisedRoute;

    }



    public static EstablishedRoute reRuteoCaminoOriginalSameLinkAndCore(Graph<Integer, Link> graph, Demand demand, Integer capacity,
                                                                        Integer cores, BigDecimal umbralRuidoMax, Double crosstalkPerUnitLength) {
        int k = 0;
        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        Integer fsIndexBegin = null;
        Integer selectedIndex;
        List<Integer> coresDemand = demand.getCoresRoute();
        List<Link> ksp = demand.getPath();
        // RECORRIDO DE LOS FS
        for (int indiceFS = 0; indiceFS <= capacity - demand.getFs(); indiceFS++) {
            List<Link> enlacesLibres = new ArrayList<>();
            List<Integer> kspCores = new ArrayList<>();
            List<BigDecimal> crosstalkFSList = new ArrayList<>();

            for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {
                crosstalkFSList.add(BigDecimal.ZERO);
            }

            // RECORRIDO DE LOS ENLACES (RED)
            for (int e = 0 ;  e < demand.getPath().size() ; e++) {
                Link link = demand.getPath().get(e);
                Integer core = coresDemand.get(e);
                // RECORRIDO DE LOS CORES

                    // OBTIENE EL BLOQUE DE FS (RED)
                    List<FrequencySlot> bloqueFS = link.getCores().get(e).getFrequencySlots().subList(indiceFS, indiceFS + demand.getFs());

                    // CONTROLA SI ESTA OCUPADO POR UNA DEMANDA
                    if (isFSBlockFree(bloqueFS)) {

                        // CONTROL DE RUIDO

                        if (esBloqueFsMayorUmbralRuido(bloqueFS, umbralRuidoMax, crosstalkFSList)) {

                            // CONTROL DE RUIDO EN LOS NUCLEOS VECINOS
                            if (isNextToCrosstalkFreeCores(link, umbralRuidoMax, coresDemand.get(e), indiceFS, demand.getFs(),
                                    crosstalkPerUnitLength)) {

                                enlacesLibres.add(link);
                                kspCores.add(coresDemand.get(e));
                                fsIndexBegin = indiceFS;
                                selectedIndex = k;
                                for (int crosstalkFsListIndex = 0; crosstalkFsListIndex < crosstalkFSList.size(); crosstalkFsListIndex++) {

                                    BigDecimal crosstalkRuta = crosstalkFSList.get(crosstalkFsListIndex);
                                    crosstalkRuta = crosstalkRuta.add(Utils.toDB(Utils.XT(Utils.getCantidadVecinos(coresDemand.get(e)),
                                            crosstalkPerUnitLength, link.getDistance())));

                                    crosstalkFSList.set(crosstalkFsListIndex, crosstalkRuta);
                                }

                                // Si todos los enlaces tienen el mismo bloque de FS libre, se agrega la ruta a
                                // la lista de rutas establecidas.
                                if (enlacesLibres.size() == ksp.size()) {
                                    kspPlacedCores.add(kspCores);
                                    indiceFS = capacity;
                                }
                            }
                        }
                    }

            }
        }
        EstablishedRoute establisedRoute;
        if (fsIndexBegin != null && !kspPlacedCores.isEmpty()) {
            Double distancia = 0.0;
            for (Link link : ksp) {
                distancia = distancia + link.getDistance();
            }
            establisedRoute = new EstablishedRoute(ksp, fsIndexBegin, demand.getFs(),
                    demand.getLifetime(), demand.getSource(), demand.getDestination(), kspPlacedCores.get(0), null, distancia, null, null);
        } else {
            // System.out.println("Bloqueo");
            establisedRoute = null;
        }
        return establisedRoute;

    }

}


