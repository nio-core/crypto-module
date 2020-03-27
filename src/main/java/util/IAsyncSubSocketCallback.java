package util;

public interface IAsyncSubSocketCallback {

    void newMessage(String message, String topic);
}
