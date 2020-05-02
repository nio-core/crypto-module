package voting;

import sawtooth.sdk.signing.Signer;

public class YesVoteStrategy implements IVotingStrategy {

    private int sleepMS;

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
        return new Vote(true, votingMatter.getHash(), signer.getPublicKey().hex(), signer);
    }
}
