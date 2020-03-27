package joinnetwork;

import client.KEManager;
import message.JNChallengeMessage;
import message.JNRequestMessage;
import message.JNResponseMessage;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import sawtooth.sdk.signing.PrivateKey;
import util.AsyncSubSocket;
import util.PubSocket;
import util.Utilities;

import java.security.SignatureException;

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
        print("Starting...");
        // Send a JoinNetworkRequestMessage to the "join network" topic initially

        //PubSocket pubSocket = new PubSocket(address);
        /*
        ZContext context2 = new ZContext();
        ZMQ.Socket pubSocket = context2.createSocket(ZMQ.PUB);
        pubSocket.bind(address);
        JNRequestMessage message1 = new JNRequestMessage(myID, myPublicKey);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pubSocket.send(KEManager.JOIN_SAWTOOTH_NETWORK_TOPIC + PubSocket.TOPIC_SUFFIX + message1.toString() );
        pubSocket.close();
        //pubSocket.send(message1.toString(), KEManager.JOIN_SAWTOOTH_NETWORK_TOPIC);

        //print("sent message:" + message1.toString());

        */
        // Listen to myID topic for a response
        String currentTopic = myID;
        ZContext context = new ZContext();
        /*ZMQ.Socket subsocket = context.createSocket(ZMQ.SUB);
        subsocket.setReceiveTimeOut(RECEIVE_TIMEOUT_MS);
        subsocket.connect(address);
        print("Subscribing " + currentTopic + PubSocket.TOPIC_SUFFIX);
        subsocket.subscribe((currentTopic + PubSocket.TOPIC_SUFFIX).getBytes()); */

        ZMQ.Socket socket = context.createSocket(ZMQ.PAIR);
        socket.connect(address);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        print("Listening for challenge...");
        //String s = subsocket.recvStr();
        String s = socket.recvStr();
        if (s == null || s.isEmpty()) {
            print("received nothing");
            return; // TODO
        }
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

        // Sign the nonce and create response
        JNResponseMessage response = new JNResponseMessage(challenge.getNonce(),
                Utilities.sign(challenge.getNonce(), myPrivateKey));
        //pubSocket.send(response.toString(), currentTopic);
        socket.send(response.toString());
        // Wait for ok
        //s = subsocket.recvStr();
        s = socket.recvStr();
        print(s);
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[JNApplicantThread" + Thread.currentThread().getId() + "][" + myID + "] " + message);
    }
}
