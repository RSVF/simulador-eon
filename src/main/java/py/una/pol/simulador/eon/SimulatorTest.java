/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package py.una.pol.simulador.eon;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.Inflater;

import org.jgrapht.Graph;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.models.enums.RSAEnum;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;
import py.una.pol.simulador.eon.rsa.Algorithms;
import py.una.pol.simulador.eon.utils.Constants;
import py.una.pol.simulador.eon.utils.MathUtils;
import py.una.pol.simulador.eon.utils.Utils;

/**
 * @author Néstor E. Reinoso Wood
 */
public class SimulatorTest {

    private Input getTestingInput() {

        Input input = new Input();
        //Cantidad de demandas
        input.setDemands(100000);

        //Topologias de Red
        input.setTopologies(new ArrayList<>());
        input.getTopologies().add(TopologiesEnum.NSFNET);
        input.getTopologies().add(TopologiesEnum.USNET);
        input.getTopologies().add(TopologiesEnum.JPNNET);

        //Ancho de banda de cada FS
        input.setFsWidth(new BigDecimal("12.5"));

        //Cantidad máxima de FS a ocupar por una demanda
        input.setFsRangeMax(8);

        //Cantidad mínima de FS a ocupar por una demanda
        input.setFsRangeMin(2);

        //Cantidad de FS del enlace
        input.setCapacity(320);

        //Cantidad de nucleos de la fibra
        input.setCores(7);

        //Promedio de demandas en cada periodo o intervalo de tiempo
        input.setLambda(5);

        //Volumen de tráfico promedio en cada instante de tiempo
        input.setErlang(2000);

        //Algoritmos RSA
        input.setAlgorithms(new ArrayList<>());
        input.getAlgorithms().add(RSAEnum.MULTIPLES_CORES);

        //Cantidad de intervalos de tiempo de la simulación
        input.setSimulationTime(MathUtils.getSimulationTime(input.getDemands(), input.getLambda()));

        //Umbral del ruido, Máxima atenuación tolerable de la red
        //input.setMaxCrosstalk(new BigDecimal("0.031622776601683793319988935444")); // XT = -15 dB
        input.setMaxCrosstalk(new BigDecimal("0.003162277660168379331998893544")); // XT = -25 dB

        //Características de la fibra utilizada. h = incremento del ruido por unidad de tiempo
        input.setCrosstalkPerUnitLenghtList(new ArrayList<>());
        //input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.0035, 2) * 0.080) / (4000000 * 0.000045)); //F1
        //input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.00040, 2) * 0.050) / (4000000 * 0.000040)); //F2
        input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.0000316, 2) * 0.055) / (4000000 * 0.000045)); //F3

        return input;
    }

    /**
     * Simulador
     *
     * @param args Argumentos de entrada (Vacío)
     */
    public static void main(String[] args) {
        try {
            createTableBloqueos();
            Input input = new SimulatorTest().getTestingInput();
            String topologia = Constants.TOPOLOGIA_JPNNET;
            String tipoDesframentacion = Constants.DESFRAGMENTACION_EMPIRICA;
            Integer tiempoSimulacion = input.getSimulationTime();
            Integer intervalo = 2000;
            Integer desf = 0;
            Double porcentajeRutas = 0.0;

            for (TopologiesEnum topology : input.getTopologies()) {
                if (topology.toString().equals(topologia)) {
                    Graph<Integer, Link> graph = Utils.createTopology(topology, input.getCores(), input.getFsWidth(), input.getCapacity());

                    // Generación de demandas a ser utilizadas en la simulación
                    Integer demandsQ = 1;
                    List<List<Demand>> listaDemandas = new ArrayList<>();
                    for (int i = 0; i < tiempoSimulacion; i++) {
                        List<Demand> demands = Utils.generateDemands(input.getLambda(), tiempoSimulacion, input.getFsRangeMin(),
                                input.getFsRangeMax(), graph.vertexSet().size(), input.getErlang() / input.getLambda(), demandsQ, i);
                        demandsQ += demands.size();
                        listaDemandas.add(demands);
                    }

                    for (Double crosstalkPerUnitLength : input.getCrosstalkPerUnitLenghtList()) {
                        for (RSAEnum algorithm : input.getAlgorithms()) {
                            // Lista de rutas establecidas durante la simulacióndem
                            List<EstablishedRoute> establishedRoutes = new ArrayList<>();
                            System.out.println("Inicializando simulación del RSA " + algorithm.label() + " para erlang: "
                                    + (input.getErlang()) + " para la topología " + topology.label() + " y H = "
                                    + crosstalkPerUnitLength.toString());
                            int demandaNumero = 0;
                            int bloqueos = 0;
                            int rutasProcesadas = 0;
                            establishedRoutes = new ArrayList<>();


                            for (int k = 1; k <= 4; k++) {
                                if(k == 2){
                                    intervalo = 5000;
                                } else if(k == 3){
                                    intervalo = 2000;
                                }else if(k == 4){
                                    intervalo = 1000;
                                }
                                demandaNumero = 0;
                                bloqueos = 0;
                                rutasProcesadas = 0;
                                establishedRoutes = new ArrayList<>();
                                graph = Utils.createTopology(topology, input.getCores(), input.getFsWidth(), input.getCapacity());


                                for (int i = 0; i < tiempoSimulacion; i++) {

                                    //  Demandas a ser transmitidas en el intervalo de tiempo i
                                    List<Demand> demands = listaDemandas.get(i);
                                    for (Demand demand : demands) {
                                        demandaNumero++;
                                        EstablishedRoute establishedRoute;
                                        // ALGORITMO RSA CON CONMUTACION DE NUCLEOS
                                        establishedRoute = Algorithms.ruteoCoreMultiple(graph, demand, input.getCapacity(),
                                                input.getCores(), input.getMaxCrosstalk(), crosstalkPerUnitLength);

                                        if (establishedRoute == null || establishedRoute.getFsIndexBegin() == -1) {
                                            // BLOQUEO
                                            System.out.println("Insertando demanda Nro : " + demandaNumero + " en el tiempo t= " + i + " ---------------------->" + " Bloqueado");
                                            demand.setBlocked(true);
                                            insertDataBloqueo(algorithm.label(), topology.label(), "" + i, "" + demand.getId(),
                                                    "" + input.getErlang(), crosstalkPerUnitLength.toString());
                                            bloqueos++;
                                        } else {
                                            // RUTA ESTABLECIDA
                                            System.out.println("Insertando demanda Nro : " + demandaNumero + " en el tiempo t= " + i + " ---->" + " Ejecutado");
                                            AssignFsResponse response = Utils.assignFs(graph, establishedRoute,
                                                    crosstalkPerUnitLength);
                                            establishedRoute = response.getRoute();
                                            graph = response.getGraph();
                                            establishedRoutes.add(establishedRoute);
                                            rutasProcesadas++;
                                        }
                                    }
                                    for (EstablishedRoute route : establishedRoutes) {
                                        route.subLifeTime();
                                    }
                                    for (int ri = 0; ri < establishedRoutes.size(); ri++) {
                                        EstablishedRoute route = establishedRoutes.get(ri);
                                        if (route.getLifetime().equals(0)) {
                                            Utils.deallocateFs(graph, route, crosstalkPerUnitLength);
                                            establishedRoutes.remove(ri);
                                            ri--;
                                        }
                                    }

                                    if ((i != 0 && i % intervalo == 0) {

                                        if (Constants.DESFRAGMENTACION_PUSH_PULL == tipoDesframentacion) {
                                            desfragmentacionPushPull(establishedRoutes, graph, input.getCapacity(), input.getMaxCrosstalk(), crosstalkPerUnitLength);

                                        } else {
                                            Integer rutasNoDesplazadas = 0;
                                            Double bfRedBefore = Utils.bfrRed(graph, input.getCapacity(),7);
                                            System.out.println("Bfr red inicial: " + bfRedBefore);
                                            //calcularBfrRutasActivas(establishedRoutes, graph, input.getCapacity());
                                            //ordenarPorBfrRutaDesc(establishedRoutes); // se ordena de forma descendente, es decir de la ruta mas fragmentada a la menos fragmentada
                                            List<EstablishedRoute> rutasSublist = Utils.obtenerPeoresRutas(establishedRoutes, porcentajeRutas);
                                            rutasSublist = Utils.ordenarRutasFsLt(rutasSublist, Constants.ORDER_ASC);
                                           // rutasSublist = Utils.ordenarRutasDistFs(rutasSublist, Constants.ORDER_ASC);
                                            int eliminado = 0;
                                            while (eliminado < rutasSublist.size()) {
                                                Utils.deallocateFs(graph, establishedRoutes.get(0), crosstalkPerUnitLength);
                                                establishedRoutes.remove(0);
                                                eliminado++;
                                            }
                                            List<Demand> demandas = Utils.generarDemandas(rutasSublist);
                                                reProcesarDemandas(demandas, graph, input.getCapacity(), input.getMaxCrosstalk(), crosstalkPerUnitLength, input.getCores(), establishedRoutes);
                                               // reProcesarDemandasRSA(demandas, graph, input.getCapacity(), input.getMaxCrosstalk(), crosstalkPerUnitLength, input.getCores(), establishedRoutes);

                                            //reProcesarDemandasSameLinkAndCore(demandas, graph, input.getCapacity(), input.getMaxCrosstalk(), crosstalkPerUnitLength, input.getCores(), establishedRoutes)
                                            Double bfRed = Utils.bfrRed(graph, input.getCapacity(),7);
                                            System.out.println("Bfr red: " + bfRed);

                                        }

                                        desf = desf + 1;
                                    }
                                }
                                System.out.println("Topología utilizada: " + topologia);
                                System.out.println("Erlangs : " + input.getErlang());
                                System.out.println("TOTAL DE BLOQUEOS: " + bloqueos);
                                System.out.println("Cantidad de demandas asignadas: " + rutasProcesadas);
                                System.out.println("Cantidad de demandas total: " + demandaNumero);
                                System.out.println("--------------------------------------------------");
                                System.out.println("Cantidad de desfragmentaciones: " + desf);
                                System.out.println(System.lineSeparator());
                                desf = 0;
                            }


                        }
                    }
                }
            }

        } catch (IOException | IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void createTableBloqueos() {
        Connection c;
        Statement stmt;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:simulador.db");
            System.out.println("Database Opened...\n");
            stmt = c.createStatement();
            String dropTable = "DROP TABLE Bloqueos ";
            String sql = "CREATE TABLE IF NOT EXISTS Bloqueos "
                    + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "erlang TEXT NOT NULL, "
                    + "rsa TEXT NOT NULL, "
                    + " topologia TEXT NOT NULL, "
                    + " h TEXT NOT NULL, "
                    + " tiempo TEXT NOT NULL, "
                    + " demanda TEXT NOT NULL, "
                    + " fecha TEXT DEFAULT (strftime('%d/%m/%Y %H:%M', 'now', 'localtime'))"
                    + ")";
            try {
                stmt.executeUpdate(dropTable);
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public static void insertDataBloqueo(String rsa, String topologia, String tiempo, String demanda, String erlang, String h) {
        Connection c;
        Statement stmt;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:simulador.db");
            c.setAutoCommit(false);
            stmt = c.createStatement();
            String sql = "INSERT INTO Bloqueos (rsa, topologia, tiempo, demanda, erlang, h) "
                    + "VALUES ('" + rsa + "','" + topologia + "', '" + tiempo + "' ,'" + demanda + "', " + "'" + erlang + "', " + "'" + h + "')";
            stmt.executeUpdate(sql);
            stmt.close();
            c.commit();
            c.close();
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public static void reProcesarDemandas(List<Demand> demandas, Graph<Integer, Link> red, Integer capacidadEnlance, BigDecimal maxCrosstalk,
                                          Double crosstalkPerUnitLength, Integer cores, List<EstablishedRoute> listasRutasActivas) {
        List<EstablishedRoute> listaRutasEstablecidas = new ArrayList<>();
        int bloqueos = 0;
        int asigancion = 0;
        for (Demand demanda : demandas) {
            EstablishedRoute rutasEstablecida;
            // Algoritmo RSA con conmutación de nucleos
            rutasEstablecida = Algorithms.reRuteoCaminoOriginal(red, demanda, capacidadEnlance, cores, maxCrosstalk, crosstalkPerUnitLength );

            if (rutasEstablecida == null || rutasEstablecida.getFsIndexBegin() == -1) {
                bloqueos++;
                System.out.println("Epaa. Se produjo bloqueos al reinsertar: " + bloqueos);
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


    public static void reProcesarDemandasSameLinkAndCore(List<Demand> demandas, Graph<Integer, Link> red, Integer capacidadEnlance, BigDecimal maxCrosstalk,
                                          Double crosstalkPerUnitLength, Integer cores, List<EstablishedRoute> listasRutasActivas) {
        List<EstablishedRoute> listaRutasEstablecidas = new ArrayList<>();
        int bloqueos = 0;
        int asigancion = 0;
        for (Demand demanda : demandas) {
            EstablishedRoute rutasEstablecida;
            // Algoritmo RSA con conmutación de nucleos
            rutasEstablecida = Algorithms.reRuteoCaminoOriginalSameLinkAndCore(red, demanda, capacidadEnlance, cores, maxCrosstalk, crosstalkPerUnitLength);

            if (rutasEstablecida == null || rutasEstablecida.getFsIndexBegin() == -1) {
                bloqueos++;
                System.out.println("Epaa. Se produjo bloqueos al reinsertar: " + bloqueos);
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


    public static void reProcesarDemandasRSA(List<Demand> demandas, Graph<Integer, Link> red, Integer capacidadEnlance, BigDecimal maxCrosstalk,
                                             Double crosstalkPerUnitLength, Integer cores, List<EstablishedRoute> listasRutasActivas) {
        List<EstablishedRoute> listaRutasEstablecidas = new ArrayList<>();
        int bloqueos = 0;
        int asigancion = 0;
        for (Demand demanda : demandas) {
            EstablishedRoute rutasEstablecida;
            // Algoritmo RSA con conmutación de nucleos
            rutasEstablecida = Algorithms.ruteoCoreMultiple(red, demanda, capacidadEnlance, cores, maxCrosstalk, crosstalkPerUnitLength);

            if (rutasEstablecida == null || rutasEstablecida.getFsIndexBegin() == -1) {
                bloqueos++;
                System.out.println("Epaa. Se produjo bloqueos al reinsertar: " + bloqueos);
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

    public static List<EstablishedRoute> calcularBfrRutasActivas(List<EstablishedRoute> listaRutasActivas, Graph<Integer, Link> red, Integer capacidadEnlace) {
        for (int ra = 0; ra < listaRutasActivas.size(); ra++) {
            EstablishedRoute rutaActiva = listaRutasActivas.get(ra);
            Double bfrRuta = Utils.bfrRuta(rutaActiva, red, capacidadEnlace);
            rutaActiva.setBfrRuta(bfrRuta);
        }
        return listaRutasActivas;
    }


    public static List<EstablishedRoute> ordenarPorBfrRutaDesc(List<EstablishedRoute> rutas) {
        Collections.sort(rutas, new Comparator<EstablishedRoute>() {
            @Override
            public int compare(EstablishedRoute r1, EstablishedRoute r2) {
                return r2.getBfrRuta().compareTo(r1.getBfrRuta());
            }
        });
        return rutas;
    }


    public static void desfragmentacionPushPull(List<EstablishedRoute> rutas, Graph<Integer, Link> red, Integer capacidadEnlance, BigDecimal umbralRuidoMax, Double crosstalkPerUnitLength) {
        Integer rutasDesplazada = -1;
        Integer rutasNoDesplazadas = 0;
        Double bfRedBefore = Utils.bfrRed(red, capacidadEnlance,7);
        System.out.println("Bfr red inicial: " + bfRedBefore);

        while ( rutasDesplazada !=0 ){
            rutasDesplazada = 0;
            for (int i = 0; i < rutas.size(); i++) {
                EstablishedRoute ruta = rutas.get(i);
                if (ruta.getFsIndexBegin() != 0) {
                    Integer FSindexBeginDF = verificarMovFsIzq(ruta, red);
                    if (FSindexBeginDF != ruta.getFsIndexBegin()) {
                        ruta.setFsIndexBeginDf(FSindexBeginDF);
                        ruta.setLifetimeDf(ruta.getLifetime());
                        // Se desintala de red ya que hay lugar para mover mas hacia la izquierda
                        Utils.deallocateFs(red, ruta, crosstalkPerUnitLength);
                        // Antes de asignar a la red en los nuevos lugares se debe controlar el ruido antes de la insersion
                        if (controlRuidoDF(red, ruta, umbralRuidoMax, crosstalkPerUnitLength)) {
                            // se recuperar el tiempo de vida y se indica en indice nuevo del fs
                            ruta.setFsIndexBegin(FSindexBeginDF);
                            rutasDesplazada++;
                        }else{
                            rutasNoDesplazadas++;
                        }
                        ruta.setLifetime(ruta.getLifetimeDf());
                        Utils.assignFs(red, ruta, crosstalkPerUnitLength);
                    }
                }
            }
            Double bfRed = Utils.bfrRed(red, capacidadEnlance,7);
            System.out.println("Bfr red: " + bfRed);
            System.out.println("Cantidad de rutas totales: " + rutas.size());
            System.out.println("Cantidad de rutas desplazadas luego de desfragmentar es: " + rutasDesplazada);
            System.out.println("Cantidad de rutas NO desplazadas luego de desfragmentar es: " + rutasNoDesplazadas);
        }
    }

    public static Integer verificarMovFsIzq(EstablishedRoute ruta, Graph<Integer, Link> red) {

        List<Link> enlacesRuta = ruta.getPath();
        List<Integer> coresRuta = ruta.getPathCores();
        Integer FSindexBegin = ruta.getFsIndexBegin();

        List<Integer> ultimosLibres = new ArrayList<>();

        // Iterar sobre cada enlace de la ruta
        for (int i = 0; i < enlacesRuta.size(); i++) {
            List<FrequencySlot> ranurasFS = enlacesRuta.get(i).getCores().get(coresRuta.get(i)).getFrequencySlots();
            Integer ultimoFSLibreEnlacei = FSindexBegin;
            for (int fs = FSindexBegin - 1; fs >= 0; fs--) {
                if (ranurasFS.get(fs).isFree()) {
                    ultimoFSLibreEnlacei = fs;
                } else {
                    break;
                }
            }
            ultimosLibres.add(ultimoFSLibreEnlacei);
        }
        return Collections.max(ultimosLibres);
    }


    public static boolean controlRuidoDF(Graph<Integer, Link> red, EstablishedRoute ruta, BigDecimal umbralRuidoMax, Double crosstalkPerUnitLength) {
        Boolean puedeAsignar = false;
        List<Link> enlacesRuta = ruta.getPath();
        List<Integer> coresRuta = ruta.getPathCores();
        Integer FSindexBeginDF = ruta.getFsIndexBegin();
        Integer FSruta = ruta.getFsWidth();
        List<BigDecimal> crosstalkFSList = new ArrayList<>();

        // Inicializar la lista de crosstalk
        for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < FSruta; fsCrosstalkIndex++) {
            crosstalkFSList.add(BigDecimal.ZERO);
        }

        // Iterar sobre cada enlace de la ruta
        for (int i = 0; i < enlacesRuta.size(); i++) {

            // Verificar los límites antes de acceder a los elementos
            if (i >= coresRuta.size()) {
                System.err.println("Error: Índice 'i' fuera de rango para coresRuta: " + i + " >= " + coresRuta.size());
                return false;
            }

            // Verificar los límites para el subList de bloqueFS
            int fsEndIndex = FSindexBeginDF + FSruta;
            if (FSindexBeginDF < 0 || fsEndIndex > enlacesRuta.get(i).getCores().get(coresRuta.get(i)).getFrequencySlots().size()) {
                System.err.println("Error: Índices de sublista fuera de rango. FSindexBeginDF: " + FSindexBeginDF +
                        ", FSruta: " + FSruta + ", tamaño de frequencySlots: " +
                        enlacesRuta.get(i).getCores().get(coresRuta.get(i)).getFrequencySlots().size());
                return false;
            }

            // Obtener bloqueFS
            List<FrequencySlot> bloqueFS = enlacesRuta.get(i).getCores().get(coresRuta.get(i)).getFrequencySlots()
                    .subList(FSindexBeginDF, fsEndIndex);

            // Lógica de control de ruido
            if (Algorithms.esBloqueFsMayorUmbralRuido(bloqueFS, umbralRuidoMax, crosstalkFSList)) {
                // CONTROL DE RUIDO EN LOS NUCLEOS VECINOS
                if (Algorithms.isNextToCrosstalkFreeCores(enlacesRuta.get(i), umbralRuidoMax, coresRuta.get(i), FSindexBeginDF, FSruta, crosstalkPerUnitLength)) {
                    for (int crosstalkFsListIndex = 0; crosstalkFsListIndex < crosstalkFSList.size(); crosstalkFsListIndex++) {
                        BigDecimal crosstalkRuta = crosstalkFSList.get(crosstalkFsListIndex);
                        crosstalkRuta = crosstalkRuta.add(Utils.toDB(Utils.XT(Utils.getCantidadVecinos(coresRuta.get(i)),
                                crosstalkPerUnitLength, enlacesRuta.get(i).getDistance())));
                        crosstalkFSList.set(crosstalkFsListIndex, crosstalkRuta);
                    }

                    if (i == enlacesRuta.size() - 1) {
                        puedeAsignar = true;
                    }
                }
            }
        }
        return puedeAsignar;
    }

}