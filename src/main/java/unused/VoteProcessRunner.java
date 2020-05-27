package unused;

import client.HyperZMQ;
import joingroup.JoinRequest;
import sawtooth.sdk.signing.Signer;
import subgrouping.ISubgroupSelector;
import voting.IVoteEvaluator;
import voting.IVotingProcess;
import voting.VotingMatter;
import voting.VotingResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

public class VoteProcessRunner implements Callable<Boolean> {

    private final Signer signer;
    private final JoinRequest request;
    private final IVotingProcess votingProcess;
    private final int votingParticipantsThreshold;
    private final ISubgroupSelector subgroupSelector;
    private final HyperZMQ hyperZMQ;
    private final IVoteEvaluator voteEvaluator;

    private final boolean doPrint = true;

    /**
     * Executes the voting process incl. sub-grouping in a callable
     *
     * @param hyperZMQ                    callback to main
     * @param signer                      signer of this client
     * @param votingProcess               the IVotingProcess implementation that should be executed
     * @param subgroupSelector            if this is null, all group members are requested to vote
     * @param votingParticipantsThreshold maximum number of group members that should participate in the vote
     * @param voteEvaluator
     */
    public VoteProcessRunner(HyperZMQ hyperZMQ, Signer signer, JoinRequest request, IVotingProcess votingProcess,
                             @Nullable ISubgroupSelector subgroupSelector, int votingParticipantsThreshold, IVoteEvaluator voteEvaluator) {
        this.hyperZMQ = hyperZMQ;
        this.signer = signer;
        this.request = request;
        this.votingProcess = votingProcess;
        this.votingParticipantsThreshold = votingParticipantsThreshold;
        this.subgroupSelector = subgroupSelector;
        this.voteEvaluator = voteEvaluator;
    }

    @Override
    public Boolean call() throws Exception {
        Objects.requireNonNull(votingProcess, "No voting process registered to execute. Cancelling JoinGroupRequest");
        print("Starting vote...");
        List<String> groupMembers = hyperZMQ.getGroupMembers(request.getGroupName());
        // Let self vote 7

        if (groupMembers.size() > votingParticipantsThreshold) {
            if (subgroupSelector == null) {
                throw new IllegalStateException("No SubGroupSelector available but is required.");
            }
            groupMembers = subgroupSelector.selectSubgroup(groupMembers, votingParticipantsThreshold);
        }

        VotingMatter votingMatter = new VotingMatter(hyperZMQ.getSawtoothPublicKey(), groupMembers, request);

        print("Requiring votes from:" + groupMembers.toString());
        VotingResult result = votingProcess.vote(votingMatter);

        return voteEvaluator.evaluateVotes(result);

    }

    void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "][VoteProcessRunner][" + hyperZMQ.getClientID() + "]   " + message);
    }

}
