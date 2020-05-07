package message;

import client.HyperZMQ;
import diffiehellman.DHMessage;
import joingroup.JoinGroupRequest;
import keyexchange.ISignableMessage;
import keyexchange.KeyExchangeReceipt;
import keyexchange.ReceiptType;
import sawtooth.sdk.signing.Signer;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * This is not a real factory as of the GoF pattern, instead this class is used to centralize the creation of
 * the different message types used in this project as well as easing the addition of new ones.
 * Additionally it helps to ensure that messages that need to be signed are signed directly after creation.
 */
public class MessageFactory {

    private final Signer signer;

    public MessageFactory(Signer signer, HyperZMQ hyperZMQ) {
        this.signer = signer;
    }

    public JNChallengeMessage jnChallengeMessage(String memberPublicKey, String nonce, String memberID) {
        return signIfPossible(new JNChallengeMessage(memberPublicKey, nonce, memberID));
    }

    public JNRequestMessage jnRequestMessage(String applicantID, String applicantPublicKey) {
        return signIfPossible(new JNRequestMessage(applicantID, applicantPublicKey));
    }

    public JNResponseMessage jnResponseMessage(String nonce) {
        return signIfPossible(new JNResponseMessage(nonce));
    }

    public DHMessage dhMessage(String publicKey, String senderID) {
        return signIfPossible(new DHMessage(publicKey, senderID));
    }

    public JoinGroupRequest joinGroupRequest(String applicantPublicKey, String contactPublicKey, String groupName, List<String> votingArgs, String address, int port) {
        return signIfPossible(new JoinGroupRequest(applicantPublicKey, contactPublicKey, groupName, votingArgs, address, port));
    }

    public KeyExchangeReceipt keyExchangeReceipt(String memberPublicKey, String applicantPublicKey, ReceiptType receiptType,
                                                 @Nullable String group, long timestamp) {
        return signIfPossible(new KeyExchangeReceipt(memberPublicKey, applicantPublicKey, receiptType, group, timestamp));
    }

    private <T> T signIfPossible(T message) {
        if (message instanceof ISignableMessage) {
            ((ISignableMessage) message).setSignature(signer.sign(((ISignableMessage) message).getSignablePayload().getBytes(StandardCharsets.UTF_8)));
        }
        return message;
    }
}
