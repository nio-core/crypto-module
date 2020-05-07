package voting;

import com.google.gson.Gson;
import keyexchange.ISignableMessage;
import sawtooth.sdk.signing.Signer;

import java.nio.charset.StandardCharsets;

public class Vote implements ISignableMessage {

    private final String votingMatterHash;
    private final String publicKey;
    private String signature;
    private final boolean approval;

    public Vote(boolean approval, String votingMatterHash, String publicKey) {
        this.votingMatterHash = votingMatterHash;
        this.publicKey = publicKey;
        this.approval = approval;
    }

    public String getVotingMatterHash() {
        return votingMatterHash;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public boolean isApproval() {
        return approval;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public String getSignablePayload() {
        return votingMatterHash + "|" + publicKey + "|" + approval;
    }

    @Override
    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
