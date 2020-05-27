import blockchain.IAllChatReceiver;
import client.HyperZMQ;
import org.junit.Test;

public class TestAllChat {

    @Test
    public void test() throws InterruptedException {
        HyperZMQ client1 = new HyperZMQ("client1", "lsdfsd", true);
        HyperZMQ client2 = new HyperZMQ("client2", "lsdfsd", true);
        client2.setAllChatReceiver((message, sender) -> {
            System.out.println("Received " + message + " from " + sender);
        });

        client1.sendAllChat("HALLO NAA");

        Thread.sleep(2000);
    }
}
