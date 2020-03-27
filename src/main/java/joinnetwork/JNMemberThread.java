package joinnetwork;

import client.KEManager;
import message.JNChallengeMessage;
import message.JNResponseMessage;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import sawtooth.sdk.signing.PrivateKey;
import util.PubSocket;
import util.Utilities;

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
    private String myPrivateKey;
    private String myPublicKeyHex;
    private static final int NONCE_SIZE_IN_CHARS = 32;
    private static final int SOCKET_TIMEOUT_S = 60;

    private Future<Boolean> votingResult;

    private String topic_prefix;

    private final boolean doPrint = true;
    private static final int RECEIVE_TIMEOUT_MS = 10000;

    public JNMemberThread(String myID, String address, String applID, String applPublicKey, String myPrivateKey,
                          String myPublicKeyHex, IJNMember callback) {
        this.myID = myID;
        this.address = address;
        this.applID = applID;
        this.applPublicKey = applPublicKey;
        this.myPrivateKey = myPrivateKey;
        this.myPublicKeyHex = myPublicKeyHex;
        this.callback = callback;
        this.topic_prefix = applID + KEManager.JOIN_SAWTOOTH_NETWORK_TOPIC;
    }

    @Override
    public void run() {
        print("Starting for applicant " + applID);
        String currentTopic = applID;
        // ZContext context = new ZContext();

        //PubSocket pubSocket = new PubSocket(address);
        //print("Sockets created");

        ZContext context = new ZContext();

        ZMQ.Socket socket = context.createSocket(ZMQ.PAIR);
        socket.bind("tcp://*:5555");

        // Send a challenge with a nonce to authenticate the applicant
        // Moreover, send our public key and a signature for the nonce so the applicant can verify us
        String nonce = Utilities.generateNonce(NONCE_SIZE_IN_CHARS);
        String signature = Utilities.sign(nonce, myPrivateKey);

        JNChallengeMessage message = new JNChallengeMessage(myPublicKeyHex, nonce, signature, myID);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            socket.send(message.toString());
            print("Sent challenge " + message.toString());
        }
        //socket.send(message.toString());
        //pubSocket.send(message.toString(), currentTopic);


        // Wait for their response with the nonce and their signature for that nonce
        // so we can verify they own the private key for the public key they claim to be

     /*   ZMQ.Socket subSocket = context.createSocket(ZMQ.SUB);
        subSocket.setReceiveTimeOut(RECEIVE_TIMEOUT_MS);
        subSocket.connect(address); // TODO address already in use
        subSocket.subscribe((currentTopic + PubSocket.TOPIC_SUFFIX).getBytes());

        String recv = subSocket.recvStr(); */
        JNResponseMessage response;
        while (true) {
            String recv = socket.recvStr();

            response = Utilities.deserializeMessage(recv, JNResponseMessage.class);
            //if (response == null) return; // TODO callback.error() ?
            if (response != null) break;
        }

        boolean valid = Utilities.verify(response.getSignablePayload(), response.getSignature(), applPublicKey);

        if (valid) {
            socket.send("OK!!!");
            //pubSocket.send("OK!!!", currentTopic);

            callback.votingRequired(applID, applPublicKey);
        } else {
            callback.error(new SignatureException("Provided public key did not match signature!"));
            return;
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
