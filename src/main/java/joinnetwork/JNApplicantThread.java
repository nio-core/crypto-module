package joinnetwork;

import client.JNManager;
import messages.JNChallengeMessage;
import messages.JNRequestMessage;
import messages.JNResponseMessage;
import sawtooth.sdk.signing.Signer;
import util.PubSocket;
import util.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * The Thread for an applicant that wants to join the network.
 * Sends id and public key initially to the JoinNetwork Topic.
 * Afterwards listens to own id topic for a challenge from a network member.
 * If a challenge is received, sign it with the public key from the first step to prove identity
 * Counterpart of JNMemberThread
 */
public class JNApplicantThread implements Runnable {

    private static final int RECEIVE_TIMEOUT_MS = 10000;

    private String address; // TODO sub/pub different addresses?
    private String myID;
    private String myPublicKey;
    private Signer mySigner;

    private JNManager client;

    private final boolean doPrint = true;

    public JNApplicantThread(String myID, String myPublicKey, Signer mySigner, JNManager client,
                             String address) {
        this.myID = myID;
        this.myPublicKey = myPublicKey;
        this.mySigner = mySigner;
        this.client = client;
        this.address = address;
    }

    @Override
    public void run() {
        // Send a JoinNetworkRequestMessage to the "join network" topic initially

        PubSocket pubSocket = new PubSocket(address);
        JNRequestMessage message1 = new JNRequestMessage(myID, myPublicKey);
        pubSocket.send(message1.toString(), JNManager.JOIN_SAWTOOTH_NETWORK_TOPIC);
        print("Sent join request");
        //print("sent message:" + message1.toString());

        // Im the server
        try {
            print("Starting server");
            ServerSocket serverSocket = new ServerSocket(5555);
            Socket clientSocket = serverSocket.accept();

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            print("Listening for challenge...");

            String s = in.readLine();
            print("Received: " + s);
            JNChallengeMessage challenge = Utilities.deserializeMessage(s, JNChallengeMessage.class);
            if (challenge == null) return; // TODO

            // Verify the network member
            boolean verified = Utilities.verify(challenge.getSignablePayload(),
                    challenge.getSignature(),
                    challenge.getMemberPublicKey());

            if (!verified) {
                print("Signature verification failed!");
                return; // TODO
            }
            print("Signature verified");

            // Sign the nonce and create response
            JNResponseMessage response = new JNResponseMessage(challenge.getNonce(),
                    mySigner.sign(challenge.getNonce().getBytes(StandardCharsets.UTF_8)));
            out.println(response.toString());
            print("Sent response: " + response);
            // Wait for ok
            s = in.readLine();
            print(s);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[JNApplicantThread" + Thread.currentThread().getId() + "][" + myID + "] " + message);
    }
}
