package joingroup;

import client.HyperZMQ;
import keyexchange.KeyExchangeReceipt;
import keyexchange.ReceiptType;
import org.junit.Test;
import voting.GroupInternVotingProcess;
import voting.VotingMatter;
import voting.VotingMatterType;
import voting.YesVoteStrategy;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JoinGroupTest implements IJoinGroupStatusCallback {

    String receivedKey;

    // Have the KeyExchangeTP and CSVStringTP BOTH!!! running

    @Test
    public void test() throws InterruptedException {
        // Prepare groups
        String groupName = "testgroup";
        HyperZMQ member1 = new HyperZMQ("member1", "password", true);
        member1.createGroup(groupName);

        HyperZMQ member2 = new HyperZMQ("member2", "password", true);
        // This does not recreate a receipt, as it is not possible in production TODO
        member2.addGroup(groupName, member1.getKeyForGroup(groupName));
        KeyExchangeReceipt receipt = new KeyExchangeReceipt(member1.getSawtoothPublicKey(),
                member2.getSawtoothPublicKey(),
                ReceiptType.JOIN_GROUP,
                groupName,
                System.currentTimeMillis());
        receipt.setSignature(member1.sign(receipt.getSignablePayload()));
        member1.sendKeyExchangeReceipt(receipt);
        Thread.sleep(1000); // Wait a moment for member1 to send the KeyExchangeReceipt for the group it created

        // Prepare Voting behavior - member2 should be responsible for the voting
        member1.setVotingStrategyGroup(new YesVoteStrategy(300));
        member2.setVotingStrategyGroup(new YesVoteStrategy(300));

        member1.setVotingProcessGroup(new GroupInternVotingProcess(member1, 50));
        member2.setVotingProcessGroup(new GroupInternVotingProcess(member2, 50));

        // Start the join process
        HyperZMQ applicant = new HyperZMQ("applicant", "password", true);
        applicant.tryJoinGroup(groupName, this);

        Thread.sleep(15000);
        if (receivedKey == null)
            fail("Received no key in time");

        assertEquals("Received the correct key for the group", member1.getKeyForGroup(groupName), receivedKey);

    }

    @Test
    public void testsendvotingmatter() throws InterruptedException {
        String groupName = "testgroup2";
        HyperZMQ member1 = new HyperZMQ("member1", "password", true);
        member1.createGroup(groupName);
        HyperZMQ member2 = new HyperZMQ("member2", "password", true);
        member2.addGroup(groupName, member1.getKeyForGroup(groupName));

        VotingMatter votingMatter = new VotingMatter("applicant public key",
                member1.getSawtoothPublicKey(),
                VotingMatterType.JOIN_GROUP,
                groupName,
                Arrays.asList(member1.getSawtoothPublicKey(), member2.getSawtoothPublicKey()));

        member1.sendVotingMatterInGroup(votingMatter);

        Thread.sleep(5000);
    }

    @Override
    public void joinGroupStatusCallback(int code, String info) {
        switch (code) {
            case FOUND_CONTACT:
                System.out.println("[TEST] contact public key: " + info);
            case REQUEST_SENT:
            case STARTING_DIFFIE_HELLMAN:
            case GETTING_KEY:
                break;
            case KEY_RECEIVED:
                receivedKey = info;

        }
    }
}
