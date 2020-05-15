package client;

import blockchain.BlockchainHelper;
import groups.Envelope;
import org.apache.commons.lang3.tuple.ImmutablePair;
import sawtooth.sdk.protobuf.Transaction;
import subgrouping.ISubgroupSelector;
import voting.IVoteEvaluator;
import voting.IVotingProcess;
import voting.IVotingStrategy;
import voting.Vote;
import voting.VotingMatter;
import voting.VotingResult;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static groups.Envelope.MESSAGETYPE_VOTE;

public class VoteManager {

    private final HyperZMQ hyperZMQ;

    private final BlockingQueue<VotingMatter> votingMatters = new ArrayBlockingQueue<>(100);
    private final BlockingQueue<VotingMatter> joinRequest = new ArrayBlockingQueue<>(100);

    private final AtomicBoolean runVoteRequiredHandler = new AtomicBoolean(true);
    private final AtomicBoolean runVotingProcessHandler = new AtomicBoolean(true);

    // Variables for selecting a subgroup of participants when voting
    private ISubgroupSelector subgroupSelector = null;
    private int votingParticipantsThreshold = 5;

    // The VotingProcess implements the behavior for when this client is the vote leader
    private IVotingProcess votingProcessGroup = null;
    private IVotingProcess votingProcessNetwork = null;
    private final ExecutorService votingProcessExecutor = Executors.newSingleThreadExecutor();

    // The VotingStrategies implement the behavior for when a vote if required from this client
    private IVotingStrategy votingStrategyNetwork = null;
    private IVotingStrategy votingStrategyGroup = null;
    private final ExecutorService voteExecutor = Executors.newSingleThreadExecutor();

    private IVoteEvaluator voteEvaluator = null;

    public VoteManager(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
    }

