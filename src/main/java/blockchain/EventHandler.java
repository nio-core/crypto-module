package blockchain;

import client.HyperZMQ;
import com.google.protobuf.InvalidProtocolBufferException;
import groups.GroupMessage;
import joingroup.JoinRequest;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import sawtooth.sdk.protobuf.*;
import sawtooth.sdk.protobuf.Message.MessageType;
import util.Utilities;

import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventHandler implements AutoCloseable {
    private final HyperZMQ hyperzmq;
    static final String CORRELATION_ID = "123";

    private String validatorURL = "";

    private final AtomicBoolean runListenerLoop = new AtomicBoolean(true);
    private final AtomicBoolean runDistributorLoop = new AtomicBoolean(true);

    private final ExecutorService eventDistributionExecutor = Executors.newSingleThreadExecutor();

    private final BlockingQueue<Message> eventQueue = new ArrayBlockingQueue<Message>(100);
    private final BlockingQueue<Message> subscriptionQueue = new ArrayBlockingQueue<Message>(100);

    private int receiveTimeoutMS = 700;

    public EventHandler(HyperZMQ callback, String validatorURL) {
        this.hyperzmq = callback;
        this.validatorURL = validatorURL == null ? GlobalConfig.VALIDATOR_URL_DEFAULT : validatorURL;
        startListenerLoop();
        startEventDistributorLoop();
    }

    void startEventDistributorLoop() {
        runDistributorLoop.set(true);
        eventDistributionExecutor.submit(new Thread(this::eventDistributorLoop));
    }

    void stopEventDistributor() {
        print("Stopping EventDistributor");
        runDistributorLoop.set(false);
    }

    /**
     * This loop distributes the events that are received by the socket to the corresponding queues
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
                            continue;
                        }
                        // TODO distribute events here depending on type
                        for (Event e : list.getEventsList()) {
                            // print("Received Event: " + e.toString());
                            // Group Messages (CSVStrings)
                            // Check whether the event is a new encrypted message or a JoinGroup request
                            // In case of JoinRequest, the attribute value of the event is just the namespace instead of a full address
                            // Because JoinRequests are not written to the blockchain
                            Event.Attribute attr = e.getAttributes(0);
                            if (BlockchainHelper.GROUP_MESSAGE_NAMESPACE.equals(attr.getValue())) {
                                JoinRequest request = SawtoothUtils.deserializeMessage(e.getData().toStringUtf8(), JoinRequest.class);
                                if (request != null) {
                                    //print("Received JoinGroupRequest: " + request.toString());

                                    // Check if we are responsible for the request
                                    if (!request.getContactPublicKey().equals(hyperzmq.getSawtoothPublicKey())) {
                                        // print("This client is not responsible for the request");
                                        continue;
                                    }

                                    // Check if we can access the requested key beforehand
                                    if (!hyperzmq.isGroupAvailable(request.getGroupName())) {
                                        printErr("Client does not have the key that was requested!");
                                        continue;
                                    }

                                    // Objects.requireNonNull(hyperzmq.getKeyForGroup(request.getGroupName()),
                                    //       "Client does not have the key that was requested!");

                                    // Pass control to VoteManager
                                    hyperzmq.getVoteManager().addJoinRequest(request);
                                    continue;
                                }
                            }
                            //-----------------------------------------------------------------------------------
                            //  Handle Group Messages (Normal message, Contract, VotingMatter, Vote)
                            //  Filter for VotingMatter and Vote to put those in the corresponding queues
                            //-----------------------------------------------------------------------------------

                            GroupMessage groupMessage = Utilities.deserializeMessage(e.getData().toStringUtf8(), GroupMessage.class);
                            if (groupMessage != null) {
                                hyperzmq.newEventReceived(groupMessage.group, groupMessage.payload);
                            } else {
                                printErr("Deserialization failed!");
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
                        printErr("Received message has unknown type: " + messageReceived.toString());
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
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(ZMQ.DEALER);
            socket.setReceiveTimeOut(receiveTimeoutMS);
            //print("Connecting to: " + getValidatorURL());
            //print("Starting EventListenerLoop...");
            socket.connect(this.validatorURL);
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
                byte[] recv = socket.recv();
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
            System.err.println(hyperzmq.getClientID() + "  closing event socket...");
            socket.disconnect(this.validatorURL);
            socket.close();
            context.close();
        });
        t.start();
    }

    public void subscribeToGroup(String groupName) {
        EventFilter eventFilter = EventFilter.newBuilder()
                .setFilterType(EventFilter.FilterType.REGEX_ANY)
                .setKey("address")
                .setMatchString(BlockchainHelper.GROUP_MESSAGE_NAMESPACE + "*")
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

    void unsubscribe(String groupName) {
        // TODO unsubscribing a single group is not possible, only to unsubscribe from all events
        // https://sawtooth.hyperledger.org/docs/core/releases/latest/app_developers_guide/zmq_event_subscription.html#unsubscribing-to-events
        // Therefore, unusable event messages (e.g. from a group we left) are just suppressed
        // ClientEventsUnsubscribeRequest request = ClientEventsUnsubscribeRequest.newBuilder().
    }

    private void print(String msg) {
        if (GlobalConfig.PRINT_EVENT_HANDLER)
            System.out.println("[" + Thread.currentThread().getId() + "]" + " [EventHandler][" + hyperzmq.getClientID() + "]  " + msg);
    }

    private void printErr(String msg) {
        System.err.println("[" + Thread.currentThread().getId() + "]" + " [EventHandler][" + hyperzmq.getClientID() + "]  " + msg);
    }

    public void setValidatorURL(String validatorURL) {
        this.validatorURL = validatorURL;
    }

    @Override
    public void close() {
        runDistributorLoop.set(false);
        runListenerLoop.set(false);
        //System.err.println(hyperzmq.getClientID() + "  closing event socket...");
        //socket.close();
        //context.close();
    }
}

