package keyexchange;

import com.google.gson.Gson;
import javax.annotation.Nullable;

public class KeyExchangeReceipt implements ISignableMessage {

    private final String memberPublicKey;
    private final String applicantPublicKey;
    private final ReceiptType receiptType;
    private String group; // is null if ReceiptType is JOIN_NETWORK
    private final long timestamp;
    private String signature;

    public KeyExchangeReceipt(String memberPublicKey, String applicantPublicKey, ReceiptType receiptType,
                              @Nullable String group, long timestamp) {
        this.memberPublicKey = memberPublicKey;
        this.applicantPublicKey = applicantPublicKey;
        this.receiptType = receiptType;
        this.group = group;
        if (receiptType == ReceiptType.JOIN_NETWORK)
            this.group = null;
        this.timestamp = timestamp;
    }

    public String getMemberPublicKey() {
        return memberPublicKey;
    }

    public String getApplicantPublicKey() {
        return applicantPublicKey;
    }

    public ReceiptType getReceiptType() {
        return receiptType;
    }

    public String getGroup() {
        return group;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    @Override
    public String getSignablePayload() {
        String ret = "" + applicantPublicKey + "|" + receiptType + "|" + timestamp;
        if (receiptType == ReceiptType.JOIN_GROUP) {
            ret += "|" + group;
        }
        return ret;
    }
}
