import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import sawtooth.sdk.signing.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class KEManager {

    String _clientID;
    JoinRequestListener _joinRequestListener;
    JoinRequestCallback _joinRequestCallback;
    PublicKey _theirPubkey;
    HyperZMQStub _hyHyperZMQStub;

    public KEManager(String clientID, HyperZMQStub hyperZMQStub) {
        this._clientID = clientID;
        this._hyHyperZMQStub = hyperZMQStub;
    }

    String sign(String message) {
        return new Secp256k1Context().sign(message.getBytes(), _hyHyperZMQStub.getPrivateKey());
    }

    boolean verify(String message, String signature, String publicKey) {
        PublicKey publicKey1 = new Secp256k1PublicKey(publicKey.getBytes());
        return new Secp256k1Context().verify(signature, message.getBytes(), publicKey1);
    }

    synchronized void newJoinRequest(String receivedMessage) {

    }


    void listenForRequests(String addr, JoinRequestCallback joinRequestCallback) {
        _joinRequestListener = new JoinRequestListener(addr, this);
        _joinRequestCallback = joinRequestCallback;
    }

    void sendJoinRequest(String addr) {
        Message message = new Message(_clientID, MessageType.JOIN_NETWORK_REQUEST,
                Collections.singletonMap("publickey", _hyHyperZMQStub.getPublicKey().hex()));

        ZContext ctx = new ZContext();
        ZMQ.Socket pubSocket = ctx.createSocket(ZMQ.PAIR);
        pubSocket.bind(addr);
        pubSocket.send(message.toString());
        System.out.println("Sent message:  " + message.toString());
    }
}
