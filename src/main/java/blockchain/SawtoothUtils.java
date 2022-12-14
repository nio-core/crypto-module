package blockchain;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import sawtooth.sdk.processor.Utils;
import sawtooth.sdk.signing.*;

import java.nio.charset.StandardCharsets;

import static blockchain.GlobalConfig.PRINT_SAWTOOTH_UTILS;

/**
 * Collect all Sawtooth related helper methods here,
 * instead of confusing the sawtooth or bitoinj packages
 */
public class SawtoothUtils {

    //private static final String HEX_CHARACTERS = "0123456789ABCDEF";
    private static final String HEX_CHARACTERS = "0123456789abcdef";

    public static String hash(String toHash) {
        return Utils.hash512(toHash.getBytes(StandardCharsets.UTF_8));
    }

    public static String hash(byte[] toHash) {
        return Utils.hash512(toHash);
    }

    /**
     * Build a Sawtooth address from the namespace and a hashable
     * An address is a hex-encoded 70 character string representing 35 bytes
     * The address format contains a 3 byte (6 hex character) namespace prefix
     * The rest of the address format is up to the implementation
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
        if (PRINT_SAWTOOTH_UTILS)
            System.out.println("[" + Thread.currentThread().getId() + "]" + "[SawtoothUtils] " + message);
    }

}
