import client.HyperZMQ;
import client.NetworkJoinManager;
import client.VoteManager;
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
        String joinNetworkSubSocketAddr = "tcp://127.0.0.1:5555";

        HyperZMQ join = new HyperZMQ("joinClient", "password", true);
        NetworkJoinManager joinManagerAppl = new NetworkJoinManager(join, joinNetworkSubSocketAddr);

        HyperZMQ member = new HyperZMQ("memberClient", "password", true);
        NetworkJoinManager joinManagerMember = new NetworkJoinManager(member, joinNetworkSubSocketAddr);

        VoteManager voteManager = new VoteManager(member);
        voteManager.setVotingProcessNetwork(new GroupInternVotingProcess(member, 50));
        voteManager.setVotingStrategyNetwork(new YesVoteStrategy(50));
        voteManager.setSubgroupSelector(new RandomSubgroupSelector(), 5);
        voteManager.setVoteEvaluator(new SimpleMajorityEvaluator(Collections.emptyList(), false, member.getClientID()));

        Thread.sleep(2000);

        //memberClient.handleJoinNetwork(joinClient.getRequest());
        //Thread.sleep(300);
        joinManagerAppl.tryJoinNetwork("localhost", 5555, Collections.emptyMap());

        while (true) {
        }
    }
}
