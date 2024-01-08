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
import py.una.pol.simulador.eon.utils.MathUtils;
import py.una.pol.simulador.eon.utils.Utils;

public class SimulatorTest {

	private Input getTestingInput() {
		Input input = new Input();
		// Cantidad de Demandas
		input.setDemands(100000);
		
		// Topologias de Red
		input.setTopologies(new ArrayList<>());
		input.getTopologies().add(TopologiesEnum.NSFNET);
		input.getTopologies().add(TopologiesEnum.USNET);
		input.getTopologies().add(TopologiesEnum.JPNNET);
		
		// Ancho de banda de cada FS 
		input.setFsWidth(new BigDecimal("12.5"));
		
		// Cantidad Máxima de FS a ocupar por una demanda
		input.setFsRangeMax(8);
		
		// Cantidad Mínima de FS a ocupar por una demanda
		input.setFsRangeMin(2);
		
		// Cantidad de FS por cada fibra, es la capacidad del enlace
		input.setCapacity(320);
		
		// Cantidad de núcleos de la fibra
		input.setCores(7);
		
		// Promedio de demandas en cada periodo o intervalo de tiempo
		input.setLambda(5);
		
		// Volumen del tráfico promedio en cada instante de tiempo
		input.setErlang(2000);
		
		// Algoritmos RSA
		input.setAlgorithms(new ArrayList<>());
		input.getAlgorithms().add(RSAEnum.MULTIPLES_CORES);
		
		// Cantidad de intervalos de tiempo de la simulación
		input.setSimulationTime(MathUtils.getSimulationTime(input.getDemands(), input.getLambda()));
		
		// Umbral del ruido: Máxima atenuación tolerable de la red
		input.setMaxCrosstalk(new BigDecimal("0.003162277660168379331998893544")); // XT = -25 dB
		// input.setMaxCrosstalk(new BigDecimal("0.031622776601683793319988935444")); // XT = -15 dB
		
		
		// Características de la fibra utilizada. h = incremento del ruido por unidad de longitud
		input.setCrosstalkPerUnitLenghtList(new ArrayList<>());
		//input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.0035, 2) * 0.080) / (4000000 * 0.000045));  // h = 1,089E-08
		//input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.00040, 2) * 0.050) / (4000000 * 0.000040)); // h= 1,000E-10
		input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.0000316, 2) * 0.055) / (4000000 * 0.000045));  // h= 6,102E-13
		
		//Cantidad de veces que se va a realizar la desfragmentación
		input.setDefragmentationCount(2);
		return input;
	}

	public static void main(String[] args) {
		try {
			
			createTableBloqueos();
			
			// Datos de entrada
			Input input = new SimulatorTest().getTestingInput();
			Graph<Integer, Link> graph = null;
			Integer intervalosDeTiempoDF = null;
			Integer countDF = null;
			Integer nDF = 0;
			String topologiaUtilizada = Constants.SIMULADOR_TOPOLOGIA_NSFNET;
			Integer intervalosDeTiempoRSA = input.getSimulationTime();
			
			if(input.getDefragmentationCount() != null && input.getDefragmentationCount() != 0) {
				 intervalosDeTiempoDF = (int) Math.round(intervalosDeTiempoRSA / (input.getDefragmentationCount() + 1.0));
				 countDF = 0;
			}
			
			for (TopologiesEnum topology : input.getTopologies()) {
				if(topology.toString().equals(topologiaUtilizada)) {
					graph = Utils.createTopology(topology, input.getCores(), input.getFsWidth(), input.getCapacity());
										
					// Generación de demandas a ser utilizadas en la simulación
					Integer demandsQ = 1;
					List<List<Demand>> listaDemandas = new ArrayList<>();
					for (int i = 0; i < intervalosDeTiempoRSA; i++) {
						List<Demand> demands = Utils.generateDemands(input.getLambda(), intervalosDeTiempoRSA,
								input.getFsRangeMin(), input.getFsRangeMax(), graph.vertexSet().size(),
								input.getErlang() / input.getLambda(), demandsQ, i);

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
							int demandaNumero = 1;
							int bloqueos = 0;
							// Iteración de intervalos de tiempo de la simulacion
							for (int i = 0; i < intervalosDeTiempoRSA; i++) {
								
								//  Demandas a ser transmitidas en el intervalo de tiempo i
								List<Demand> demands = listaDemandas.get(i);
								for (Demand demand : demands) {
									demandaNumero++;
									EstablishedRoute establishedRoute;
									// Algoritmo RSA con conmutación de nucleos
									establishedRoute = Algorithms.ruteoCoreMultiple(graph, demand, input.getCapacity(),
												input.getCores(), input.getMaxCrosstalk(), crosstalkPerUnitLength);
									
									if (establishedRoute == null || establishedRoute.getFsIndexBegin() == -1) {
										// Bloqueo
										System.out.println("Insertando demanda Nro : " + demandaNumero + " en el tiempo t= "+ i +" ---------------------->"+ " Bloqueado");
										demand.setBlocked(true);
										insertDataBloqueo(algorithm.label(), topology.label(), "" + i, "" + demand.getId(),
												"" + input.getErlang(), crosstalkPerUnitLength.toString());
										bloqueos++;
									} else {
										// Ruta establecida
										System.out.println("Insertando demanda Nro : " + demandaNumero + " en el tiempo t= "+ i +" ---->"+ " Ejecutado");
										AssignFsResponse response = Utils.assignFs(graph, establishedRoute,
												crosstalkPerUnitLength);
										establishedRoute = response.getRoute();
										graph = response.getGraph();
										establishedRoutes.add(establishedRoute);
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
								
								// Proceso de Desfgragmentación
								if(intervalosDeTiempoDF != null && i == intervalosDeTiempoDF && countDF <= input.getDefragmentationCount()) {
									System.out.println("Iniciando proceso de Desfragmentación....: ");
									Algorithms.inciarProcesoDesfragmentacion(establishedRoutes, graph, input.getCapacity());					
									intervalosDeTiempoDF = i + intervalosDeTiempoDF;
								    nDF = nDF +1;
								}
									
										
	
								
							}
							System.out.println("Topología utilizada: " + topologiaUtilizada);
							System.out.println("Erlangs : " + input.getErlang() );
							System.out.println("TOTAL DE BLOQUEOS: " + bloqueos);
							System.out.println("Cantidad de demandas: " + demandaNumero);
							System.out.println("Cantidad de veces que se desfragmentó: " + nDF);
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
                    + " fecha TEXT DEFAULT (strftime('%d/%m/%Y %H:%M', 'now', 'localtime'))" // Nueva columna "Fecha" con valor por defecto
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
