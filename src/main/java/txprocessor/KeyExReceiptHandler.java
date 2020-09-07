package txprocessor;

import blockchain.SawtoothUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.ByteString;
import keyexchange.KeyExchangeReceipt;
import keyexchange.ReceiptType;
import sawtooth.sdk.processor.Context;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.signing.PublicKey;
import sawtooth.sdk.signing.Secp256k1Context;
import sawtooth.sdk.signing.Secp256k1PublicKey;

import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KeyExReceiptHandler implements TransactionHandler {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private static final String TRANSACTION_FAMILY_NAME = "KeyExchangeReceipt";
    private static final String TRANSACTION_FAMILY_VERSION = "0.1";
    private final String namespace = "ac0cab";
    private boolean doPrint = false;

    public KeyExReceiptHandler() {
        // Convention
        //namespace = SawtoothUtils.hash(transactionFamilyName()).substring(0, 6);
        print("Starting KeyExchangeReceiptTP with namespace '" + namespace + "'");
    }

    public String transactionFamilyName() {
        return TRANSACTION_FAMILY_NAME;
    }

    public String getVersion() {
        return TRANSACTION_FAMILY_VERSION;
    }

    public Collection<String> getNameSpaces() {
        return Collections.singletonList(namespace);
    }

    public void apply(TpProcessRequest transactionRequest, Context state) throws InvalidTransactionException, InternalError {
        // Payload is expected to be of KeyExchangeReceipt.class
        String payloadStr = transactionRequest.getPayload().toString(UTF_8);
        print("Got payload: " + payloadStr);
        if (payloadStr == null || payloadStr.isEmpty()) {
            print("Empty payload!");
            throw new InvalidTransactionException("Empty payload!");
        }

        // DEBUG FUNCTION //
        if (payloadStr.contains("CLEARALL,")) {
            String[] parts = payloadStr.split(",");
            if (parts.length > 1) {
                String addr = SawtoothUtils.namespaceHashAddress(namespace, parts[1]);
                print("[DEBUG] clearing contents of " + parts[1] + " at address " + addr);
                TPUtils.writeToAddress("", addr, state);
                return;
            }
        }
        // END DEBUG FUNCTION //

        KeyExchangeReceipt receipt;
        try {
            receipt = new Gson().fromJson(payloadStr, KeyExchangeReceipt.class);
        } catch (JsonSyntaxException e) {
            print("Malformed payload: " + payloadStr);
            throw new InvalidTransactionException("Malformed payload: " + payloadStr);
        }

        // Verify the member that shared a key is the one that submitted the KeyExchangeReceipt
        if (!transactionRequest.getHeader().getSignerPublicKey().equals(receipt.getMemberPublicKey())) {
            print("Member and signer key are different!\nsigner: "
                    + transactionRequest.getHeader().getSignerPublicKey()
                    + "\nreceipt: " + receipt.getMemberPublicKey());

            throw new InvalidTransactionException("Member and signer key are different!");
        }

        // Check integrity of receipt
        PublicKey publicKey = new Secp256k1PublicKey(
                SawtoothUtils.hexDecode(
                        receipt.getMemberPublicKey()
                ));

        if (receipt.getSignature() == null) {
            print("Unsigned receipt!");
            throw new InvalidTransactionException("Unsigned receipt!");
        }

        Secp256k1Context context = new Secp256k1Context();
        boolean verified = context.verify(
                receipt.getSignature(),
                receipt.getSignablePayload().getBytes(UTF_8),
                publicKey
        );

        if (!verified) {
            print("Signature verification failed!");
            throw new InvalidTransactionException("Receipt signature is invalid");
        }
        // TODO uncomment <<
        // Prepare the address to write to: hash(pubMember, pubJoiner, group), if JOIN_NETWORK without group
        String toHash = receipt.getMemberPublicKey() + receipt.getApplicantPublicKey();
        if (receipt.getReceiptType() == ReceiptType.JOIN_GROUP) {
            toHash += receipt.getGroup();
        }

        String address = SawtoothUtils.namespaceHashAddress(this.namespace, toHash);
        //print("toHash for address: " + toHash);
        print("Calculated Address: " + address);

        if (!TPUtils.writeToAddress(receipt.toString(), address, state)) {
            throw new InvalidTransactionException("Unable to write receipt to state");
        }

        // Update the entry that has the keys which are in the given group
        if (receipt.getReceiptType() == ReceiptType.JOIN_GROUP) {
            print("Receipt is of JOIN_GROUP type, updating group entry...");
            String groupAddress = SawtoothUtils.namespaceHashAddress(namespace, receipt.getGroup());
            print("Group address: " + groupAddress);
            List<String> entries = readKeysFromAddress(groupAddress, state);
            print("Entries at address before update: " + entries.toString());
            print("Adding " + receipt.getApplicantPublicKey());
            entries.add(receipt.getApplicantPublicKey());
            entries.add(receipt.getMemberPublicKey()); // double check, gets removed here vvv if it is already in it

            String strToWrite = listToCSV(entries);

            if (!TPUtils.writeToAddress(strToWrite, groupAddress, state)) {
                throw new InvalidTransactionException("Unable to update group member entry");
            }

            print("Keys after writing: " + readKeysFromAddress(groupAddress, state).toString());
        } else if (receipt.getReceiptType() == ReceiptType.LEAVE_GROUP) {
            String groupAddress = SawtoothUtils.namespaceHashAddress(namespace, receipt.getGroup());
            List<String> entries = readKeysFromAddress(groupAddress, state);
            String pubKeyToRemove = transactionRequest.getHeader().getSignerPublicKey();

            if (entries.remove(pubKeyToRemove)) {
                print("Removed " + pubKeyToRemove + " successfully.");
            } else {
                print("Could not find " + pubKeyToRemove + " in the list");
            }

            String strToWrite = listToCSV(entries);

            if (!TPUtils.writeToAddress(strToWrite, groupAddress, state)) {
                throw new InvalidTransactionException("Unable to update group member entry");
            }
        }
    }

    private String listToCSV(List<String> list) {
        String ret = list.stream().distinct().reduce("", (s1, s2) -> {
            if (s2.isEmpty()) return s1;
            return s1 += s2 + ",";
        });
        if (ret.length() > 0) {
            ret = ret.substring(0, (ret.length() - 1)); // remove trailing ','
        }
        return ret;
    }

    private ArrayList<String> readKeysFromAddress(String address, Context state) throws InternalError, InvalidTransactionException {
        // Get entries form address
        Map<String, ByteString> entries = state.getState(
                Collections.singletonList(address));

        ByteString bsEntry = entries.get(address);
        return bsEntry == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(bsEntry.toStringUtf8().split(",")));
    }

    private void print(String message) {
        if (doPrint)
            System.out.println("[KeyExchangeReceiptTP][" + sdf.format(Calendar.getInstance().getTime()) + "]  " + message);
    }
}


