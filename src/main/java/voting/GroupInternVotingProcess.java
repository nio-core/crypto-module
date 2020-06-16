package voting;

import client.HyperZMQ;
import groups.IGroupVoteReceiver;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GroupInternVotingProcess implements IVotingProcess, IGroupVoteReceiver {

    private final HyperZMQ hyperZMQ;

    private final AtomicBoolean otherFactor = new AtomicBoolean(true);

    private final BlockingQueue<Vote> resultBuffer = new ArrayBlockingQueue<Vote>(100);

    public GroupInternVotingProcess(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
    }

    @Override
    public VotingResult vote(VotingMatter votingMatter, int timeInMs) {
        System.out.println("[" + Thread.currentThread().getId() + "] [GroupInternVotingProcess]  STARTING -- desired Voters: " + votingMatter.getDesiredVoters().toString());
        // Register callback to evaluate the votes
        hyperZMQ.addGroupVoteReceiver(this);

        // Send the voting matter to the group
        hyperZMQ.sendVotingMatterInGroup(votingMatter);

        // Prepare the result
        VotingResult result = new VotingResult(votingMatter, new ArrayList<>());

        long endTime = System.currentTimeMillis() + timeInMs;

        // End conditions are the time, if all required votes were received or termination from setting the boolean
        while (result.getVotesSize() < votingMatter.getDesiredVoters().size() && otherFactor.get()
                && System.currentTimeMillis() < endTime) {
            try {
                Vote v = resultBuffer.poll(100, TimeUnit.MILLISECONDS);
                if (v != null) {
                    result.addVote(v);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        hyperZMQ.removeGroupVoteReceiver(this);
        return result;
    }

    public void stop() {
        otherFactor.set(false);
    }

    @Override
    public void voteReceived(Vote vote, String group) {
        //System.out.println("Received vote: " + vote.toString());
        try {
            resultBuffer.put(vote);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
