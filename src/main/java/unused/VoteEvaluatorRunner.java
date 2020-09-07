package unused;

import voting.IVoteEvaluator;
import voting.VotingResult;

import java.util.concurrent.Callable;

public class VoteEvaluatorRunner implements Callable<Boolean> {

    private final IVoteEvaluator voteEvaluator;
    private final VotingResult votingResult;

    public VoteEvaluatorRunner(IVoteEvaluator voteEvaluator, VotingResult votingResult) {
        this.voteEvaluator = voteEvaluator;
        this.votingResult = votingResult;
    }

    @Override
    public Boolean call() throws Exception {
        return voteEvaluator.evaluateVotes(votingResult);
    }
}
