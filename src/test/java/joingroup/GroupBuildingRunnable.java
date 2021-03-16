package joingroup;

import client.HyperZMQ;
import org.junit.Assert;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Multiple of these are run from the test. The run() includes the sequence of actions for which the time should be measured.
 * <p>
 * Creator creates a group and joiner tries to join, then the group is disbanded.
 * The group name and port are predefined so there are no conflicts if multiple of this thread are run in the test.
 */
public class GroupBuildingRunnable implements Runnable {

    public final List<HyperZMQ> agents;
    public final String groupName;
    public final int port;

    public GroupBuildingRunnable(List<HyperZMQ> agents, String groupName, int port) {
        this.agents = agents;
        this.groupName = groupName;
        this.port = port;
    }

    @Override
    public void run() {
        System.out.println("Running GroupBuildingThread for group'" + groupName + "' with agents: "
                + agents.stream().map(HyperZMQ::getClientID).reduce("", (s, s2) -> s2 + ", " + s));

        // The first agents creates the group
        agents.get(0).createGroup(groupName, ((group, message, senderID) -> {
            //System.out.println(senderID + " in " + group + ": " + message);
        }));

        String creatorPublicKey = agents.get(0).getSawtoothPublicKey();
        String groupKey = agents.get(0).getKeyForGroup(groupName);

        try {
            // FIXME need some time to have the creation published on the blockchain and the event subscription up
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Now the other agents join SEQUENTIALLY
        for (int i = 1; i < agents.size(); i++) {
            CountDownLatch latch = new CountDownLatch(1);

            agents.get(i).tryJoinGroup(groupName, "localhost", port + i, null, (code, info) -> {
                if (code >= 200000) {
                    fail("Failure: " + code);
                    latch.countDown();
                }

                if (code == IJoinGroupStatusCallback.FOUND_CONTACT) {
                    Assert.assertEquals(info, creatorPublicKey);
                }

                if (code == IJoinGroupStatusCallback.KEY_RECEIVED) {
                    Assert.assertEquals(info, groupKey);
                    latch.countDown();
                }
            }, creatorPublicKey);

            // tryJoinGroup is async therefore we wait here
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            assertTrue(agents.get(i).isGroupAvailable(groupName));

            try {
                // FIXME
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (HyperZMQ agent : agents) {
            try {
                agent.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*
        agents.get(0).requestDisbandGroup(groupName);

        // Wait until group is deleted as requested by creator
        // The time it takes to propagate the disband request varies a lot

        try {
            Thread.sleep(1000); // min
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (HyperZMQ a : agents) {
            assertFalse(a.isGroupAvailable(groupName));
        }
        */
        //System.out.println("Exiting...");
    }
}
