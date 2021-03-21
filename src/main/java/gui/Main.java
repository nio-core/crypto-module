package gui;

import client.HyperZMQ;
import org.eclipse.jetty.server.Server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class Main {

    public static void main(String[] args) {
        HyperZMQ hyperZMQ = new HyperZMQ.Builder("GuiTestClient", "password")
                .createNewIdentity(true)
                .build();

        GuiServer guiServer = new GuiServer(hyperZMQ);

        guiServer.start();
        guiServer.stopBGThread();
    }
}
