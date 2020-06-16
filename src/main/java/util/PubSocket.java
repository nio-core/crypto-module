package util;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class PubSocket {

    private String address;
    private ZContext context = new ZContext();
    private ZMQ.Socket socket;

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
        String toSend = topic == null ? message :
                (topic + message);
        boolean ret = false;

        ret = socket.send(toSend);
        if (ret) {
            System.out.println("Sending message: " + toSend);
        } else {
            //System.out.println("Sending failed!");
        }
        return ret;
    }
}
