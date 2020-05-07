package messages;

import com.google.gson.Gson;
import keyexchange.ISignableMessage;

public class JNResponseMessage implements ISignableMessage {

    private final String nonce;
    private String signature;

    public JNResponseMessage(String nonce) {
        this.nonce = nonce;
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
        return nonce;
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
