package messages;

import diffiehellman.DHMessage;
import joingroup.JoinRequest;
import keyexchange.ISignableMessage;
import keyexchange.Receipt;
import keyexchange.ReceiptType;
import sawtooth.sdk.signing.Signer;
import voting.JoinRequestType;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * This is not a real factory as of the GoF pattern, instead this class is used to centralize the creation of
 * the different message types used in this project as well as easing the addition of new ones.
 * Additionally it helps to ensure that messages that need to be signed are signed directly after creation.
 */
public class MessageFactory {

    private Signer signer;

    public MessageFactory(Signer signer) {
        this.signer = signer;
    }

    public void setSigner(Signer signer) {
        this.signer = signer;
    }

    public JNChallengeMessage jnChallengeMessage(String memberPublicKey, String nonce, String memberID) {
        return signIfPossible(new JNChallengeMessage(memberPublicKey, nonce, memberID));
    }

    public JNResponseMessage jnResponseMessage(String nonce) {
        return signIfPossible(new JNResponseMessage(nonce));
    }

    public DHMessage dhMessage(String publicKey, String senderID) {
        return signIfPossible(new DHMessage(publicKey, senderID));
    }

    public JoinRequest joinGroupRequest(String applicantPublicKey, String contactPublicKey, JoinRequestType type,
                                        String groupName, Map<String, String> votingArgs, String address, int port) {
        return signIfPossible(new JoinRequest(applicantPublicKey, contactPublicKey, type, groupName, votingArgs, address, port));
    }

    public Receipt keyExchangeReceipt(String applicantPublicKey, ReceiptType receiptType, String group) {
        return signIfPossible(new Receipt(signer.getPublicKey().hex(), applicantPublicKey, receiptType, group, System.currentTimeMillis()));
    }

    private <T> T signIfPossible(T message) {
        if (message instanceof ISignableMessage) {
            ((ISignableMessage) message).setSignature(signer.sign(((ISignableMessage) message).getSignablePayload().getBytes(StandardCharsets.UTF_8)));
        }
        return message;
    }
}
