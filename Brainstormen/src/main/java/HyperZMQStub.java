import sawtooth.sdk.signing.Context;
import sawtooth.sdk.signing.PrivateKey;
import sawtooth.sdk.signing.PublicKey;
import sawtooth.sdk.signing.Secp256k1Context;

public class HyperZMQStub {

    PrivateKey _privateKey;
    PublicKey _publicKey;

    public HyperZMQStub() {
        Context context = new Secp256k1Context();
        _privateKey = context.newRandomPrivateKey();
        _publicKey = context.getPublicKey(_privateKey);
    }

    PrivateKey getPrivateKey() {
        return _privateKey;
    }

    PublicKey getPublicKey() {
        return _publicKey;
    }

    void send() {

    }
}
