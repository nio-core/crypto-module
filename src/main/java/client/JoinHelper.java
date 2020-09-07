package client;

import joinnetwork.IJoinNetworkStatusCallback;
import joinnetwork.JNApplicantThread;
import sawtooth.sdk.signing.Context;
import sawtooth.sdk.signing.PrivateKey;
import sawtooth.sdk.signing.Secp256k1Context;
import sawtooth.sdk.signing.Signer;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JoinHelper {

    private Signer signer; // external usage - does not expose the private key
    private PrivateKey privateKey; // internal usage
    private final boolean doPrint = true;
    private final String clientID;
    private final ExecutorService jnApplicantExService = Executors.newSingleThreadExecutor();

    public JoinHelper(String clientID, PrivateKey privateKey) {
        this.clientID = clientID;
        this.privateKey = privateKey;
        this.signer = new Signer(new Secp256k1Context(), privateKey);
    }

    public JoinHelper(String clientID) {
        this.clientID = clientID;
        Context context = new Secp256k1Context();
        this.privateKey = context.newRandomPrivateKey();
        this.signer = new Signer(context, this.privateKey);
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        this.signer = new Signer(new Secp256k1Context(), this.privateKey);
    }

    public Signer getSigner() {
        return this.signer;
    }

    public String getPrivateKey() {
        return privateKey.hex();
    }

    /**
     * DOES !!!NOT!!! SET THE OBJECT TO THE JUST CREATED PRIVATE KEY
     *
     * @return new private key object
     */
    public static PrivateKey generateNewSigner() {
        return new Secp256k1Context().newRandomPrivateKey();
    }

    /**
     * @param joinNetworkAddress the address of the subsocket of a network member which is listening for join requests
     * @param serverAddress      the address on which this client should listen for a response (client-server model)
     * @param serverPort         the port on which this client should listen for a response (client-server model)
     * @param additionalInfo     additional info that can be processed while voting
     */
    public void tryJoinNetwork(String joinNetworkAddress, String serverAddress, int serverPort, Map<String,
            String> additionalInfo, @Nullable IJoinNetworkStatusCallback callback) {
        jnApplicantExService.submit(
                new JNApplicantThread(
                        clientID,
                        signer,
                        serverAddress,
                        serverPort,
                        additionalInfo,
                        joinNetworkAddress,
                        callback));
    }
}
