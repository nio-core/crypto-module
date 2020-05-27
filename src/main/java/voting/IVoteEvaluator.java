package voting;

/**
 * Different implementations of this can count the votes however they want
 */
public interface IVoteEvaluator {
    boolean evaluateVotes(VotingResult votingResult);
}
