package voting;

import sawtooth.sdk.signing.Signer;

public class MockVotingStrategy implements IVotingStrategy {

    @Override
    public Vote castVote(VotingMatter votingMatter, Signer signer) {
        return new Vote(true, votingMatter.getHash(), signer.getPublicKey().hex(), signer);
    }
}
