package gui;

import client.HyperZMQ;
import client.VoteManager;
import groups.IGroupCallback;
import joingroup.JoinRequest;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import unused.VotingConfig;
import unused.VotingConfigServlet;
import voting.IVoteStatusCallback;
import voting.Vote;
import voting.VotingMatter;
import voting.VotingResult;

import javax.servlet.Servlet;
import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is responsible to poll/gather and prepare the data from the HyperZMQ instance.
 * The servlets will then query the data from this class.
 */
public class GuiServer implements Closeable, IGroupCallback, IVoteStatusCallback {

    private Server server;
    private final HyperZMQ hyperZMQ;
    private int port = 8090;
    List<String> groups = new ArrayList<>();
    //List<Activity> activities = new ArrayList<>();
    List<String> activities = new ArrayList<>();
    List<String> knownClients = new ArrayList<>();
    private final AtomicBoolean runBG = new AtomicBoolean(true);

    public GuiServer(HyperZMQ hyperZMQ) {
        this.hyperZMQ = hyperZMQ;
    }

    public GuiServer(HyperZMQ hyperZMQ, int port) {
        this.hyperZMQ = hyperZMQ;
        this.port = port;
    }

    public void start() {
        try {
            backgroundThread();
            setup();
            server.start();
            server.dump(System.err);
            //server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setup() throws URISyntaxException, MalformedURLException {
        server = new Server(port);

        // Prepare the index.html page
        URL url = Main.class.getClassLoader().getResource("html/");
        Objects.requireNonNull(url, "Cannot find index resource!");
        URI webRootUri = url.toURI();
        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setBaseResource(Resource.newResource(webRootUri));
        context.setWelcomeFiles(new String[]{"index.html"});

        ServletHolder holderPwd = new ServletHolder("default",
                DefaultServlet.class);
        holderPwd.setInitParameter("dirAllowed", "true");
        context.addServlet(holderPwd, "/");

        // Setup the servlets for each endpoint
        // TODO add new data endpoints here
        // Groups servlet
        Servlet groupsServlet = new GroupsServlet(this);
        ServletHolder groupsHolder = new ServletHolder("groups", groupsServlet);
        context.addServlet(groupsHolder, "/groups");

        // Known Clients Servlet
        Servlet clientsServlet = new KnownClientsServlet(this);
        ServletHolder clientsHolder = new ServletHolder("clients", clientsServlet);
        context.addServlet(clientsHolder, "/clients");

        // Activity Servlet
        Servlet activityServlet = new ActivityServlet(this);
        ServletHolder activityHolder = new ServletHolder("activities", activityServlet);
        context.addServlet(activityHolder, "/activities");

        // Name Servlet
        Servlet nameServlet = new NameServlet(this);
        ServletHolder nameHolder = new ServletHolder("name", nameServlet);
        context.addServlet(nameHolder, "/clientname");

        // VotingConfig Servlet
        /*
        Servlet vcServlet = new VotingConfigServlet(this);
        ServletHolder vcHolder = new ServletHolder("VotingConfig", vcServlet);
        context.addServlet(vcHolder, "/vconfig");
        */
        server.setHandler(context);
    }

    public List<String> getGroups() {
        return this.groups;
    }

    private void backgroundThread() {
        //runBG.set(true);
        Thread t = new Thread(() -> {
            while (runBG.get()) {
                // Check for new groups, register a callback for them
                // If a group is removed, all callbacks are removed automatically so there is no need to act on that
                List<String> newList = hyperZMQ.getGroupNames();
                System.out.println("queried groupnames: " + newList.toString());
                for (String s : newList) {
                    if (!this.groups.contains(s)) {
                        this.hyperZMQ.addCallbackToGroup(s, this);
                    }
                }
                this.groups = newList;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    @Override
    public void close() throws IOException {
        this.runBG.set(false);
    }

    @Override
    public void newMessageOnChain(String group, String message, String senderID) {
        this.activities.add(new Activity(ActivityType.MESSAGE, group, senderID, message).toString());
    }

    public void stopBGThread() {
        this.runBG.set(false);
    }

    public String getHyperZMQName() {
        return this.hyperZMQ.getClientID() + " [ID: " + shortenID(hyperZMQ.getSawtoothPublicKey() + "]");
    }

    public static String shortenID(String input) {
        if (input.length() > 6) {
            String first3 = input.substring(0, 2);
            String last3 = input.substring(input.length() - 3);
            return first3 + ".." + last3;
        }
        return input;
    }

    @Override
    public void newVoteCasted(VotingMatter votingMatter, Vote vote) {
        activities.add(new Activity(ActivityType.VOTE, votingMatter.getJoinRequest().getGroupName(), vote.getPublicKey(), String.valueOf(vote.isApproval())).toString());
    }

    @Override
    public void newJoinRequestToModerate(JoinRequest joinRequest) {
        activities.add(new Activity(ActivityType.JOIN_GROUP, joinRequest.getGroupName(), joinRequest.getApplicantPublicKey(), joinRequest.toString()).toString());
    }

    @Override
    public void votingProcessFinished(VotingResult votingResult, boolean approved) {
        activities.add(new Activity(ActivityType.JOIN_GROUP, votingResult.getVotingMatter().getJoinRequest().getGroupName(), votingResult.getVotingMatter().getVoteDirectorPublicKey(), String.valueOf(approved)).toString());
    }
}