    /**
     * This is loop that handles incoming votingMatters and casts a vote on those using the given
     * IVotingStrategy that is set at the time the matter is handled
     */
    private void handleVoteRequiredLoop() {
        print("Starting VoteRequiredLoop...");
        while (runVoteRequiredHandler.get()) {
            VotingMatter matter = null;
            try {
                // Use a timeout here, because the thread could have been shut down meanwhile
                matter = votingMatters.poll(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Only continue if there is a object to handle, otherwise check if loop should continue
            if (matter != null) {
                print("Received VotingMatter to vote on: " + matter.toString());
                IVotingStrategy strategy = null;
                switch (matter.getJoinRequest().getType()) {
                    case GROUP: {
                        strategy = votingStrategyGroup;
                        break;
                    }
                    case NETWORK: {
                        strategy = votingStrategyNetwork;
                        break;
                    }
                    default:
                        break;
                }
                if (strategy != null) {
                    Vote vote = strategy.castVote(matter, hyperZMQ.getSawtoothSigner());
                    print("Vote was cast: " + vote.toString());
                    // Send the vote back in the group TODO make this more generic?
                    Envelope env = new Envelope(hyperZMQ.getClientID(), MESSAGETYPE_VOTE, vote.toString());
                    Transaction transaction;
                    transaction = hyperZMQ.blockchainHelper.buildTransaction(BlockchainHelper.CSVSTRINGS_FAMILY,
                            "0.1",
                            hyperZMQ.encryptEnvelope(matter.getJoinRequest().getGroupName(), env),
                            null);

                    hyperZMQ.blockchainHelper.buildAndSendBatch(Collections.singletonList(transaction));
                } else {
                    // TODO possibly add a fallback strategy or vote no by default?
                    print("No strategy found to vote on the matter: " + matter.toString());
                }
            }
        }
    }

    /**
     * This loop moderates the voting process for incoming applications.
     * First the IVotingProcess is executed. Afterwards the IVotingEvaluator is executed
     */
    private void handleVotingProcessLoop() {
        print("Starting VoteProcessLoop...");
        while (runVotingProcessHandler.get()) {
            VotingMatter matter = null;
            try {
                // Use a timeout here, because the thread could have been shut down meanwhile
                matter = joinRequest.poll(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (matter != null) {
                print("Received VotingMatter to moderate process for: " + matter.toString());
                IVotingProcess process = null;
                switch (matter.getJoinRequest().getType()) {
                    case GROUP: {
                        process = votingProcessGroup;
                        break;
                    }
                    case NETWORK: {
                        process = votingProcessNetwork;
                        break;
                    }
                    default:
                        break;
                }
                if (process != null) {
                    // Select the participants // TODO do this when votingMatter is created
                    print("Starting VoteProcess...");
                    VotingResult result = process.vote(matter);
                    print("VoteProcess finished with result: " + result.toString());
                    boolean isApproved = voteEvaluator.evaluateVotes(result); // TODO how to pass this and where???
                    print("Votes were evaluated with result: " + isApproved);
                    ImmutablePair<VotingResult, Boolean> resultPair = new ImmutablePair<>(result, isApproved);

                }
            }
        }

    }

    private void print(String message) {
        System.out.println("[" + Thread.currentThread().getId() + "] [VoteManager][" + hyperZMQ.getClientID() + "]  " + message);
    }

    /*
     * vvv PUBLIC API OF THIS OBJECT vvv
     */

    public IVotingProcess getVotingProcessGroup() {
        return votingProcessGroup;
    }

    public IVotingProcess getVotingProcessNetwork() {
        return votingProcessNetwork;
    }

    public IVotingStrategy getVotingStrategyNetwork() {
        return votingStrategyNetwork;
    }

    public IVotingStrategy getVotingStrategyGroup() {
        return votingStrategyGroup;
    }

    public void setVotingStrategyGroup(IVotingStrategy votingStrategyGroup) {
        this.votingStrategyGroup = votingStrategyGroup;
    }

    public void setVotingStrategyNetwork(IVotingStrategy votingStrategyNetwork) {
        this.votingStrategyNetwork = votingStrategyNetwork;
    }

    public int getVotingParticipantsThreshold() {
        return votingParticipantsThreshold;
    }

    public void setSubgroupSelector(ISubgroupSelector selector, int votingParticipantsThreshold) {
        this.subgroupSelector = selector;
        this.votingParticipantsThreshold = votingParticipantsThreshold;
    }

    public void setVotingProcessGroup(@Nullable IVotingProcess votingProcessGroup) {
        this.votingProcessGroup = votingProcessGroup;
    }

    public void setVotingProcessNetwork(@Nullable IVotingProcess votingProcessNetwork) {
        this.votingProcessNetwork = votingProcessNetwork;
    }

    public void setVoteEvaluator(IVoteEvaluator voteEvaluator) {
        this.voteEvaluator = voteEvaluator;
    }

    public void setSubgroupSelector(ISubgroupSelector subgroupSelector) {
        this.subgroupSelector = subgroupSelector;
    }

    public void setVotingParticipantsThreshold(int votingParticipantsThreshold) {
        this.votingParticipantsThreshold = votingParticipantsThreshold;
    }

    public void stopVoteRequiredHandler() {
        print("Stopping VoteRequiredHandler...");
        runVoteRequiredHandler.set(false);
    }

    public void startVoteRequiredHandler() {
        Thread t = new Thread(this::handleVoteRequiredLoop);
        voteExecutor.submit(t);
    }

    public void stopVotingProcessHandler() {
        print("Stopping VotingProcessHandler...");
        runVotingProcessHandler.set(false);
    }

    public void startVotingProcessHandler() {
        Thread t = new Thread(this::handleVotingProcessLoop);
        votingProcessExecutor.submit(t);
    }

    public void addVoteRequired(VotingMatter votingMatter) {
        votingMatters.add(votingMatter);
    }

    public void addJoinRequest(VotingMatter votingMatter) {
        joinRequest.add(votingMatter);
    }
}
