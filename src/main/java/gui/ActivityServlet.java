package gui;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This servlet transforms the list of activities to strings. Moreover, it applies filters that are set in the gui.
 */
public class ActivityServlet extends HttpServlet {

    private final GuiServer guiServer;

    public ActivityServlet(GuiServer guiServer) {
        this.guiServer = guiServer;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        String json = new Gson().toJson(guiServer.activities);
        System.out.println("Activities json: " + json);
        response.getWriter().println("{\"result\":" + json + "}");
    }


}
