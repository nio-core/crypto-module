package gui;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class KnownClientsServlet extends HttpServlet {

    private final GuiServer guiServer;

    public KnownClientsServlet(GuiServer guiServer) {
        this.guiServer = guiServer;
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        String json = new Gson().toJson(guiServer.knownClients);

        response.getWriter().println("{\"result\":" + json + "}");
    }

}
