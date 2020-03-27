package util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.bitcoinj.core.Utils;
import sawtooth.sdk.signing.*;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class Utilities {

    public static String sign(String message, String privateKey) {
        PrivateKey privateKey1 = new Secp256k1PrivateKey(Utils.HEX.decode(privateKey));
        return sign(message, privateKey1);
    }

    public static String sign(String message, PrivateKey privateKey) {
        return new Secp256k1Context().sign(message.getBytes(), privateKey);
    }

    public static boolean verify(String message, String signature, String publicKey) {
        PublicKey publicKey1 = new Secp256k1PublicKey(Utils.HEX.decode(publicKey));
        return verify(message, signature, publicKey);
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
            System.out.println("Cannot deserialize message: " + message + " to " + classOfT.toString());
            return null;
        }
    }

    public static String generateNonce(int characterCount) {
        int leftLimit = 48; // '0'
        int rightLimit = 122; // 'z'

        return new SecureRandom().ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(characterCount)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

}
