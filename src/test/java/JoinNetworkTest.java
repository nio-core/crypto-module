import client.HyperZMQ;
import client.NetworkJoinManager;
import client.VoteManager;

import java.util.Collections;

import org.junit.Test;
import subgrouping.RandomSubgroupSelector;
import voting.AllChatVotingProcess;
import voting.GroupInternVotingProcess;
import voting.SimpleMajorityEvaluator;
import voting.YesVoteStrategy;

public class JoinNetworkTest {

    boolean run = true;

    @Test
    public void testJoining() throws InterruptedException {
        String joinNetworkSubSocketAddr = "tcp://127.0.0.1:5555";

        HyperZMQ join = new HyperZMQ("joinClient", "password", true);
        // TODO the listener should be split up
        // TODO: New flow: New clients start out with some static joinNetwork(addr, myKey, ...) that generates NetworkInformation which
        // TODO: can be used to create a HyperZMQ instance that connects to the network.
        // TODO: By default, the allchat is accessible and tryjoingroup can then be used
        NetworkJoinManager joinManagerAppl = new NetworkJoinManager(join, joinNetworkSubSocketAddr, true);

        HyperZMQ member = new HyperZMQ("memberClient", "password", true);
        NetworkJoinManager joinManagerMember = new NetworkJoinManager(member, joinNetworkSubSocketAddr, true);

        member.getVoteManager().setVotingProcessNetwork(new AllChatVotingProcess(member));
        member.getVoteManager().setVotingStrategyNetwork(new YesVoteStrategy(50));
        member.getVoteManager().setSubgroupSelector(new RandomSubgroupSelector(), 5);
        member.getVoteManager().setVoteEvaluator(new SimpleMajorityEvaluator(Collections.emptyList(), false, member.getClientID()));

        Thread.sleep(2000);

        joinManagerAppl.tryJoinNetwork("localhost", 5555, Collections.emptyMap());

        while (true) {
        }
    }
}
