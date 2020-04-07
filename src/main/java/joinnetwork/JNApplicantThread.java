package joinnetwork;

import client.KEManager;
import message.JNChallengeMessage;
import message.JNRequestMessage;
import message.JNResponseMessage;
import util.PubSocket;
import util.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The Thread for an applicant that wants to join the network.
 * Sends id and public key initially to the JoinNetwork Topic.
 * Afterwards listens to own id topic for a challenge from a network member.
 * If a challenge is received, sign it with the public key from the first step to prove identity
 * Counterpart of JNMemberThread
 */
public class JNApplicantThread implements Runnable {

    private static final int RECEIVE_TIMEOUT_MS = 10000;

    String address; // TODO sub/pub different addresses?
    String myID;
    String myPublicKey;
    String myPrivateKey;

    KEManager client;

    private final boolean doPrint = true;

    public JNApplicantThread(String myID, String myPublicKey, String myPrivateKey, KEManager client,
                             String address) {
        this.myID = myID;
        this.myPublicKey = myPublicKey;
        this.myPrivateKey = myPrivateKey;
        this.client = client;
        this.address = address;
    }

    @Override
    public void run() {
        // Send a JoinNetworkRequestMessage to the "join network" topic initially

        PubSocket pubSocket = new PubSocket(address);
        JNRequestMessage message1 = new JNRequestMessage(myID, myPublicKey);
        pubSocket.send(message1.toString(), KEManager.JOIN_SAWTOOTH_NETWORK_TOPIC);
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
                    Utilities.sign(challenge.getNonce(), myPrivateKey));
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
