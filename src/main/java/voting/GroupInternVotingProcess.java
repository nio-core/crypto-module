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
    private VotingResult result;
    private final int sleepMS;

    private AtomicBoolean otherFactor = new AtomicBoolean(true);

    private BlockingQueue<Vote> resultBuffer = new ArrayBlockingQueue<Vote>(100);

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

        while (result.getVotesSize() < votingMatter.getDesiredVoters().size() && otherFactor.get()) {
            try {
                Vote v = resultBuffer.poll(500, TimeUnit.MILLISECONDS);
                if (v != null) {
                    result.addVote(v);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
