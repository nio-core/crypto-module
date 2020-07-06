package client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import joingroup.JoinRequest;
import joinnetwork.JNMemberThread;
import util.AsyncSubSocket;
import util.IAsyncSubSocketCallback;
import util.Utilities;
import voting.JoinRequestType;

/**
 * This class can be added OPTIONALLY to a HyperZMQ instance to make it listen for clients that want to join the network.
 */
public class JoinNetworkExtension implements IAsyncSubSocketCallback {

    private final String clientID;
    private final HyperZMQ hyperZMQ;

    private final ExecutorService jnMemberExService = Executors.newFixedThreadPool(2);
    private final ExecutorService jnListenerExService = Executors.newSingleThreadExecutor();
    private final String joinNetworkAddress;

    private AsyncSubSocket jnSubSocket;
    private static final int JOIN_NETWORK_RECEIVE_TIMEOUT_MS = 5000;
    private static final int DEFAULT_PORT = 5555;

    public static final String JOIN_SAWTOOTH_NETWORK_TOPIC = "JOIN_NETWORK";
    private final boolean doPrint = true;

    /**
     * @param hyperZMQ              hyperZMQ instance to bind this to
     * @param joinNetworkSubAddress to address on which to listen (with a ZMQ sub socket) for requests
     */
    public JoinNetworkExtension(HyperZMQ hyperZMQ, String joinNetworkSubAddress) {
        this.clientID = hyperZMQ.getClientID();
        this.hyperZMQ = hyperZMQ;
        this.joinNetworkAddress = joinNetworkSubAddress;
        this.jnSubSocket = null;
        this.jnSubSocket = new AsyncSubSocket(clientID, this, joinNetworkSubAddress, JOIN_SAWTOOTH_NETWORK_TOPIC,
                JOIN_NETWORK_RECEIVE_TIMEOUT_MS);
        jnListenerExService.submit(jnSubSocket);
    }

    /**
     * Received a message from the SubSocket that is listening for applicants that want to join the network
     *
     * @param message message
     * @param topic   topic
     */
    @Override
    public void newMessage(String message, String topic) {
        print("New message " + message + " topic: " + topic);
        // This method can be used by different sockets - using different topics,
        // therefore topic is given too
        switch (topic) {
            case JOIN_SAWTOOTH_NETWORK_TOPIC: {
                JoinRequest joinRequest = Utilities.deserializeMessage(message, JoinRequest.class);
                if (joinRequest != null) {
                    if (joinRequest.getType() == JoinRequestType.NETWORK) {
                        JNMemberThread t = new JNMemberThread(clientID, joinRequest, hyperZMQ.getSawtoothSigner(), hyperZMQ.getSawtoothPublicKey(), this);
                        jnMemberExService.submit(t);
                    } else {
                        print("Received JoinRequest of wrong type on this channel: " + joinRequest.toString() + ", with topic=" + topic);
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    public void authenticationSuccess(JoinRequest joinRequest) {
        // JoinRequest can be queued for Vote
        hyperZMQ.getVoteManager().addJoinRequest(joinRequest);
    }

    public void authenticationFailure(JoinRequest joinRequest) {
        // TODO nothing else to do?
        // the JNMemberThread already notified the applicant that the authentication failed
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "] [JoiningManager][" + clientID + "] " + message);
    }

}
