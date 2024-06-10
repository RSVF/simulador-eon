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
import java.util.List;
import org.jgrapht.Graph;
import py.una.pol.simulador.eon.models.AssignFsResponse;
import py.una.pol.simulador.eon.models.Demand;
import py.una.pol.simulador.eon.models.EstablishedRoute;
import py.una.pol.simulador.eon.models.Input;
import py.una.pol.simulador.eon.models.Link;
import py.una.pol.simulador.eon.models.enums.RSAEnum;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;
import py.una.pol.simulador.eon.rsa.Algorithms;
import py.una.pol.simulador.eon.utils.Constants;
import py.una.pol.simulador.eon.utils.MathUtils;
import py.una.pol.simulador.eon.utils.Utils;

/**
 *
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
            String topologia = Constants.TOPOLOGIA_NSFNET;
            Integer tiempoSimulacion =  input.getSimulationTime();

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
                            // Lista de rutas establecidas durante la simulación
                            List<EstablishedRoute> establishedRoutes = new ArrayList<>();
                            System.out.println("Inicializando simulación del RSA " + algorithm.label() + " para erlang: "
                                    + (input.getErlang()) + " para la topología " + topology.label() + " y H = "
                                    + crosstalkPerUnitLength.toString());
                            int demandaNumero = 0;
                            int bloqueos = 0;
                            int rutasProcesadas =0;
                            establishedRoutes = new ArrayList<>();

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

                                    if( i == 5000){
                                        System.out.println("Primera parada para desfragmentar ");


                                    }

                                }
                                System.out.println("Topología utilizada: " + topologia);
                                System.out.println("Erlangs : " + input.getErlang());
                                System.out.println("TOTAL DE BLOQUEOS: " + bloqueos);
                                System.out.println("Cantidad de demandas asignadas: " + rutasProcesadas);
                                System.out.println("Cantidad de demandas total: " + demandaNumero);
                                System.out.println(System.lineSeparator());
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
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "  // se grega la columna "id"
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
}

