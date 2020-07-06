package joinnetwork;

public interface IJoinNetworkStatusCallback {
    void joinNetworkStatusCallback(int code, String info);

    // The join request was published on the specified address, info contains the serialized request
    final static int SENT_JOIN_REQUEST_ZMQ = 100000;

    // Ready to listen for a challenge to authenticate, no info
    final static int SERVER_SOCKET_SET_UP = SENT_JOIN_REQUEST_ZMQ + 1;

    // The challenge signature was verified, no info
    final static int SIGNATURE_VERIFIED = SERVER_SOCKET_SET_UP + 1;

    // The response to the challenge was sent, info contains the message
    final static int RESPONSE_SENT = SIGNATURE_VERIFIED + 1;

    // The authentication was successful, the KeyExchange will be started, no info
    final static int AUTHENTICATION_RESPONSE_OK = RESPONSE_SENT + 1;

    // The result of the voting process (whether the client may join the network)
    final static int ACCESS_GRANTED = AUTHENTICATION_RESPONSE_OK + 1; // info contains the validator address
    final static int ACCESS_DENIED = ACCESS_GRANTED + 1;


    ///////////////////////////////////////////////////
    //              ERRORS, no info                  //
    ///////////////////////////////////////////////////

    // The received challenge could not be deserialized, info contains what was received
    final static int MALFORMED_CHALLENGE_MESSAGE = 200000;

    // The received message contained an invalid signature - the peer could not be verified
    // info contains the message
    final static int SIGNATURE_VERIFICATION_FAILED = MALFORMED_CHALLENGE_MESSAGE + 1;

    // Some IOException occurred while networking, info contains the exception as string
    final static int IO_EXCEPTION_OCCURRED = SIGNATURE_VERIFICATION_FAILED + 1;

    // The key exchange could not be performed because the peer was not listening for incoming connections
    // info contains address : port
    final static int KEY_EXCHANGE_NO_PEER = IO_EXCEPTION_OCCURRED + 1;
}
