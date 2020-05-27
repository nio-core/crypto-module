package txprocessor;

import blockchain.ValidatorAddress;
import sawtooth.sdk.processor.TransactionProcessor;

/**
 * Runs Transaction Handlers for the families CSVStrings, KeyExchangeReceipt and AllChat
 */
public class CombinedTP {
    public static void main(String[] args) {
        String url = ValidatorAddress.VALIDATOR_URL_DEFAULT;
        if (args != null && args.length > 0) {
            url = args[0];
        }
        // Connect the transaction processor to the validator
        TransactionProcessor tp = new TransactionProcessor(url);
        // The handler implements the actual chaincode
        tp.addHandler(new CSVStringsHandler());
        tp.addHandler(new KeyExReceiptHandler());
        tp.addHandler(new AllChatHandler());
        Thread t = new Thread(tp);
        t.start();
    }
}