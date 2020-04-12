import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet responsible for logging a user out.
 * 
 * @see SearchServer
 * @author evancarlson
 *
 */
public class LogoutServlet extends HttpServlet {

	/** ID used for serialization, which we are not using. */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		HttpSession session = request.getSession(true);

		response.setStatus(HttpServletResponse.SC_OK);

		boolean loggedIn = false;
		if (session.getAttribute("username") != null) {
			loggedIn = true;
		}

		if (loggedIn) {
			session.invalidate();
		}

		response.sendRedirect("/");
	}
}