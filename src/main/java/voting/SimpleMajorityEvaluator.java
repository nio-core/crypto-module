package voting;

import blockchain.SawtoothUtils;

import java.util.List;

public class SimpleMajorityEvaluator implements IVoteEvaluator {

    private final boolean doPrint = true;

    private final List<String> allGroupMembers;
    private final boolean enforceCompleteVote;
    private final String clientID;

    public SimpleMajorityEvaluator(List<String> allGroupMembers, boolean enforceCompleteVote, String clientID) {
        this.allGroupMembers = allGroupMembers;
        this.enforceCompleteVote = enforceCompleteVote;
        this.clientID = clientID;
    }

    @Override
    public boolean evaluateVotes(VotingResult votingResult) {
        // Evaluate the result
        boolean approved = false;
        if (votingResult.getVotesSize() < allGroupMembers.size() && enforceCompleteVote) {
            print("Not enough votes received while enforceCompleteVote is on. Failing the vote");
        } else {
            // Count the votes
            int yes = 0, no = 0;
            List<String> desiredVoters = votingResult.getVotingMatter().getDesiredVoters();
            for (Vote v : votingResult.getVotes()) {
                // Verify the vote to make it count
                if (!desiredVoters.contains(v.getPublicKey())) {
                    print("VotingResult contained vote of unrequested entity: " + v.toString());
                    continue;
                }
                boolean verified = SawtoothUtils.verify(v.getSignablePayload(), v.getSignature(), v.getPublicKey());
                if (verified) {
                    if (v.isApproval())
                        yes++;
                    else
                        no++;
                } else {
                    print("Signature of vote is invalid:" + v.toString());
                }
            }

            approved = (yes > no);
        }

        return approved;
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "][SimpleMajorityEvaluator][" + clientID + "]   " + message);
    }
}
