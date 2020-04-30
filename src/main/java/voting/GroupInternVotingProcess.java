package voting;

import client.IGroupCallback;
import client.HyperZMQ;

import java.util.ArrayList;
import java.util.List;

public class GroupInternVotingProcess implements IVotingProcess, IGroupVoteReceiver {

    private final HyperZMQ hyperZMQ;

    public GroupInternVotingProcess(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
    }

    @Override
    public VotingResult vote(VotingMatter votingMatter, List<String> desiredVoters) {
        // Register callback to evaluate the votes
        hyperZMQ.setGroupVoteReceiver(this);

        // Send the voting matter to the group
        hyperZMQ.sendVotingMatterInGroup(votingMatter);

        // Prepare the result
        VotingResult result = new VotingResult(votingMatter, new ArrayList<>());

        while(true) {

            if()
        }

        // Then
        return null;
    }

    @Override
    public void voteReceived(Vote vote, String group) {

    }
}
