package smartgossip;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SmartGossipNode {

    private static final boolean debugPrint = true;

    private String pid;

    private List<String> neighborSet = new ArrayList<>();
    private List<String> childSet = new ArrayList<>();
    private List<String> parentSet = new ArrayList<>();
    private List<String> siblingSet = new ArrayList<>();

    // This percentage implies that the gossip source expects X % of its packets received everywhere
    // (Also over multiple hops)
    private double avgReceptionPercentage = 1.0;

    private double perHopReceptionPercentage = 1.0;

    public SmartGossipNode(String pid, double avgRerceptionPercentage) {
        this.pid = pid;
        this.avgReceptionPercentage = avgRerceptionPercentage;
        // During initialization, a node permanently inserts itself into
        // NeighborSet and SiblingSet, while the other two sets are empty

        neighborSet.add(pid);
        siblingSet.add(pid);
    }

    /**
     * Whenever the node receives a message from any node X with pid field set to some Y,
     * it uses the following concise rules:
     * • Add X to NeighborSet
     * • If Y is not in NeighborSet, add X to ParentSet
     * • else if Y is in ParentSet, add X to SiblingSet
     * • else if Y is in SiblingSet, add X to ChildSet
     */
    void updateSets(String x, String y) {
        neighborSet.add(x);
        print("Adding " + x + " to neighbors set.");
        if (!neighborSet.contains(y)) {
            parentSet.add(x);
            print("Adding " + x + " to parents.");
        } else if (parentSet.contains(y)) {
            siblingSet.add(x);
            print("Adding " + x + " to siblings.");
        } else if (siblingSet.contains(y)) {
            childSet.add(x);
            print("Adding " + x + " to children.");
        }

        // Remove all duplicates
        neighborSet = neighborSet.stream().distinct().collect(Collectors.toList());
        parentSet = parentSet.stream().distinct().collect(Collectors.toList());
        siblingSet = siblingSet.stream().distinct().collect(Collectors.toList());
        childSet = childSet.stream().distinct().collect(Collectors.toList());
    }

    void print(String s) {
        if (debugPrint)
            System.out.println("[SmartGossipNode" + pid + "] " + s);
    }

}
