package util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import message.Message;
import message.MessageType;
import sawtooth.sdk.signing.PrivateKey;
import sawtooth.sdk.signing.PublicKey;
import sawtooth.sdk.signing.Secp256k1Context;
import sawtooth.sdk.signing.Secp256k1PublicKey;

import java.security.SecureRandom;

public class Utilities {

    public static String sign(String message, PrivateKey privateKey) {
        return new Secp256k1Context().sign(message.getBytes(), privateKey);
    }

    public static boolean verify(String message, String signature, String publicKey) {
        PublicKey publicKey1 = new Secp256k1PublicKey(publicKey.getBytes());
        return new Secp256k1Context().verify(signature, message.getBytes(), publicKey1);
    }

    /**
     * Encapsulate Gson object creation from Json
     *
     * @param message string to deserialize
     * @return Message object or null
     */
    public static Message deserializeMessage(String message) {
        try {
            return new Gson().fromJson(message, Message.class);
        } catch (JsonSyntaxException e) {
            //e.printStackTrace();
            System.out.println("Cannot deserialize message: " + message);
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
