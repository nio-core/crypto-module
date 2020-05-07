package client;

import sawtooth.sdk.protobuf.Transaction;
import subgrouping.ISubgroupSelector;
import voting.IVotingProcess;
import voting.IVotingStrategy;
import voting.Vote;
import voting.VotingMatter;

import javax.annotation.Nullable;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static client.Envelope.MESSAGETYPE_VOTE;

public class VoteManager {

    // Variables for selecting a subgroup of participants when voting
    private ISubgroupSelector subgroupSelector = null;
    private int votingParticipantsThreshold = 5;

    // The VotingProcess implements the behavior for when this client is the vote leader
    private IVotingProcess votingProcessGroup = null;
    private IVotingProcess votingProcessNetwork = null;
    private ExecutorService processExecutor = Executors.newSingleThreadExecutor();

    // The VotingStrategies implement the behavior for when a vote if required from this client
    private IVotingStrategy votingStrategyNetwork = null;
    private IVotingStrategy votingStrategyGroup = null;
    private ExecutorService strategyExecutor = Executors.newSingleThreadExecutor();

    private final HyperZMQ hyperZMQ;

    public VoteManager(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
    }

    void handleVotingMatter(VotingMatter votingMatter) {
        if (votingMatter.getDesiredVoters().contains(hyperZMQ.getSawtoothPublicKey())) {
            IVotingStrategy strategy = null;
            // TODO switch new voting strategies here
            switch (votingMatter.getType()) {
                case JOIN_GROUP: {
                    strategy = votingStrategyGroup;
                    break;
                }
                case JOIN_NETWORK: {
                    strategy = votingStrategyNetwork;
                    break;
                }
                default:
                    break;
            }

            if (strategy == null) {
                // TODO possibly add a fallback strategy or vote no by default?
                print("No strategy found to vote on the matter: " + votingMatter.toString());
                return;
            }



            // Vote vote =
/*
            // Send the vote back in the group
            Envelope env = new Envelope(this.clientID, MESSAGETYPE_VOTE, vote.toString());

            Transaction t;
            try {
                t = blockchainHelper.buildTransaction(BlockchainHelper.CSVSTRINGS_FAMILY,
                        "0.1",
                        Base64.getDecoder().decode(crypto.encrypt(vote.toString(), votingMatter.getGroup())),
                        null);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                return;
            }

            blockchainHelper.buildAndSendBatch(Collections.singletonList(t));
        }
        */
        }
    }

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


    protected void print(String message) {
        System.out.println("[" + Thread.currentThread().getId() + "] [VoteManager][" + hyperZMQ.getClientID() + "]  " + message);
    }

}
