package blockchain;

public interface IAllChatReceiver {
    /**
     * @param message message
     * @param sender  sender, identified by the sawtooth public key
     */
    void allChatMessageReceived(String message, String sender);
}
