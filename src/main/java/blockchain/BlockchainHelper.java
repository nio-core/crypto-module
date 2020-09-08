package blockchain;

import client.HyperZMQ;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import keyexchange.KeyExchangeReceipt;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import sawtooth.sdk.protobuf.*;
import sawtooth.sdk.signing.Signer;

import javax.annotation.Nullable;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Time;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BlockchainHelper {

    private String baseRestAPIUrl;
    private final HyperZMQ hyperZMQ;
    private final boolean printRESTAPIResponse = false;
    private final ZContext zContext = new ZContext();
    private final ZMQ.Socket submitSocket;

    public static final String KEY_EXCHANGE_RECEIPT_FAMILY = "KeyExchangeReceipt";
    public static final String KEY_EXCHANGE_RECEIPT_NAMESPACE = "ac0cab";
    public static final String CSVSTRINGS_FAMILY = "csvstrings";
    public static final String CSVSTRINGS_NAMESPACE = "2f9d35";
    private final boolean doPrint = false;

    private final BlockingQueue<BatchList> batchListQueue = new ArrayBlockingQueue<BatchList>(100);
    private final AtomicBoolean runSender = new AtomicBoolean(true);
    private final ExecutorService senderExecutor = Executors.newSingleThreadExecutor();

    private String validatorAddress;

    public BlockchainHelper(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
        // TODO unused vvv: refactor or remove
        baseRestAPIUrl = ValidatorAddress.REST_URL_DEFAULT;

        submitSocket = zContext.createSocket(ZMQ.DEALER);
        submitSocket.connect(hyperZMQ.getValidatorAddress());

        senderExecutor.submit(new Thread(this::batchSenderLoop));
    }

    public void setValidatorAddress(String validatorAddress) {
        if (!this.validatorAddress.equals(validatorAddress)) {
            this.submitSocket.disconnect(this.validatorAddress);
            this.validatorAddress = validatorAddress;
            this.submitSocket.connect(this.validatorAddress);
        }
    }

    public String getValidatorAddress() {
        return this.validatorAddress;
    }

    public void setBaseRestAPIUrl(String baseRestAPIUrl) {
        this.baseRestAPIUrl = baseRestAPIUrl;
    }

    public Transaction csvStringsTransaction(byte[] payload) {
        return csvStringsTransaction(payload, null);
    }

    public Transaction csvStringsTransaction(byte[] payload, @Nullable String outputAddress) {
        Objects.requireNonNull(payload);
        return buildTransaction(
                BlockchainHelper.CSVSTRINGS_FAMILY,
                "0.1",
                payload,
                outputAddress);
    }

    public Transaction keyExchangeReceiptTransaction(KeyExchangeReceipt keyExchangeReceipt) {
        Objects.requireNonNull(keyExchangeReceipt);
        return buildTransaction(BlockchainHelper.KEY_EXCHANGE_RECEIPT_FAMILY,
                "0.1",
                keyExchangeReceipt.toString().getBytes(UTF_8),
                null);
    }

    public Transaction keyExchangeReceiptTransaction(String payload) {
        Objects.requireNonNull(payload);
        return buildTransaction(BlockchainHelper.KEY_EXCHANGE_RECEIPT_FAMILY,
                "0.1",
                payload.toString().getBytes(UTF_8),
                null);
    }

    private Transaction buildTransaction(String transactionFamily, String txFamVersion, byte[] payload) {
        return buildTransaction(transactionFamily, txFamVersion, payload, null);
    }

    private Transaction buildTransaction(String transactionFamily, String txFamVersion, byte[] payload, String outputAddr) {
        Signer signer = hyperZMQ.getSawtoothSigner();
        if (signer == null) {
            throw new IllegalStateException("Signer for transaction could not be acquired!");
        }

        // Set in- and outputs to the transaction family namespace to limit their read and write to their own namespaces
        // TODO do a switch here do support more tx families easier
        String input = KEY_EXCHANGE_RECEIPT_NAMESPACE;
        if (transactionFamily.equals(CSVSTRINGS_FAMILY)) {
            input = CSVSTRINGS_NAMESPACE;
        }
        String outputs = outputAddr == null ? input : outputAddr;

        // Create Transaction Header
        TransactionHeader header = TransactionHeader.newBuilder()
                .setSignerPublicKey(signer.getPublicKey().hex())
                .setFamilyName(transactionFamily)       // Has to be identical in TP
                .setFamilyVersion(txFamVersion)         // Has to be identical in TP
                .addOutputs(outputs)
                .addInputs(input)
                .setPayloadSha512(SawtoothUtils.hash(payload))
                .setBatcherPublicKey(signer.getPublicKey().hex())
                .setNonce(UUID.randomUUID().toString())
                .build();

        // Create the Transaction
        String signature = signer.sign(header.toByteArray());
        return Transaction.newBuilder()
                .setHeader(header.toByteString())
                .setPayload(ByteString.copyFrom(payload))
                .setHeaderSignature(signature)
                .build();
    }

    public boolean buildAndSendBatch(List<Transaction> transactionList) {
        // Wrap the transactions in a Batch (atomic unit)
        // Create the BatchHeader
        BatchHeader batchHeader = BatchHeader.newBuilder()
                .setSignerPublicKey(hyperZMQ.getSawtoothSigner().getPublicKey().hex())
                .addAllTransactionIds(
                        transactionList
                                .stream()
                                .map(Transaction::getHeaderSignature)
                                .collect(Collectors.toList())
                )
                .build();

        // Create the Batch
        // The signature of the batch acts as the Batch's ID
        String batchSignature = hyperZMQ.getSawtoothSigner().sign(batchHeader.toByteArray());
        Batch batch = Batch.newBuilder()
                .setHeader(batchHeader.toByteString())
                .addAllTransactions(transactionList)
                .setHeaderSignature(batchSignature)
                .build();

        // Encode Batches in BatchList
        // The validator expects a batchlist (which is not atomic)
        BatchList batchList = BatchList.newBuilder()
                .addBatches(batch)
                .build();

        batchListQueue.add(batchList);

        return true;
    }

    private boolean sendBatchListZMQ(BatchList batchList) {
        try {
            ClientBatchSubmitRequest req = ClientBatchSubmitRequest.parseFrom(batchList.toByteArray());
            //System.out.println("ClientBatchSubmitRequest: " + req.toString());

            Message message = Message.newBuilder()
                    .setMessageType(Message.MessageType.CLIENT_BATCH_SUBMIT_REQUEST)
                    .setContent(req.toByteString())
                    .setCorrelationId(EventHandler.CORRELATION_ID)
                    .build();

            submitSocket.send(message.toByteArray());

            byte[] bResponse = submitSocket.recv();
            Message respMessage = Message.parseFrom(bResponse);
            //System.out.println("response message: " + respMessage.toString());
            ClientBatchSubmitResponse cbsResp = ClientBatchSubmitResponse.parseFrom(respMessage.getContent());
            //System.out.println("ClientBatchSubmitResponse parsed: " + cbsResp);
            // Check submission status
            boolean success = cbsResp.getStatus() == ClientBatchSubmitResponse.Status.OK;
            if (success) {
                print("Batch submit of " + batchList.toString() + "successful");
            } else {
                printErr("Batch submit of " + batchList.toString() + "failed!");
            }
            return success;
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Deprecated, use ZMQ variant instead
    private boolean sendBatchListRESTAPI(byte[] body) throws IOException {
        URL url = new URL(baseRestAPIUrl + "/batches");
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "application/octet-stream");
        http.connect();

        try (OutputStream os = http.getOutputStream()) {
            os.write(body);
        }

        String response;

        try (InputStream is = http.getInputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            Stream<String> lines = br.lines();
            response = lines.reduce("", (accu, s) -> accu += s);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (response != null && printRESTAPIResponse) {
            print(response);
        }
        return true;
    }

    // Deprecated, use ZMQ variant instead
    private String sendToRestEndpoint(String restEndpoint, String requestMethod, byte[] payload) throws IOException {
        URL url = new URL(baseRestAPIUrl + restEndpoint);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod(requestMethod); // PUT is another valid option
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "application/octet-stream");
        http.connect();

        if (payload != null) {
            try (OutputStream os = http.getOutputStream()) {
                os.write(payload);
            }
        }

        String response;

        try (InputStream is = http.getInputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            Stream<String> lines = br.lines();
            response = lines.reduce("", (accu, s) -> accu += s);
        } catch (FileNotFoundException e) {
            System.out.println("Address did not match any resource");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (response != null && printRESTAPIResponse) {
            print(response);
        }
        return response;
    }

    public String getStateZMQ(String address) {
        ClientStateGetRequest req = ClientStateGetRequest.newBuilder()
                .clearStateRoot()
                .setAddress(address)
                .build();
        //System.out.println("ClientStateGetRequest: " + req.toString());

        Message message = Message.newBuilder()
                .setMessageType(Message.MessageType.CLIENT_STATE_GET_REQUEST)
                .setContent(req.toByteString())
                .setCorrelationId(EventHandler.CORRELATION_ID)
                .build();
        //System.out.println("Request Message: " + message.toString());

        submitSocket.send(message.toByteArray());
        byte[] bResponse = submitSocket.recv();
        //System.out.println("get state response raw: " + new String(bResponse));
        Message respMessage;
        try {
            respMessage = Message.parseFrom(bResponse);
            // Extract the ClientStateGetResponse
            ClientStateGetResponse csgr = ClientStateGetResponse.parseFrom(respMessage.getContent());
            //System.out.println("csgr: " + csgr.toString());
            return csgr.getValue().toStringUtf8();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String queryStateAddress(String addr) throws IOException {
        String resp = sendToRestEndpoint(("/state/" + addr), "GET", null);
        if (resp != null) {
            JSONObject o = new JSONObject(resp);
            try {
                return o.getString("data");
            } catch (JSONException e) {
                System.out.println("Field 'data' not found in response " + resp);
                return null;
            }
        } else {
            return null;
        }
    }

    public void setRestAPIUrl(String url) {
        baseRestAPIUrl = url;
        if (baseRestAPIUrl.endsWith("/")) {
            baseRestAPIUrl = baseRestAPIUrl.substring(0, baseRestAPIUrl.length() - 2);
        }
    }

    private void print(String message) {
        if (doPrint)
            System.out.println("[" + Thread.currentThread().getId() + "] [BlockchainHelper][" + hyperZMQ.getClientID() + "]  " + message);
    }

    private void printErr(String message) {
        System.err.println("[" + Thread.currentThread().getId() + "] [BlockchainHelper][" + hyperZMQ.getClientID() + "]  " + message);
    }

    private void batchSenderLoop() {
        print("Starting BatchSender...");
        while (this.runSender.get()) {
            BatchList list = null;
            try {
                list = batchListQueue.poll(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (list != null) {
                sendBatchListZMQ(list);
            }
        }
        print("Stopping BatchSender...");
    }

    void stopSender() {
        this.runSender.set(false);
    }

    void runSender() {
        this.runSender.set(true);
        senderExecutor.submit(new Thread(this::batchSenderLoop));
    }

}
