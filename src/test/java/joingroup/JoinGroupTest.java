package joingroup;

import client.HyperZMQ;
import org.junit.Test;
import voting.GroupInternVotingProcess;
import voting.YesVoteStrategy;

public class JoinGroupTest implements IJoinGroupStatusCallback {

    @Test
    public void test() {
        // Prepare groups
        String groupName = "testgroup";
        HyperZMQ member1 = new HyperZMQ("member1", "password", true);
        member1.createGroup(groupName);
        HyperZMQ member2 = new HyperZMQ("member2", "password", true);
        member1.addGroup(groupName, member1.getKeyForGroup(groupName));

        // Prepare Voting behavior - member2 should be responsible for the voting
        member1.setVotingStrategyGroup(new YesVoteStrategy(300));
        member2.setVotingStrategyGroup(new YesVoteStrategy(300));
        GroupInternVotingProcess votingProcess = new GroupInternVotingProcess(member2, 50);
        member2.setVotingProcessGroup(votingProcess);

        // Start the join process
        HyperZMQ applicant = new HyperZMQ("applicant", "password", true);
        applicant.tryJoinGroup(groupName, this);

    }

    @Override
    public void joinGroupStatusCallback(String status) {
        System.out.println("[TEST] " + status);
    }
}
