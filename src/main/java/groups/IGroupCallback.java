package groups;

public interface IGroupCallback {
    void newMessageOnChain(String group, String message, String senderID);
}
