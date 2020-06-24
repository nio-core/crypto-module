import client.HyperZMQ;
import client.NetworkJoinManager;

import java.util.Collections;

import org.junit.Test;
import subgrouping.RandomSubgroupSelector;
import voting.AllChatVotingProcess;
import voting.SimpleMajorityEvaluator;
import voting.YesVoteStrategy;

public class JoinNetworkTest {

    boolean run = true;

    final String PRIVATE_1 = "ba58429bc686bcf14725c60b11bee7b09d1d66c1b4f38acf9fe51d91ab6cc060";
    final String PUBLIC_1 = "022fb56a55248087549e1595baf214445f81b0f40c47197846bc873becc1c4bd83";

    final String PRIVATE_2 = "80e5a4da195f625a90549e2f7d9f6f8e017b284447a43114d678b9dfa7d0dcd0";
    final String PUBLIC_2 = "0238a097292e6aba1714a8f9dc17c1eab7d76a18502ee867cc527b9c0fd3c9f944";

    final String PRIVATE_3 = "d17f08a4b8b4e7f75f326d8c15f0445ec27a6561171a169bb03245ff7625ba41";
    final String PUBLIC_3 = "02fb9e47838133e9d4d1ad3c5ba69b607d7b3377e0f2d8dd03b610d1f15b9ea3c8";

    final String PRIVATE_4 = "51ea8b07806fe89b8675ecee0d140146f48abf1a5cff31f6f2ee8c75ae900e2d";
    final String PUBLIC_4 = "0203be51b27de3bd42d09ef53928c7d7d963dc9134055a57c9c2a90527764eecf3";

    @Test
    public void testJoining() throws InterruptedException {
        String joinNetworkSubSocketAddr = "tcp://127.0.0.1:5556";

        HyperZMQ join = new HyperZMQ("joinClient", "password", true);
        join.setPrivateKey(PRIVATE_1);
        // TODO the listener should be split up
        // TODO: New flow: New clients start out with some static joinNetwork(addr, myKey, ...) that generates NetworkInformation which
        // TODO: can be used to create a HyperZMQ instance that connects to the network.
        // TODO: By default, the allchat is accessible and tryjoingroup can then be used
        NetworkJoinManager joinManagerAppl = new NetworkJoinManager(join, joinNetworkSubSocketAddr, false);

        HyperZMQ member = new HyperZMQ("memberClient", "password", true);
        member.setPrivateKey(PRIVATE_2);
        member.registerClient();

        NetworkJoinManager joinManagerMember = new NetworkJoinManager(member, joinNetworkSubSocketAddr, true);

        member.getVoteManager().setVotingProcessNetwork(new AllChatVotingProcess(member));
        member.getVoteManager().setVotingStrategyNetwork(new YesVoteStrategy(50));
        member.getVoteManager().setSubgroupSelector(new RandomSubgroupSelector(), 5);
        member.getVoteManager().setVoteEvaluator(new SimpleMajorityEvaluator(Collections.emptyList(), false, member.getClientID()));

        Thread.sleep(2000);

        joinManagerAppl.tryJoinNetwork("localhost", 5555, Collections.emptyMap());

        Thread.sleep(15000);
    }
}
