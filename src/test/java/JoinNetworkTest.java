import client.HyperZMQStub;
import client.KEManager;
import org.junit.Test;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class JoinNetworkTest {

    boolean run = true;

    @Test
    public void testJoining() throws InterruptedException {
        String address = "tcp://127.0.0.1:5555";
        KEManager joinClient = new KEManager("joinClient", new HyperZMQStub(), address);
        KEManager memberClient = new KEManager("memberClient", new HyperZMQStub(), address);

        Thread.sleep(1000);

        memberClient.handleJoinNetwork(joinClient.getRequest());
        Thread.sleep(300);
        joinClient.sendJoinRequest(address);

        while(true) {}

    }

    @Test
    public void testSign() {
        /*
        Context context = new Secp256k1Context();
        PrivateKey privateKey = context.newRandomPrivateKey();
        System.out.println("Privatekey: " + privateKey.hex());
        byte[] message = "hallo".getBytes();

        String signature = context.sign(message, privateKey);
        System.out.println("Signature: " + signature);

        PublicKey publicKey = context.getPublicKey(privateKey);
        System.out.println("Publickey: " + publicKey.hex());

        boolean isValid = context.verify(signature, message, publicKey);
        System.out.println("isValid: " + isValid);
        */
    }


}
