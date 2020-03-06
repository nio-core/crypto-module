package transactionprocessors;

import sawtooth.sdk.processor.Context;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.TransactionProcessor;
import sawtooth.sdk.processor.Utils;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.TpProcessRequest;

import java.util.Collection;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KeyExchangeProcessor implements TransactionHandler {

    private static final String TRANSACTION_FAMILY_NAME = "KeyExchangeReceipt";
    private static final String TRANSACTION_FAMILY_VERSION = "0.1";
    private String _namespace;

    public KeyExchangeProcessor() {
        // Convention
        _namespace = Utils.hash512(transactionFamilyName().getBytes(UTF_8)).substring(0, 6);
        print("Starting KeyExchangeReceiptTP with namespace '" + _namespace + "'");
    }

    private void print(String s) {
        System.out.println("[KeyExchangeReceiptTP] " + s);
    }

    public String transactionFamilyName() {
        return TRANSACTION_FAMILY_NAME;
    }

    public String getVersion() {
        return TRANSACTION_FAMILY_VERSION;
    }

    public Collection<String> getNameSpaces() {
        return Collections.singletonList(_namespace);
    }

    public void apply(TpProcessRequest transactionRequest, Context state) throws InvalidTransactionException, InternalError {

    }

    public static void main(String[] args) {
        String url = "tcp://192.168.178.124:4004";
        if (args != null && args.length > 0) {
            url = args[0];
        }
        // Connect the transaction processor to the validator
        TransactionProcessor tp = new TransactionProcessor(url);
        // The handler implements the actual chaincode
        tp.addHandler(new KeyExchangeProcessor());
        Thread t = new Thread(tp);
        t.start();
    }
}
