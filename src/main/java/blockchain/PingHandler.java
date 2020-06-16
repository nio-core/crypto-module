package blockchain;

import client.HyperZMQ;
import groups.Envelope;
import groups.MessageType;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PingHandler {

    private final HyperZMQ hyperZMQ;
    private final ArrayBlockingQueue<Envelope> pingResponses = new ArrayBlockingQueue<Envelope>(100);

    private String currentPingNonce = "";

    public PingHandler(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
    }

    public ArrayList<String> getGroupMembersByPing(String nonce, int timeInMs) {
        ArrayList<String> ret = new ArrayList<>();

        // Gather incoming pings for the given time
        long endTime = System.currentTimeMillis() + timeInMs;
        currentPingNonce = nonce;
        while (System.currentTimeMillis() < endTime) {
            try {
                Envelope e = pingResponses.poll(50, TimeUnit.MILLISECONDS);
                if (e != null) {
                    String msg = e.getRawMessage();
                    String[] parts = msg.split(",");
                    // payload = nonce, signature, publickey (of responder)
                    if (parts.length >= 3) {
                        // It has to be a response to the request that was made
                        // Pings that
                        if (parts[0].equals(nonce)) {
                            if (SawtoothUtils.verify(nonce, parts[1], parts[2])) {
                                ret.add(parts[2]);
                            } else {
                                print("Ping response contained invalid signature");
                            }
                        } else {
                            print("Got ping response for wrong ping request");
                        }
                    }
                }
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
                break;
            }
        }
        currentPingNonce = "";
        return ret;
    }

    public void addPingResponse(Envelope envelope) {
        if (envelope != null && envelope.getType() == MessageType.PING_RESPONSE) {
            try {
                pingResponses.put(envelope);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            print("Trying to add message of wrong type to ping responses queue!");
        }
    }

    public String getCurrentPingNonce() {
        return currentPingNonce.isEmpty() ? null : currentPingNonce;
    }

    private void print(String message) {
        System.out.println("[" + Thread.currentThread().getId() + "] [PingHandler][" + hyperZMQ.getClientID() + "]  " + message);
    }

}
