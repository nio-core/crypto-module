package util;

public interface IAsyncSocketReceiver {

    void newMessage(String message, String topic);
}
