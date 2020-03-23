import client.HyperZMQStub;
import client.KEManager;
import org.junit.Test;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class JoinNetworkTest {

    boolean run = true;

    @Test
    public void testJoining() throws InterruptedException {
        KEManager joinClient = new KEManager("joinClient", new HyperZMQStub());
        KEManager memberClient = new KEManager("memberClient", new HyperZMQStub());
        String addr = "tcp://127.0.0.1:5555";

        memberClient.listenForRequests(addr);

        Thread.sleep(2000);

        joinClient.sendJoinRequest(addr);

        Thread.sleep(4000);

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

    @Test
    public void testSendReceive() throws InterruptedException {
        ZContext ctx1 = new ZContext();
        ZContext ctx2 = new ZContext();

        String addr = "tcp://127.0.0.1:5555";

        ZMQ.Socket subSocket = ctx1.createSocket(ZMQ.SUB);
        subSocket.connect(addr);
        subSocket.subscribe("A".getBytes());
        ZMQ.Socket pubSocket = ctx2.createSocket(ZMQ.PUB);
        pubSocket.bind("tcp://*:5555");
        Thread t = new Thread(() -> {
            while (run) {
                System.out.println("Receiving in thread: " + Thread.currentThread().getId());
                String s = subSocket.recvStr();
                System.out.println("received:" + s);
            }
        });
        t.start();
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000);
            //pubSocket.sendMore("A".getBytes());
            pubSocket.send("AHALLO");
            System.out.println("message.Message sent");

        }
        Thread.sleep(2000);
        run = false;
        ctx1.close();
        ctx2.close();
    }
}
