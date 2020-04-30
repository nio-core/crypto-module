package voting;

import sawtooth.sdk.signing.Signer;

/**
 * This is the algorithm by which a client decides on a given vote
 */
public interface IVotingStrategy {

    /**
     * @param votingMatter
     * @param signer       signer is required to sign the vote,
     *                     should be the signer for the sawtooth identity that was also
     *                     specified in the votingMatters desiredVoters
     * @return vote
     */
    Vote castVote(VotingMatter votingMatter, Signer signer);

}
