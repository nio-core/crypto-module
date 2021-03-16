package joingroup;

import client.HyperZMQ;
import org.junit.Assert;
import subgrouping.RandomSubgroupSelector;
import voting.*;

import java.util.ArrayList;
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
public class GroupBuildingRunnable2 implements Runnable {

    public final String groupName;
    public final int dhPort, zmqPort;
    final int agentCount;

    boolean useZMQ = false;

    public GroupBuildingRunnable2(int agentCount, String groupName, int port, int zmqPort) {
        this.agentCount = agentCount;
        this.groupName = groupName;
        this.dhPort = port;
        this.zmqPort = zmqPort;
    }

    @Override
    public void run() {
        //System.out.println("Running GroupBuildingThread for group'" + groupName + "' with agents: "
        //        + agents.stream().map(HyperZMQ::getClientID).reduce("", (s, s2) -> s2 + ", " + s));
        System.err.println("Running GBR2 for group " + groupName + " with " + agentCount + " agents");
        List<HyperZMQ> list = new ArrayList<>();
        if (agentCount == 0) return;
        HyperZMQ c1 = new HyperZMQ.Builder("client1-" + groupName, "sdfsd").build();
        c1.isVolatile = true;
        c1.getVoteManager().setVoteEvaluator(new SimpleMajorityEvaluator(c1.getClientID()));
        list.add(c1);

        if (useZMQ) {
            c1.getVoteManager().setVotingProcessGroup(new ZMQVotingProcess(c1, zmqPort));
            c1.getVoteManager().setVoteSender(new ZMQVoteSender());
        } else {
            c1.getVoteManager().setVotingProcessGroup(new SawtoothVotingProcess(c1));
            c1.getVoteManager().setVoteSender(new SawtoothVoteSender());
        }


        c1.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(0));

        c1.getVoteManager().setVotingParticipantsThreshold(5);
        c1.getVoteManager().setSubgroupSelector(new RandomSubgroupSelector());

        // The first agent creates the group
        c1.createGroup(groupName, ((group, message, senderID) -> {
            //System.out.println(senderID + " in " + group + ": " + message);
        }));

        String creatorPublicKey = c1.getSawtoothPublicKey();
        String groupKey = c1.getKeyForGroup(groupName);

        try {
            // FIXME need some time to have the creation published on the blockchain and the event subscription up
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Now the other agents join SEQUENTIALLY
        for (int i = 0; i < agentCount - 1; i++) {
            CountDownLatch latch = new CountDownLatch(1);

            HyperZMQ tmp = new HyperZMQ.Builder("client" + String.valueOf(i + 1) + "-" + groupName, "whatever").build();
            tmp.isVolatile = true; // Don't save any data

            //tmp.getVoteManager().setVoteEvaluator(new SimpleMajorityEvaluator(tmp.getClientID()));

            if (useZMQ) {
                tmp.getVoteManager().setVotingProcessGroup(new ZMQVotingProcess(tmp, zmqPort + i));
                tmp.getVoteManager().setVoteSender(new ZMQVoteSender());
            } else {
                tmp.getVoteManager().setVotingProcessGroup(new SawtoothVotingProcess(tmp));
                tmp.getVoteManager().setVoteSender(new SawtoothVoteSender());
            }

            tmp.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(0));

            tmp.getVoteManager().setVotingParticipantsThreshold(5);
            tmp.getVoteManager().setSubgroupSelector(new RandomSubgroupSelector());

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println("joining " + tmp.getClientID());
            tmp.tryJoinGroup(groupName, "localhost", dhPort + 1 + i, null, (code, info) -> {
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

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            assertTrue(tmp.isGroupAvailable(groupName));

            try {
                // FIXME
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            list.add(tmp);
        }

        for (HyperZMQ hyperZMQ : list) {
            try {
                hyperZMQ.close();
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
