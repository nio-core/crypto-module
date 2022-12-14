package diffiehellman;

import client.Crypto;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;

public class EncryptedStream implements AutoCloseable {
    private final PrintWriter out;
    private final BufferedReader in;

    private final SecretKey secretKey;

    public EncryptedStream(BufferedReader inputStream, PrintWriter outputStream, SecretKey secretKey) {
        this.in = inputStream;
        this.out = outputStream;
        this.secretKey = secretKey;
    }

    public void write(String line) {
        try {
            String enc = client.Crypto.encrypt(line, secretKey);
            out.println(enc);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public String readLine() throws IOException {
        String enc = in.readLine();
        if (enc == null) return null;

        try {
            return Crypto.decrypt(enc, secretKey);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        //System.err.println("Closing streams...");
        in.close();
        out.close();
    }
}
