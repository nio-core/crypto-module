package voting;

import client.HyperZMQ;
import groups.Envelope;
import groups.GroupMessage;
import groups.MessageType;
import sawtooth.sdk.protobuf.Transaction;

import java.util.Collections;
import java.util.Map;

public class SawtoothVoteSender implements IVoteSender {

    @Override
    public void sendVote(HyperZMQ hyperZMQ, Vote vote, String group, Map<String, String> requestArgs) {
        // If the votingMatter was about joining group, sent the vote back in the specified group
        Envelope env = new Envelope(hyperZMQ.getClientID(), MessageType.VOTE, vote.toString());
        String payload = hyperZMQ.encryptEnvelope(group, env);
        GroupMessage message = new GroupMessage(group, payload, false, true);
        Transaction transaction = hyperZMQ.blockchainHelper.
                csvStringsTransaction(message.getBytes());

        hyperZMQ.blockchainHelper.buildAndSendBatch(Collections.singletonList(transaction));
    }
}
