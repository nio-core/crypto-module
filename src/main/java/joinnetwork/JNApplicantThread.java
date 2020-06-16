package joinnetwork;

import blockchain.SawtoothUtils;
import client.NetworkJoinManager;
import diffiehellman.DHKeyExchange;
import diffiehellman.EncryptedStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import joingroup.JoinRequest;
import messages.JNChallengeMessage;
import messages.JNResponseMessage;
import messages.MessageFactory;
import sawtooth.sdk.signing.Signer;
import util.PubSocket;
import util.Utilities;
import voting.JoinRequestType;

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
    private final NetworkJoinManager client;
    private final Map<String, String> additionalInfo;
    private final boolean doPrint = true;
    private final String pubSocketAddress;

    public JNApplicantThread(String myID, String myPublicKey, Signer mySigner, NetworkJoinManager client,
                             String address, int port, Map<String, String> additionalInfo, String pubSocketAddress) {
        this.myID = myID;
        this.myPublicKey = myPublicKey;
        this.mySigner = mySigner;
        this.client = client;
        this.address = address;
        this.port = port;
        this.additionalInfo = additionalInfo;
        this.pubSocketAddress = pubSocketAddress;
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
        pubSocket.send(joinRequest.toString(), NetworkJoinManager.JOIN_SAWTOOTH_NETWORK_TOPIC);
        print("Sent join request");
        /*
        for (int i = 0; i < 6; i++) {

        }
        */
        pubSocket.disconnect();

        // Im the server
        try {
            print("Starting server to listen for challenge");
            ServerSocket serverSocket = new ServerSocket(port);
            Socket clientSocket = serverSocket.accept();

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            print("Listening for challenge...");

            String s = in.readLine();
            print("Received: " + s);
            JNChallengeMessage challenge = Utilities.deserializeMessage(s, JNChallengeMessage.class);
            if (challenge == null) return; // TODO

            // Verify the network member
            boolean verified = SawtoothUtils.verify(challenge.getSignablePayload(),
                    challenge.getSignature(),
                    challenge.getMemberPublicKey());

            if (!verified) {
                print("Signature verification failed!");
                return; // TODO
            }
            print("Signature verified");

            JNResponseMessage response = new MessageFactory(mySigner)
                    .jnResponseMessage(challenge.getNonce());

            out.println(response.toString());
            print("Sent response: " + response);
            // Wait for ok
            s = in.readLine();
            print("Received authentication response: " + s);

            clientSocket.close();
            serverSocket.close();

            // TODO vvv
            if ("OK!!!".equals(s)) {
                DHKeyExchange exchange = new DHKeyExchange(myID,
                        mySigner,
                        joinRequest.getApplicantPublicKey(),
                        joinRequest.getAddress(),
                        joinRequest.getPort(), true);
                EncryptedStream encryptedStream = null;
                try {
                    encryptedStream = exchange.call();
                } catch (ConnectException e) {
                    // TODO applicant has not set up a server
                    print("Cannot notify the applicant because no server is listening for a connection on: "
                            + joinRequest.getAddress() + ":" + joinRequest.getPort());
                    //e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
            // TODO if ok authenticated - setup server again to listen for vote result


        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "] [JoinNetworkApplicant][" + myID + "] " + message);
    }
}
