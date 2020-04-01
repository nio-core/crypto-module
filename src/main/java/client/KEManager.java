package client;

import joinnetwork.IJNMember;
import joinnetwork.JNApplicantThread;
import joinnetwork.JNMemberThread;
import message.JNRequestMessage;
import sawtooth.sdk.signing.PublicKey;
import util.AsyncSubSocket;
import util.IAsyncSubSocketCallback;
import util.Utilities;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KEManager implements IJNMember, IAsyncSubSocketCallback {

    String clientID;
    PublicKey theirPubkey;
    HyperZMQStub hyHyperZMQStub;

    ExecutorService jnApplicantExService = Executors.newFixedThreadPool(2);
    ExecutorService jnMemberExService = Executors.newFixedThreadPool(2);
    ExecutorService jnListenerExService = Executors.newSingleThreadExecutor();
    String joinNetworkAddress;

    AsyncSubSocket jnSubSocket;
    private static final int JOIN_NETWORK_RECEIVE_TIMEOUT_MS = 5000;

    public static final String JOIN_SAWTOOTH_NETWORK_TOPIC = "JOIN_NETWORK";
    private boolean doPrint = true;

    public KEManager(String clientID, HyperZMQStub hyperZMQStub, String joinNetworkAddress) {
        this.clientID = clientID;
        this.hyHyperZMQStub = hyperZMQStub;
        this.joinNetworkAddress = joinNetworkAddress;
        this.jnSubSocket = new AsyncSubSocket(this, joinNetworkAddress, JOIN_SAWTOOTH_NETWORK_TOPIC,
                JOIN_NETWORK_RECEIVE_TIMEOUT_MS);
        jnListenerExService.submit(jnSubSocket);
    }

    public JNRequestMessage getRequest() {
        return new JNRequestMessage(clientID, hyHyperZMQStub.getPublicKey().hex());
    }

    public void sendJoinRequest(String addr) {
        // TODO use address
        JNApplicantThread t = new JNApplicantThread(clientID,
                hyHyperZMQStub.getPublicKey().hex(),
                hyHyperZMQStub.getPrivateKey().hex(),
                this,
                joinNetworkAddress);

        jnApplicantExService.submit(t);
    }

    @Override
    public void votingRequired(String applicantID, String applicantPublicKey) {

    }

    @Override
    public void error(Throwable t) {

    }

    @Override
    public void newMessage(String message, String topic) {
        print("New message " + message + " topic: " + topic);
        // This method can be used by different sockets - using different topics,
        // therefore topic is given too
        switch (topic) {
            case JOIN_SAWTOOTH_NETWORK_TOPIC: {
                JNRequestMessage m = Utilities.deserializeMessage(message, JNRequestMessage.class);
                if (m != null)
                    handleJoinNetwork(m);
                break;
            }
            default:
                break;
        }
    }

    public void handleJoinNetwork(JNRequestMessage message) {
        JNMemberThread t = new JNMemberThread(this.clientID,
                this.joinNetworkAddress,
                message.getApplicantID(),
                message.getApplicantPublicKey(),
                this.hyHyperZMQStub.getPrivateKey().hex(),
                this.hyHyperZMQStub.getPublicKey().hex(),
                this);

        jnMemberExService.submit(t);
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[KEManager][" + clientID + "] " + message);
    }
}
