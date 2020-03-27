package message;

import com.google.gson.Gson;

public class JNChallengeMessage implements ISignablePayload {

    private String memberPublicKey;
    private String nonce;
    private String signature;
    private String memberID;

    public JNChallengeMessage(String memberPublicKey, String nonce, String signature, String memberID) {
        this.memberPublicKey = memberPublicKey;
        this.nonce = nonce;
        this.signature = signature;
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

    public String getSignature() {
        return signature;
    }

    @Override
    public String getSignablePayload() {
        return memberID + "|" + nonce;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
