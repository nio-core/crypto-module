package voting;

import sawtooth.sdk.signing.Signer;

import java.nio.charset.StandardCharsets;

public class YesVoteStrategy implements IVotingStrategy {

    private final int sleepMS;

    public YesVoteStrategy(int delayMS) {
        this.sleepMS = delayMS;
    }

    @Override
    public Vote castVote(VotingMatter votingMatter, Signer signer) {
        int sleep = (int) ((Math.random() * (sleepMS - 1)) + 1);

        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Vote v = new Vote(true, votingMatter.getHash(), signer.getPublicKey().hex());
        v.setSignature(signer.sign(v.getSignablePayload().getBytes(StandardCharsets.UTF_8)));
        return v;
    }
}
