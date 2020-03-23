package util;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;

public class AsyncReceiveSocket implements Runnable {

    private IAsyncSocketReceiver callback;
    private ZContext context = new ZContext();
    private ZMQ.Socket socket;
    private boolean doRun = true;
    private int receiveTimeoutMS;
    private String topic;
    private static final boolean doPrint = true;

    public AsyncReceiveSocket(IAsyncSocketReceiver callback, String address, String topic, int receiveTimeoutMS) {
        this.callback = callback;
        this.socket = context.createSocket(ZMQ.SUB);
        socket.connect(address);
        socket.subscribe(topic.getBytes(StandardCharsets.UTF_8));
        socket.setReceiveTimeOut(receiveTimeoutMS);
        this.receiveTimeoutMS = receiveTimeoutMS;
        this.topic = topic;
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
            String s = socket.recvStr();
            print(s);
            if (s != null && !s.isEmpty()) {
                callback.newMessage(s.replaceFirst(this.topic, ""), this.topic);
            }
        }
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[AsyncRSocket][" + topic + "] " + message);
    }
}
