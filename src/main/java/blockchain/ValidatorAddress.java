package blockchain;

/**
 * This will be used if no address is set to #HyperZMQ.Builder
 */
public class ValidatorAddress {

    public static final String SAWTOOTH_BASE_ADDRESS = "192.168.178.55";

    public static final String VALIDATOR_URL_DEFAULT = "tcp://" + SAWTOOTH_BASE_ADDRESS + ":4004";
    public static final String REST_URL_DEFAULT = "http://" + SAWTOOTH_BASE_ADDRESS + ":8008";
}
