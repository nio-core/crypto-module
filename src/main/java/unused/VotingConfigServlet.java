package unused;

import gui.GuiServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class VotingConfigServlet extends HttpServlet {
    private final GuiServer guiServer;

    public VotingConfigServlet(GuiServer guiServer) {
        this.guiServer = guiServer;
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("{ \"result\": \"voting config\"}");
    }
}
