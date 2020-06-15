package util;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class PubSocket {

    private String address;
    private ZContext context = new ZContext();
    private ZMQ.Socket socket;
    public final static String TOPIC_SUFFIX = "-!-";


    public PubSocket(String address) {
        this.address = address;
        this.socket = context.createSocket(ZMQ.PUB);
    }

    public String getAddress() {
        return address;
    }

    public void bind() {
        socket.bind(address);
    }

    public void disconnect() {
        socket.disconnect(address);
    }


    public boolean send(String message, String topic) {

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //String toSend = topic == null ? message :
        //      (topic + TOPIC_SUFFIX + message);
        //System.out.println("Sending message: " + toSend);
        boolean ret = false;
        if (topic != null) {
            ret = socket.send(topic + TOPIC_SUFFIX);
            ret = socket.sendMore(message);
        } else {
            ret = socket.send(message);
        }
        System.out.println("Sent message:" + message);

        return ret;
    }
}
