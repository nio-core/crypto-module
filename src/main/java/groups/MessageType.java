package groups;

/**
 * These are the types of messages that an envelope can contain.
 * The envelope will be encrypted => the messages of these types are encrypted (inside groups)
 */
public enum MessageType {
    TEXT,
    CONTRACT,
    CONTRACT_RECEIPT,
    VOTING_MATTER,
    VOTE,
    PING_REQUEST, // payload = nonce
    PING_RESPONSE // payload = nonce, signature, publickey (of responder)
}
