package joinnetwork;

import client.KEManager;
import message.Message;
import message.MessageType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import sawtooth.sdk.signing.PrivateKey;
import util.Utilities;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the thread for the Network member that authenticates an applicant and notifies it about the result of the voting
 * The communication uses PUB/SUB with the topic of the applicant id + topic suffix
 */
public class JoinNetworkThread implements Runnable {

    private String myID;
    private String applID;
    private String applPublicKey;
    private JoinNetworkCallback joinNetworkCallback;
    private String address;
    private PrivateKey myPrivateKey;
    private String myPublicKeyHex;
    private static final int NONCE_SIZE_IN_CHARS = 32;
    private static final int SOCKET_TIMEOUT_S = 60;

    private String topic_prefix;

    public JoinNetworkThread(String myID, String address, String applID, String applPublicKey, PrivateKey myPrivateKey,
                             String myPublicKeyHex, JoinNetworkCallback joinNetworkCallback) {
        this.myID = myID;
        this.address = address;
        this.applID = applID;
        this.applPublicKey = applPublicKey;
        this.myPrivateKey = myPrivateKey;
        this.myPublicKeyHex = myPublicKeyHex;
        this.joinNetworkCallback = joinNetworkCallback;
        this.topic_prefix = applID + JoinRequestListener.JOIN_SAWTOOTH_NETWORK_TOPIC;
    }

    @Override
    public void run() {
        ZContext context = new ZContext();
        ZMQ.Socket subSocket = context.createSocket(ZMQ.SUB);
        subSocket.connect(address);
        subSocket.subscribe((applID + KEManager.TOPIC_SUFFIX).getBytes());

        ZMQ.Socket pubSocket = context.createSocket(ZMQ.PUB);
        pubSocket.bind(address);

        // Send a response with a nonce to authenticate the applicant
        // Moreover, send our public key and a signature for the nonce so the applicant can verify us
        String nonce = Utilities.generateNonce(NONCE_SIZE_IN_CHARS);
        String signature = Utilities.sign(nonce, myPrivateKey);
        Map<String, String> data = new HashMap<>();
        data.put("publickey", myPublicKeyHex);
        data.put("nonce", nonce);
        data.put("signature", signature);
        Message message = new Message(myID, MessageType.JOIN_NETWORK_CHALLENGE, data);

        pubSocket.send((topic_prefix + message.toString()).getBytes());

        String recv = subSocket.recvStr();
        // Wait for their response with the nonce and their signature for that nonce
        // so we can verify they own the private key for the public key they claim to be

        Message response = Utilities.deserializeMessage(recv);
        if (response == null) return; // TODO callback.error() ?
        if (response.messageType != MessageType.JOIN_NETWORK_RESPONSE) return; // TODO callback.error() ?



    }


}
