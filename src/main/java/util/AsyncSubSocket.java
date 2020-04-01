package util;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class AsyncSubSocket implements Runnable {

    private IAsyncSubSocketCallback callback;
    private ZContext context;
    private ZMQ.Socket socket;
    private boolean doRun = true;
    private int receiveTimeoutMS;
    private String topic;
    private static final boolean doPrint = false;
    private String address;

    public AsyncSubSocket(IAsyncSubSocketCallback callback, String address, String topic, int receiveTimeoutMS) {
        this.callback = callback;
        this.address = address;
        this.topic = topic;
        this.receiveTimeoutMS = receiveTimeoutMS;
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.SUB);
        socket.setReceiveTimeOut(receiveTimeoutMS);

        socket.connect(address);
        socket.subscribe(topic.getBytes());
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
            //print(s);
            if (s != null && !s.isEmpty()) {
                print(s);
                callback.newMessage(s.replaceFirst(this.topic + PubSocket.TOPIC_SUFFIX, ""), this.topic);
            }
        }
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[AsyncRSocket][" + topic + "][" + address + "] " + message);
    }
}