package gui;

import com.google.gson.Gson;

enum ActivityType {
    MESSAGE,
    CONTRACT,
    CONTRACT_RECEIPT,
    VOTE,
    VOTING_MATTER,
    JOIN_GROUP,
    LEAVE_GROUP
}

public class Activity {

    final ActivityType type;
    final String group, sourceID, payload;

    public Activity(ActivityType type, String group, String sourceID, String payload) {
        this.type = type;
        this.group = group;
        this.sourceID = sourceID;
        this.payload = payload;
    }

    public String toJSON() {
        return new Gson().toJson(this);
    }

    @Override
    public String toString() {
        return "";
    }
}
