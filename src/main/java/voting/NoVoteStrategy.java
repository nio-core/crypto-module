package voting;

import sawtooth.sdk.signing.Signer;

public class NoVoteStrategy implements IVotingStrategy {

    private int sleepMS;

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
        return new Vote(false, votingMatter.getHash(), signer.getPublicKey().hex(), signer);
    }
}
