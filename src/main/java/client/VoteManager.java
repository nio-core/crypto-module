package client;

import blockchain.GlobalConfig;
import diffiehellman.DHKeyExchange;
import diffiehellman.EncryptedStream;
import groups.Envelope;
import groups.GroupMessage;
import groups.MessageType;
import joingroup.JoinRequest;
import joinnetwork.IPostJoinNetworkHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;
import sawtooth.sdk.protobuf.Transaction;
import subgrouping.ISubgroupSelector;
import voting.*;

import javax.annotation.Nullable;
import java.net.ConnectException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoteManager {

    private final HyperZMQ hyperZMQ;

    private final BlockingQueue<VotingMatter> votingMatters = new ArrayBlockingQueue<>(100);
    private final BlockingQueue<JoinRequest> joinRequests = new ArrayBlockingQueue<>(100);
    private final BlockingQueue<ImmutablePair<VotingResult, Boolean>> finishedVotes = new ArrayBlockingQueue<>(100);

    private final AtomicBoolean runVoteRequiredHandler = new AtomicBoolean(true);
    private final AtomicBoolean runVotingProcessHandler = new AtomicBoolean(true);
    private final AtomicBoolean runVotingCompleter = new AtomicBoolean(true);

    private final ExecutorService votingFinisherExecutor = Executors.newSingleThreadExecutor();

    // Variables for selecting a subgroup of participants when voting
    private ISubgroupSelector subgroupSelector = null;
    private int votingParticipantsThreshold = GlobalConfig.DEFAULT_VOTING_PARTICIPANTS_THRESHOLD;

    // The VotingProcess implements the behavior for when this client is the vote leader
    private IVotingProcess votingProcessGroup = null;
    private IVotingProcess votingProcessNetwork = null;
    private final ExecutorService votingProcessExecutor = Executors.newSingleThreadExecutor();

    // The VotingStrategies implement the behavior for when a vote if required from this client
    private IVotingStrategy votingStrategyNetwork = null;
    private IVotingStrategy votingStrategyGroup = null;
    private final ExecutorService voteExecutor = Executors.newSingleThreadExecutor();

    private IVoteEvaluator voteEvaluator = null;

    private IVoteSender voteSender = null;

    public void setVoteSender(IVoteSender voteSender) {
        this.voteSender = voteSender;
    }

    private IVoteStatusCallback statusCallback = null;
    public IPostJoinNetworkHandler postJoinNetworkHandler = null;

    public VoteManager(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
        startVoteRequiredHandler();
        startVotingProcessHandler();
        startVotingFinisher();
    }

    /**
     * This is loop that handles incoming votingMatters and casts a vote on those using the given
     * IVotingStrategy that is set at the time the matter is handled
     */
    private void handleVoteRequiredLoop() {
        //print("Starting VoteRequiredLoop...");
        while (runVoteRequiredHandler.get()) {
            VotingMatter matter = null;
            try {
                // Use a timeout here, because the thread could have been shut down meanwhile
                matter = votingMatters.poll(GlobalConfig.VOTE_REQUIRED_QUEUE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
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

                    if (statusCallback != null) {
                        statusCallback.newVoteCasted(matter, vote);
                    }
                    Envelope env = new Envelope(hyperZMQ.getClientID(), MessageType.VOTE, vote.toString());
                    // If the votingMatter was about joining network, send the vote back in the dedicated JoinNetworkVoters group
                    if (matter.getJoinRequest().getType().equals(JoinRequestType.NETWORK)) {
                        //hyperZMQ.sendAllChat(vote.toString());
                        if (hyperZMQ.isGroupAvailable(HyperZMQ.JOIN_NETWORK_VOTE_GROUP)) {
                            String payload = hyperZMQ.encryptEnvelope(HyperZMQ.JOIN_NETWORK_VOTE_GROUP, env);
                            GroupMessage message = new GroupMessage(HyperZMQ.JOIN_NETWORK_VOTE_GROUP, payload, false, true);
                            Transaction t = hyperZMQ.blockchainHelper.csvStringsTransaction(message.getBytes());
                            hyperZMQ.blockchainHelper.buildAndSendBatch(Collections.singletonList(t));
                        } else {
                            print("NetworkVoters group is not available on this client");
                        }
                    } else if (matter.getJoinRequest().getType().equals(JoinRequestType.GROUP)) {
                        this.voteSender.sendVote(this.hyperZMQ, vote, matter.getJoinRequest().getGroupName(), matter.getJoinRequest().votingArgs);
                    } else {
                        print("Voting matter is of type " + matter.getJoinRequest().getType() + ", no way of sending the vote back registered");
                    }
                } else {
                    // TODO possibly add a fallback strategy or vote no by default?
                    print("No strategy found to vote on the matter: " + matter.toString());
                }
            }
        }
        print("Stopping VoteRequiredLoop...");
    }

    /**
     * This loop moderates the voting process for incoming applications.
     * First the IVotingProcess is executed. Afterwards the IVotingEvaluator is executed
     */
    private void handleVotingProcessLoop() {
        //print("Starting VoteProcessLoop...");
        while (runVotingProcessHandler.get()) {
            JoinRequest joinRequest = null;
            try {
                // Use a timeout here, because the thread could have been shut down meanwhile
                joinRequest = this.joinRequests.poll(GlobalConfig.JOIN_REQUEST_QUEUE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (joinRequest != null) {
                print("Received JoinRequest to moderate process for: " + joinRequest.toString());

                if (statusCallback != null) {
                    statusCallback.newJoinRequestToModerate(joinRequest);
                }

                IVotingProcess process = null;
                switch (joinRequest.getType()) {
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
                    // Select the participants
                    print("Selecting participants for vote...");
                    String groupToGet = joinRequest.getType().equals(JoinRequestType.GROUP) ?
                            joinRequest.getGroupName() : HyperZMQ.JOIN_NETWORK_VOTE_GROUP;
                    List<String> groupMembers = hyperZMQ.getGroupMembersFromReceipts(groupToGet);

                    // Let self vote
                    if (groupMembers.size() > votingParticipantsThreshold) {
                        if (subgroupSelector == null) {
                            printErr("No SubGroupSelector available but is required. Voting will be aborted!!");
                            continue;
                        }
                        groupMembers = subgroupSelector.selectSubgroup(groupMembers, votingParticipantsThreshold);
                    }

                    VotingMatter votingMatter = new VotingMatter(hyperZMQ.getSawtoothPublicKey(), groupMembers, joinRequest);
                    // TODO enforce timeout here - if reached reject the request or take best shot from process?
                    VotingResult result = process.vote(votingMatter, GlobalConfig.VOTING_PROCESS_TIMEOUT_MS);
                    //print("VoteProcess finished with result: " + result.toString());
                    boolean isApproved = voteEvaluator.evaluateVotes(result);
                    //print("Votes were evaluated with result: " + isApproved);
                    ImmutablePair<VotingResult, Boolean> resultPair = new ImmutablePair<>(result, isApproved);
                    try {
                        finishedVotes.put(resultPair);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    printErr("No Voting process found for type " + joinRequest.getType());
                }
            }
        }
        print("Stopping VotingProcessLoop...");
    }

    private void handleFinishedVotes() {
        //print("Starting VoteFinisherLoop...");
        while (runVotingCompleter.get()) {
            ImmutablePair<VotingResult, Boolean> resultPair = null;
            try {
                resultPair = finishedVotes.poll(GlobalConfig.VOTING_FINISHER_QUEUE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (resultPair != null) {
                // Finish the voting by telling the applicant the result
                // if the result is yes and voting was about joining a group, send the group key to the applicant
                // using an encrypted channel that is created by doing diffie hellman

                if (statusCallback != null) {
                    statusCallback.votingProcessFinished(resultPair.left, resultPair.right);
                }

                // The applicant should be listening on the address specified in the JoinRequest
                VotingResult result = resultPair.left;
                JoinRequest joinRequest = result.getVotingMatter().getJoinRequest();
                DHKeyExchange exchange = new DHKeyExchange(hyperZMQ.getClientID(),
                        hyperZMQ.getSawtoothSigner(),
                        joinRequest.getApplicantPublicKey(),
                        joinRequest.getAddress(),
                        joinRequest.getPort(), false);
                print("Starting Diffie-Hellman to create session key...");
                try {
                    try (EncryptedStream encryptedStream = exchange.call()) {
                        if (resultPair.right) {
                            if (joinRequest.getType() == JoinRequestType.GROUP) {
                                print("Sending key of group: " + joinRequest.getGroupName());
                                encryptedStream.write(hyperZMQ.getKeyForGroup(joinRequest.getGroupName()));
                                hyperZMQ.sendKeyExchangeReceiptFor(joinRequest.getGroupName(), joinRequest.getApplicantPublicKey());
                            } else {
                                // SUCCESSFUL JOIN NETWORK VOTE
                                // TODO vvv the strings to indicate success / failure
                                encryptedStream.write("Allowed!" + hyperZMQ.getValidatorAddressToSend());
                                // Execute postjoinnetworkhandler
                                if (postJoinNetworkHandler != null) {
                                    postJoinNetworkHandler.newNetworkMember(joinRequest.getApplicantPublicKey());
                                } else {
                                    print("No IPostJoinNetworkHandler configured!");
                                }
                            }
                        } else {
                            // TODO vvv the strings to indicate success / failure
                            //print("Sending vote denied response.");
                            encryptedStream.write("Vote denied");
                        }
                    } catch (ConnectException e) {
                        // TODO applicant has not set up a server
                        printErr("Cannot notify the applicant because no server is listening for a connection on: "
                                + joinRequest.getAddress() + ":" + joinRequest.getPort());
                        //e.printStackTrace();
                    }
                } catch (Exception e) {
                    // TODO
                    printErr("Diffie-Hellman key exchange failed!");
                }
            }
        }
        print("Stopping VoteFinisherLoop...");
    }

    private void print(String message) {
        if (GlobalConfig.PRINT_VOTE_MANAGER) {
            System.out.println("[" + Thread.currentThread().getId() + "] [VoteManager][" + hyperZMQ.getClientID() + "]  " + message);
        }
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
        runVoteRequiredHandler.set(false);
    }

    public void startVoteRequiredHandler() {
        Thread t = new Thread(this::handleVoteRequiredLoop);
        voteExecutor.submit(t);
    }

    public void stopVotingProcessHandler() {
        runVotingProcessHandler.set(false);
    }

    public void startVotingProcessHandler() {
        Thread t = new Thread(this::handleVotingProcessLoop);
        votingProcessExecutor.submit(t);
    }

    public void startVotingFinisher() {
        Thread t = new Thread(this::handleFinishedVotes);
        votingFinisherExecutor.submit(t);
    }

    private void printErr(String message) {
        System.err.println("[" + Thread.currentThread().getId() + "] [VoteManager][" + hyperZMQ.getClientID() + "]  " + message);
    }

    public void stopVotingFinisher() {
        runVotingCompleter.set(false);
    }

    public void addVoteRequired(VotingMatter votingMatter) {
        votingMatters.add(votingMatter);
    }

    public void addJoinRequest(JoinRequest joinRequest) {
        joinRequests.add(joinRequest);
    }

    public IVoteStatusCallback getStatusCallback() {
        return statusCallback;
    }

    public void setStatusCallback(IVoteStatusCallback statusCallback) {
        this.statusCallback = statusCallback;
    }
}
