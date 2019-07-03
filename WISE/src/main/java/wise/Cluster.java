/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wise;

import java.util.List;

/**
 *
 * @author mammar
 */
public class Cluster implements Comparable<Cluster> {

    public List<Node> lstNodes;
    public int comOverhead;
    public String name;

    @Override
    public int compareTo(Cluster o) {
        return this.comOverhead - o.comOverhead;

    }
}
