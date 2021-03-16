package blockchain;

/**
 * Gather all config stuff and other "magic" numbers here
 */
public class GlobalConfig {

    public static final String SAWTOOTH_BASE_ADDRESS = "192.168.178.99";

    public static final String VALIDATOR_URL_DEFAULT = "tcp://" + SAWTOOTH_BASE_ADDRESS + ":4004";
    public static final String REST_URL_DEFAULT = "http://" + SAWTOOTH_BASE_ADDRESS + ":8008";

    public static final boolean PRINT_HYPERZMQ_MAIN = true;
    public static final boolean PRINT_BLOCKCHAIN_HELPER = false;
    public static final boolean PRINT_EVENT_HANDLER = false;
    public static final boolean PRINT_VOTE_MANAGER = false;
    public static final boolean PRINT_JOIN_NETWORK_MEMBER = false;
    public static final boolean PRINT_JOIN_NETWORK_APPLICANT = false;
    public static final boolean PRINT_GROUP_TRANSACTION_PROCESSOR = false;
    public static final boolean PRINT_RECEIPT_TRANSACTION_PROCESSOR = false;
    public static final boolean PRINT_DIFFIE_HELLMAN = false;
    public static final boolean PRINT_SAWTOOTH_UTILS = false;

    public static final int DEFAULT_VOTING_PARTICIPANTS_THRESHOLD = 100;

    public static final int KEY_EXCHANGE_FUTURE_TIMEOUT_MS = 25000;
    public static final int VOTING_PROCESS_TIMEOUT_MS = 5000;

    public static final int JOIN_REQUEST_QUEUE_POLL_TIMEOUT_MS = 1000;
    public static final int VOTE_REQUIRED_QUEUE_POLL_TIMEOUT_MS = 1000;
    public static final int VOTING_FINISHER_QUEUE_POLL_TIMEOUT_MS = 1000;

    public static final int BATCH_SENDER_QUEUE_POLL_TIMEOUT_MS = 1000;

    // Unused
    public static final int GROUP_PING_NONCE_SIZE = 20;
}
