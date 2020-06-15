import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import util.AsyncSubSocket;
import util.IAsyncSubSocketCallback;
import util.PubSocket;

import static org.hamcrest.CoreMatchers.notNullValue;

public class ZMQSocketsTest implements IAsyncSubSocketCallback {
    public static final String JOIN_SAWTOOTH_NETWORK_TOPIC = "JOIN_NETWORK";
    private static final int JOIN_NETWORK_RECEIVE_TIMEOUT_MS = 5000;

    @Test
    public void test() {
        String addressRec = "tcp://*:5555";
        String addressSend = "tcp://127.0.0.1:5555";
        AsyncSubSocket subSocket = new AsyncSubSocket(this, addressRec, JOIN_SAWTOOTH_NETWORK_TOPIC,
                JOIN_NETWORK_RECEIVE_TIMEOUT_MS);

        Thread t1 = new Thread(subSocket);
        t1.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PubSocket pubSocket = new PubSocket(addressSend);
        pubSocket.bind();
        pubSocket.send("HALLO!!!", JOIN_SAWTOOTH_NETWORK_TOPIC);

        while (true) {
        }
    }

    @Test
    public void test01() {
        String addressRec = "tcp://*:5555";
        String addressSend = "tcp://127.0.0.1:5555";

        Thread t1 = new Thread(() -> {
            ZContext context = new ZContext();
            ZMQ.Socket pubBind = context.createSocket(ZMQ.PUB);
            pubBind.bind(addressRec);
            System.out.println("receiving...");
            String s = pubBind.recvStr();

            System.out.println("Received: " + s);
        });
        t1.start();
    }

    @Override
    public void newMessage(String message, String topic) {
        Assert.assertEquals("HALLO!!!", message);
    }
}
