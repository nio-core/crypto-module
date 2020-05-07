package messages;

import com.google.gson.Gson;

public class JNRequestMessage {

    private String applicantID;
    private String applicantPublicKey;

    public JNRequestMessage(String applicantID, String applicantPublicKey) {
        this.applicantID = applicantID;
        this.applicantPublicKey = applicantPublicKey;
    }

    public String getApplicantID() {
        return applicantID;
    }

    public String getApplicantPublicKey() {
        return applicantPublicKey;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
