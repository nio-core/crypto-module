package groups;

import com.google.gson.Gson;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 *
 */
public class Envelope {
    private final String sender;
    private final MessageType type;
    private final String rawMessage;
    private final String uuid;

    public Envelope(@Nonnull String sender, @Nonnull MessageType type, @Nonnull String rawMessage) {
        this.sender = sender;
        this.rawMessage = rawMessage;
        this.type = type;
        this.uuid = UUID.randomUUID().toString();
    }

    public String getUUID() {
        return uuid;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
