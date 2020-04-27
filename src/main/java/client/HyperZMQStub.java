package client;

import sawtooth.sdk.signing.Context;
import sawtooth.sdk.signing.PrivateKey;
import sawtooth.sdk.signing.PublicKey;
import sawtooth.sdk.signing.Secp256k1Context;

public class HyperZMQStub {

    PrivateKey privateKey;
    PublicKey publicKey;

    public HyperZMQStub() {
        Context context = new Secp256k1Context();
        privateKey = context.newRandomPrivateKey();
        publicKey = context.getPublicKey(privateKey);
    }

    PrivateKey getPrivateKey() {
        return privateKey;
    }

    PublicKey getPublicKey() {
        return publicKey;
    }

    void send() {

    }

    public String getClientID() {
        return "hyperzmq";
    }
}
