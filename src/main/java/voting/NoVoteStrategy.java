package voting;

import sawtooth.sdk.signing.Signer;

import java.nio.charset.StandardCharsets;

public class NoVoteStrategy implements IVotingStrategy {

    private final int sleepMS;

    public NoVoteStrategy(int delayMS) {
        this.sleepMS = delayMS;
    }

    @Override
    public Vote castVote(VotingMatter votingMatter, Signer signer) {
        try {
            Thread.sleep(sleepMS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Vote v = new Vote(false, votingMatter.getHash(), signer.getPublicKey().hex());
        v.setSignature(signer.sign(v.getSignablePayload().getBytes(StandardCharsets.UTF_8)));
        return v;
    }
}
