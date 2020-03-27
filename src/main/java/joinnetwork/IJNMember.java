package joinnetwork;

/**
 * This is the interface for the "main" thread that spawns JoinNetworkThreads and acts as their callback for errors
 * and when
 */
public interface IJNMember {

    /**
     * After the applicant was authenticated with challenge-response, this method is called to indicate that voting is required
     *
     * @param applicantID
     * @param applicantPublicKey
     */
    void votingRequired(String applicantID, String applicantPublicKey);

    void error(Throwable t);


}
