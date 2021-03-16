package gui;

import client.HyperZMQ;
import groups.IGroupCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class GUI extends Application implements IGroupCallback {

    private final HyperZMQ hyperZMQ;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final ExecutorService activityProcessorService = Executors.newSingleThreadExecutor();
    private final BlockingQueue<Activity> activityQueue = new ArrayBlockingQueue<Activity>(1000);

    private final AtomicBoolean runBGThread = new AtomicBoolean(true);

    private static final int POLL_INTERVAL_MS = 1000;

    // DATA
    private final List<Activity> activities = new ArrayList<>();
    private final List<String> groups = new ArrayList<>();

    public GUI(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.backgroundExecutor.submit(this::backgroundThread);
        this.activityProcessorService.submit(this::activityProcessor);
        updateGroups();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("gui.fxml"));
        AnchorPane root = (AnchorPane) loader.load();
        Scene scene = new Scene(root);
        stage.setTitle("Sawtooth Network Overview - " + hyperZMQ.getClientID());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Processes new activities and updates the corresponding UI elements
     */
    private void activityProcessor() {
        Activity a = null;
        try {
            a = activityQueue.poll(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (a != null) {

        }
    }

    /**
     * Check if new groups are available for the observed instance.
     * If that is the case, attach a listener.
     */
    private void updateGroups() {
        List<String> newGroups = hyperZMQ.getGroupNames().stream().filter(s -> !this.groups.contains(s)).collect(Collectors.toList());
        newGroups.forEach(name -> {
            hyperZMQ.addCallbackToGroup(name, this);
            this.groups.add(name);
        });
        // TODO Update UI
    }

    /**
     * Poll data from the validator
     */
    private void backgroundThread() {
        while (runBGThread.get()) {
            // Check for new data

        }
    }

    public void stopBackgroundThread() {
        this.runBGThread.set(false);
    }

    public void startBackgroundThread() {
        if (!this.runBGThread.get()) {
            this.runBGThread.set(true);
            this.backgroundExecutor.submit(this::backgroundThread);
        }
    }

    @Override
    public void newMessageOnChain(String group, String message, String senderID) {
        activityQueue.add(new Activity(ActivityType.MESSAGE, group, senderID, message));
    }
}
