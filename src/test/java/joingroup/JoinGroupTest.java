package joingroup;

import client.HyperZMQ;
import java.util.Arrays;
import java.util.Collections;
import keyexchange.KeyExchangeReceipt;
import keyexchange.ReceiptType;
import org.junit.Test;
import voting.GroupInternVotingProcess;
import voting.JoinRequestType;
import voting.SimpleMajorityEvaluator;
import voting.VotingMatter;
import voting.YesVoteStrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JoinGroupTest implements IJoinGroupStatusCallback {

    String receivedKey;

    // Have the KeyExchangeTP and CSVStringTP BOTH!!! running

    // Use static keys so the member entry in the blockchain does not get flooded
    final String PRIVATE_MEMBER_1 = "ba58429bc686bcf14725c60b11bee7b09d1d66c1b4f38acf9fe51d91ab6cc060";
    final String PUBLIC_MEMBER_1 = "022fb56a55248087549e1595baf214445f81b0f40c47197846bc873becc1c4bd83";

    final String PRIVATE_MEMBER_2 = "80e5a4da195f625a90549e2f7d9f6f8e017b284447a43114d678b9dfa7d0dcd0";
    final String PUBLIC_MEMBER_2 = "0238a097292e6aba1714a8f9dc17c1eab7d76a18502ee867cc527b9c0fd3c9f944";

    final String PRIVATE_APPLICANT = "d17f08a4b8b4e7f75f326d8c15f0445ec27a6561171a169bb03245ff7625ba41";
    final String PUBLIC_APPLICANT = "02fb9e47838133e9d4d1ad3c5ba69b607d7b3377e0f2d8dd03b610d1f15b9ea3c8";

    /*
        @Test
        public void generateKeys() {
            Context context = new Secp256k1Context();
            PrivateKey privateKey = context.newRandomPrivateKey();
            System.out.println(privateKey.hex());
            System.out.println(context.getPublicKey(privateKey).hex());
        }
    */

    @Test
    public void test091() throws InterruptedException {
        String groupName = "testgroup";
        HyperZMQ member1 = new HyperZMQ("member1", "password", true);
        member1.setPrivateKey(PRIVATE_MEMBER_1);
        member1.createGroup(groupName);

        member1.addCallbackToGroup(groupName, ((group, message, senderID) -> System.out.println(message)));
        Thread.sleep(1000);
        member1.sendTextToChain(groupName, "dshfsdlfsfs");
        Thread.sleep(2000);
    }

    @Test
    public void test01() throws InterruptedException {
        // Prepare groups
        String groupName = "testgroup";
        HyperZMQ member1 = new HyperZMQ("member1", "password", true);
        member1.setPrivateKey(PRIVATE_MEMBER_1);
        member1.createGroup(groupName);

        Thread.sleep(1000); // Wait a moment for member1 to send the KeyExchangeReceipt for the group it created

        // Prepare Voting behavior - member1 should be responsible for the voting
        member1.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));
        member1.getVoteManager().setVotingProcessGroup(new GroupInternVotingProcess(member1, 50));
        member1.getVoteManager().setVoteEvaluator(new SimpleMajorityEvaluator(Collections.emptyList(), false, "member1"));

        // Start the join process
        HyperZMQ applicant = new HyperZMQ("applicant", "password", true);
        applicant.setPrivateKey(PRIVATE_APPLICANT);

        Thread.sleep(1000); // Wait a moment for member1 to send the KeyExchangeReceipt for the group it created
        applicant.tryJoinGroup(groupName, "localhost", 5555, null, this);

        Thread.sleep(15000);
        if (receivedKey == null) {
            fail("Received no key in time");
        }
        assertEquals("Received the correct key for the group", member1.getKeyForGroup(groupName), receivedKey);
    }

    @Test
    public void test() throws InterruptedException {
        // Prepare groups
        String groupName = "testgroup";
        HyperZMQ member1 = new HyperZMQ("member1", "password", true);
        member1.setPrivateKey(PRIVATE_MEMBER_1);
        member1.createGroup(groupName);

        HyperZMQ member2 = new HyperZMQ("member2", "password", true);
        member2.setPrivateKey(PRIVATE_MEMBER_2);
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
        member1.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));
        member2.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));
        member2.getVoteManager().setVotingProcessGroup(new GroupInternVotingProcess(member2, 50));
        member2.getVoteManager().setVoteEvaluator(new SimpleMajorityEvaluator(Collections.emptyList(), false, "member2"));

        // Start the join process
        HyperZMQ applicant = new HyperZMQ("applicant", "password", true);
        applicant.setPrivateKey(PRIVATE_APPLICANT);

        Thread.sleep(1000); // Wait a moment for member1 to send the KeyExchangeReceipt for the group it created
        applicant.tryJoinGroup(groupName, "localhost", 5555, null, this);

        Thread.sleep(15000);
        if (receivedKey == null) {
            fail("Received no key in time");
        }
        assertEquals("Received the correct key for the group", member1.getKeyForGroup(groupName), receivedKey);

    }

    @Test
    public void testsendvotingmatter() throws InterruptedException {
        String groupName = "testgroup2";

        HyperZMQ member1 = new HyperZMQ("member1", "password", true);
        member1.setPrivateKey(PRIVATE_MEMBER_1);
        member1.createGroup(groupName);

        HyperZMQ member2 = new HyperZMQ("member2", "password", true);
        member2.setPrivateKey(PRIVATE_MEMBER_2);
        member2.addGroup(groupName, member1.getKeyForGroup(groupName));

        JoinRequest joinRequest = new JoinRequest("AAAAAAAAAAAAA",
                member1.getSawtoothPublicKey(),
                JoinRequestType.GROUP,
                groupName,
                null,
                "localhost",
                5555);

        VotingMatter votingMatter = new VotingMatter(member1.getSawtoothPublicKey(),
                Arrays.asList(member1.getSawtoothPublicKey(), member2.getSawtoothPublicKey()),
                joinRequest);

        member1.sendVotingMatterInGroup(votingMatter);
        Thread.sleep(200);
        member1.sendVotingMatterInGroup(votingMatter);
        Thread.sleep(200);
        member1.sendVotingMatterInGroup(votingMatter);
        //member1.getVoteManager().addJoinRequest(joinRequest);

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
