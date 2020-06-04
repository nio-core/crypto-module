package blockchain;

import client.HyperZMQ;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import joingroup.JoinRequest;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import sawtooth.sdk.protobuf.ClientEventsSubscribeRequest;
import sawtooth.sdk.protobuf.ClientEventsSubscribeResponse;
import sawtooth.sdk.protobuf.Event;
import sawtooth.sdk.protobuf.EventFilter;
import sawtooth.sdk.protobuf.EventList;
import sawtooth.sdk.protobuf.EventSubscription;
import sawtooth.sdk.protobuf.Message;
import sawtooth.sdk.protobuf.Message.MessageType;

public class EventHandler implements AutoCloseable {
    private final HyperZMQ hyperzmq;
    static final String CORRELATION_ID = "123";

    private String validatorURL = "";
    private final ZMQ.Socket socket;

    private final AtomicBoolean runListenerLoop = new AtomicBoolean(true);
    private final AtomicBoolean runDistributorLoop = new AtomicBoolean(true);

    private final ExecutorService eventDistributionExecutor = Executors.newSingleThreadExecutor();

    private final BlockingQueue<Message> eventQueue = new ArrayBlockingQueue<Message>(100);
    private final BlockingQueue<Message> subscriptionQueue = new ArrayBlockingQueue<Message>(100);

    private int receiveTimeoutMS = 700;
    private final ZContext context;

    public EventHandler(HyperZMQ callback) {
        this.hyperzmq = callback;
        context = new ZContext();
        this.socket = context.createSocket(ZMQ.DEALER);
        //this.sendSocket = context.createSocket(ZMQ.DEALER);
        this.socket.setReceiveTimeOut(receiveTimeoutMS);
        startListenerLoop();
        startEventDistributorLoop();
        // Automatically subscribe to the AllChat
        queueNewSubscription("AllChat", EventFilter.newBuilder()
                .setFilterType(EventFilter.FilterType.REGEX_ANY)
                .build());
    }

    void startEventDistributorLoop() {
        runDistributorLoop.set(true);
        Thread t = new Thread(this::eventDistributorLoop);
        eventDistributionExecutor.submit(t);
    }

    void stopEventDistributor() {
        print("Stopping EventDistributor");
        runDistributorLoop.set(false);
    }

