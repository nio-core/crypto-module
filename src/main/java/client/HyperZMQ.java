package client;

import blockchain.BlockchainHelper;
import blockchain.EventHandler;
import blockchain.IAllChatReceiver;
import blockchain.PingHandler;
import blockchain.SawtoothUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import contracts.Contract;
import contracts.ContractReceipt;
import contracts.IContractProcessingCallback;
import contracts.IContractProcessor;
import diffiehellman.DHKeyExchange;
import diffiehellman.EncryptedStream;
import groups.Envelope;
import groups.GroupMessage;
import groups.IGroupCallback;
import groups.IGroupVoteReceiver;
import groups.MessageType;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import joingroup.IJoinGroupStatusCallback;
import joingroup.JoinRequest;
import keyexchange.KeyExchangeReceipt;
import keyexchange.ReceiptType;
import messages.MessageFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import sawtooth.sdk.protobuf.Transaction;
import sawtooth.sdk.signing.Secp256k1Context;
import sawtooth.sdk.signing.Secp256k1PrivateKey;
import sawtooth.sdk.signing.Signer;
import util.Utilities;
import voting.JoinRequestType;
import voting.Vote;
import voting.VotingMatter;
import zmq.io.mechanism.curve.Curve;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HyperZMQ implements AutoCloseable {

    private final EventHandler eventHandler;
    private final Crypto crypto;
    private final String clientID;
    private final List<IContractProcessor> contractProcessors = new ArrayList<>();
    private final Map<String, List<IGroupCallback>> textmessageCallbacks = new HashMap<>();
    private final Map<String, IContractProcessingCallback> contractCallbacks = new HashMap<>(); // key is the contractID
    final BlockchainHelper blockchainHelper;
    private final ZContext zContext = new ZContext();

    private final VoteManager voteManager;

    // Outlets for messages of Vote type received in groups
    private final List<IGroupVoteReceiver> groupVoteReceivers = new ArrayList<>();

    // Outlets for messages received in allchat
    private final List<IAllChatReceiver> allChatReceivers = new ArrayList<>();

    // All group names in this array are reserved and cannot be used to create a new group
    private static final List<String> reservedGroupNames = Arrays.asList("AllChat");

    // if this is set, passes all contract messages received in a group to all callbacks that are registered
    // also invokes the group callback with ContractReceipt additionally to the ReceiptCallback
    // (i.e. receipts for other clients will invoke group callbacks if this is set)
    // by default, the contract processing is done without invoking any callback
    private boolean passthroughAll = false;

    private final MessageFactory messageFactory;

    private final PingHandler pingHandler;

    /**
     * @param id               id
     * @param pathToKeyStore   path to a keystore file including .jks
     * @param keystorePassword password for the keystore
     * @param createNewStore   whether a new keystore should be created, if true a new signer (=blockchain identity)
     *                         and encryption key will be created
     */
    public HyperZMQ(String id, String pathToKeyStore, String keystorePassword, String dataFilePath, boolean createNewStore) {
        this.clientID = id;
        //_crypto = new Crypto(this, pathToKeyStore, keystorePassword.toCharArray(), createNewStore);

        this.crypto = new Crypto(this, pathToKeyStore, keystorePassword.toCharArray(), dataFilePath, createNewStore);
        this.eventHandler = new EventHandler(this);
        this.blockchainHelper = new BlockchainHelper(this);
        this.voteManager = new VoteManager(this);
        this.messageFactory = new MessageFactory(getSawtoothSigner());
        this.pingHandler = new PingHandler(this);
        // registerClient();
    }

    /**
     * Using the default keystore file path
     */
    public HyperZMQ(String id, String keystorePassword, boolean createNewStore) {
        this.clientID = id;
        this.crypto = new Crypto(this, keystorePassword.toCharArray(), createNewStore);
        this.eventHandler = new EventHandler(this);
        this.blockchainHelper = new BlockchainHelper(this);
        this.voteManager = new VoteManager(this);
        this.messageFactory = new MessageFactory(getSawtoothSigner());
        this.pingHandler = new PingHandler(this);
        // registerClient();
    }

    public void registerClient() {
        // Upon startup, send KeyExchangeReceipt for AllChat to have this client added the NetworkMembers list
        KeyExchangeReceipt receipt = messageFactory.keyExchangeReceipt(getSawtoothPublicKey(),
                getSawtoothPublicKey(),
                ReceiptType.JOIN_NETWORK,
                null,
                System.currentTimeMillis());

        sendKeyExchangeReceipt(receipt);
    }

    /**
     * Signs the message with the Sawtooth private key.
     *
     * @param message to sign
     * @return signature for the input message
     */
    public String sign(String message) {
        Secp256k1Context context = new Secp256k1Context();
        return crypto.getSigner().sign(message.getBytes(UTF_8));
    }

    /**
     * The the URL of the RestAPI to something else than localhost
     *
     * @param url base URL of the rest api
     */
    public void setRestAPIUrl(String url) {
        blockchainHelper.setRestAPIUrl(url);
    }

    /**
     * Create a socket with encryption set up as a server which binds.
     * (Other clients need this clients public key to create a socket which can
     * receive from the one created)
     *
     * @param type        type of socket
     * @param myKeysAlias alias for this clients key
     * @param addr        address to call bind for
     * @param context     context socket is bound to, can be null to use HyperZMQ's context
     * @return socket or null if error
     */
    public ZMQ.Socket makeServerSocket(int type, String myKeysAlias, String addr, ZContext context) {
        Keypair kp = crypto.getKeypair(myKeysAlias);
        if (kp == null) {
            System.out.println("No keys for alias " + myKeysAlias + "found!");
            return null;
        }

        ZMQ.Socket s;
        if (context == null) {
            s = zContext.createSocket(type);
        } else {
            s = context.createSocket(type);
        }

        s.setAsServerCurve(true);
        s.setCurvePublicKey(kp.publicKey.getBytes());
        s.setCurveSecretKey(kp.privateKey.getBytes());
        s.bind(addr);
        return s;
    }

    /**
     * Create a socket with encryption set up as client which connects
     *
     * @param type          type of socket
     * @param myKeysAlias   alias for this clients key
     * @param theirKeyAlias alias for the key of entity we want to receive from
     * @param addr          address to call connect for
     * @param context       context socket is bound to, can be null to use HyperZMQ's context
     * @return socket or null if error
     */
    public ZMQ.Socket makeClientSocket(int type, String myKeysAlias, String theirKeyAlias, String addr, ZContext context) {
        Keypair server = crypto.getKeypair(theirKeyAlias);
        if (server == null) {
            System.out.println("No keys for alias " + theirKeyAlias + "found!");
            return null;
        }
        Keypair client = crypto.getKeypair(myKeysAlias);
        if (client == null) {
            System.out.println("No keys for alias " + myKeysAlias + "found!");
            return null;
        }

        ZMQ.Socket s;
        if (context == null) {
            s = zContext.createSocket(type);
        } else {
            s = context.createSocket(type);
        }

        s.setCurvePublicKey(client.publicKey.getBytes());
        s.setCurveSecretKey(client.privateKey.getBytes());
        s.setCurveServerKey(server.publicKey.getBytes());
        s.connect(addr);
        return s;
    }

    /**
     * Generate a Keypair of 256bit key for the Curve25519 elliptic curve.
     * The keys are encoded in Z85 (a ascii85 variant).
     * The keypair is also added to the store.
     *
     * @return keypair
     */
    public Keypair generateZ85Keypair(String alias) {
        Curve curve = new Curve();
        String[] keys = curve.keypairZ85();
        Keypair kp = new Keypair(alias, keys[1], keys[0]);
        crypto.addKeypair(kp);
        return kp;
    }

    public void addForeignKeypair(String alias, String publicKey) {
        crypto.addKeypair(new Keypair(alias, null, publicKey));
    }

    /**
     * @param alias alias
     * @return keypair or null if not found
     */
    public Keypair getKeypair(String alias) {
        return crypto.getKeypair(alias);
    }

    public boolean removeKeypair(String alias) {
        return crypto.removeKeypair(alias);
    }

    public boolean isGroupAvailable(String groupName) {
        return crypto.hasKeyForGroup(groupName);
    }

    /**
     * Query the given address of the global state.
     * DEPRECATED - USE ZMQ VARIANT INSTEAD
     *
     * @param addr address to query (70 hex chars)
     * @return response or null if error
     */
    public Envelope queryStateAddress(String addr) {
        try {
            // The data is stored in Base64, decode it first
            String raw = blockchainHelper.queryStateAddress(addr);
            if (raw == null) {
                return null;
            }
            byte[] bytes = Base64.getDecoder().decode(raw);
            String data = new String(bytes, UTF_8);

            // Now we have <group>,<decrypted msg>
            String[] strings = data.split(",");
            if (strings.length < 2) {
                System.out.println("Queried data format is incorrect: " + data);
                return null;
            }
            if (!crypto.hasKeyForGroup(strings[0])) {
                System.out.println("Can't decrypt queried data, because key is missing for group: " + strings[0]);
                return null;
            }

            try {
                String clearText = crypto.decrypt(strings[1], strings[0]);
                return new Gson().fromJson(clearText, Envelope.class);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                return null;
            } catch (JsonSyntaxException e) {
                System.out.println("Queried data could not be deserialized to Envelope");
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

    /**
     * Set to true to get a callback for ALL messages in groups (incl contracts and receipts), not just text.
     * If true, the callback's message can contain Contract and ContractReceipt objects in serialized form (JSON).
     *
     * @param value value to set
     */
    void setPassthroughAllMessages(boolean value) {
        passthroughAll = value;
    }

    /**
     * Send a single message to a group
     * Builds batch list with a single batch with a single transaction in it.
     *
     * @param groupName group
     * @param message   message
     */
    public boolean sendTextToChain(String groupName, String message) {
        if (groupName == null || message == null || groupName.isEmpty() || message.isEmpty()) {
            print("Empty group and/or message!");
            return false;
        }
        // Wrap the message - the complete envelope will be encrypted
        Envelope envelope = new Envelope(clientID, MessageType.TEXT, message);
        return sendSingleEnvelope(groupName, envelope, null);
    }

    /**
     * Send multiple messages in a group
     * Builds a batch list with a single batch with multiple transactions in it
     *
     * @param groupName groups name
     * @param messages  messages
     */
    public boolean sendTextsToChain(String groupName, List<String> messages) {
        if (groupName == null || messages == null || groupName.isEmpty() || messages.isEmpty()) {
            print("Empty group and/or message!");
            return false;
        }
        return sendTextsToChain(Collections.singletonMap(groupName, messages));
    }

    /**
     * Send multiple messages in multiple groups
     * Builds a batch list with a single batch with multiple transactions in it
     *
     * @param map map of group with their corresponding messages
     */
    public boolean sendTextsToChain(Map<String, List<String>> map) {
        if (map == null || map.isEmpty()) {
            print("Empty map!");
            return false;
        }
        Map<String, List<Envelope>> list = new HashMap<>();

        map.forEach((group, messages) -> {
            List<Envelope> envelopeList = new ArrayList<>();
            for (String message : messages) {
                envelopeList.add(new Envelope(clientID, MessageType.TEXT, message));
            }
            list.put(group, envelopeList);
        });
        return sendEnvelopeList(list);
    }

    private boolean sendSingleEnvelope(String group, Envelope envelope, String outputAddr) {
        String payload = encryptEnvelope(group, envelope);

        GroupMessage groupMessage = null;
        if (outputAddr == null) {
            groupMessage = new GroupMessage(group, payload, true, true);
        } else {
            groupMessage = new GroupMessage(group, payload, true, outputAddr, true);
        }

        List<Transaction> transactionList =
                Collections.singletonList(
                        blockchainHelper.
                                csvStringsTransaction(groupMessage.getBytes(), outputAddr));

        return blockchainHelper.buildAndSendBatch(transactionList);
    }

    private boolean sendEnvelopeList(Map<String, List<Envelope>> list) {
        List<Transaction> transactionList = new ArrayList<>();
        list.forEach((groupName, envelopeList) -> {
            envelopeList.forEach((envelope -> {
                String payload = encryptEnvelope(groupName, envelope);
                // TODO write to chain / broadcast behavior
                GroupMessage groupMessage = new GroupMessage(groupName, payload, true, true);
                transactionList.add(
                        blockchainHelper.
                                csvStringsTransaction(groupMessage.getBytes()));
            }));
        });

        return blockchainHelper.buildAndSendBatch(transactionList);
    }

    public boolean sendContractToChain(String groupName, Contract contract, IContractProcessingCallback callback) {
        if (callback != null) {
            contractCallbacks.put(contract.getContractID(), callback);
        }
        return sendContractToChain(groupName, contract);
    }

    /**
     * Send a list of contracts and callbacks (can be null) in a group
     *
     * @param groupName group name
     * @param map       map of contracts and callback
     * @return success
     */
    public boolean sendContractsToChain(String groupName, Map<Contract, IContractProcessingCallback> map) {
        if (groupName == null || map.isEmpty()) {
            print("Empty group and/or contracts");
            return false;
        }
        List<Envelope> envelopes = new ArrayList<>();
        map.forEach((contract, callback) -> {
            Envelope e = new Envelope(clientID, MessageType.CONTRACT, contract.toString());
            envelopes.add(e);
            if (callback != null) {
                contractCallbacks.put(contract.getContractID(), callback);
            }
        });
        return sendEnvelopeList(Collections.singletonMap(groupName, envelopes));
    }

    public boolean sendContractToChain(String groupName, Contract contract) {
        if (groupName == null || contract == null) {
            print("Empty group and/or contract!");
            return false;
        }
        // Wrap the contract - the complete envelope will be encrypted
        Envelope envelope = new Envelope(clientID, MessageType.CONTRACT, contract.toString());
        return sendSingleEnvelope(groupName, envelope, contract.getOutputAddr());
    }

    private boolean sendReceiptToChain(String groupName, ContractReceipt receipt, String resultOutputAddr) {
        if (groupName == null || receipt == null) {
            print("Empty group and/or receipt!");
            return false;
        }

        Envelope envelope = new Envelope(clientID, MessageType.CONTRACT_RECEIPT, receipt.toString());
        //System.out.println("Sending receipt to addr: " + resultOutputAddr);
        return sendSingleEnvelope(groupName, envelope, resultOutputAddr);
    }

    public void addContractProcessor(IContractProcessor contractProcessor) {
        contractProcessors.add(contractProcessor);
    }

    public void removeContractProcessor(IContractProcessor contractProcessor) {
        contractProcessors.remove(contractProcessor);
    }

    /**
     * Create a new group. Generates a new secret key which can be accessed by getKeyForGroup afterwards.
     *
     * @param groupName group name
     * @param callback  callback that is called when a new message arrives
     */
    public void createGroup(String groupName, IGroupCallback callback) {
        if (reservedGroupNames.contains(groupName)) {
            throw new IllegalArgumentException("The group name is reserved, try another one");
        }

        crypto.createGroup(groupName);
        if (callback != null) {
            putCallback(groupName, callback);
        }
        eventHandler.subscribeToGroup(groupName);

        // Create a receipt to update the entry in the blockchain of who is in the group
        KeyExchangeReceipt receipt = new KeyExchangeReceipt(getSawtoothPublicKey(),
                getSawtoothPublicKey(),
                ReceiptType.JOIN_GROUP,
                groupName,
                System.currentTimeMillis());

        receipt.setSignature(sign(receipt.getSignablePayload()));
        sendKeyExchangeReceipt(receipt);
    }

    /**
     * Create a new group. Generates a new secret key which can be accessed by getKeyForGroup afterwards.
     *
     * @param groupName group name
     */
    public void createGroup(String groupName) {
        createGroup(groupName, null);
    }

    /**
     * Add a group with an external key.
     * The group name has to be identical with the one that messages are sent to.
     *
     * @param groupName name of the group
     * @param key       the key in Base64
     * @param callback  callback to be called when a new messages arrives
     */
    public void addGroup(String groupName, String key, IGroupCallback callback) {
        crypto.addGroup(groupName, key);
        if (callback != null) {
            putCallback(groupName, callback);
        }
        eventHandler.subscribeToGroup(groupName);
    }

    /**
     * Add a group with an external key.
     * The group name has to be identical with the one that messages are sent to.
     *
     * @param groupName name of the group
     * @param key       the key in Base64
     */
    public void addGroup(String groupName, String key) {
        addGroup(groupName, key, null);
    }

    /**
     * Add a new callback to a group
     *
     * @param groupName group to add the callback to
     * @param callback  callback to add
     * @return true if successful, false if error or already existent
     */
    public boolean addCallbackToGroup(String groupName, IGroupCallback callback) {
        return putCallback(groupName, callback);
    }

    /**
     * ALL CALLBACKS ARE INVALIDATED WHEN THE GROUP IS REMOVED
     *
     * @param groupName name of group to remove key and callbacks for
     */
    public void removeGroup(String groupName) {
        crypto.removeGroup(groupName);
        textmessageCallbacks.remove(groupName);
    }

    /**
     * List of known group names for which the secret key is present
     *
     * @return list of names
     */
    public List<String> getGroupNames() {
        return crypto.getGroupNames();
    }

    /**
     * Returns the secret key for the specified group
     *
     * @param groupName group name to return key for
     * @return key in Base64 format or null if not found
     */
    public String getKeyForGroup(String groupName) {
        return crypto.getKeyForGroup(groupName);
    }

    /**
     * Receives the message from the blockchain.EventHandler. The message is not decrypted yet.
     * TODO synchronized not needed anymore?
     *
     * @param group            group name
     * @param encryptedMessage encrypted message
     */
    public synchronized void newEventReceived(String group, String encryptedMessage) {
        String plainMessage;
        try {
            plainMessage = crypto.decrypt(encryptedMessage, group);
            //logprint("New message in group '" + group + "': " + plainMessage);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return;
        } catch (IllegalStateException e) {
            print("Received a message in a group for which a key is not present. Message: (" + group + "," + encryptedMessage + ")");
            return;
        }
        Envelope envelope = new Gson().fromJson(plainMessage, Envelope.class);
        // TODO PROCESS NEW MESSAGE TYPES HERE
        switch (envelope.getType()) {
            case CONTRACT: {
                handleContractMessage(group, envelope);
                if (passthroughAll) {
                    handleTextMessage(group, envelope);
                }
                break;
            }
            case TEXT: {
                handleTextMessage(group, envelope);
                break;
            }
            case CONTRACT_RECEIPT: {
                handleContractReceipt(group, envelope);
                if (passthroughAll) {
                    handleTextMessage(group, envelope);
                }
                break;
            }
            case VOTING_MATTER: {
                handleVotingMatter(group, envelope);
                // TODO maybe not do this?
                /*if (passthroughAll) {
                    handleTextMessage(group, envelope);
                } */
                break;
            }
            case VOTE: {
                // Pass the vote to the outlet if one is available
                if (!groupVoteReceivers.isEmpty()) {
                    Vote vote = SawtoothUtils.deserializeMessage(envelope.getRawMessage(), Vote.class);
                    if (vote != null) {
                        for (IGroupVoteReceiver groupVoteReceiver : groupVoteReceivers) {
                            groupVoteReceiver.voteReceived(vote, group);
                        }
                    }
                }
                break;
            }
            default:
                print("Unknown message type: " + envelope.getType());
                break;
        }
    }

    /**
     * CALL THIS METHOD WHEN FINISHED WITH THIS INSTANCE AND YOU STILL WANT TO CONTINUE.
     * STOPS ALL THREADS RELATED TO THIS INSTANCE
     */
    @Override
    public void close() throws Exception {
        eventHandler.close();
    }

    /**
     * Return the public key of the current Sawtooth entity.
     * Can be used to regulate permissions on the validator.
     * (Could be denied or allowed)
     *
     * @return public key of the current entity in hex encoding
     */
    public String getSawtoothPublicKey() {
        // vvv can be used for better readability when debugging
        //return clientID + "-PublicKey";
        return crypto.getSawtoothPublicKey();
    }

    private void handleContractReceipt(String group, Envelope envelope) {
        ContractReceipt receipt;
        try {
            receipt = new Gson().fromJson(envelope.getRawMessage(), ContractReceipt.class);
        } catch (JsonSyntaxException e) {
            print("Cannot convert to ContractReceipt: " + envelope.getRawMessage());
            return;
        }
        IContractProcessingCallback cb = contractCallbacks.get(receipt.getContract().getContractID());
        if (cb != null) {
            cb.processingFinished(receipt);
        }
    }

    private void handleContractMessage(String group, Envelope envelope) {
        Contract contract;
        try {
            contract = new Gson().fromJson(envelope.getRawMessage(), Contract.class);
        } catch (JsonSyntaxException e) {
            print("Could not extract contract from envelope: " + envelope.toString());
            return;
        }
        if (clientID.equals(contract.getRequestedProcessor()) || Contract.REQUESTED_PROCESSOR_ANY.equals(contract.getRequestedProcessor())) {
            // Find a processor to handle the message
            Object result = null;
            for (IContractProcessor processor : contractProcessors) {
                if (processor.getSupportedOperations().contains(contract.getOperation())) {
                    result = processor.processContract(contract);
                    if (result != null) {
                        break;
                    }
                }
            }
            if (result == null) {
                print("No processor found for contract: " + contract.toString());
                return;
            }
            //
            //logprint("Contract processed with result: " + result);

            // Process the result: Build a receipt to send back
            ContractReceipt receipt = new ContractReceipt(clientID, String.valueOf(result), contract);
            sendReceiptToChain(group, receipt, contract.getResultOutputAddr());
        } else {
            //logprint("Contract was not for this client: " + contract);
        }

    }

    private void handleTextMessage(String group, Envelope envelope) {
        // Send the message to all subscribers of that group
        List<IGroupCallback> list = textmessageCallbacks.get(group);
        if (list != null) {
            //logprint("Callback(s) found for the group...");
            list.forEach(c -> c.newMessageOnChain(group, envelope.getRawMessage(), envelope.getSender()));
        }
    }

    private boolean putCallback(String groupName, IGroupCallback callback) {
        //logprint("New subscription for group: " + groupName);
        if (textmessageCallbacks.containsKey(groupName)) {
            List<IGroupCallback> list = textmessageCallbacks.get(groupName);
            if (list.contains(callback)) {
                //logprint("Subscription skipped, callback already registered.");
                return false;
            } else {
                return list.add(callback);
                //logprint("Subscription completed, callback registered to existing group.");
            }
        } else {
            List<IGroupCallback> newList = new ArrayList<>();
            newList.add(callback);
            textmessageCallbacks.put(groupName, newList);
            return true;
            //logprint("Subscription completed, new group created.");
        }
    }

    String encryptEnvelope(String group, Envelope envelope) {
        // Create the payload in CSV format
        // The group stays in clearText so clients attempting to decrypt can know if they can without trial and error
        StringBuilder msgBuilder = new StringBuilder();
        // Encrypt the whole message
        try {
            msgBuilder.append(crypto.encrypt(envelope.toString(), group));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            print("Message will not be send.");
            return null;
        } catch (IllegalStateException e) {
            print("Trying to encrypt for group for which the key is not present (" + group + "). Message will not be send.");
            return null;
        }
        return msgBuilder.toString();
    }

    void print(String message) {
        System.out.println("[" + Thread.currentThread().getId() + "] [HyperZMQ]" + "[" + clientID + "]  " + message);
    }

    public void setValidatorURL(String url) {
        eventHandler.setValidatorURL(url);
    }

    // vvv NEW WITH KEY EXCHANGE vvv

    public void sendKeyExchangeReceipt(KeyExchangeReceipt receipt) {
        Objects.requireNonNull(receipt);
        Transaction t = blockchainHelper.keyExchangeReceiptTransaction(receipt);
        blockchainHelper.buildAndSendBatch(Collections.singletonList(t));
    }

    public void sendKeyExchangeReceiptFor(String groupName, String applicantPublicKey) {
        Objects.requireNonNull(groupName);
        Objects.requireNonNull(applicantPublicKey);

        if (!isGroupAvailable(groupName)) {
            throw new IllegalArgumentException("Cannot create receipt for group you do not have key for");
        }

        KeyExchangeReceipt receipt = messageFactory.keyExchangeReceipt(this.getSawtoothPublicKey(),
                applicantPublicKey,
                ReceiptType.JOIN_GROUP,
                groupName,
                System.currentTimeMillis());

        Transaction t = blockchainHelper.keyExchangeReceiptTransaction(receipt);
        blockchainHelper.buildAndSendBatch(Collections.singletonList(t));
    }

    public ArrayList<String> getNetworkMembers() {
        String address = SawtoothUtils.namespaceHashAddress(BlockchainHelper.KEY_EXCHANGE_RECEIPT_NAMESPACE, "AllChat");
        print("Getting members of network at address " + address);
        String resp = blockchainHelper.getStateZMQ(address);
        if (resp == null) return null;
        //print("getGroupMembers: " + resp);
        return new ArrayList<>(Arrays.asList(resp.split(",")));
    }

    /**
     * Send a ping in the group and collect ping responses for the given time.
     * This method blocks for at least the given time.
     *
     * @param groupName group to ping
     * @param timeInMs  time to run this for
     * @return list of publickey of entities that responded and were verified, empty list if no responses
     */
    public ArrayList<String> getGroupMembersByPing(String groupName, int timeInMs) {
        if (!isGroupAvailable(groupName)) {
            print("Cant ping members of group that the client is not part of!");
            return new ArrayList<>();
        }

        // generate some nonce that identifies this ping request
        String nonce = Utilities.generateNonce(20);
        Envelope envelope = new Envelope(this.clientID, MessageType.PING_REQUEST, nonce);

        // Send the ping request to the group
        GroupMessage groupMessage = new GroupMessage(groupName, encryptEnvelope(groupName, envelope), false, true);
        Transaction t = blockchainHelper.csvStringsTransaction(groupMessage.getBytes());
        blockchainHelper.buildAndSendBatch(Collections.singletonList(t));

        // Let the ping handler collect the ping responses for the given time and return
        return pingHandler.getGroupMembersByPing(nonce, timeInMs);
    }

    public ArrayList<String> getGroupMembersFromReceipts(String groupName) {
        Objects.requireNonNull(groupName);
        /* TODO
        if (!groupIsAvailable(groupName)) {
            throw new IllegalArgumentException("Cant get members of group that the client is not part of!");
        }
        */
        String address = SawtoothUtils.namespaceHashAddress(BlockchainHelper.KEY_EXCHANGE_RECEIPT_NAMESPACE, groupName);
        print("Getting members of group '" + groupName + "' at address " + address);
        String resp = blockchainHelper.getStateZMQ(address);
        if (resp == null) return null;
        //print("getGroupMembers: " + resp);
        return new ArrayList<>(Arrays.asList(resp.split(",")));
    }

    public KeyExchangeReceipt getKeyExchangeReceipt(String memberPublicKey, String applicantPublicKey, @Nullable String group) {
        Objects.requireNonNull(memberPublicKey);
        Objects.requireNonNull(applicantPublicKey);
        String toHash = memberPublicKey + applicantPublicKey;
        if (group != null) {
            toHash += group;
        }

        String address = SawtoothUtils.namespaceHashAddress(BlockchainHelper.KEY_EXCHANGE_RECEIPT_NAMESPACE, toHash);
        print("Getting receipt at address: " + address);
        String recv = blockchainHelper.getStateZMQ(address);
        try {
            return new Gson().fromJson(recv, KeyExchangeReceipt.class);
        } catch (JsonSyntaxException e) {
            print("Could not deserialize receipt: " + recv);
        }
        return null;
    }

    public String getClientID() {
        return clientID;
    }

    // Debug function: changes sawtooth identity to the given key
    public void setPrivateKey(String privateKeyHex) {
        Objects.requireNonNull(privateKeyHex);
        crypto.setPrivateKey(new Secp256k1PrivateKey(SawtoothUtils.hexDecode(privateKeyHex)));
        messageFactory.setSigner(new Signer(new Secp256k1Context(), new Secp256k1PrivateKey(SawtoothUtils.hexDecode(privateKeyHex))));
    }

    public Signer getSawtoothSigner() {
        return crypto.getSigner();
    }

    public void tryJoinGroup2(@Nonnull String groupName, @Nonnull String address, @Nonnull int port, @Nullable Map<String, String> additionalInfo,
                              @Nullable IJoinGroupStatusCallback callback, @Nullable String contactPublicKey) {
        Objects.requireNonNull(groupName);
        String realContactKey = contactPublicKey;
        if (realContactKey == null) {
            // If no contact is given, get the client responsible for the group by checking the entry
            List<String> members = getGroupMembersFromReceipts(groupName);
            if (members.isEmpty()) {
                notifyCallback(IJoinGroupStatusCallback.NO_CONTACT_FOUND,
                        "No contact could be found from the blockchain", callback);
                print(groupName + " does not have any members. Try creating the group.");
                return;
            }

            realContactKey = members.get(members.size() - 1); // Last one to join the group is responsible
        }
        notifyCallback(IJoinGroupStatusCallback.FOUND_CONTACT, realContactKey, callback);

        JoinRequest request = new JoinRequest(getSawtoothPublicKey(),
                realContactKey,
                JoinRequestType.GROUP,
                groupName,
                additionalInfo,
                address,
                port);

        Transaction transaction = blockchainHelper.
                csvStringsTransaction(request.toString().getBytes(UTF_8));

        blockchainHelper.buildAndSendBatch(Collections.singletonList(transaction));
        notifyCallback(IJoinGroupStatusCallback.REQUEST_SENT, request.toString(), callback);
    }

    public void waitForResponse(String address, int port, String realContactKey) {
        Thread t = new Thread(() -> {
            DHKeyExchange exchange = new DHKeyExchange(clientID,
                    getSawtoothSigner(), realContactKey, address, port, true);
            try {
                EncryptedStream stream = exchange.call();
                String s = stream.readLine();
                System.out.println("Received from encrypted stream: " + s);
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
        t.start();
    }

    /**
     * @see HyperZMQ#tryJoinGroup(String, String, int, Map, IJoinGroupStatusCallback, String)
     */
    public void tryJoinGroup(@Nonnull String groupName, @Nonnull String address, @Nonnull int port,
                             @Nullable Map<String, String> additionalInfo, @Nullable IJoinGroupStatusCallback callback) {
        tryJoinGroup(groupName, address, port, additionalInfo, callback, null);
    }

    /**
     * @param groupName
     * @param address
     * @param port
     * @param additionalInfo
     * @param callback
     * @param contactPublicKey
     */
    public void tryJoinGroup(@Nonnull String groupName, @Nonnull String address, @Nonnull int port,
                             @Nullable Map<String, String> additionalInfo,
                             @Nullable IJoinGroupStatusCallback callback, @Nullable String contactPublicKey) {
        Objects.requireNonNull(groupName);
        Thread t = new Thread(() -> {
            String realContactKey = contactPublicKey;
            if (realContactKey == null) {
                // If no contact is given, get the client responsible for the group by checking the entry
                List<String> members = getGroupMembersFromReceipts(groupName);
                if (members.isEmpty()) {
                    notifyCallback(IJoinGroupStatusCallback.NO_CONTACT_FOUND,
                            "No contact could be found from the blockchain", callback);
                    print(groupName + " does not have any members. Try creating the group.");
                    return;
                }

                realContactKey = members.get(members.size() - 1); // Last one to join the group is responsible
            }
            notifyCallback(IJoinGroupStatusCallback.FOUND_CONTACT, realContactKey, callback);

            JoinRequest request = new JoinRequest(getSawtoothPublicKey(),
                    realContactKey,
                    JoinRequestType.GROUP,
                    groupName,
                    additionalInfo,
                    address,
                    port);

            Transaction transaction = blockchainHelper.
                    csvStringsTransaction(request.toString().getBytes(UTF_8));

            blockchainHelper.buildAndSendBatch(Collections.singletonList(transaction));
            notifyCallback(IJoinGroupStatusCallback.REQUEST_SENT, request.toString(), callback);

            // Now wait for the contact to perform key exchange
            FutureTask<EncryptedStream> server = new FutureTask<EncryptedStream>(new DHKeyExchange(clientID,
                    getSawtoothSigner(), realContactKey, address, port, true));
            notifyCallback(IJoinGroupStatusCallback.STARTING_DIFFIE_HELLMAN, null, callback);
            new Thread(server).start();
            print("TryJoinGroup - Waiting for response");
            try (EncryptedStream stream = server.get(25000, TimeUnit.MILLISECONDS)) {
                notifyCallback(IJoinGroupStatusCallback.GETTING_KEY, null, callback);
                String received = stream.readLine();
                if (received == null) {
                    notifyCallback(IJoinGroupStatusCallback.EMPTY_RESPONSE, callback);
                } else if (received.equals("Vote denied")) {
                    notifyCallback(IJoinGroupStatusCallback.VOTE_DENIED, callback);
                } else {
                    notifyCallback(IJoinGroupStatusCallback.KEY_RECEIVED, received, callback);
                    addGroup(request.getGroupName(), received);
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                notifyCallback(IJoinGroupStatusCallback.TIMEOUT, callback);
                //e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();
    }

    /**
     * Send a message in the AllChat, which is broadcasted to all participants in the blockchain
     *
     * @param message message
     */
    public void sendAllChat(String message) {
        Transaction t = blockchainHelper.allChatTransaction(message.getBytes(UTF_8));
        blockchainHelper.buildAndSendBatch(Collections.singletonList(t));
    }

    public void handleAllChatMessage(String message, String sender) {
        // Only check for votingMatter type messages to handle automatically
        // Vote type messages should be passed to all registered receivers because they might use it for
        // custom voting processes

        if (message.contains("voteDirectorPublicKey") && message.contains("desiredVoters")) {
            VotingMatter votingMatter = Utilities.deserializeMessage(message, VotingMatter.class);
            if (votingMatter != null) {
                voteManager.addVoteRequired(votingMatter);
            }
        } else {
            // Default procedure - notify all
            System.out.println("NOTIFIYING ALLCHAT RECEIVERS");
            for (IAllChatReceiver receiver : allChatReceivers) {
                receiver.allChatMessageReceived(message, sender);
            }
        }
    }

    /**
     * Encapsulate the call on the callback that is possibly null.
     * If it is null this method does nothing
     *
     * @param message  message
     * @param callback callback
     */
    private void notifyCallback(int code, @Nullable String message, @Nullable IJoinGroupStatusCallback callback) {
        if (callback != null)
            callback.joinGroupStatusCallback(code, message);
    }

    /**
     * @see HyperZMQ#notifyCallback(int, String, IJoinGroupStatusCallback)
     */
    private void notifyCallback(int code, @Nullable IJoinGroupStatusCallback callback) {
        notifyCallback(code, null, callback);
    }

    public void sendVotingMatterInGroup(VotingMatter votingMatter) {
        Objects.requireNonNull(votingMatter);
        if (!isGroupAvailable(votingMatter.getJoinRequest().getGroupName())) {
            throw new IllegalArgumentException("The group specified in the VotingMatter is not available on this client!");
        }

        Envelope envelope = new Envelope(this.clientID, MessageType.VOTING_MATTER, votingMatter.toString());

        String payload = encryptEnvelope(votingMatter.getJoinRequest().getGroupName(), envelope);
        // TODO write to chain behavior?
        GroupMessage groupMessage = new GroupMessage(votingMatter.getJoinRequest().getGroupName(), payload, true, true);

        Transaction t = blockchainHelper.csvStringsTransaction(groupMessage.getBytes());
        print("Sending VotingMatter in group: " + groupMessage.toString());
        blockchainHelper.buildAndSendBatch(Collections.singletonList(t));
    }

    private void handleVotingMatter(String group, Envelope envelope) {
        print("HandleVotingMatter: " + envelope.toString());
        VotingMatter matter;
        try {
            matter = new Gson().fromJson(envelope.getRawMessage(), VotingMatter.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        if (matter.getDesiredVoters().contains(getSawtoothPublicKey())) {
            voteManager.addVoteRequired(matter);
        } else {
            print("This client is not required to vote on the matter.");
        }
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public void addGroupVoteReceiver(IGroupVoteReceiver groupVoteReceiver) {
        this.groupVoteReceivers.add(groupVoteReceiver);
    }

    public void removeGroupVoteReceiver(IGroupVoteReceiver groupVoteReceiver) {
        this.groupVoteReceivers.remove(groupVoteReceiver);
    }

    public List<IAllChatReceiver> getAllChatReceivers() {
        return allChatReceivers;
    }

    public void addAllChatReceiver(IAllChatReceiver obj) {
        this.allChatReceivers.add(obj);
    }

    public void remoteAllChatReceiver(IAllChatReceiver obj) {
        this.allChatReceivers.remove(obj);
    }
}
