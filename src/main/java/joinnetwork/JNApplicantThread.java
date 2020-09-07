package joinnetwork;

import blockchain.SawtoothUtils;
import client.JoinNetworkExtension;
import diffiehellman.DHKeyExchange;
import diffiehellman.EncryptedStream;
import joingroup.JoinRequest;
import messages.JNChallengeMessage;
import messages.JNResponseMessage;
import messages.MessageFactory;
import sawtooth.sdk.signing.Signer;
import util.PubSocket;
import util.Utilities;
import voting.JoinRequestType;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

/**
 * The Thread for an applicant that wants to join the network.
 * Sends id and public key initially to the JoinNetwork Topic.
 * Afterwards listens to own id topic for a challenge from a network member.
 * If a challenge is received, sign it with the public key from the first step to prove identity
 * Counterpart of JNMemberThread
 */
public class JNApplicantThread implements Runnable {

    private static final int RECEIVE_TIMEOUT_MS = 10000;

    private final String address;
    private final String myID;
    private final String myPublicKey;
    private final Signer mySigner;
    private final int port;
    private final Map<String, String> additionalInfo;
    private final boolean doPrint = true;
    private final String pubSocketAddress;
    private final IJoinNetworkStatusCallback callback;

    public JNApplicantThread(String myID, Signer mySigner, String address, int port,
                             Map<String, String> additionalInfo, String pubSocketAddress, @Nullable IJoinNetworkStatusCallback callback) {
        this.myID = myID;
        this.myPublicKey = mySigner.getPublicKey().hex();
        this.mySigner = mySigner;
        this.address = address;
        this.port = port;
        this.additionalInfo = additionalInfo;
        this.pubSocketAddress = pubSocketAddress;
        this.callback = callback;
    }

    @Override
    public void run() {
        // Send a JoinNetworkRequestMessage to the "join network" topic initially
        print("Starting JNApplicantThread...");
        PubSocket pubSocket = new PubSocket(pubSocketAddress);
        JoinRequest joinRequest = new JoinRequest(myPublicKey,
                null,
                JoinRequestType.NETWORK,
                null,
                additionalInfo,
                address,
                port);

        pubSocket.bind();
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pubSocket.send(joinRequest.toString(), JoinNetworkExtension.JOIN_SAWTOOTH_NETWORK_TOPIC);
        print("Sent join request");
        pubSocket.disconnect();

        notifyCallback(IJoinNetworkStatusCallback.SENT_JOIN_REQUEST_ZMQ, joinRequest.toString());

        // Im the server
        try {
            print("Starting server to listen for challenge");
            ServerSocket serverSocket = new ServerSocket(port);
            Socket clientSocket = serverSocket.accept();

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            notifyCallback(IJoinNetworkStatusCallback.SERVER_SOCKET_SET_UP, null);

            print("Listening for challenge...");

            String s = in.readLine();
            print("Received: " + s);
            JNChallengeMessage challenge = Utilities.deserializeMessage(s, JNChallengeMessage.class);
            if (challenge == null) {
                notifyCallback(IJoinNetworkStatusCallback.MALFORMED_CHALLENGE_MESSAGE, s);
                return; // TODO
            }

            // Verify the network member
            boolean verified = SawtoothUtils.verify(challenge.getSignablePayload(),
                    challenge.getSignature(),
                    challenge.getMemberPublicKey());

            if (!verified) {
                print("Signature verification failed!");
                notifyCallback(IJoinNetworkStatusCallback.SIGNATURE_VERIFICATION_FAILED, challenge.toString());
                return; // TODO
            }
            print("Signature verified");
            notifyCallback(IJoinNetworkStatusCallback.SIGNATURE_VERIFIED, null);

            JNResponseMessage response = new MessageFactory(mySigner)
                    .jnResponseMessage(challenge.getNonce());

            out.println(response.toString());
            print("Sent response: " + response);

            notifyCallback(IJoinNetworkStatusCallback.RESPONSE_SENT, response.toString());
            // Wait for ok
            s = in.readLine();
            print("Received authentication response: " + s);

            clientSocket.close();
            serverSocket.close();

            // TODO vvv the strings to indicate success / failure
            if ("OK!!!".equals(s)) {
                notifyCallback(IJoinNetworkStatusCallback.AUTHENTICATION_RESPONSE_OK, null);

                DHKeyExchange exchange = new DHKeyExchange(myID,
                        mySigner,
                        challenge.getMemberPublicKey(),
                        joinRequest.getAddress(),
                        joinRequest.getPort(), true);
                try {
                    try (EncryptedStream encryptedStream = exchange.call()) {
                        String s1 = encryptedStream.readLine();
                        if (s1.startsWith("Allowed!")) {
                            notifyCallback(IJoinNetworkStatusCallback.ACCESS_GRANTED, s1.split("!")[1]);
                        } else {
                            notifyCallback(IJoinNetworkStatusCallback.ACCESS_DENIED, null);
                        }
                    } catch (ConnectException e) {
                        // TODO applicant has not set up a server
                        notifyCallback(IJoinNetworkStatusCallback.KEY_EXCHANGE_NO_PEER, joinRequest.getAddress() + ":" + joinRequest.getPort());

                        print("Cannot notify the applicant because no server is listening for a connection on: "
                                + joinRequest.getAddress() + ":" + joinRequest.getPort());
                    }
                } catch (Exception e) {
                    notifyCallback(IJoinNetworkStatusCallback.IO_EXCEPTION_OCCURRED, e.toString());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            notifyCallback(IJoinNetworkStatusCallback.IO_EXCEPTION_OCCURRED, e.toString());
            return;
        }
    }

    private void notifyCallback(int code, String info) {
        if (callback != null) {
            callback.joinNetworkStatusCallback(code, info);
        }
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "] [JoinNetworkApplicant][" + myID + "] " + message);
    }
}
