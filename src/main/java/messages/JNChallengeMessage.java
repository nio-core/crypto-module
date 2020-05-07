package messages;

import com.google.gson.Gson;

public class JNChallengeMessage implements txprocessor.ISignablePayload {

    private String memberPublicKey;
    private String nonce;
    private String signature;
    private String memberID;

    public JNChallengeMessage(String memberPublicKey, String nonce, String memberID) {
        this.memberPublicKey = memberPublicKey;
        this.nonce = nonce;
        this.memberID = memberID;
    }

    public String getMemberID() {
        return memberID;
    }

    public void setSignature(String signature) {
        this.signature = signature;
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
