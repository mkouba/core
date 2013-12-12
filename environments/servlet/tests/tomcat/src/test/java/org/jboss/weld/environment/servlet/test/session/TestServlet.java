package org.jboss.weld.environment.servlet.test.session;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/test")
@SuppressWarnings("serial")
public class TestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String action = req.getParameter("action");
        System.out.println("ACTION: " + action);

        if ("init".equals(action)) {
            req.getSession(true);
        } else {
            req.getSession().invalidate();
            resp.sendRedirect(req.getServletContext().getContextPath() + "/redir");
        }
    }
}
