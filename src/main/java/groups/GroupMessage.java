package groups;

import blockchain.BlockchainHelper;
import blockchain.SawtoothUtils;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

/**
 * This object is deserialized by the Transaction Processor to extract the info of what to do with the transaction.
 * New instructions can be passed easily by expanding this class.
 */
public class GroupMessage {
    public final String group;
    public final String payload; // Payload is the encrypted envelope which is opaque to the Transaction Processor

    public final boolean doWriteToChain;
    public final String addressOnChain;
    public final boolean doBroadcast;

    public GroupMessage(String group, String payload, boolean doWriteToChain, boolean doBroadcast) {
        this.doWriteToChain = doWriteToChain;
        // If it should be written to the chain, generate an address for it, otherwise its null
        this.addressOnChain = doWriteToChain ? SawtoothUtils.namespaceHashAddress(BlockchainHelper.CSVSTRINGS_NAMESPACE, (group + payload)) : null;
        this.doBroadcast = doBroadcast;
        this.group = group;
        this.payload = payload;
    }

    public GroupMessage(String group, String payload, boolean doWriteToChain, String addressOnChain, boolean doBroadcast) {
        this.group = group;
        this.payload = payload;
        this.doWriteToChain = doWriteToChain;
        this.addressOnChain = addressOnChain;
        this.doBroadcast = doBroadcast;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public byte[] getBytes() {
        return toString().getBytes(StandardCharsets.UTF_8);
    }
}
