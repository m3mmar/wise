/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author mammar
 */
public class OptimizationAlgorithms {

    public static List<Node> getInsecureNodes(int current_iteration, double coverage_threshold, List<Node> lstNodes, HashMap<String, List<Node>> objClutsers) {
        List<Node> insec_nodes = new ArrayList<>();

        lstNodes.forEach((objNode) -> {
            HashMap<Feature.Features, Integer> objObservation = new HashMap<>();

            objObservation.put(Feature.Features.Neighbour_Attacked_Ratio, RatioAttackedNeighbours(objNode));
            objObservation.put(Feature.Features.Cluster_Attacked_Ratio, RatioAttackedClusters(objNode, objClutsers));

            objNode.attestation_probability = objNode.predictiveModel.probablityBeingSucessAttessted(objObservation);
            objNode.objCurrentObservation = objObservation;
        });

        Collections.sort(lstNodes);

        //  System.out.println("Top 10 Nodes");
        //  for (int i = 0; i < 10; i++) {
        //     System.out.println("Node ID:" + lstNodes.get(i).nodeName + ", Success Attessted:" + lstNodes.get(i).attestation_probability + "%");
        //  }
        for (int i = 0; i < (int) (lstNodes.size() * coverage_threshold); i++) {
            insec_nodes.add(lstNodes.get(i));

        }

        for (int i = (int) (lstNodes.size() * coverage_threshold); i < lstNodes.size(); i++) {
            Node objNode = lstNodes.get(i);

            if (objNode.LastIterationIncluded + objNode.MaxIterationsToWait <= current_iteration) {
                insec_nodes.add(objNode);
            }
        }

        System.out.println("Ratio Insecure Nodes:" + insec_nodes.size() * 100.0 / lstNodes.size());
        return insec_nodes;

    }

    public static List<Node> getIntermediateNodes(List<Node> insecureNodes) {
        List<Node> interMediateNodes = new ArrayList<>();

        insecureNodes.forEach((Node objNode) -> {
            objNode.lstShortestPathNode.stream().filter((n) -> (!interMediateNodes.contains(n))).forEachOrdered((n) -> {
                interMediateNodes.add(n);
            });
        });

        return interMediateNodes;
    }

