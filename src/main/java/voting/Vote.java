package voting;

import com.google.gson.Gson;
import sawtooth.sdk.signing.Signer;
import txprocessor.ISignablePayload;

import java.nio.charset.StandardCharsets;

public class Vote implements ISignablePayload {

    private final String votingMatterHash;
    private final String publicKey;
    private final String signature;
    private final boolean approval;

    public Vote(boolean approval, String votingMatterHash, String publicKey, Signer signer) {
        this.votingMatterHash = votingMatterHash;
        this.publicKey = publicKey;
        this.approval = approval;
        this.signature = signer.sign(getSignablePayload().getBytes(StandardCharsets.UTF_8));
    }

    public String getVotingMatterHash() {
        return votingMatterHash;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isApproval() {
        return approval;
    }

    @Override
    public String getSignablePayload() {
        return votingMatterHash + "|" + publicKey + "|" + approval;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
