package messages;

import com.google.gson.Gson;
import keyexchange.ISignableMessage;

public class JNChallengeMessage implements ISignableMessage {

    private final String memberPublicKey;
    private final String nonce;
    private String signature;
    private final String memberID;

    public JNChallengeMessage(String memberPublicKey, String nonce, String memberID) {
        this.memberPublicKey = memberPublicKey;
        this.nonce = nonce;
        this.memberID = memberID;
    }

    public String getMemberID() {
        return memberID;
    }

    public String getMemberPublicKey() {
        return memberPublicKey;
    }

    public String getNonce() {
        return nonce;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public String getSignablePayload() {
        return memberID + "|" + nonce;
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
