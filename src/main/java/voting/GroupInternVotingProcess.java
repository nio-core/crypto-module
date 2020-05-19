package voting;

import client.HyperZMQ;
import groups.IGroupVoteReceiver;

import java.util.ArrayList;

public class GroupInternVotingProcess implements IVotingProcess, IGroupVoteReceiver {

    private final HyperZMQ hyperZMQ;
    private VotingResult result;
    private final int sleepMS;

    public GroupInternVotingProcess(HyperZMQ hyperZMQ, int sleepMS) {
        this.hyperZMQ = hyperZMQ;
        this.sleepMS = sleepMS;
    }

    @Override
    public VotingResult vote(VotingMatter votingMatter) {
        System.out.println("[" + Thread.currentThread().getId() + "] [GroupInternVotingProcess]  STARTING");
        // Register callback to evaluate the votes
        hyperZMQ.setGroupVoteReceiver(this);

        // Send the voting matter to the group
        hyperZMQ.sendVotingMatterInGroup(votingMatter);

        // Prepare the result
        result = new VotingResult(votingMatter, new ArrayList<>());

        boolean run = true;
        while (run) {
            try {
                Thread.sleep(sleepMS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (result) {
                if (result.getVotesSize() >= votingMatter.getDesiredVoters().size()) {
                    run = false;
                }
            }
        }
        return result;
    }

    @Override
    public void voteReceived(Vote vote, String group) {
        System.out.println("Received vote: " + toString());
        synchronized (result) {
            result.addVote(vote);
        }
    }
}
