package joingroup;

public interface IJoinGroupStatusCallback {

    // info contains the contacts public key in hex
    final static int FOUND_CONTACT = 100000;

    // info contains the serialized request that was sent
    final static int REQUEST_SENT = 100001;

    // no value
    final static int STARTING_DIFFIE_HELLMAN = 100002;

    // no value
    final static int GETTING_KEY = 100003;

    // info contains the received secret key in Base64
    final static int KEY_RECEIVED = 100004;

    ///////////////////////////////////////////////////
    //              ERRORS                           //
    // contain no info, the error code is sufficient //
    final static int NO_CONTACT_FOUND = 200000;
    final static int VOTE_DENIED = 200001;
    final static int EMPTY_RESPONSE = 200002;
    final static int TIMEOUT = 200003;

    void joinGroupStatusCallback(int code, String info);
}
