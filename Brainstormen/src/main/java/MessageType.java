public enum MessageType {

    // "Protocol" Description:

    // First contact, contains the applicant id and the associated public key (sawtooth key).
    // - Additional information could be requested for the application (e.g. which agents do you know etc)
    // The goal is to bind the id to the public key creating an trusted entity in the network
    // The applicant wants its public key added to the sawtooth network
    JOIN_NETWORK_REQUEST,
    // When receiving a JOIN_NETWORK_REQUEST, the receiver sends a nonce, signature and their public key to the applicant
    // Signature is created for the nonce

    // When receiving a JOIN_NETWORK_REQUEST, a new thread is started that handles

    // The network member expects a JOIN_NETWORK_RESPONSE which contains the nonce and a signature for the nonce
    JOIN_NETWORK_RESPONSE,
    // If the application was successful, the applicants public key has to be added to the permitted keys for the sawtooth network.
    // To do that, a settings transaction has to be submitted, updating the policy the network role with the appl. pub key.


//      [ VOTING PROCESS MESSAGES ]

//      ![ VOTING PROCESS MESSAGES ]

    JOIN_NETWORK_VOTING_RESULT,
    // JOIN_NETWORK_VOTING_RESULT tells the applicant if it was added to the network

    // Now the applicant is part of the network and can request to join groups
    // POSSIBLY TRANSACTION FAMILY FOR GROUP_JOIN_REQUESTS TO DOCUMENT IT IN LEDGER
    JOIN_GROUP_REQUEST,

    // [VOTING PROCESS]

    JOIN_GROUP_RESPONSE;

}
// the receiving client starts the voting process OR if it is the only client decides
// by itself if the applicant can join

//      [ VOTING PROCESS MESSAGES ]

//      ![ VOTING PROCESS MESSAGES ]

// After the decision, the applicant is notified