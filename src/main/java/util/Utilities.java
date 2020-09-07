package util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.security.SecureRandom;

public class Utilities {
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
