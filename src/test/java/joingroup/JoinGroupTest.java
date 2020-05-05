package joingroup;

import client.HyperZMQ;
import org.junit.Test;
import voting.GroupInternVotingProcess;
import voting.YesVoteStrategy;

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

        Thread.sleep(1000); // Wait a moment for member1 to send the KeyExchangeReceipt for the group it created
        HyperZMQ member2 = new HyperZMQ("member2", "password", true);
        // This does not recreate a receipt, as it is not possible in production TODO
        member2.addGroup(groupName, member1.getKeyForGroup(groupName));

        // Prepare Voting behavior - member2 should be responsible for the voting
        member1.setVotingStrategyGroup(new YesVoteStrategy(300));
        member2.setVotingStrategyGroup(new YesVoteStrategy(300));
        GroupInternVotingProcess votingProcess = new GroupInternVotingProcess(member2, 50);
        member2.setVotingProcessGroup(votingProcess);


        // Start the join process
        HyperZMQ applicant = new HyperZMQ("applicant", "password", true);
        applicant.tryJoinGroup(groupName, this);

        Thread.sleep(3000);
        if (receivedKey == null)
            fail("Received no key in time");

        assertEquals("Received the correct key for the group", member1.getKeyForGroup(groupName), receivedKey);

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