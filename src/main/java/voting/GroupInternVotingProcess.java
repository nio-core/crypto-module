package voting;

import client.GroupCallback;
import client.HyperZMQ;

import java.util.List;

public class GroupInternVotingProcess implements VotingProcess, GroupCallback {

    private final HyperZMQ hyperZMQ;

    public GroupInternVotingProcess(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
    }

    @Override
    public boolean vote(VotingMatter votingMatter, List<String> desiredVoters) {
        // Register group callback to evaluate the votes
        hyperZMQ.addCallbackToGroup(votingMatter.getGroup(), this);

        // Send the voting matter to the group
        hyperZMQ.sendVotingMatterInGroup(votingMatter);


        // Then
        return false;
    }

    @Override
    public void newMessageOnChain(String group, String message, String senderID) {

    }
}
