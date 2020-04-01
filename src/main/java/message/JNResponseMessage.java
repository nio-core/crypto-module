package message;

import com.google.gson.Gson;
import txprocessor.ISignablePayload;

public class JNResponseMessage implements ISignablePayload {

    private String nonce;
    private String signature;

    public JNResponseMessage(String nonce, String signature) {
        this.nonce = nonce;
        this.signature = signature;
    }

    public String getNonce() {
        return nonce;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public String getSignablePayload() {
        return nonce;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
