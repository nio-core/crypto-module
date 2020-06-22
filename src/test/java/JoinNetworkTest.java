import client.HyperZMQ;
import client.NetworkJoinManager;
import java.util.Collections;
import org.junit.Test;
import subgrouping.RandomSubgroupSelector;
import voting.GroupInternVotingProcess;
import voting.SimpleMajorityEvaluator;
import voting.YesVoteStrategy;

public class JoinNetworkTest {

    boolean run = true;

    @Test
    public void testJoining() throws InterruptedException {
        String joinNetworkSubSocketAddr = "tcp://127.0.0.1:5556";

        HyperZMQ join = new HyperZMQ.Builder("joinClient", "password", null)
                .createNewIdentity(true)
                .build();
        NetworkJoinManager joinManagerAppl = new NetworkJoinManager(join, joinNetworkSubSocketAddr, false);

        // TODO the listener should be split up
        // TODO: New flow: New clients start out with some static joinNetwork(addr, myKey, ...) that generates NetworkInformation which
        // TODO: can be used to create a HyperZMQ instance that connects to the network.
        // TODO: By default, the allchat is accessible and tryjoingroup can then be used

        HyperZMQ member = new HyperZMQ.Builder("memberClient", "password", null)
                .createNewIdentity(true)
                .build();

        NetworkJoinManager joinManagerMember = new NetworkJoinManager(member, joinNetworkSubSocketAddr, true);

        member.getVoteManager().setVotingProcessNetwork(new GroupInternVotingProcess(member));
        member.getVoteManager().setVotingStrategyNetwork(new YesVoteStrategy(50));
        member.getVoteManager().setSubgroupSelector(new RandomSubgroupSelector(), 5);
        member.getVoteManager().setVoteEvaluator(new SimpleMajorityEvaluator(Collections.emptyList(), false, member.getClientID()));

        Thread.sleep(2000);

        joinManagerAppl.tryJoinNetwork("localhost", 5555, Collections.emptyMap());

        Thread.sleep(15000);
    }
}