    public static HashMap<String, List<Cluster>> CommunincationOverheadClusters(int currentIteration, List<Node> interNodes, ArrayList<Node> insecNodes, HashMap<String, List<Node>> objClutsers) {
        HashMap<String, List<Cluster>> objMap = new HashMap<>();

        List<Cluster> lst_includedClusters = new ArrayList<>();
        List<Cluster> lst_ExecludedClusters = new ArrayList<>();
        List<Cluster> lst_attestation_clusters = new ArrayList<>();

        List<Cluster> lst_brodcasting_clsuters = new ArrayList<>();

        System.out.println("Num Unique Clusters:" + objClutsers.size());

        objClutsers.entrySet().forEach((ent) -> {
            boolean hasNode = false;
            Set<Node> objNodesSet = new HashSet<>();
            int highestNumberOfHops = 0;

            for (Node n : ent.getValue()) {
                if (interNodes.contains(n) || insecNodes.contains(n)) {
                    hasNode = true;
                }
                if (highestNumberOfHops < n.lstShortestPathNode.size()) {
                    highestNumberOfHops = n.lstShortestPathNode.size();
                }
                //for (Node s : n.lstShortestPathNode) {
                //   objNodesSet.add(s);
                //}
            }

            if (hasNode) {
                Cluster objCl = new Cluster();
                objCl.lstNodes = new ArrayList<>();
                objCl.lstNodes.addAll(ent.getValue());
                objCl.comOverhead = highestNumberOfHops;
                objCl.name = ent.getKey();
                lst_includedClusters.add(objCl);

            } else {
                Cluster objCl = new Cluster();
                objCl.lstNodes = new ArrayList<>();
                objCl.lstNodes.addAll(ent.getValue());
                objCl.name = ent.getKey();
                lst_ExecludedClusters.add(objCl);
            }
        });

        Collections.sort(lst_includedClusters);

        List<Node> shadowInsecNodes = new ArrayList<>();
        shadowInsecNodes.addAll(insecNodes);
        for (int i = 0; i < lst_includedClusters.size(); i++) {

            boolean attesationCluster = false;
            boolean broadcastedCluster = false;
            for (Node n : lst_includedClusters.get(i).lstNodes) {
                if (shadowInsecNodes.contains(n)) {
                    shadowInsecNodes.remove(n);
                    attesationCluster = true;
                }

                if (interNodes.contains(n)) {
                    broadcastedCluster = true;
                    interNodes.remove(n);
                }

            }

            if (attesationCluster) {
                lst_attestation_clusters.add(lst_includedClusters.get(i));

            } else if (broadcastedCluster) {
                lst_brodcasting_clsuters.add(lst_includedClusters.get(i));
            } else {
                lst_ExecludedClusters.add(lst_includedClusters.get(i));
            }

        }
        HashMap<Cluster, Integer> tempClusters = new HashMap<>();
        insecNodes.forEach((Node node) -> {
            lst_attestation_clusters.stream().filter((c) -> (c.lstNodes.contains(node))).forEachOrdered((c) -> {
                if (tempClusters.containsKey(c)) {
                    tempClusters.put(c, tempClusters.get(c) + 1);
                } else {
                    tempClusters.put(c, 1);
                }
            });
        });
        HashMap<Cluster, Integer> sortedTempClusters = Sorter.sortByValue(tempClusters);
        List<Cluster> final_lst_attestation_clusters = new ArrayList<>();
        List<Node> removedInsecNodes = new ArrayList<>();
        Set attestSet = sortedTempClusters.entrySet();
        Iterator iterator = attestSet.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Cluster, Integer> ent = (Map.Entry) iterator.next();
            Boolean included = false;
            for (Node n : insecNodes) {
                if (!removedInsecNodes.contains(n)) {
                    if (ent.getKey().lstNodes.contains(n)) {
                        included = true;
                        removedInsecNodes.add(n);
                    }
                }
            }
            if (included) {
                final_lst_attestation_clusters.add(ent.getKey());
            }
        }
        final_lst_attestation_clusters.forEach((c) -> {
            lst_attestation_clusters.remove(c);
        });
        //System.out.println("*************** Optimised : " + lst_attestation_clusters.size() + " **************");
        lst_attestation_clusters.forEach((c) -> {
            lst_ExecludedClusters.add(c);
        });

        final_lst_attestation_clusters.forEach((Cluster cl) -> {
            cl.lstNodes.forEach((n) -> {
                n.LastIterationIncluded = currentIteration;
            });
        });

        objMap.put("broadcasting", lst_brodcasting_clsuters);
        objMap.put("attestation", final_lst_attestation_clusters);
        objMap.put("excluded", lst_ExecludedClusters);

        return objMap;

    }

    public static Integer RatioAttackedNeighbours(Node objNode) {
        int num_attacked = 0;

        num_attacked = objNode.lstNeihbours.stream().filter((o) -> (o.predictiveModel.last_Attest_state == HMM_Model.Attesation_States.ZERO)).map((_item) -> 1).reduce(num_attacked, Integer::sum);

        int val = (int) (num_attacked * 100.0 / objNode.lstNeihbours.size());

        return val;
    }

    public static Integer RatioAttackedClusters(Node objNode, HashMap<String, List<Node>> objMap) {
        int all_nodes = 0;
        int attacked_nodes = 0;

        for (String st : objNode.lstClusters) {
            List<Node> lst_node = objMap.get(st);

            for (Node o : lst_node) {
                if (o.predictiveModel.last_Attest_state == HMM_Model.Attesation_States.ZERO) {
                    attacked_nodes++;
                }
                all_nodes++;
            }
        }
        int val = (int) (attacked_nodes * 100.0 / all_nodes);
        return val;
    }

}
