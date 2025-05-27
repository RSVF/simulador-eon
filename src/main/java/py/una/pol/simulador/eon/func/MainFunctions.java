package py.una.pol.simulador.eon.func;

import org.jgrapht.Graph;
import py.una.pol.simulador.eon.models.*;
import py.una.pol.simulador.eon.models.enums.RSAEnum;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;
import py.una.pol.simulador.eon.rsa.Algorithms;
import py.una.pol.simulador.eon.utils.MathUtils;
import py.una.pol.simulador.eon.utils.Utils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainFunctions {

    public static Input getTestingInput() {

        Input input = new Input();
        //Cantidad de demandas
        input.setDemands(10000);

        //Topologias de Red
        input.setTopologies(new ArrayList<>());
        input.getTopologies().add(TopologiesEnum.NSFNET);
        //input.getTopologies().add(TopologiesEnum.USNET);
        //input.getTopologies().add(TopologiesEnum.JPNNET);

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
        input.setLambda(100);

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
