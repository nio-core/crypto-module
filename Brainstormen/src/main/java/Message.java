import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class Message {

    public String source;
    public MessageType messageType;
    public Map<String, String> data;

    public Message(String source, MessageType messageType, Map<String, String> data) {
        this.source = source;
        this.messageType = messageType;
        this.data = data;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this, Message.class);
    }
}
