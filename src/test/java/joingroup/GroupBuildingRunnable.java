package joingroup;

import client.HyperZMQ;
import org.junit.Assert;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * Multiple of these are run from the test. The run() includes the sequence of actions for which the time should be measured.
 * <p>
 * Creator creates a group and joiner tries to join, then the group is disbanded.
 * The group name and port are predefined so there are no conflicts if multiple of this thread are run in the test.
 */
public class GroupBuildingRunnable implements Runnable {

    public final HyperZMQ creator, joiner;
    public final String groupName;
    public final int port;

    public GroupBuildingRunnable(HyperZMQ creator, HyperZMQ joiner, String groupName, int port) {
        this.creator = creator;
        this.joiner = joiner;
        this.groupName = groupName;
        this.port = port;
    }

    @Override
    public void run() {
        System.out.println("Running GroupBuildingThread for " + creator.getClientID() + " and " + joiner.getClientID() + " with group " + groupName);
        creator.createGroup(groupName, ((group, message, senderID) -> {
            System.out.println(senderID + " in " + group + ": " + message);
        }));

        try {
            // FIXME need some time to have the creation published on the blockchain and the event subscription up
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        CountDownLatch latch = new CountDownLatch(1);

        joiner.tryJoinGroup(groupName, "localhost", port, null, (code, info) -> {
            if (code >= 200000) {
                fail("Failure: " + code);
                latch.countDown();
            }

            if (code == IJoinGroupStatusCallback.FOUND_CONTACT) {
                Assert.assertEquals(info, creator.getSawtoothPublicKey());
            }

            if (code == IJoinGroupStatusCallback.KEY_RECEIVED) {
                Assert.assertEquals(info, creator.getKeyForGroup(groupName));
                latch.countDown();
            }
        }, creator.getSawtoothPublicKey());

        // tryJoinGroup is async therefore we wait here
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(joiner.isGroupAvailable(groupName));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        creator.requestDisbandGroup(groupName);

        // Wait until group is deleted as requested by creator

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(joiner.isGroupAvailable(groupName));


        System.out.println("Exiting...");
    }
}
