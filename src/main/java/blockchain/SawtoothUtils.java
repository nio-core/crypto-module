package blockchain;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import sawtooth.sdk.processor.Utils;
import sawtooth.sdk.signing.PrivateKey;
import sawtooth.sdk.signing.PublicKey;
import sawtooth.sdk.signing.Secp256k1Context;
import sawtooth.sdk.signing.Secp256k1PrivateKey;
import sawtooth.sdk.signing.Secp256k1PublicKey;

import java.nio.charset.StandardCharsets;

/**
 * Collect all Sawtooth related helper methods here,
 * instead of confusing the sawtooth or bitoinj packages
 */
public class SawtoothUtils {

    //private static final String HEX_CHARACTERS = "0123456789ABCDEF";
    private static final String HEX_CHARACTERS = "0123456789abcdef";
    private static boolean doPrint = false;

    public static String hash(String toHash) {
        return Utils.hash512(toHash.getBytes(StandardCharsets.UTF_8));
    }

    public static String hash(byte[] toHash) {
        return Utils.hash512(toHash);
    }

    /**
     * Build a Sawtooth address from the namespace and a hashable
     *
     * @param namespace namespace of the transaction family the address is for
     * @param toHash    hashable object which will make up the address
     * @return the address build from the namespace and the last 64 characters of the hash value
     */
    public static String namespaceHashAddress(String namespace, String toHash) {
        print("Hashing: " + toHash);
        String hash = hash(toHash);
        return namespace + hash.substring(hash.length() - 64);
    }

    public static byte[] hexDecode(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static String hexEncode(byte[] data) {
        final StringBuilder hex = new StringBuilder(2 * data.length);
        for (final byte b : data) {
            hex.append(HEX_CHARACTERS.charAt((b & 0xF0) >> 4)).append(HEX_CHARACTERS.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    public static String sign(String message, String privateKey) {
        PrivateKey privateKey1 = new Secp256k1PrivateKey(hexDecode(privateKey));
        return sign(message, privateKey1);
    }

    public static String sign(String message, PrivateKey privateKey) {
        print("Signing " + message);
        print("Using private key:" + privateKey.hex());
        return new Secp256k1Context().sign(message.getBytes(), privateKey);
    }

    public static boolean verify(String message, String signature, String publicKey) {
        print("verifying " + message);
        print("with signature: " + signature);
        PublicKey publicKey1 = new Secp256k1PublicKey(hexDecode(publicKey));
        print("Public key: " + publicKey1.hex());
        return verify(message, signature, publicKey1);
    }

    public static boolean verify(String message, String signature, PublicKey publicKey) {
        return new Secp256k1Context().verify(signature, message.getBytes(), publicKey);
    }

    /**
     * Encapsulate Gson object creation from Json
     *
     * @param message string to deserialize
     * @return Message object or null
     */
    public static <T> T deserializeMessage(String message, Class<T> classOfT) {
        try {
            return new Gson().fromJson(message, classOfT);
        } catch (JsonSyntaxException e) {
            //e.printStackTrace();
            System.err.println("Cannot deserialize message: " + message + " to " + classOfT.toString());
            return null;
        }
    }

    private static void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "]" + "[SawtoothUtils] " + message);
    }

}
