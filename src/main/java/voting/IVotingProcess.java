package voting;

/**
 * This is the VotingProcess that is started when a voting is required.
 * Implementations can choose how they communicate with the participants
 */
public interface IVotingProcess {

    // TODO enforce the timeout by measuring the time in the votemanager - how to return a half processed result?
    VotingResult vote(VotingMatter votingMatter, int timeInMs);

}
