package joinnetwork;

/**
 * This is the interface for the thread that handles applicants that want to join the network
 */
public interface IJoinNetworkCallback {

    /**
     * After the applicant was authenticated with challenge-response, this method is called to indicate that voting is required
     *
     * @param applicantID
     * @param applicantPublicKey
     */
    void votingRequired(String applicantID, String applicantPublicKey);

    void error(Throwable t);


}
