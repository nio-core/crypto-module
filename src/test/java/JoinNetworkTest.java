import client.HyperZMQ;
import client.JoiningManager;
import org.junit.Test;

public class JoinNetworkTest {

    boolean run = true;

    @Test
    public void testJoining() throws InterruptedException {
        HyperZMQ join = new HyperZMQ("joinClient", "password", true);
        HyperZMQ member = new HyperZMQ("memberClient", "password", true);
        String address = "tcp://127.0.0.1:5555";
        JoiningManager joinClient = new JoiningManager(join, address);
        JoiningManager memberClient = new JoiningManager(member, address);

        Thread.sleep(1000);

        //memberClient.handleJoinNetwork(joinClient.getRequest());
        //Thread.sleep(300);
        joinClient.sendJoinRequest(address);

        while (true) {
        }
    }
}
