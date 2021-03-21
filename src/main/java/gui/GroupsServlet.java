package gui;

import client.HyperZMQ;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class GroupsServlet extends HttpServlet {

    private final GuiServer guiServer;

    public GroupsServlet(GuiServer guiServer) {
        this.guiServer = guiServer;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String result = "";
        List<String> groups = guiServer.getGroups();
        //System.out.println("groups: " + groups);
        result = groups.toString().replace("[", "").replace("]", "");

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("{\"result\": \"" + result + "\"}");
    }
}
