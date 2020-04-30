package voting;

public interface IGroupVoteReceiver {

    void voteReceived(Vote vote, String group);
}
