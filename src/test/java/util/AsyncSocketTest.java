package util;

import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncSocketTest implements IAsyncSubSocketCallback {

    private String topic = "testTopic";
    private String message = "testmessage";
    private String address = "tcp://127.0.0.1:5555";
    private boolean run = true;

    @Test
    public void test() throws InterruptedException {
        AsyncSubSocket rsock = new AsyncSubSocket("test", this, address, topic, 4000);
        ExecutorService e = Executors.newSingleThreadExecutor();
        e.submit(rsock);

        Thread.sleep(3000);

        ZContext context = new ZContext();
        ZMQ.Socket ssock = context.createSocket(ZMQ.PUB);
        ssock.bind("tcp://*:5555");
        Thread.sleep(500); // Without this, the message is not sent properly...
        ssock.send((topic + message).getBytes());
        System.out.println("Sent message: " + (topic + message));
        Thread.sleep(5000);

    }

    @Override
    public void newMessage(String message, String topic) {
        System.out.println("Callback invoked");
        Assert.assertEquals(this.message, message);
        Assert.assertEquals(this.topic, topic);
    }
}
