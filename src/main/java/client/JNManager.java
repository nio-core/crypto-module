package client;

import joinnetwork.IJNMember;
import joinnetwork.JNApplicantThread;
import joinnetwork.JNMemberThread;
import messages.JNRequestMessage;
import util.AsyncSubSocket;
import util.IAsyncSubSocketCallback;
import util.Utilities;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JNManager implements IJNMember, IAsyncSubSocketCallback {

    private final String clientID;
    private final HyperZMQ hyperZMQ;

    private ExecutorService jnApplicantExService = Executors.newFixedThreadPool(2);
    private ExecutorService jnMemberExService = Executors.newFixedThreadPool(2);
    private ExecutorService jnListenerExService = Executors.newSingleThreadExecutor();
    private final String joinNetworkAddress;

    private AsyncSubSocket jnSubSocket;
    private static final int JOIN_NETWORK_RECEIVE_TIMEOUT_MS = 5000;

    public static final String JOIN_SAWTOOTH_NETWORK_TOPIC = "JOIN_NETWORK";
    private boolean doPrint = true;

    public JNManager(HyperZMQ hyperZMQ, String joinNetworkAddress) {
        this.clientID = hyperZMQ.getClientID();
        this.hyperZMQ = hyperZMQ;
        this.joinNetworkAddress = joinNetworkAddress;
        this.jnSubSocket = new AsyncSubSocket(this, joinNetworkAddress, JOIN_SAWTOOTH_NETWORK_TOPIC,
                JOIN_NETWORK_RECEIVE_TIMEOUT_MS);
        jnListenerExService.submit(jnSubSocket);
    }

    public JNRequestMessage getRequest() {
        return new JNRequestMessage(clientID, hyperZMQ.getSawtoothPublicKey());
    }

    public void sendJoinRequest(String addr) {
        // TODO use address
        JNApplicantThread t = new JNApplicantThread(clientID,
                hyperZMQ.getSawtoothPublicKey(),
                hyperZMQ.getSawtoothSigner(),
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
                hyperZMQ.getSawtoothSigner(),
                hyperZMQ.getSawtoothPublicKey(),
                this);

        jnMemberExService.submit(t);
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "] [JNManager][" + clientID + "] " + message);
    }
}
