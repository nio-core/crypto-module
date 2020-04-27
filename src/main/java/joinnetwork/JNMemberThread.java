package joinnetwork;

import client.JNManager;
import message.JNChallengeMessage;
import message.JNResponseMessage;
import sawtooth.sdk.signing.Signer;
import util.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This is the thread for the Network member that authenticates an applicant and notifies it about the result of the voting
 * The communication uses PUB/SUB with the topic of the applicant id + topic suffix
 * Counterpart is JNApplicantThread
 **/
public class JNMemberThread implements Runnable, IJNMemberThread {

    private String myID;
    private String applID;
    private String applPublicKey;
    private IJNMember callback;
    private String address;
    private Signer mySigner;
    private String myPublicKeyHex;
    private static final int NONCE_SIZE_IN_CHARS = 32;
    private static final int SOCKET_TIMEOUT_S = 60;

    private Future<Boolean> votingResult;

    private String topic_prefix;

    private final boolean doPrint = true;
    private static final int RECEIVE_TIMEOUT_MS = 10000;

    public JNMemberThread(String myID, String address, String applID, String applPublicKey, Signer mySigner,
                          String myPublicKeyHex, IJNMember callback) {
        this.myID = myID;
        this.address = address;
        this.applID = applID;
        this.applPublicKey = applPublicKey;
        this.mySigner = mySigner;
        this.myPublicKeyHex = myPublicKeyHex;
        this.callback = callback;
        this.topic_prefix = applID + JNManager.JOIN_SAWTOOTH_NETWORK_TOPIC;
    }

    @Override
    public void run() {
        print("Starting for applicant " + applID);

        // Connect to the server at the address+port
        try {
            Socket socket = new Socket("localhost", 5555);
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
            response = Utilities.deserializeMessage(recv, JNResponseMessage.class);
            if (response == null) return; // TODO
            boolean valid = Utilities.verify(response.getSignablePayload(), response.getSignature(), applPublicKey);

            if (valid) {
                out.println("OK!!!");
                //pubSocket.send("OK!!!", currentTopic);

                callback.votingRequired(applID, applPublicKey);
            } else {
                callback.error(new SignatureException("Provided public key did not match signature!"));
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Wait for voting
        try {
            votingResult.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[JNMemberThread" + Thread.currentThread().getId() + "][" + myID + "] " + message);
    }

    @Override
    public void votingFinished(boolean result) {
    }
}
