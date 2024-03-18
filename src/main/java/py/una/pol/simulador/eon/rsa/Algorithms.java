package py.una.pol.simulador.eon.rsa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;

import py.una.pol.simulador.eon.Constants;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.utils.Utils;

/**
 * @author Fernando Ortiz Gaette
 */
public class Algorithms {
    /**
     * Algoritmo RSA con conmutación de núcleos
     *
     * @param graph                  Grafo de la topología de la red
     * @param demand                 Demanda a insertar
     * @param capacity               Capacidad de la red
     * @param cores                  Cantidad total de núcleos
     * @param maxCrosstalk           Máximo nivel de crosstalk permitido
     * @param crosstalkPerUnitLength Crosstalk por unidad de longitud (h) de la
     *                               fibra
     * @return Ruta establecida, o null si hay bloqueo
     */
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

        ordenarKsPathsPorPeso(kspaths);



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
                                        int a = 1;
                                        kspPlaced.clear();
                                        kspPlaced.add(kspaths.get(selectedIndex));
                                        kspPlacedCores.clear();
                                        kspPlacedCores.add(kspCores);

                                        //k = kspaths.size();
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
            establisedRoute = new EstablishedRoute(kspPlaced.get(0).getEdgeList(), fsIndexBegin, demand.getFs(),
                    demand.getLifetime(), demand.getSource(), demand.getDestination(), kspPlacedCores.get(0), null, null);
        } else {
            // System.out.println("Bloqueo");
            establisedRoute = null;
        }
        return establisedRoute;

    }


    public static EstablishedRoute ruteoCoreMultipleK(Graph<Integer, Link> graph, Demand demand, Integer capacity,
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
            establisedRoute = new EstablishedRoute(kspPlaced.get(0).getEdgeList(), fsIndexBegin, demand.getFs(),
                    demand.getLifetime(), demand.getSource(), demand.getDestination(), kspPlacedCores.get(0), null, null);
        } else {
            // System.out.println("Bloqueo");
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


    private static Boolean esBloqueFsMayorUmbralRuido(List<FrequencySlot> bloqueFS, BigDecimal umbralRuidoMax,
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

    private static Boolean isNextToCrosstalkFreeCores(Link link, BigDecimal maxCrosstalk, Integer core,
                                                      Integer fsIndexBegin, Integer fsWidth, Double crosstalkPerUnitLength) {
        List<Integer> vecinos = Utils.getCoreVecinos(core);

        for (Integer coreVecino : vecinos) {
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                if (!fsVecino.isFree()) {
                    BigDecimal crosstalkASumar = Utils
                            .toDB(Utils.XT(Utils.getCantidadVecinos(core), crosstalkPerUnitLength, link.getDistance()));
                    BigDecimal crosstalk = fsVecino.getCrosstalk().add(crosstalkASumar);
                    // BigDecimal crosstalkDB = Utils.toDB(crosstalk.doubleValue());
                    if (crosstalk.compareTo(maxCrosstalk) >= 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void inciarProcesoDesfragmentacion(List<EstablishedRoute> listaRutasActivas, Graph<Integer, Link> red,
                                                     Integer capacidadEnlace, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength) {

        // Se recorre todas las rutas activas, por cada ruta activa se calcula el BFR y se guarda en el objeto
        for (int ra = 0; ra < listaRutasActivas.size(); ra++) {
            EstablishedRoute rutaActiva = listaRutasActivas.get(ra);
            Double bfrRuta = bfrRutaActiva(rutaActiva, red, capacidadEnlace);
            rutaActiva.setBfrRuta(bfrRuta);
        }
        calcularDijstra(listaRutasActivas, red);

        // Ordenar la lista de rutas activas por BFR de forma descendente
        //ordenarRutasPorBfr(listaRutasActivas, Constants.ORDENAMIENTO_DESCENDENTE);
        //ordenarRutasPorFsDijstra(listaRutasActivas, Constants.ORDENAMIENTO_ASCENDENTE);
        //ordenarRutasPorDijstraFs(listaRutasActivas, Constants.ORDENAMIENTO_ASCENDENTE);
        ordenarRutasPorDijstra(listaRutasActivas, Constants.ORDENAMIENTO_ASCENDENTE);


        List<EstablishedRoute> sublista = obtenerPeoresRutasActivas(listaRutasActivas, Constants.PORCENTAJE_100);

        int eliminado = 0;
        while (eliminado < sublista.size()) {
            Utils.deallocateFs(red, listaRutasActivas.get(0), crosstalkPerUnitLength);
            listaRutasActivas.remove(0);
            eliminado++;
        }

        Double bfrRed = Algorithms.bfrRed(red, capacidadEnlace, 7);
        System.out.println("El BFR de la red luego de desasignar  es :" + bfrRed);


        // SE GENERA UNA LISTA DE DEMANDAS CON LA SUBLISTA, PARA LUEGO VOLVER A RERUTEAR
        List<Demand> listaDemandasR = generarDemandas(sublista);

        reProcesarDemandasCaminoOriginal(sublista, red, capacidadEnlace, maxCrosstalk, crosstalkPerUnitLength, 7, listaRutasActivas);
        //reProcesarDemandas(listaDemandasR, red, capacidadEnlace, maxCrosstalk, crosstalkPerUnitLength, 7, listaRutasActivas);
    }


    public static Double bfrRutaActiva(EstablishedRoute ruta, Graph<Integer, Link> red, Integer capacidadEnlace) {
        // BFRenlace = 1 - maxBloqLibre / (N - cantFSocupados))
        Double sumBfr = 0.0;
        Integer iEnlace = 0;
        for (Link link : ruta.getPath()) {
            // Por cada enlace se inicializa las variables
            Double bfrEnlace = 0.0;
            Integer cantFSocupados = 0;
            Integer auxBloqLibre = 0;
            Integer maxBloqLibre = 0;

            for (int indiceFs = 0; indiceFs < capacidadEnlace; indiceFs++) {

                Boolean ranuraFS = link.getCores().get(ruta.getPathCores().get(iEnlace)).getFrequencySlots().get(indiceFs).isFree();
                if (ranuraFS) {
                    // Si encontramos un elemento ocupado, actualizamos el máximo bloque libre
                    maxBloqLibre = Math.max(maxBloqLibre, auxBloqLibre);
                    auxBloqLibre = 0;
                    cantFSocupados = cantFSocupados + 1;
                } else {
                    auxBloqLibre = auxBloqLibre + 1;
                }
            }
            // se aplica la formula para calcular el BFR en un enlace
            bfrEnlace = 1.0 - ((double) maxBloqLibre / (capacidadEnlace - cantFSocupados));
            // se va sumando los BFR de cada enlace para posteriormente hallar el promedio según la formula
            sumBfr = sumBfr + bfrEnlace;
            iEnlace++;
        }
        // se halla el BFRruta
        Double BFRruta = sumBfr / ruta.getPath().size();
        return BFRruta;
    }


    public static Double bfrRed(Graph<Integer, Link> red, Integer capacidadEnlace, Integer core) {
        List<Link> listaEnlaces = new ArrayList<>(red.edgeSet());
        Double sumBfrEnlaceCore = 0.0;

        for (Link enlace : listaEnlaces) {
            Double sumBfrEnlace = 0.0;

            for (int c = 0; c < core; c++) {
                Double bfrEnlace = 0.0;
                Integer cantFSocupados = 0;
                Integer auxBloqLibre = 0;
                Integer maxBloqLibre = 0;

                for (int iFs = 0; iFs < capacidadEnlace; iFs++) {
                    Boolean ranuraFS = enlace.getCores().get(c).getFrequencySlots().get(iFs).isFree();
                    if (ranuraFS) {
                        maxBloqLibre = Math.max(maxBloqLibre, auxBloqLibre);
                        auxBloqLibre = 0;
                        cantFSocupados++;
                    } else {
                        auxBloqLibre++;
                    }
                }
                if (capacidadEnlace - cantFSocupados != 0) {
                    bfrEnlace = 1.0 - ((double) maxBloqLibre / (capacidadEnlace - cantFSocupados));
                }
                sumBfrEnlace += bfrEnlace;
            }
            sumBfrEnlaceCore += sumBfrEnlace;
        }
        // Verifica que no haya división por cero antes de calcular el promedio
        Double bfrRed = (listaEnlaces.size() * core > 0) ? sumBfrEnlaceCore / (listaEnlaces.size() * core) : 0.0;
        return bfrRed;
    }

    public static void ordenarRutasPorFsDijstra(List<EstablishedRoute> listaRutasActivas, String orden) {
        Comparator<EstablishedRoute> comparadorFsRuta = Comparator.comparingDouble(EstablishedRoute::getFsWidth);

        Collections.sort(listaRutasActivas, (ruta1, ruta2) -> {
            int comparacion = comparadorFsRuta.compare(ruta1, ruta2);
            if (comparacion != 0) {
                return comparacion; // Si la comparación de fs es diferente de cero, devuelve el resultado
            } else {
                // Si hay un empate en bfrRuta, desempata usando dijstra sin alterar el orden original
                return Double.compare(ruta1.getDijkstra(), ruta2.getDijkstra());
            }
        });
    }

    public static void ordenarRutasPorDijstraFs(List<EstablishedRoute> listaRutasActivas, String orden) {
        Comparator<EstablishedRoute> comparadorFsRuta = Comparator.comparingDouble(EstablishedRoute::getDijkstra);

        Collections.sort(listaRutasActivas, (ruta1, ruta2) -> {
            int comparacion = comparadorFsRuta.compare(ruta1, ruta2);
            if (comparacion != 0) {
                return comparacion; // Si la comparación de fs es diferente de cero, devuelve el resultado
            } else {
                // Si hay un empate en bfrRuta, desempata usando dijstra sin alterar el orden original
                return Double.compare(ruta1.getFsWidth(), ruta2.getFsWidth());
            }
        });
    }

    public static void ordenarRutasPorDijstra(List<EstablishedRoute> listaRutasActivas, String orden) {
        Collections.sort(listaRutasActivas, Comparator.comparingDouble(EstablishedRoute::getDijkstra).reversed());
    }

    public static void ordenarKsPathsPorPeso(List<GraphPath<Integer, Link>> kspaths) {
        Collections.sort(kspaths, Comparator.comparingDouble(GraphPath<Integer, Link>::getWeight).reversed());
    }

    public static void ordenarRutasPorBfr(List<EstablishedRoute> listaRutasActivas, String orden) {
        if (Constants.ORDENAMIENTO_DESCENDENTE.equals(orden)) {
            Collections.sort(listaRutasActivas, Comparator.comparingDouble(EstablishedRoute::getBfrRuta).reversed());
        } else {
            Collections.sort(listaRutasActivas, Comparator.comparingDouble(EstablishedRoute::getBfrRuta));
        }
    }

    public static void ordenarRutasPorFs(List<EstablishedRoute> listaRutasActivas, String orden) {
        if (Constants.ORDENAMIENTO_ASCENDENTE.equals(orden)) {
            Collections.sort(listaRutasActivas, Comparator.comparingInt(EstablishedRoute::getFsWidth));
        } else {
            Collections.sort(listaRutasActivas, Comparator.comparingInt(EstablishedRoute::getFsWidth).reversed());
        }
    }

    public static List<EstablishedRoute> obtenerPeoresRutasActivas(List<EstablishedRoute> listaRutasActivas, Double porcentaje) {
        int tamañoSubLista = (int) (listaRutasActivas.size() * porcentaje);
        List<EstablishedRoute> sublista = new ArrayList<>(listaRutasActivas.subList(0, tamañoSubLista));
        return sublista;
    }

    public static void desinstalarRutas(List<EstablishedRoute> subListaRutasLiberar, List<EstablishedRoute> listaRutasActivas, Graph<Integer, Link> red,
                                        Double crosstalkPerUnitLength) {

        for (int rl = 0; rl < subListaRutasLiberar.size(); rl++) {
            EstablishedRoute route = subListaRutasLiberar.get(rl);
            Utils.deallocateFs(red, route, crosstalkPerUnitLength);
        }
    }

    public static List<Demand> generarDemandas(List<EstablishedRoute> listaRutasReRuteo) {
        List<Demand> listaDemandas = new ArrayList<>();
        for (int rr = 0; rr < listaRutasReRuteo.size(); rr++) {
            EstablishedRoute r = listaRutasReRuteo.get(rr);
            Demand demanda = new Demand(rr, r.getFrom(), r.getTo(), r.getFsWidth(), r.getLifetime(), null, null, r.getPath());
            // Agrega la demanda a la lista
            listaDemandas.add(demanda);
        }

        // Devuelve la lista de demandas generadas
        return listaDemandas;
    }

    public static void calcularDijstra(List<EstablishedRoute> subLista, Graph<Integer, Link> red) {
        DijkstraShortestPath<Integer, Link> dijkstraFinder = new DijkstraShortestPath<>(red);
        for (int rr = 0; rr < subLista.size(); rr++) {
            EstablishedRoute r = subLista.get(rr);
            GraphPath<Integer, Link> dijstraPaths = dijkstraFinder.getPath(r.getFrom(), r.getTo());
            r.setDijkstra(dijstraPaths.getWeight());
        }
    }

    public static void reProcesarDemandasCaminoOriginal(List<EstablishedRoute> rutas, Graph<Integer, Link> red,
                                          Integer capacidadEnlance, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength, Integer cores,
                                          List<EstablishedRoute> listasRutasActivas) {
        List<EstablishedRoute> listaRutasEstablecidas = new ArrayList<>();
        int bloqueos = 0;
        int asigancion = 0;
        for (EstablishedRoute ruta : rutas) {
            EstablishedRoute rutasEstablecida;
            // Algoritmo RSA con conmutación de nucleos
            rutasEstablecida = Algorithms.reRuteo(red, ruta, capacidadEnlance,
                    cores, maxCrosstalk, crosstalkPerUnitLength, ruta.getPath());

            if (rutasEstablecida == null || rutasEstablecida.getFsIndexBegin() == -1) {
                bloqueos++;
                System.out.println("Epaaaa. Se produjo bloqueos al reinsertar: " + bloqueos);
            } else {
                // Ruta establecida
                AssignFsResponse response = Utils.assignFs(red, rutasEstablecida, crosstalkPerUnitLength);
                rutasEstablecida = response.getRoute();
                red = response.getGraph();
                listasRutasActivas.add(rutasEstablecida);
                asigancion++;
            }
        }
        System.out.println("Cantidad de Bloqueos luego de reruteo es: " + bloqueos);
    }

    public static void reProcesarDemandas(List<Demand> demandas, Graph<Integer, Link> red,
                                                        Integer capacidadEnlance, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength, Integer cores,
                                                        List<EstablishedRoute> listasRutasActivas) {
        List<EstablishedRoute> listaRutasEstablecidas = new ArrayList<>();
        int bloqueos = 0;
        int asigancion = 0;
        for (Demand demanda : demandas) {
            EstablishedRoute rutasEstablecida;
            // Algoritmo RSA con conmutación de nucleos
            rutasEstablecida = Algorithms.ruteoCoreMultipleK(red, demanda, capacidadEnlance,
                    cores, maxCrosstalk, crosstalkPerUnitLength);

            if (rutasEstablecida == null || rutasEstablecida.getFsIndexBegin() == -1) {
                bloqueos++;
                System.out.println("Epaaaa. Se produjo bloqueos al reinsertar: " + bloqueos);
            } else {
                // Ruta establecida
                AssignFsResponse response = Utils.assignFs(red, rutasEstablecida, crosstalkPerUnitLength);
                rutasEstablecida = response.getRoute();
                red = response.getGraph();
                listasRutasActivas.add(rutasEstablecida);
                asigancion++;
            }
        }
        System.out.println("Cantidad de Bloqueos luego de reruteo es: " + bloqueos);
    }

    public static EstablishedRoute reRuteo(Graph<Integer, Link> graph, EstablishedRoute ruta, Integer capacity,
                                           Integer cores, BigDecimal umbralRuidoMax, Double crosstalkPerUnitLength, List<Link> path) {
        int k = 0;
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        Integer fsIndexBegin = null;
        List<Link> ksp = path;

        // RECORRIDO DE LOS FS
        for (int indiceFS = 0; indiceFS <= capacity - ruta.getFsWidth(); indiceFS++) {
            List<Link> enlacesLibres = new ArrayList<>();
            List<Integer> kspCores = new ArrayList<>();
            List<BigDecimal> crosstalkFSList = new ArrayList<>();

            for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < ruta.getFsWidth(); fsCrosstalkIndex++) {
                crosstalkFSList.add(BigDecimal.ZERO);
            }

            // RECORRIDO DE LOS ENLACES (RED)
            for (Link link : ksp) {
                // RECORRIDO DE LOS CORES
                for (int core = 0; core < cores; core++) {

                    // OBTIENE EL BLOQUE DE FS (RED)
                    List<FrequencySlot> bloqueFS = link.getCores().get(core).getFrequencySlots().subList(indiceFS,
                            indiceFS + ruta.getFsWidth());

                    // CONTROLA SI ESTA OCUPADO POR UNA DEMANDA
                    if (isFSBlockFree(bloqueFS)) {

                        // CONTROL DE RUIDO

                        if (esBloqueFsMayorUmbralRuido(bloqueFS, umbralRuidoMax, crosstalkFSList)) {

                            // CONTROL DE RUIDO EN LOS NUCLEOS VECINOS
                            if (isNextToCrosstalkFreeCores(link, umbralRuidoMax, core, indiceFS, ruta.getFsWidth(),
                                    crosstalkPerUnitLength)) {
                                enlacesLibres.add(link);
                                kspCores.add(core);
                                fsIndexBegin = indiceFS;
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
            establisedRoute = new EstablishedRoute(path, fsIndexBegin, ruta.getFsWidth(),
                    ruta.getLifetime(), ruta.getFrom(), ruta.getTo(), kspPlacedCores.get(0), null, null);
        } else {
            // System.out.println("Bloqueo");
            establisedRoute = null;
        }
        return establisedRoute;
    }
}			
		
		
			
		

