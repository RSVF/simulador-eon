/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package py.una.pol.simulador.eon.utils;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class MathUtils {

    /**
     *
     * @param lambda
     * @return
     */
    public static Integer poisson(Integer lambda) {
        Integer b, bFact;
        Double s, a;
        Double e = Math.E;
        a = (Math.random() * 1) + 0;
        b = 0;
        bFact = factorial(b);
        s = (Math.pow(e, (-lambda))) * ((Math.pow(lambda, b)) / (bFact));
        while (a > s) {
            b++;
            bFact = factorial(b);
            s = s + ((Math.pow(e, (-lambda))) * ((Math.pow(lambda, b)) / (bFact)));
        }
        return b;
    }

    /**
     * 
     * @param ht
     * @return 
     */
    public static Integer getLifetime(Integer ht) {
        Double b;
        Double s, a, aux, auxB, auxHT;
        Double e = Math.E;
        a = (Math.random() * 1) + 0;
        b = 1D;
        auxB = (double) b;
        auxHT = (double) ht;
        aux = (-1) * (auxB / auxHT);
        s = 1 - (Math.pow(e, (aux)));
        while (s < a) {
            b++;
            auxB = b;
            aux = (-1) * (auxB / auxHT);
            s = 1 - (Math.pow(e, (aux)));
        }
        return b.intValue();
    }

    public static Integer factorial(Integer n) {
        Integer resultado = 1;
        for (Integer i = 1; i <= n; i++) {
            resultado *= i;
        }
        return resultado;
    }
    
    public static Integer getSimulationTime(Integer demands, Integer lambda) {
        Integer simulationTime;
        return simulationTime=demands/lambda;
    }
}
