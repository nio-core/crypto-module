package txprocessor;

import blockchain.SawtoothUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import groups.GroupMessage;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import joingroup.JoinRequest;
import sawtooth.sdk.processor.Context;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.protobuf.TransactionHeader;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CSVStringsHandler implements TransactionHandler {
    private final String namespace = "2f9d35";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    CSVStringsHandler() {
        // Convention
        //namespace = SawtoothUtils.hash(transactionFamilyName()).substring(0, 6);
        print("Starting CSVStringsTP with namespace '" + namespace + "'");
    }

    @Override
    public String transactionFamilyName() {
        return "csvstrings";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public Collection<String> getNameSpaces() {
        ArrayList<String> namespaces = new ArrayList<>();
        namespaces.add(namespace);
        return namespaces;
    }

    /**
     * A TRANSACTION CAN BE SENT TO THE TRANSACTION PROCESSOR MULTIPLE TIMES.
     * IT IS IMPORTANT THAT THE APPLY METHOD IS IDEMPOTENT!!!!
     * I.E. THE ADDRESS SHOULD ONLY RELY ON DATA FROM THE PAYLOAD/HEADER.
     *
     * @param tpProcessRequest tpProcessRequest
     * @param context          context
     * @throws InvalidTransactionException InvalidTransactionException
     * @throws InternalError               InternalError
     */
    @Override
    public void apply(TpProcessRequest tpProcessRequest, Context context) throws InvalidTransactionException, InternalError {
        // Decode the payload which is a CSV String or JoinGroupRequest in UTF-8
        if (tpProcessRequest.getPayload().isEmpty()) {
            throw new InvalidTransactionException("Payload is empty!");
        }

        String payloadStr = tpProcessRequest.getPayload().toString(UTF_8);
        print("Got payload: " + payloadStr);
        print("From: " + tpProcessRequest.getHeader().getSignerPublicKey());

        TransactionHeader header = tpProcessRequest.getHeader();

        // Check payload integrity
        String receivedHash = SawtoothUtils.hash(tpProcessRequest.getPayload().toString(UTF_8));
        if (!header.getPayloadSha512().equals(receivedHash)) {
            throw new InvalidTransactionException("Payload or Header is corrupted!");
        }

        if (payloadStr.contains("applicantPublicKey") && payloadStr.contains("contactPublicKey")) {
            // Its a JoinRequest
            try {
                JoinRequest request = new Gson().fromJson(payloadStr, JoinRequest.class);
                // First check if the applicant submitted the request
                //TODO
                if (!tpProcessRequest.getHeader().getSignerPublicKey().equals(request.getApplicantPublicKey())) {
                    print("JoinRequest: public keys do not match. The transaction is invalid.");
                    return;
                }

                print("Broadcasting JoinRequest");
                // Broadcast the the request via event subsystem
                Map.Entry<String, String> e = new AbstractMap.SimpleEntry<>("address", namespace);
                Collection<Map.Entry<String, String>> collection = Arrays.asList(e);
                try {
                    context.addEvent(request.getGroupName(), collection, tpProcessRequest.getPayload());
                } catch (InternalError internalError) {
                    internalError.printStackTrace();
                }

                // Nothing else to do?
            } catch (JsonSyntaxException ignored) {
            }
        } else {
            GroupMessage message = null;
            try {
                message = new Gson().fromJson(payloadStr, GroupMessage.class);
            } catch (JsonSyntaxException e) {
                // Have to stop now since we cannot interpret the payload
                throw new InvalidTransactionException("Invalid or malformed transaction payload: " + payloadStr);
            }

            if (message != null) {
                // Check if the message should be written
                if (message.doWriteToChain) {
                    if (!TPUtils.writeToAddress(message.payload, message.addressOnChain, context)) {
                        throw new InvalidTransactionException("Set state error");
                    }
                }

                // Check if it should be broadcasted
                if (message.doBroadcast) {
                    // the value cannot be null, but in some cases we dont care if it has a value (especially if it is not written anyways)
                    // so just translate to "null" as string
                    String eventAddr = message.addressOnChain != null ? message.addressOnChain : "null";
                    Map.Entry<String, String> e = new AbstractMap.SimpleEntry<>("address", eventAddr);
                    Collection<Map.Entry<String, String>> eventAttributes = Collections.singletonList(e);
                    print("Firing event with attributes: " + eventAttributes.toString() + ", Eventype: " + message.group);
                    try {
                        context.addEvent(message.group, eventAttributes, tpProcessRequest.getPayload());
                        //print("Event triggered");
                    } catch (InternalError internalError) {
                        internalError.printStackTrace();
                    }
                }
            }
        }
    }

    void print(String message) {
        System.out.println("[CSVStringTP][" + sdf.format(Calendar.getInstance().getTime()) + "]  " + message);
    }
}
