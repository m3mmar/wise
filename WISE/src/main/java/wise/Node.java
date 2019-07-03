/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wise;

import java.util.HashMap;
import java.util.List;

/**
 *
 * @author mammar
 */
public class Node implements Comparable<Node> {

    public List<Node> lstNeihbours;
    public HMM_Model predictiveModel;
    public List<String> lstClusters;
    public String nodeName;
    public List<Node> lstShortestPathNode;
    public int MaxIterationsToWait;
    public int LastIterationIncluded = Integer.MIN_VALUE;
    public int attestation_probability;
    public boolean isAttacked;
    public HashMap<Feature.Features, Integer> objCurrentObservation = new HashMap<>();

    @Override
    public int compareTo(Node o) {

        return this.attestation_probability - o.attestation_probability;

    }
}
