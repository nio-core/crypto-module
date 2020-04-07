package diffiehellman;

import client.HyperZMQ;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.Arrays;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class DHTest {

    @Test
    public void testDH() throws ExecutionException, InterruptedException {
        String address = "localhost";
        int port = 5555;
        HyperZMQ hzmqServer= new HyperZMQ("server", "password", true);
        HyperZMQ hzmqClient= new HyperZMQ("client", "password", true);

        FutureTask<SecretKey> server = new FutureTask<SecretKey>(new DHKeyExchange("server",
                hzmqServer.getSawtoothPrivateKey(),
                hzmqClient.getSawtoothPublicKey(),
                address,
                5555,
                true));

        FutureTask<SecretKey> client = new FutureTask<SecretKey>(new DHKeyExchange("client",
                hzmqClient.getSawtoothPrivateKey(),
                hzmqServer.getSawtoothPublicKey(),
                address,
                5555,
                false));

        new Thread(server).start();
        new Thread(client).start();

        SecretKey key1 = server.get();
        SecretKey key2 = client.get();

        Assert.assertTrue(Arrays.areEqual(key1.getEncoded(), key2.getEncoded()));
    }
}
