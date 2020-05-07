package voting;

import client.SawtoothUtils;
import com.google.gson.Gson;

import javax.annotation.Nullable;
import java.util.List;

public class VotingMatter {

    private final String applicantPublicKey;
    private final String voteDirectorPublicKey;
    // The hash is used to refer to this voting matter. When casting a voting, the participant signs its vote as well
    // as the hash of the voting matter
    private final String hash;
    private final VotingMatterType type;
    private final String group; // Group is empty String for VotingMatterType.JOIN_NETWORK
    private final List<String> desiredVoters;


    public VotingMatter(String applicantPublicKey, String voteDirectorPublicKey, VotingMatterType type, @Nullable String group, List<String> desiredVoters) {
        this.applicantPublicKey = applicantPublicKey;
        this.voteDirectorPublicKey = voteDirectorPublicKey;
        this.type = type;
        this.desiredVoters = desiredVoters;
        this.group = this.type == VotingMatterType.JOIN_GROUP ? (group != null ? group : "") : "";
        this.hash = SawtoothUtils.hash(this.applicantPublicKey + this.voteDirectorPublicKey
                + this.type + this.getDesiredVoters().toString());
    }


    public String getApplicantPublicKey() {
        return applicantPublicKey;
    }

    public String getHash() {
        return hash;
    }

    public VotingMatterType getType() {
        return type;
    }

    public String getVoteDirectorPublicKey() {
        return voteDirectorPublicKey;
    }

    public String getGroup() {
        return group;
    }

    public List<String> getDesiredVoters() {
        return desiredVoters;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