    /**
     * This loop distributes the events that received by the socket to the corresponding queues
     */
    private void eventDistributorLoop() {
        //print("Starting EventDistributorLoop...");
        while (runDistributorLoop.get()) {
            Message messageReceived = null;
            try {
                messageReceived = eventQueue.poll(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (messageReceived != null) {
                switch (messageReceived.getMessageType()) {
                    case CLIENT_EVENTS: {
                        EventList list = null;
                        try {
                            list = EventList.parseFrom(messageReceived.getContent());
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                            break;
                        }
                        // TODO distribute events here depending on type
                        for (Event e : list.getEventsList()) {
                            String received = e.toString();
                            //print("Received Event: " + received);
                            if (e.getEventType().equals("AllChat")) {

                            } else {
                                // Group Messages (CSVStrings)
                                // Check whether the event is a new encrypted message or a JoinGroup request
                                Event.Attribute attr = e.getAttributes(0);
                                if (BlockchainHelper.CSVSTRINGS_NAMESPACE.equals(attr.getValue())) {
                                    JoinRequest request = SawtoothUtils.deserializeMessage(e.getData().toStringUtf8(), JoinRequest.class);
                                    if (request != null) {
                                        print("Received JoinGroupRequest: " + request.toString());

                                        // Check if we are responsible for the request
                                        if (!request.getContactPublicKey().equals(hyperzmq.getSawtoothPublicKey())) {
                                            print("This client is not responsible for the request");
                                            continue;
                                        }

                                        // Check if we can access the requested key beforehand
                                        Objects.requireNonNull(hyperzmq.getKeyForGroup(request.getGroupName()),
                                                "Client does not have the key that was requested!");

                                        // Pass control to VoteManager
                                        hyperzmq.getVoteManager().addJoinRequest(request);
                                        continue;
                                    }
                                }
                                //-----------------------------------------------------------------------------------
                                //  Handle Group Messages (Normal message, Contract, VotingMatter, Vote)
                                //  Filter for VotingMatter and Vote to put those in the corresponding queues
                                //-----------------------------------------------------------------------------------
                                String fullMessage = received.substring(received.indexOf("data"));
                                //print("fullMessage: " + fullMessage);

                                String csvMessage = fullMessage.substring(7, fullMessage.length() - 2); //TODO
                                //print("csvMessage: " + csvMessage);

                                String[] parts = csvMessage.split(",");
                                if (parts.length < 2) {
                                    print("Malformed event payload: " + csvMessage);
                                    return;
                                }
                                String group = parts[0];
                                String encMessage = parts[1];
                                //print("Group: " + group);
                                //print("Encrypted Message: " + encMessage);
                                hyperzmq.newEventReceived(group, encMessage);
                            }
                        }
                        break;
                    }
                    case CLIENT_EVENTS_SUBSCRIBE_RESPONSE: {
                        // Check for subscription success, nothing else to do
                        try {
                            ClientEventsSubscribeResponse cesr = ClientEventsSubscribeResponse.parseFrom(messageReceived.getContent());
                            print("Subscription was " + (cesr.getStatus() == ClientEventsSubscribeResponse.Status.OK ?
                                    "successful" : "unsuccessful"));
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case PING_REQUEST: {
                        // IGNORE!!!
                        break;
                    }
                    default: {
                        print("Received message has unknown type: " + messageReceived.toString());
                        break;
                    }
                }
            }
        }
    }

    private void startListenerLoop() {
        // The loop consists of the socket receiving with a timeout.
        // After each receive, the queue is checked whether there are messages to send
        Thread t = new Thread(() -> {
            //print("Connecting to: " + getValidatorURL());
            //print("Starting EventListenerLoop...");
            socket.connect(getValidatorURL());
            while (runListenerLoop.get()) {
                // If something is in the queue, send that message
                Message messageToSent = null;
                try {
                    messageToSent = subscriptionQueue.remove();
                } catch (NoSuchElementException ignored) {
                    // It's ok that the queue is empty
                }

                if (messageToSent != null) {
                    // The message is already protobuf, ready to be sent
                    socket.send(messageToSent.toByteArray());
                    //print("Sent message:" + messageToSent.toString());
                }

                // Try to receive a message
                //print("!!!Receiving Event...!!!");
                byte[] recv = socket.recv();
                //print("!!!Receiving end!!!");
                if (recv != null) {
                    try {
                        Message messageReceived = Message.parseFrom(recv);
                        eventQueue.put(messageReceived);
                    } catch (InvalidProtocolBufferException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            // End while
            print("Exiting EventListenerLoop...");
            socket.close();
        });
        t.start();
    }

    public void subscribeToGroup(String groupName) {
        EventFilter eventFilter = EventFilter.newBuilder()
                .setFilterType(EventFilter.FilterType.REGEX_ANY)
                .setKey("address")
                .setMatchString(BlockchainHelper.CSVSTRINGS_NAMESPACE + "*")
                .build();
        queueNewSubscription(groupName, eventFilter);
    }

    void queueNewSubscription(String eventName, EventFilter eventFilter) {
        // Build a subscription message ready to be sent which will be queued
        EventSubscription eventSubscription = EventSubscription.newBuilder()
                // .addFilters(eventFilter)
                .setEventType(eventName)
                .build();

        ClientEventsSubscribeRequest request = ClientEventsSubscribeRequest.newBuilder()
                .addSubscriptions(eventSubscription)
                .build();

        Message message = Message.newBuilder()
                .setCorrelationId(CORRELATION_ID)
                .setMessageType(MessageType.CLIENT_EVENTS_SUBSCRIBE_REQUEST)
                .setContent(request.toByteString())
                .build();

        try {
            //print("Queueing subscription: " + request.toString());
            subscriptionQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getValidatorURL() {
        return validatorURL.isEmpty() ? ValidatorAddress.VALIDATOR_URL_DEFAULT : validatorURL;
    }

    private void print(String msg) {
        System.out.println("[" + Thread.currentThread().getId() + "]" + " [EventHandler][" + hyperzmq.getClientID() + "]  " + msg);
    }

    public void setValidatorURL(String validatorURL) {
        this.validatorURL = validatorURL;
    }

    @Override
    public void close() throws Exception {
        runDistributorLoop.set(false);
        runListenerLoop.set(false);
        socket.close();
        context.close();
    }
}

