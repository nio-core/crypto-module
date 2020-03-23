package client;

import joinnetwork.JoinRequestListener;
import message.Message;
import message.MessageType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import sawtooth.sdk.signing.*;

import java.util.Collections;


public class KEManager {

    String clientID;
    JoinRequestListener joinRequestListener;
    PublicKey theirPubkey;
    HyperZMQStub hyHyperZMQStub;

    public final static String TOPIC_SUFFIX = "-!-";

    public KEManager(String clientID, HyperZMQStub hyperZMQStub) {
        this.clientID = clientID;
        this.hyHyperZMQStub = hyperZMQStub;
    }


    public synchronized void newJoinRequest(Message message) {

    }

    public void listenForRequests(String addr) {
        joinRequestListener = new JoinRequestListener(addr, this);
    }

    public void sendJoinRequest(String addr) {
        Message message = new Message(clientID, MessageType.JOIN_NETWORK_REQUEST,
                Collections.singletonMap("publickey", hyHyperZMQStub.getPublicKey().hex()));

        ZContext ctx = new ZContext();
        ZMQ.Socket pubSocket = ctx.createSocket(ZMQ.PAIR);
        pubSocket.bind(addr);
        pubSocket.send(JoinRequestListener.JOIN_SAWTOOTH_NETWORK_TOPIC + message.toString());
        System.out.println("Sent message:  " + message.toString());
    }
}
