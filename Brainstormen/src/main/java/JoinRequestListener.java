import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class JoinRequestListener implements AutoCloseable {
    private KEManager _keManager;
    private String _address;
    //private AtomicBoolean _doListen = new AtomicBoolean(true);
    private boolean _doListen = true;

    private static final int NONCE_SIZE_IN_CHARS = 32;


    public JoinRequestListener(String address, KEManager _callback) {
        this._address = address;
        this._keManager = _callback;
        // one thread per instance
        Thread _thread = new Thread(this::startListening);
        _thread.start();
    }

    void setListening(boolean value) {
        //_doListen.set(value);
        _doListen = value;
    }

    private void startListening() {
        ZContext ctx = new ZContext();
        ZMQ.Socket subSocket = ctx.createSocket(ZMQ.PAIR);
        subSocket.connect(_address);
        System.out.println("[JoinRequestListener] starting to listen on " + _address);
        while (_doListen) {
            String recv = subSocket.recvStr();
            System.out.println("[JoinRequestListener] received:" + recv);
            // TODO parse message where?
            _keManager.newJoinRequest(recv);
            // Construct message
            Message message;
            try {
                message = new Gson().fromJson(recv, Message.class);
            } catch (JsonSyntaxException e) {
                //e.printStackTrace();
                System.out.println("Cannot deserialize message: " + recv);
                return;
            }
            // Handle message type
            switch (message.messageType) {
                case JOIN_NETWORK_REQUEST:
                    handleJoinRequest(message);
                    break;
                case JOIN_NETWORK_RESPONSE:
                    handleJoinRequestReponse(message);
                    break;
                default:
                    break;
            }

        }

    }

    private Message handleJoinRequest(Message message) {
        _keManager._joinRequestCallback.onNewJoinRequest(message.source, message.data.get("publickey"));

        // Send a reponse with a nonce to authenticate the applicant
        // Moreover, send our publickey and a signature for the nonce so the applicant can verify us

        String nonce = generateNonce();
        String signature = _keManager.sign(nonce);
        Map<String, String> data = new HashMap<>();
        data.put("publickey", _keManager._hyHyperZMQStub.getPublicKey().hex());
        data.put("nonce", nonce);
        data.put("signature", signature);
        return new Message(_keManager._clientID, MessageType.JOIN_NETWORK_RESPONSE, data);
    }

    private String generateNonce() {
        int leftLimit = 48; // '0'
        int rightLimit = 122; // 'z'

        return new SecureRandom().ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(NONCE_SIZE_IN_CHARS)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private Message handleJoinRequestReponse(Message message) {

    }

    @Override
    public void close() throws Exception {
        _doListen = false;
    }
}
