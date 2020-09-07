package diffiehellman;

import client.HyperZMQ;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class DHTest {

    @Test
    public void testDH() throws ExecutionException, InterruptedException, IOException {
        String address = "localhost";
        int port = 5555;

        HyperZMQ hzmqServer = new HyperZMQ.Builder("server", "password")
                .createNewIdentity(true)
                .build();

        HyperZMQ hzmqClient = new HyperZMQ.Builder("client", "password")
                .createNewIdentity(true)
                .build();

        FutureTask<EncryptedStream> server = new FutureTask<EncryptedStream>(new DHKeyExchange("server",
                hzmqServer.getSawtoothSigner(),
                hzmqClient.getSawtoothPublicKey(),
                address,
                5555,
                true));

        FutureTask<EncryptedStream> client = new FutureTask<EncryptedStream>(new DHKeyExchange("client",
                hzmqClient.getSawtoothSigner(),
                hzmqServer.getSawtoothPublicKey(),
                address,
                5555,
                false));

        new Thread(server).start();
        new Thread(client).start();

        EncryptedStream stream1 = server.get();
        EncryptedStream stream2 = client.get();

        stream1.write("test");
        String test = stream2.readLine();

        Assert.assertEquals("test", test);
    }
}
