package voting;

import blockchain.IAllChatReceiver;
import blockchain.SawtoothUtils;
import client.HyperZMQ;
import util.Utilities;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AllChatVotingProcess implements IVotingProcess, IAllChatReceiver {

    private final HyperZMQ hyperZMQ;
    private VotingResult result;
    private AtomicBoolean otherFactor = new AtomicBoolean(true);

    private BlockingQueue<Vote> resultBuffer = new ArrayBlockingQueue<Vote>(100);

    public AllChatVotingProcess(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
    }

    @Override
    public VotingResult vote(VotingMatter votingMatter) {
        System.out.println("[" + Thread.currentThread().getId() + "] [AllChatVotingProcess]  STARTING");
        // Prepare to aggregate the result
        result = new VotingResult(votingMatter, new ArrayList<>());

        // We want to get the messages from all chat here
        hyperZMQ.addAllChatReceiver(this);

        // Send the voting matter to initiate the process
        hyperZMQ.sendAllChat(votingMatter.toString());

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
        hyperZMQ.remoteAllChatReceiver(this);
        return result;
    }

    public void stop() {
        otherFactor.set(false);
    }

    @Override
    public void allChatMessageReceived(String message, String sender) {
        // This is called from the EventReceiver thread
        // Put the votes from here in the queue of the main thread

        Vote vote = Utilities.deserializeMessage(message, Vote.class);
        if (vote != null) {
            if (!vote.getVotingMatterHash().equals(this.result.getVotingMatter().getHash())) {
                System.out.println("Received vote for different VotingMatter");
                return;
            }
            if (!result.getVotingMatter().getDesiredVoters().contains(sender)) {
                System.out.println("Vote was cast by unrequested entity");
                return;
            }
            // Verify integrity first
            boolean verified = SawtoothUtils.verify(vote.getSignablePayload(), vote.getSignature(), sender);
            if (verified) {
                resultBuffer.add(vote);
            }
        } else {
            // useless message
        }
    }
}
