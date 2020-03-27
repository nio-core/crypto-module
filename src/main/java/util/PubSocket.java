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

    public boolean send(String message, String topic) {
        socket.bind(address);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String toSend = topic == null ? message :
                (topic + TOPIC_SUFFIX + message);
        System.out.println("Sending message: " + toSend);

        boolean ret = socket.send(toSend);
        socket.disconnect(address);
        return ret;
    }
}
