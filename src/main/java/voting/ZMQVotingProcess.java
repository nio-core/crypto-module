package voting;

import client.HyperZMQ;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import util.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZMQVotingProcess implements IVotingProcess {
    private final HyperZMQ hyperZMQ;

    private final AtomicBoolean otherFactor = new AtomicBoolean(true);
    private int port;

    public ZMQVotingProcess(HyperZMQ hyperZMQ, int port) {
        this.hyperZMQ = hyperZMQ;
        this.port = port;
    }

    @Override
    public VotingResult vote(VotingMatter votingMatter, int timeInMs) {
        //System.err.println("ZMQVOTING STARTED");

        // FIXME Add the (complete) return address to the voting matter so that receiving clients ZMQVoteSender knows where to send the vote
        if (votingMatter.getJoinRequest().votingArgs == null) {
            //System.err.println("voting args not found, adding new map");
            votingMatter.getJoinRequest().votingArgs = new HashMap<>();
        }

        votingMatter.getJoinRequest().votingArgs.put("returnAddress", "tcp://127.0.0.1:" + port);

        hyperZMQ.sendVotingMatterInGroup(votingMatter);
        VotingResult result = new VotingResult(votingMatter, new ArrayList<>());
        long endTime = System.currentTimeMillis() + timeInMs;

        try (ZContext context = new ZContext()) {
            ZMQ.Socket receiver = context.createSocket(ZMQ.PULL);
            String addr = "tcp://*:" + port;
            //System.err.println("ZMQVoting listening on " + addr);
            receiver.bind(addr);
            receiver.setReceiveTimeOut(1000);
            while (result.getVotesSize() < votingMatter.getDesiredVoters().size() && System.currentTimeMillis() < endTime) {
                //  Prepare our context and socket
                //System.err.println("ZMQVOTING: listening...");
                String string = new String(receiver.recv(0));
                //System.err.println("ZMQVOTING: received: " + string);

                Vote vote = Utilities.deserializeMessage(string, Vote.class);
                if (vote != null) {
                    result.addVote(vote);
                }
            }
            receiver.close();
        }
        return result;
    }
}
