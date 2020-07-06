package voting;

import joingroup.JoinRequest;

/**
 * Implement and set this in the VoteManager to be notified about ongoing Votes
 */
public interface IVoteStatusCallback {

    void newVoteCasted(VotingMatter votingMatter, Vote vote);

    void newJoinRequestToModerate(JoinRequest joinRequest);

    void votingProcessFinished(VotingResult votingResult, boolean approved);
}
