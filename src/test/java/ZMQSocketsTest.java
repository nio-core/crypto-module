import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import util.AsyncSubSocket;
import util.IAsyncSubSocketCallback;
import util.PubSocket;

public class ZMQSocketsTest implements IAsyncSubSocketCallback {
    public static final String JOIN_SAWTOOTH_NETWORK_TOPIC = "JOIN_NETWORK";
    private static final int JOIN_NETWORK_RECEIVE_TIMEOUT_MS = 5000;

    @Test
    public void test() throws InterruptedException {
        String addressRec = "tcp://*:5555";
        String addressSend = "tcp://127.0.0.1:5555";
        String topic = "TOPICNAME";

        AsyncSubSocket subSocket = new AsyncSubSocket("test", this, addressSend, topic,
                JOIN_NETWORK_RECEIVE_TIMEOUT_MS);

        Thread t1 = new Thread(subSocket);
        t1.start();

/*

        Thread t2 = new Thread(() -> {
            ZContext context = new ZContext();
            ZMQ.Socket subSocket = context.createSocket(ZMQ.SUB);
            subSocket.subscribe(topic);
            subSocket.connect(addressSend);
            while (true) {
                System.out.println("Receiving...");
                String s = subSocket.recvStr();
                System.out.println("Got: " + s.replaceFirst(topic, ""));
            }
        });
        t2.start();
*/

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PubSocket pubSocket = new PubSocket(addressSend);
        pubSocket.bind();
        for (int i = 0; i < 5; i++) {
            Thread.sleep(500);
            pubSocket.send("HALLO!!!", topic);
        }

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
