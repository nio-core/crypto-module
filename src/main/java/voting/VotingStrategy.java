package voting;

/**
 * This is the algorithm by which a client decides on a given vote
 */
public interface VotingStrategy {

    boolean castVote(VotingMatter votingMatter);

}
