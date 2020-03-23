package smartgossip;

import com.google.gson.Gson;

public class SmartGossipMessage {

    private String pid;

    public SmartGossipMessage(String pid) {
        this.pid = pid;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
