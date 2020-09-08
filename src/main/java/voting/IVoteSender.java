package voting;

import client.HyperZMQ;

import java.util.Map;

public interface IVoteSender {
    void sendVote(HyperZMQ hyperZMQ, Vote vote, String group, Map<String,String> requestArgs);
}
