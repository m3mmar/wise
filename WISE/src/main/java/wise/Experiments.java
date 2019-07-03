/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import static wise.NetworkUtils.getNodesClusters;
import static wise.NetworkUtils.readNetworkNodes;
import static wise.OptimizationAlgorithms.RatioAttackedClusters;
import static wise.OptimizationAlgorithms.RatioAttackedNeighbours;

/**
 *
 * @author mammar
 */
public class Experiments {

    public static void main(String[] args) throws Exception {

        List<String> lstAttackedClusters = new ArrayList<>();
        lstAttackedClusters.add("h5");
        lstAttackedClusters.add("h6");

        doExperiments("networks/network1/1000graph.csv",
                "ShortestPaths_Newtork1_10000.csv",
                "networks/clusters.csv",
                10,//number of iterations for simulation
                0.01,//coverage threshold
                10,//number iterations per sequence for HMM (m value)
                .2,// maximum attacked nodes ratio per iteration
                lstAttackedClusters,// list of clusters names to be attacked random
                10,// number of iterations for learning
                4);  // min Time 

    }

    public static void RandomAttck(List<Node> lstNodes, double ratio, List<String> targetClusters) {
        int num = lstNodes.size();
        List<Integer> nodesToAttack = new ArrayList<>();
        Random objRand = new Random();

        int max_nodes = (int) (lstNodes.size() * ratio);
        int num_attacked_nodes = objRand.nextInt(max_nodes);

        for (int j = 0; j < 100; j++) {
            for (int i = 0; i < num; i++) {
                int node_num = Math.abs(objRand.nextInt()) % lstNodes.size();

                if (!nodesToAttack.contains(node_num)) {

                    if (targetClusters != null && !targetClusters.isEmpty()) {
                        boolean belongToTargetCluster = false;
                        for (String cl : targetClusters) {
                            if (lstNodes.get(node_num).lstClusters.contains(cl)) {
                                belongToTargetCluster = true;
                                break;
                            }

                        }

                        if (belongToTargetCluster) {
                            nodesToAttack.add(node_num);
                        }
                    } else {
                        nodesToAttack.add(node_num);
                    }
                }

                if (nodesToAttack.size() == num_attacked_nodes) {
                    break;
                }
            }

            if (nodesToAttack.size() == num_attacked_nodes) {
                break;
            }
        }

        nodesToAttack.forEach((i) -> {
            lstNodes.get(i).isAttacked = true;
        });

    }

