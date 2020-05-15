package joingroup;

import com.google.gson.Gson;
import voting.JoinRequestType;

import java.util.Map;

public class JoinRequest {

    // Basic info required
    private final String applicantPublicKey;
    private final String contactPublicKey;
    // Group is empty String for JoinRequestType
    private final String groupName;
    private final JoinRequestType type;

    // The applicant can give some additional info that could be evaluated when members cast their vote
    private final Map<String, String> votingArgs;

    // The address+port on which the applicant will wait for a response (applicant will act as "server")
    private final String address;
    private final int port;

    public JoinRequest(String applicantPublicKey, String contactPublicKey, JoinRequestType type, String groupName,
                       Map<String, String> votingArgs, String address, int port) {
        this.applicantPublicKey = applicantPublicKey;
        this.contactPublicKey = contactPublicKey;
        this.type = type;
        this.groupName = this.type == JoinRequestType.GROUP ? (groupName != null ? groupName : "") : "";
        this.votingArgs = votingArgs;
        this.address = address;
        this.port = port;
    }

    public String getApplicantPublicKey() {
        return applicantPublicKey;
    }

    public String getContactPublicKey() {
        return contactPublicKey;
    }

    public String getGroupName() {
        return groupName;
    }

    public Map<String, String> getVotingArgs() {
        return votingArgs;
    }

    public String getAddress() {
        return address;
    }

    public JoinRequestType getType() {
        return type;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
