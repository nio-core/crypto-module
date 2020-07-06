package joinnetwork;

import blockchain.SawtoothUtils;
import client.JoinNetworkExtension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import joingroup.JoinRequest;
import messages.JNChallengeMessage;
import messages.JNResponseMessage;
import sawtooth.sdk.signing.Signer;
import util.Utilities;

/**
 * This is the thread for the Network member that authenticates an applicant using challenge response.
 * The identity that is verified is the Sawtooth public key.
 * If the authentication is successful, the request is passed to the VoteManager of the bound HyperZMQ instance
 * Counterpart is JNApplicantThread
 **/
public class JNMemberThread implements Runnable {

    private final String myID;
    private final JoinRequest joinRequest;
    private final Signer mySigner;
    private final String myPublicKeyHex;
    private static final int NONCE_SIZE_IN_CHARS = 32;
    private static final int SOCKET_TIMEOUT_S = 60;

    private final JoinNetworkExtension joiningManager;

    private final boolean doPrint = true;
    private static final int RECEIVE_TIMEOUT_MS = 10000;

    public JNMemberThread(String myID, JoinRequest joinRequest, Signer mySigner,
                          String myPublicKeyHex, JoinNetworkExtension joiningManager) {
        this.myID = myID;
        this.joinRequest = joinRequest;
        this.mySigner = mySigner;
        this.myPublicKeyHex = myPublicKeyHex;
        this.joiningManager = joiningManager;
    }

    @Override
    public void run() {
        print("Starting for request " + joinRequest.toString());

        // Connect to the server at the address+port
        try {
            Socket socket = new Socket(joinRequest.getAddress(), joinRequest.getPort());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send a challenge with a nonce to authenticate the applicant
            // Moreover, send our public key and a signature for the nonce so the applicant can verify us
            String nonce = Utilities.generateNonce(NONCE_SIZE_IN_CHARS);

            JNChallengeMessage message = new JNChallengeMessage(myPublicKeyHex, nonce, myID);
            message.setSignature(mySigner.sign(message.getSignablePayload().getBytes(StandardCharsets.UTF_8)));
            out.println(message.toString());
            print("Sent challenge: " + message.toString());

            // Wait for their response with the nonce and their signature for that nonce
            // so we can verify they own the private key for the public key they claim to be
            String recv = in.readLine();
            JNResponseMessage response;
            response = Utilities.deserializeMessage(recv, JNResponseMessage.class); // this prints the error already
            if (response == null) return; // TODO
            boolean valid = SawtoothUtils.verify(response.getSignablePayload(),
                    response.getSignature(),
                    joinRequest.getApplicantPublicKey());
            // TODO which messages to send here in the first step
            if (valid) {
                out.println("OK!!!");
                joiningManager.authenticationSuccess(joinRequest);
            } else {
                out.println("Invalid signature!");
                joiningManager.authenticationFailure(joinRequest);
            }
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "] [JoinNetworkMember][" + myID + "] " + message);
    }
}
