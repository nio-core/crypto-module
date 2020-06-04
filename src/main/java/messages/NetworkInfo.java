package messages;

public class NetworkInfo {

    private final String validatorURL;
    private final String encryptionKey; // optional

    public NetworkInfo(String validatorURL, String encryptionKey) {
        this.validatorURL = validatorURL;
        this.encryptionKey = encryptionKey;
    }

    public String getValidatorURL() {
        return validatorURL;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }
}
