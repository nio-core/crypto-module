package joingroup;

public interface IJoinGroupStatusCallback {

    final static int FOUND_CONTACT = 100000;
    // info contains the contacts public key in hex

    final static int REQUEST_SENT = 100001;
    // info contains the serialized request that was sent

    final static int STARTING_DIFFIE_HELLMAN = 100002;
    // no value

    final static int GETTING_KEY = 100003;
    // no value

    final static int KEY_RECEIVED = 100004;
    // info contains the received secret key in Base64

    void joinGroupStatusCallback(int code, String info);
}
