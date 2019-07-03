/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wise;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import wise.Feature.Features;

/**
 *
 * @author mammar
 */
public class NetworkUtils {

    public static HashMap<String, List<String>> ParseNetworkMatrx(String f) throws IOException {
        HashMap<String, List<String>> objMap = new HashMap<>();

        List<String> lstLines = FileUtils.readLines(new File(f));

        lstLines.stream().map((l) -> l.split(",")).forEachOrdered((arr) -> {
            List<String> objIds = new ArrayList<>();

            for (int i = 1; i < arr.length; i++) {
                if (!arr[i].trim().isEmpty()) {
                    objIds.add(arr[i]);
                }
            }
            objMap.put(arr[0], objIds);
        });
        return objMap;
    }

    public static HashMap<String, List<String>> ParseNodeClusters(String f) throws IOException {
        HashMap<String, List<String>> objMap = new HashMap<>();

        List<String> lstLines = FileUtils.readLines(new File(f));

        lstLines.stream().map((l) -> l.replaceAll("\"", "").split(",")).forEachOrdered((arr) -> {
            List<String> objIds = new ArrayList<>();

            for (int i = 1; i < arr.length; i++) {
                if (!arr[i].trim().isEmpty()) {
                    objIds.add(arr[i]);

                }
            }

            objMap.put(arr[0], objIds);
        });

        return objMap;
    }

    public static HashMap<String, List<String>> ParseShortestPaths(String f) throws IOException {
        HashMap<String, List<String>> objMap = new HashMap<>();

        List<String> lstLines = FileUtils.readLines(new File(f));

        lstLines.stream().map((l) -> l.split(",")).forEachOrdered((arr) -> {
            List<String> objIds = new ArrayList<>();

            for (int i = 0; i < arr.length - 1; i++) {
                if (!arr[i].trim().isEmpty()) {
                    objIds.add(arr[i]);
                }
            }

            objMap.put(arr[arr.length - 1], objIds);
        });

        return objMap;
    }

    public enum RelationshipTypes implements RelationshipType {
        link;
    }

    public static void InsertNetworkIntoGraph(HashMap<String, List<String>> objMap, String graphName, int num_nodes) throws IOException {

        GraphDatabaseFactory graphDbFactory = new GraphDatabaseFactory();
        GraphDatabaseService graphDb = graphDbFactory.newEmbeddedDatabase(new File(graphName));
        graphDb.beginTx();
        HashMap<String, org.neo4j.graphdb.Node> objNodes = new HashMap<>();

        objMap.entrySet().forEach((ent) -> {
            org.neo4j.graphdb.Node car = graphDb.createNode(Label.label("Node"));
            car.setProperty("name", ent.getKey());
            objNodes.put(ent.getKey(), car);
        });

        objMap.entrySet().forEach((Map.Entry<String, List<String>> ent) -> {
            org.neo4j.graphdb.Node obj_n1 = objNodes.get(ent.getKey());

            ent.getValue().stream().map((s) -> objNodes.get(s)).forEachOrdered((obj_n2) -> {
                obj_n1.createRelationshipTo(obj_n2, RelationshipTypes.link);
            });
        });

        StringBuilder objBuild = new StringBuilder();

        for (int i = 2; i < num_nodes; i++) {
            try (Result result = graphDb.execute(String.format("MATCH path=shortestPath((n1:Node {name:'1'})-[*0..10000]-(n2:Node {name:'%d'}))\n"
                    + "RETURN path", i))) {
                while (result.hasNext()) {

                    ResourceIterator<Path> nnod = result.columnAs("path");

                    while (nnod.hasNext()) {
                        Path tt = nnod.next();

                        for (org.neo4j.graphdb.Node n : tt.nodes()) {
                            objBuild.append(n.getProperty("name")).append(",");
                        }
                        objBuild.append("\n");
                    }
                }
                System.out.println(i);
            }
        }
        FileUtils.writeStringToFile(new File(graphName + "_" + num_nodes + ".csv"), objBuild.toString());
    }

    public static List<Node> readNetworkNodes(String adj_path, String verfier_shortest_paths, String node_categories_path, int maxTime) throws IOException {
        HashMap<String, List<String>> objMap = ParseNetworkMatrx(adj_path);
        List<Node> lstNodes = new ArrayList<>();
        HashMap<String, Node> objMapNode = new HashMap<>();
        Random objRand = new Random();
        objMap.entrySet().stream().map((ent) -> {
            Node objNode = new Node();
            objNode.nodeName = ent.getKey().replaceAll("\"", "");
            objNode.lstNeihbours = new ArrayList<>();
            objNode.lstShortestPathNode = new ArrayList<>();
            objNode.lstClusters = new ArrayList<>();
            objNode.predictiveModel = new HMM_Model();
            objNode.predictiveModel.lstFeatures = new ArrayList<>();
            objNode.MaxIterationsToWait = Math.abs(objRand.nextInt()) % maxTime + maxTime;
            for (Features objVal : Feature.Features.values()) {
                Feature objFeature = new Feature();
                objFeature.FeatureName = objVal;
                objNode.predictiveModel.lstFeatures.add(objFeature);
            }
            objMapNode.put(ent.getKey().replaceAll("\"", ""), objNode);
            return objNode;
        }).forEachOrdered((objNode) -> {
            lstNodes.add(objNode);
        });

        objMap.entrySet().forEach((Map.Entry<String, List<String>> ent) -> {
            ent.getValue().stream().filter((n) -> (objMapNode.get(ent.getKey()) != null)).forEachOrdered((n) -> {
                objMapNode.get(ent.getKey()).lstNeihbours.add(objMapNode.get(n));
            });
        });

        HashMap<String, List<String>> objShortestPaths = ParseShortestPaths(verfier_shortest_paths);

        objShortestPaths.entrySet().forEach((ent) -> {
            ent.getValue().stream().filter((n) -> (objMapNode.get(ent.getKey()) != null)).forEachOrdered((n) -> {
                objMapNode.get(ent.getKey()).lstShortestPathNode.add(objMapNode.get(n));
            });
        });

        HashMap<String, List<String>> objNodeClusters = ParseNodeClusters(node_categories_path);

        objNodeClusters.entrySet().stream().filter((ent) -> (objMapNode.get(ent.getKey().replaceAll("\"", "")) != null)).forEachOrdered((ent) -> {
            objMapNode.get(ent.getKey()).lstClusters.addAll(ent.getValue());
        });

        return lstNodes;

    }

    public static HashMap<String, List<Node>> getNodesClusters(List<Node> lstNodes) {
        HashMap<String, List<Node>> objMap = new HashMap<>();

        lstNodes.forEach((n) -> {
            n.lstClusters.forEach((cN) -> {
                if (objMap.containsKey(cN)) {
                    objMap.get(cN).add(n);
                } else {
                    List<Node> lst_n = new ArrayList<>();
                    lst_n.add(n);
                    objMap.put(cN, lst_n);
                }
            });
        });

        return objMap;
    }

}
