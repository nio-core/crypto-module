package util;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class AsyncSubSocket implements Runnable {

    private final IAsyncSubSocketCallback callback;
    private final ZContext context;
    private final ZMQ.Socket socket;
    private final String id;
    private boolean doRun = true;
    private final int receiveTimeoutMS;
    private final String topic;
    private static final boolean doPrint = true;
    private final String address;

    public AsyncSubSocket(String id, IAsyncSubSocketCallback callback, String address, String topic, int receiveTimeoutMS) {
        this.id = id;
        this.callback = callback;
        this.address = address;
        this.topic = topic;
        this.receiveTimeoutMS = receiveTimeoutMS;
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.SUB);
        socket.setReceiveTimeOut(receiveTimeoutMS);

        socket.connect(address);
        if (topic != null) {
            print("Subscribing: " + topic);
            socket.subscribe(topic);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getTopic() {
        return topic;
    }

    public void setRunning(boolean value) {
        this.doRun = value;
    }

    public void terminate() {
        this.doRun = false;
        try {
            Thread.sleep(receiveTimeoutMS + 50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        context.close();
    }

    @Override
    public void run() {
        print("Starting...");
        while (doRun) {
            //print("Receive...");
            String s = socket.recvStr();
            if (s != null && !s.isEmpty()) {
                print(s.replaceFirst(this.topic, ""));
                callback.newMessage(s.replaceFirst(this.topic, ""), this.topic);
            }
        }
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "] [AsyncSubSocket][" + id + "][" + address + "] " + message);
    }
}
