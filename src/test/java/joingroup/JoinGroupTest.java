package joingroup;

import blockchain.SawtoothUtils;
import client.HyperZMQ;
import diffiehellman.DHKeyExchange;
import diffiehellman.EncryptedStream;
import java.io.IOException;
import java.sql.Time;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import keyexchange.KeyExchangeReceipt;
import keyexchange.ReceiptType;
import org.junit.Test;
import sawtooth.sdk.signing.Context;
import sawtooth.sdk.signing.PrivateKey;
import sawtooth.sdk.signing.Secp256k1Context;
import sawtooth.sdk.signing.Secp256k1PrivateKey;
import sawtooth.sdk.signing.Signer;
import subgrouping.RandomSubgroupSelector;
import voting.GroupInternVotingProcess;
import voting.JoinRequestType;
import voting.NoVoteStrategy;
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

    String groupKey;


    /*@Test
    public void generateKeys() {
        Context context = new Secp256k1Context();
        PrivateKey privateKey = context.newRandomPrivateKey();
        System.out.println(privateKey.hex());
        System.out.println(context.getPublicKey(privateKey).hex());
    }
*/
    @Test
    public void test() throws InterruptedException {
        // Prepare groups
        final String groupName = "testgroup";

        HyperZMQ member1 = new HyperZMQ("member1", "password", true);
        member1.setPrivateKey(PRIVATE_MEMBER_1);
        member1.createGroup(groupName);
        groupKey = member1.getKeyForGroup(groupName);

        KeyExchangeReceipt receipt = new KeyExchangeReceipt(member1.getSawtoothPublicKey(),
                PUBLIC_MEMBER_2, // shortcut
                ReceiptType.JOIN_GROUP,
                groupName,
                System.currentTimeMillis());
        receipt.setSignature(member1.sign(receipt.getSignablePayload()));
        member1.sendKeyExchangeReceipt(receipt);

        member1.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));

        Thread.sleep(1000);

        HyperZMQ member2 = new HyperZMQ("member2", "password", true);
        member2.setPrivateKey(PRIVATE_MEMBER_2);
        // This does not recreate a receipt, as it is not possible in production TODO
        member2.addGroup(groupName, member1.getKeyForGroup(groupName));

        // Prepare Voting behavior - member2 should be responsible for the voting
        member2.getVoteManager().setVotingStrategyGroup(new YesVoteStrategy(300));
        member2.getVoteManager().setVotingProcessGroup(new GroupInternVotingProcess(member2));
        member2.getVoteManager().setVoteEvaluator(new SimpleMajorityEvaluator(Collections.emptyList(), false, "member2"));

        Thread.sleep(1000); // Wait a moment for member1 to send the KeyExchangeReceipt for the group it created

        // Start the join process
        HyperZMQ applicant = new HyperZMQ("applicant", "password", true);
        applicant.setPrivateKey(PRIVATE_APPLICANT);

        Thread.sleep(1000); // Wait a moment for member1 to send the KeyExchangeReceipt for the group it created

        applicant.tryJoinGroup(groupName, "localhost", 5555, null, this, PUBLIC_MEMBER_2);

        Thread.sleep(5000);
        if (receivedKey == null) {
            fail("Received no key in time");
        }
        assertEquals("Received the correct key for the group", groupKey, receivedKey);
    }

    @Override
    public void joinGroupStatusCallback(int code, String info) {
        switch (code) {
            case FOUND_CONTACT:
                System.out.println("[TEST] contact public key: " + info);
                break;
            case REQUEST_SENT:
            case STARTING_DIFFIE_HELLMAN:
            case GETTING_KEY:
                break;
            case KEY_RECEIVED:
                System.out.println("[TEST] received key: " + info);
                receivedKey = info;
                break;
            case TIMEOUT:
            case VOTE_DENIED:
            case EMPTY_RESPONSE:
                break;
            default:
                break;
        }
    }
}
