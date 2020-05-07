package diffiehellman;

import com.google.gson.Gson;
import keyexchange.ISignableMessage;

public class DHMessage implements ISignableMessage {

    private final String publicKey; // DH PublicKey in Base64
    private final String senderID;
    private String signature;

    public DHMessage(String publicKey, String senderID) {
        this.publicKey = publicKey;
        this.senderID = senderID;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getSenderID() {
        return senderID;
    }

    @Override
    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public String getSignablePayload() {
        return senderID + "|" + publicKey;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
