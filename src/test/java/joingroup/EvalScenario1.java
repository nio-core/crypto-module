package joingroup;

import client.HyperZMQ;
import keyexchange.Receipt;
import keyexchange.ReceiptType;
import org.junit.Assert;
import org.junit.Test;
import subgrouping.RandomSubgroupSelector;
import voting.SawtoothVoteSender;
import voting.SawtoothVotingProcess;
import voting.SimpleMajorityEvaluator;
import voting.YesVoteStrategy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class EvalScenario1 {
    // Use static keys so the member entry in the blockchain does not get flooded
    final String PRIVATE_1 = "ba58429bc686bcf14725c60b11bee7b09d1d66c1b4f38acf9fe51d91ab6cc060";
    final String PUBLIC_1 = "022fb56a55248087549e1595baf214445f81b0f40c47197846bc873becc1c4bd83";

    final String PRIVATE_2 = "80e5a4da195f625a90549e2f7d9f6f8e017b284447a43114d678b9dfa7d0dcd0";
    final String PUBLIC_2 = "0238a097292e6aba1714a8f9dc17c1eab7d76a18502ee867cc527b9c0fd3c9f944";

    final String PRIVATE_3 = "d17f08a4b8b4e7f75f326d8c15f0445ec27a6561171a169bb03245ff7625ba41";
    final String PUBLIC_3 = "02fb9e47838133e9d4d1ad3c5ba69b607d7b3377e0f2d8dd03b610d1f15b9ea3c8";

    final String PRIVATE_4 = "51ea8b07806fe89b8675ecee0d140146f48abf1a5cff31f6f2ee8c75ae900e2d";
    final String PUBLIC_4 = "0203be51b27de3bd42d09ef53928c7d7d963dc9134055a57c9c2a90527764eecf3";

    AtomicBoolean c1Joined = new AtomicBoolean(false);
    AtomicBoolean c2Joined = new AtomicBoolean(false);
    AtomicBoolean c3Joined = new AtomicBoolean(false);
    AtomicBoolean c4Joined = new AtomicBoolean(false);

    @Test
    public void increasingGroupSizeTimeMeasure() throws InterruptedException {
        AtomicBoolean finished = new AtomicBoolean(false);
        String group = "superGroup";
        int numOfClients = 100;
        List<Long> times = new ArrayList<>();

        HyperZMQ originalClient = new HyperZMQ.Builder("originalClient", "password")
                .setIdentity(PRIVATE_1)
                .build();
        originalClient.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));
        originalClient.getVoteManager().setVoteEvaluator(
                new SimpleMajorityEvaluator("originalClient"));


        originalClient.getVoteManager().setVotingProcessGroup(new SawtoothVotingProcess(originalClient));
        originalClient.getVoteManager().setVoteSender(new SawtoothVoteSender());

        // TODO test with selection before voting
        originalClient.getVoteManager().setVotingParticipantsThreshold(10);
        originalClient.getVoteManager().setSubgroupSelector(new RandomSubgroupSelector());

        originalClient.debugClearGroupMembers(group);

        Thread.sleep(1000);

        originalClient.createGroup(group);

        Thread.sleep(1000);

        for (int i = 0; i < numOfClients; i++) {
            HyperZMQ tmp = new HyperZMQ.Builder("client" + String.valueOf(i), "password")
                    .createNewIdentity(true)
                    .build();
            tmp.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(10));
            tmp.getVoteManager().setVoteSender(new SawtoothVoteSender());

            Thread.sleep(500);
            CountDownLatch latch = new CountDownLatch(1);
            long startTime = System.currentTimeMillis();

            tmp.tryJoinGroup(group, "localhost", (5555 + i), Collections.emptyMap(), (code, info) -> {
                if (code == IJoinGroupStatusCallback.KEY_RECEIVED) {
                    latch.countDown();
                }
            }, PUBLIC_1); // Explicitly let the original client do the voting - saves a lot of boilerplate code here

            latch.await();

            long endTime = System.currentTimeMillis() - startTime;
            times.add(endTime);
            System.out.println("Cur: " + i + ", Times: " + times.toString());
        }

        System.out.println("[TEST] Up to " + numOfClients + " times:" + times.toString());
    }

    @Test
    public void votingTimeWithXClientsInGroup() throws InterruptedException {
        /// Transaction Processors should be running
        /////// PARAMETERS ///////////
        int groupSize = 100;
        int numberOfRuns = 1;
        int numberOfWarmups = 0;
        //////////////////////////////

        String groupName = "testgroup";
        List<Long> times = new ArrayList<>();

        HyperZMQ originalClient = new HyperZMQ.Builder("originalClient", "password")
                .setIdentity(PRIVATE_1)
                .build();
        originalClient.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));
        originalClient.getVoteManager().setVotingProcessGroup(new SawtoothVotingProcess(originalClient));
        originalClient.getVoteManager().setVoteEvaluator(
                new SimpleMajorityEvaluator("originalClient"));

        originalClient.getVoteManager().setVotingParticipantsThreshold(10);
        originalClient.getVoteManager().setSubgroupSelector(new RandomSubgroupSelector());

        originalClient.debugClearGroupMembers(groupName);

        Thread.sleep(1000);

        originalClient.createGroup(groupName);
        String groupKey = originalClient.getKeyForGroup(groupName);
        Thread.sleep(1000);

        // Fill the group with the specified number of clients - do the shortcut with inserting the key directly + send receipt manually
        // We are not interested in the voting for the setup phase
        /* minus the original client */
        for (int i = 0; i < (groupSize - 1); i++) {
            HyperZMQ tmp = new HyperZMQ.Builder("client" + String.valueOf(i), "password")
                    .createNewIdentity(true)
                    .build();
            tmp.isVolatile = true; // Otherwise saving data will throw concurrency exceptions

            tmp.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(50));
            tmp.addGroup(groupName, groupKey);
            Receipt receipt = originalClient.messageFactory.keyExchangeReceipt(tmp.getSawtoothPublicKey(), ReceiptType.JOIN_GROUP, groupName);

            originalClient.sendKeyExchangeReceipt(receipt);
            Thread.sleep(500); // Time to process the receipt
        }

        // Let a new client try to join for given number of times and measure that
        HyperZMQ tmp = new HyperZMQ.Builder("joiner", "sdfsd")
                .createNewIdentity(true)
                .build();
        tmp.isVolatile = true;

        for (int i = 0; i < (numberOfRuns + numberOfWarmups); i++) {
            CountDownLatch latch = new CountDownLatch(1);
            long start = System.currentTimeMillis();

            tmp.tryJoinGroup(groupName, "localhost", 5555 + i, Collections.emptyMap(), (code, info) -> {
                if (code == IJoinGroupStatusCallback.KEY_RECEIVED) {
                    latch.countDown();
                }
            }, PUBLIC_1);

            latch.await();
            long elapsedTime = System.currentTimeMillis() - start;

            if (i >= numberOfWarmups) {
                times.add(elapsedTime);
            }

            System.out.println("Times: " + times.toString());

            // Remove the client from the group to keep it at X members
            tmp.removeGroup(groupName);
            Thread.sleep(1000); // wait for receipt to be processed
        }

        System.out.println("[TEST] Result for " + groupSize + " Clients: " + times.toString() + "[ms]");
        Double avg = times.stream().mapToLong(Long::longValue).average().getAsDouble();
        DecimalFormat df = new DecimalFormat("####.###");
        System.out.println("[TEST] Average: " + df.format(avg) + "[ms]");
    }

    /**
     * We have 4 clients that form a group
     * Afterwards, 2 clients each form another group
     * <p>
     * Transaction Processors need to be running
     */
    @Test
    public void test() throws InterruptedException {
        // Client setup
        HyperZMQ client1 = new HyperZMQ.Builder("client1", "password")
                .setIdentity(PRIVATE_1)
                .build();

        HyperZMQ client2 = new HyperZMQ.Builder("client2", "password")
                .setIdentity(PRIVATE_2)
                .build();

        HyperZMQ client3 = new HyperZMQ.Builder("client3", "password")
                .setIdentity(PRIVATE_3)
                .build();

        HyperZMQ client4 = new HyperZMQ.Builder("client4", "password")
                .setIdentity(PRIVATE_4)
                .build();

        // Setup for voting - just vote YES
        client1.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));
        client1.getVoteManager().setVotingProcessGroup(new SawtoothVotingProcess(client1));
        client1.getVoteManager().setVoteEvaluator(
                new SimpleMajorityEvaluator("client1"));

        client2.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));
        client2.getVoteManager().setVotingProcessGroup(new SawtoothVotingProcess(client2));
        client2.getVoteManager().setVoteEvaluator(
                new SimpleMajorityEvaluator("client2"));

        client3.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));
        client3.getVoteManager().setVotingProcessGroup(new SawtoothVotingProcess(client3));
        client3.getVoteManager().setVoteEvaluator(
                new SimpleMajorityEvaluator("client3"));

        client4.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));
        client4.getVoteManager().setVotingProcessGroup(new SawtoothVotingProcess(client4));
        client4.getVoteManager().setVoteEvaluator(
                new SimpleMajorityEvaluator("client4"));


        // Client1 creates the new group
        String superGroupName = "superGroup";
        client1.createGroup(superGroupName);
        // Wait for it to publish on the blockchain
        Thread.sleep(500);

        // Values for the sockets that will do diffie hellman
        String dhSocketAddr = "localhost";
        int dhSocketPort = 5555;

        // Start joining - client2
        client2.tryJoinGroup(superGroupName, dhSocketAddr, dhSocketPort, Collections.emptyMap(), (code, info) -> {
                    if (code == IJoinGroupStatusCallback.KEY_RECEIVED) {
                        c2Joined.set(true);
                    }
                },
                // Explicitly set the public key of the entity that will moderate the voting
                // Because if the test is re-run, the order in which the clients joined is already set in the blockchain
                // Which will cause the test to fail
                PUBLIC_1);

        while (!c2Joined.get()) {
            Thread.sleep(200);
        }

        Assert.assertTrue(client2.isGroupAvailable(superGroupName));
        Thread.sleep(200);
        // Client 3
        client3.tryJoinGroup(superGroupName, dhSocketAddr, 5556, Collections.emptyMap(), (code, info) -> {
            if (code == IJoinGroupStatusCallback.KEY_RECEIVED) {
                c3Joined.set(true);
            }
        }, PUBLIC_2);

        while (!c3Joined.get()) {
            Thread.sleep(200);
        }

        Assert.assertTrue(client3.isGroupAvailable(superGroupName));
        Thread.sleep(200);
        // Client4
        client4.tryJoinGroup(superGroupName, dhSocketAddr, 5557, Collections.emptyMap(), (code, info) -> {
            if (code == IJoinGroupStatusCallback.KEY_RECEIVED) {
                c4Joined.set(true);
            }
        }, PUBLIC_3);

        while (!c4Joined.get()) {
            Thread.sleep(200);
        }

        Assert.assertTrue(client4.isGroupAvailable(superGroupName));
        Thread.sleep(200);

    }
}
