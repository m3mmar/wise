/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wise;

import java.util.HashMap;

/**
 *
 * @author mammar
 */
public class Feature {

    public static enum Features {
        Neighbour_Attacked_Ratio, Cluster_Attacked_Ratio
    };

    public Feature() {
        for (int i = 0; i <= 100; i++) {
            ZeroValDistribution.put(i, 1);
            OneValDistribution.put(i, 1);
        }
    }

    public Features FeatureName;
    public HashMap<Integer, Integer> ZeroValDistribution = new HashMap<>();

    public HashMap<Integer, Integer> OneValDistribution = new HashMap<>();

    public void updateZeroDistribution(Integer objVal) {
        ZeroValDistribution.put(objVal, ZeroValDistribution.get(objVal) + 1);
    }

    public void updateOneDistribution(Integer objVal) {

        OneValDistribution.put(objVal, OneValDistribution.get(objVal) + 1);
    }

}