    public static void doExperiments(String networkPath, String shortestPath, String clusterPath,
            int num_iterations, double coverageTh, int num_iterations_per_seq,
            double attckedNodesRatio, List<String> lstTargetAttackedClusters, int num_learning_iterations, int maxTimeForSleepingNodes) throws IOException {

        //random attack issue
        List<Node> lstNodes = readNetworkNodes(networkPath, shortestPath, clusterPath, maxTimeForSleepingNodes);
        HashMap<String, List<Node>> objMapCluserNodes = getNodesClusters(lstNodes);

        //start learning phase
        for (int i = 0; i < num_learning_iterations; i++) {
            RandomAttck(lstNodes, attckedNodesRatio, lstTargetAttackedClusters);

            for (Node n : lstNodes) {
                HashMap<Feature.Features, Integer> objObservation = new HashMap<>();

                objObservation.put(Feature.Features.Neighbour_Attacked_Ratio, RatioAttackedNeighbours(n));
                objObservation.put(Feature.Features.Cluster_Attacked_Ratio, RatioAttackedClusters(n, objMapCluserNodes));

                if (n.isAttacked) {
                    n.predictiveModel.UpdateModel(n.predictiveModel.last_Attest_state, HMM_Model.Attesation_States.ZERO, objObservation, i, num_iterations_per_seq);

                } else {
                    n.predictiveModel.UpdateModel(n.predictiveModel.last_Attest_state, HMM_Model.Attesation_States.ONE, objObservation, i, num_iterations_per_seq);

                }
            }
            lstNodes.stream().map((n) -> {
                if (n.isAttacked) {

                    n.predictiveModel.last_Attest_state = HMM_Model.Attesation_States.ZERO;
                } else {
                    n.predictiveModel.last_Attest_state = HMM_Model.Attesation_States.ONE;
                }
                return n;
            }).map((n) -> {
                n.isAttacked = false;
                return n;
            }).forEachOrdered((n) -> {
                n.LastIterationIncluded = 0;
            });
        }

        //end learning phase
        double avgRecall = 0;
        double bestRecall = 0;
        double worstRecall = 100;
        int avgNodesAttested = 0;
        for (int i = 0; i < num_iterations; i++) {

            double num_Attacked_nodes = 0;
            HashSet<Node> lstNodesDetected = new HashSet<>();
            RandomAttck(lstNodes, attckedNodesRatio, lstTargetAttackedClusters);
            ArrayList<Node> lstSecure = (ArrayList<Node>) OptimizationAlgorithms.getInsecureNodes(i, coverageTh, lstNodes, objMapCluserNodes);
            List<Node> lstInter = OptimizationAlgorithms.getIntermediateNodes(lstSecure);
            avgNodesAttested += lstSecure.size();

            HashMap<String, List<Cluster>> objBroadCasting_Attesation_clsuters = OptimizationAlgorithms.CommunincationOverheadClusters(i, lstInter, lstSecure, objMapCluserNodes);
            List<Cluster> lstBroadcasting_clusters = objBroadCasting_Attesation_clsuters.get("broadcasting");
            List<Cluster> lstAttesation_clusters = objBroadCasting_Attesation_clsuters.get("attestation");
            List<Cluster> lstExcluded_clusters = objBroadCasting_Attesation_clsuters.get("excluded");

            num_Attacked_nodes = lstNodes.stream().filter((n) -> (n.isAttacked)).map((_item) -> 1.0).reduce(num_Attacked_nodes, (accumulator, _item) -> accumulator + 1);

            for (Cluster objCluster : lstAttesation_clusters) {
                for (Node n : objCluster.lstNodes) {
                    if (n.isAttacked) {
                        if (!lstNodesDetected.contains(n)) {
                            lstNodesDetected.add(n);

                        }
                        n.predictiveModel.UpdateModel(n.predictiveModel.last_Attest_state, HMM_Model.Attesation_States.ZERO, n.objCurrentObservation, i, num_iterations_per_seq);
                    } else {
                        n.predictiveModel.UpdateModel(n.predictiveModel.last_Attest_state, HMM_Model.Attesation_States.ONE, n.objCurrentObservation, i, num_iterations_per_seq);

                    }
                }
            }

            lstBroadcasting_clusters.forEach((objCluster) -> {
                objCluster.lstNodes.forEach((n) -> {
                    n.predictiveModel.last_Attest_state = HMM_Model.Attesation_States.UN;
                });
            });

            lstExcluded_clusters.forEach((objCluster) -> {
                objCluster.lstNodes.forEach((n) -> {
                    n.predictiveModel.last_Attest_state = HMM_Model.Attesation_States.UN;
                });
            });

            lstAttesation_clusters.forEach((Cluster objCluster) -> {
                objCluster.lstNodes.stream().map((n) -> {
                    if (n.isAttacked) {

                        n.predictiveModel.last_Attest_state = HMM_Model.Attesation_States.ZERO;
                    } else {
                        n.predictiveModel.last_Attest_state = HMM_Model.Attesation_States.ONE;
                    }
                    return n;
                }).forEachOrdered((n) -> {
                    n.isAttacked = false;
                });
            });

            System.out.println("Num Excluded Clusters:" + objBroadCasting_Attesation_clsuters.get("excluded").size());
            System.out.println("Num Attested Clusters:" + objBroadCasting_Attesation_clsuters.get("attestation").size());
            System.out.println("Num Bridge-based Clusters:" + objBroadCasting_Attesation_clsuters.get("broadcasting").size());
            System.out.println("Num detected nodes = " + lstNodesDetected.size());
            System.out.println("Num Compromised nodes = " + num_Attacked_nodes);
            double recall = (lstNodesDetected.size() * 100 / num_Attacked_nodes);
            System.out.println("Recall:" + recall);
            avgRecall += recall;
            if (bestRecall < recall) {
                bestRecall = recall;
            }
            if (worstRecall > recall) {
                worstRecall = recall;
            }
            HashSet<Node> uniqueNodes = new HashSet<>();
            lstAttesation_clusters.forEach((c) -> {
                uniqueNodes.addAll(c.lstNodes);
            });

            System.out.println("Number of attested devices = " + uniqueNodes.size());

            System.out.print("Attestation clusters:");
            objBroadCasting_Attesation_clsuters.get("attestation").forEach((objCluster) -> {
                System.out.print(objCluster.name + ",");
            });

            System.out.println();
            System.out.print("Broadcasting clusters:");
            objBroadCasting_Attesation_clsuters.get("broadcasting").forEach((objCluster) -> {
                System.out.print(objCluster.name + ",");
            });

            System.out.println();

            System.out.println("------------------");

            lstNodes.forEach((n) -> {
                n.isAttacked = false;
            });

        }
        System.out.println("Worst Recall  = " + worstRecall);
        System.out.println("Best Recall = " + bestRecall);
        System.out.println("Average Recall = " + avgRecall / num_iterations);
        System.out.println("Average number of attested nodes = " + avgNodesAttested / num_iterations);
        System.out.println("*********************************************************************");

    }

}
