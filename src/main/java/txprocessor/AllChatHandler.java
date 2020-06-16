package txprocessor;

import blockchain.SawtoothUtils;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import sawtooth.sdk.processor.Context;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.TpProcessRequest;

/**
 * This handler simply broadcasts the payload so that all clients that are in the blockchain network can communicate
 * If the network is using encryption of data in motion, this channel is guarded by that too
 */
public class AllChatHandler implements TransactionHandler {

    private final String VERSION = "0.1";
    private final String TRANSACTION_FAMILY_NAME = "AllChat";
    private final String namespace;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");


    public AllChatHandler() {
        this.namespace = SawtoothUtils.hash(transactionFamilyName()).substring(0, 6);
        print("Starting AllChatTP with namespace '" + namespace + "'");
    }

    @Override
    public String transactionFamilyName() {
        return TRANSACTION_FAMILY_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Collection<String> getNameSpaces() {
        return Collections.singletonList(namespace);
    }

    @Override
    public void apply(TpProcessRequest transactionRequest, Context state) throws InvalidTransactionException, InternalError {
        Map.Entry<String, String> e = new AbstractMap.SimpleEntry<>("sender", transactionRequest.getHeader().getSignerPublicKey());
        Collection<Map.Entry<String, String>> collection = Collections.singletonList(e);
        try {
            state.addEvent("AllChat", collection, transactionRequest.getPayload());
        } catch (InternalError internalError) {
            internalError.printStackTrace();
        }
    }

    void print(String message) {
        System.out.println("[AllChatTP][" + sdf.format(Calendar.getInstance().getTime()) + "]  " + message);
    }
}
