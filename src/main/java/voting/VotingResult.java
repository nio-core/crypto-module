package voting;

import com.google.gson.Gson;

import java.util.List;

/**
 * Since the votes refer to the hash of the votingMatter, integrity of this object is provable
 */
public class VotingResult {

    private final VotingMatter votingMatter;
    private final List<Vote> votes;

    public VotingResult(VotingMatter votingMatter, List<Vote> votes) {
        this.votingMatter = votingMatter;
        this.votes = votes;
    }

    // Make it possible to aggregate the Votes over time
    public void addVote(Vote vote) {
        System.out.println("checking vote: " + vote.toString());
        if (votes.stream().noneMatch(v ->
                v.getSignablePayload().equals(vote.getSignablePayload()) && v.getSignature().equals(vote.getSignature()))) {
            System.out.println("adding vote");
            votes.add(vote);
        }
    }

    public int getVotesSize() {
        return votes.size();
    }

    public VotingMatter getVotingMatter() {
        return votingMatter;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
