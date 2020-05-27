package groups;

import voting.Vote;

public interface IGroupVoteReceiver {

    void voteReceived(Vote vote, String group);
}
