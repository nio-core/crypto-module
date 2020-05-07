package voting;

import sawtooth.sdk.signing.Signer;

import java.util.concurrent.Callable;

public class VoteStrategyRunner implements Callable<Vote> {

    private final IVotingStrategy strategy;
    private final Signer signer;
    private final VotingMatter votingMatter;

    public VoteStrategyRunner(IVotingStrategy strategy, Signer signer, VotingMatter votingMatter) {
        this.strategy = strategy;
        this.signer = signer;
        this.votingMatter = votingMatter;
    }

    @Override
    public Vote call() throws Exception {
        return strategy.castVote(votingMatter, signer);
    }
}
