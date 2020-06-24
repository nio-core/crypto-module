package groups;

/**
 * These are the types of messages that an envelope can contain.
 * The envelope will be encrypted => the messages of these types are encrypted (inside groups)
 */
public enum MessageType {
    TEXT, // payload = string
    CONTRACT, // payload = Contract.class
    CONTRACT_RECEIPT, // payload = ContractReceipt.class
    VOTING_MATTER, // payload = VotingMatter.class
    VOTE, // payload = Vote.class
    PING_REQUEST, // payload = nonce
    PING_RESPONSE // payload = nonce, signature, publickey (of responder)
}
