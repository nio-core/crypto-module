package voting;

import java.nio.charset.StandardCharsets;
import sawtooth.sdk.signing.Signer;

public class YesVoteStrategy implements IVotingStrategy {

    private final int sleepMS;

    public YesVoteStrategy(int delayMS) {
        this.sleepMS = delayMS;
    }

    @Override
    public Vote castVote(VotingMatter votingMatter, Signer signer) {
        try {
            Thread.sleep(sleepMS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Vote v = new Vote(true, votingMatter.getHash(), signer.getPublicKey().hex());
        v.setSignature(signer.sign(v.getSignablePayload().getBytes(StandardCharsets.UTF_8)));
        return v;
    }
}
