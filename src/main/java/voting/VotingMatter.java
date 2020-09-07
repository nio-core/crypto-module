package voting;

import blockchain.SawtoothUtils;
import com.google.gson.Gson;
import joingroup.JoinRequest;

import java.util.List;

public class VotingMatter {

    // Public key of the entity that handles the voting
    private final String voteDirectorPublicKey;

    // The hash is used to refer to this voting matter. When casting a voting, the participant signs its vote as well as the hash of the voting matter
    private final String hash;
    private final List<String> desiredVoters;

    private final JoinRequest joinRequest;

    public VotingMatter(String voteDirectorPublicKey, List<String> desiredVoters, JoinRequest joinRequest) {
        this.voteDirectorPublicKey = voteDirectorPublicKey;
        this.desiredVoters = desiredVoters;
        this.joinRequest = joinRequest;
        this.hash = SawtoothUtils.hash(this.voteDirectorPublicKey + joinRequest.toString() + this.getDesiredVoters().toString());
    }

    public String getHash() {
        return hash;
    }

    public String getVoteDirectorPublicKey() {
        return voteDirectorPublicKey;
    }

    public List<String> getDesiredVoters() {
        return desiredVoters;
    }

    public JoinRequest getJoinRequest() {
        return joinRequest;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
