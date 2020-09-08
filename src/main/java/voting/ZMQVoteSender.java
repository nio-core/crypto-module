package voting;

import client.HyperZMQ;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Map;

public class ZMQVoteSender implements IVoteSender {

    @Override
    public void sendVote(HyperZMQ hyperZMQ, Vote vote, String group, Map<String, String> requestArgs) {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket sender = context.createSocket(ZMQ.PUSH);

            String strRetAddr = requestArgs.get("returnAddress");
            if (strRetAddr == null) {
                System.err.println("Cannot return vote because address is not in request args");
                return;
            }
            System.err.println("Returning vote to: " + strRetAddr);
            sender.connect(strRetAddr);
            Thread.sleep(100);
            //System.err.println("ZMQVoteSender sending vote...");
            sender.send(vote.toString());
            sender.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}