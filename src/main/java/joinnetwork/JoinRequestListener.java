package joinnetwork;

import client.KEManager;
import message.Message;
import message.MessageType;
import util.AsyncReceiveSocket;
import util.IAsyncSocketReceiver;
import util.Utilities;

public class JoinRequestListener implements AutoCloseable, IAsyncSocketReceiver {
    private KEManager keManager;
    private String address;
    //private AtomicBoolean _doListen = new AtomicBoolean(true);
    private boolean doListen = true;

    public static final String JOIN_SAWTOOTH_NETWORK_TOPIC = "JOIN_NETWORK" + KEManager.TOPIC_SUFFIX;
    private AsyncReceiveSocket socket;

    public JoinRequestListener(String address, KEManager callback) {
        this.address = address;
        this.keManager = callback;
        // one thread per instance
        Thread thread = new Thread(this::startListening);
        thread.start();
    }

    void setListening(boolean value) {
        socket.setRunning(value);
    }

    private void startListening() {
       /* ZContext ctx = new ZContext();
        ZMQ.Socket subSocket = ctx.createSocket(ZMQ.SUB);
        subSocket.connect(address);
        subSocket.subscribe(JOIN_SAWTOOTH_NETWORK_TOPIC.getBytes());
        print("listening on " + address);
        while (doListen) {
            String recv = subSocket.recvStr().replaceFirst(JOIN_SAWTOOTH_NETWORK_TOPIC, "");
            print("received:" + recv);

        } */

        socket = new AsyncReceiveSocket(this, address, JOIN_SAWTOOTH_NETWORK_TOPIC, 2000);
        socket.run();
    }

    private void print(String msg) {
        System.out.println("[JoinRequestListener] " + msg);
    }

    @Override
    public void close() throws Exception {
        doListen = false;
        socket.terminate();
    }

    @Override
    public void newMessage(String message, String topic) {
        // Construct message and check for type
        Message m = Utilities.deserializeMessage(message);
        if (m != null) {
            if (m.messageType == MessageType.JOIN_NETWORK_REQUEST) {
                keManager.newJoinRequest(m);
            } else {
                print("received wrong message type: " + message);
            }
        }
    }
}
