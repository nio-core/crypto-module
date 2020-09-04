package voting;

import blockchain.SawtoothUtils;

import java.util.List;

/**
 * This evaluator just counts the votes that were actually cast. If some votes were not received in time by the {@link IVotingProcess},
 * it does not matter to this evaluator.
 */
public class SimpleMajorityEvaluator implements IVoteEvaluator {

    private final boolean doPrint = true;

    private final String clientID;

    public SimpleMajorityEvaluator(String clientID) {
        this.clientID = clientID;
    }

    @Override
    public boolean evaluateVotes(VotingResult votingResult) {
        // Evaluate the result
        //print("Evaluating voting results...");
        boolean approved = false;
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
        print("Vote result: YES-" + yes + " NO-" + no);
        approved = (yes > no);

        return approved;
    }

    void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "] [SimpleMajorityEvaluator][" + clientID + "]   " + message);
    }
}
