package voting;

import java.util.List;

/**
 * This is the VotingProcess that is started when a voting is required.
 * Implementations can choose how they communicate with the participants
 */
public interface VotingProcess {
    boolean vote(VotingMatter votingMatter, List<String> desiredVoters);
}
