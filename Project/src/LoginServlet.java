import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet responsible for displaying the login page and handling user
 * authentication.
 * 
 * @see SearchServer
 * @author evancarlson
 */
public class LoginServlet extends HttpServlet {

	/** A logger specifically for this class. */
	private static final Logger log = LogManager.getLogger(LoginServlet.class);

	/** ID used for serialization, which we are not using. */
	private static final long serialVersionUID = 1L;

	/** Title of web page. */
	private static final String TITLE = "Login";

	/** The JDBC to a user database */
	private final DatabaseConnector connector;

	/**
	 * @param connector the connector to a SQL user database
	 */
	public LoginServlet(DatabaseConnector connector) {
		this.connector = connector;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		log.info("LoginServlet ID " + this.hashCode() + " handling GET request.");

		// initialize variables
		boolean authenticated = false;
		boolean invalid = false;
		boolean error = false;

		// get login status
		if (request.getParameter("status") != null) {
			String status = request.getParameter("status");
			if (!status.isBlank()) {
				// sanitize it
				status = StringEscapeUtils.escapeHtml4(status);

				if (status.equals("authenticated")) {
					authenticated = true;
				}
				if (status.equals("invalid")) {
					invalid = true;
				}
				if (status.equals("error")) {
					error = true;
				}
			}
			else {
				error = true;
			}
		}

		// form HTML
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();

		out.printf("<!DOCTYPE html>%n");
		out.printf("	<head>%n");
		out.printf("		<meta charset=\"utf-8\">%n");
		out.printf("		<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">%n");
		// if authenticated, give meta tag to redirect in 3 seconds
		if (authenticated) {
			out.printf("	<meta http-equiv=\"refresh\" content=\"3;url=/\"/>%n");
		}
		out.printf("		<title>%s</title>%n", TITLE);
		out.printf(
				"			<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bulma@0.8.0/css/bulma.min.css\">%n");
		out.printf("		<script defer src=\"https://use.fontawesome.com/releases/v5.3.1/js/all.js\"></script>%n");
		out.printf("	</head>%n");
		out.printf("	<body>%n");
		out.printf("		<section class=\"hero is-primary is-bold\" style=\"height:100vh; text-align:center;\">%n");
		out.printf("	  		<div class=\"hero-body\">%n");
		out.printf("	    		<div class=\"container\" style=\"padding-top:20vh;\">%n");
		out.printf("	      			<h1 class=\"title\">Sign In</h1>%n");
		out.printf(
				"	      			<h2 class=\"subtitle\"><i class=\"fas fa-plus\"></i>&nbsp;Welcome back!</h2>%n");

		// the login form
		out.printf(
				"						<form method=\"post\" action=\"/login\" style=\"max-width:600px; margin-left:auto; margin-right:auto;\"%n>");
		out.printf("						<div class=\"field\">%n");
		out.printf("							<div class=\"control\">%n");
		out.printf(
				"									<p><input class=\"input\" type=\"text\"  name=\"username\" placeholder=\"Username\">%n");
		out.printf("			  				</div>%n");
		out.printf("			  			</div>%n");
		out.printf("						<div class=\"field\">%n");
		out.printf("							<div class=\"control\" style=\"padding:1px\">%n");
		out.printf(
				"									<p><input class=\"input\" type=\"text\"  name=\"userpass\" placeholder=\"Password\">%n");
		out.printf("				  			</div>%n");
		out.printf("			  			</div>%n");
		out.printf("						<div class=\"field\" style=\"text-align:center;\">%n");
		out.printf(" 							<div class=\"container\" style=\"padding-bottom:20px;\">%n");
		out.printf("			    				<button class=\"button is-primary\">Log in</button>%n");
		out.printf(" 							</div>%n");
		out.printf("						</div>%n");
		out.printf("					</form>%n");

		if (authenticated) {
			out.printf("				<p>You are logged in! Redirecting in 3 seconds...</p>%n");
		}
		if (invalid) {
			out.printf("				<p>The username and password do not match our records.</p>%n");
		}
		if (error) {
			out.printf("				<p>Please fill both forms.</p>%n");
		}
		out.printf(
				"						<p>Go <a href=\"/\" style=\"text-decoration:underline;\">home</a> or <a href=\"/create\" style=\"text-decoration:underline;\">create an account</a>.%n");
		out.printf("	    		</div>%n");
		out.printf("	    	</div>%n");

		// thread info
		out.printf("			<div style=\"padding:30px;\">%n");
		out.printf("				<p>This request was handled by thread %s.</p>%n", Thread.currentThread().getName());
		out.printf("			</div>%n");

		out.printf("		</section>%n");
		out.printf("	</body>%n");
		out.printf("</html>%n");
		out.close();
		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		log.info("LoginServlet ID " + this.hashCode() + " handling POST request.");

		HttpSession session = request.getSession(true);

		response.setStatus(HttpServletResponse.SC_OK);

		boolean authenticated = false;

		// get input
		String username = request.getParameter("username");
		String password = request.getParameter("userpass");

		// make sure the input is valid
		if (!username.isBlank() && !password.isBlank()) {
			// sanitize it
			username = StringEscapeUtils.escapeHtml4(username);
			password = StringEscapeUtils.escapeHtml4(password);

			// connect to the database
			try (Connection db = connector.getConnection()) {
				// safely perform a SQL search query
				PreparedStatement statement = db
						.prepareStatement("SELECT * FROM users WHERE username=? AND password=?");
				statement.setString(1, username);
				statement.setString(2, password);
				ResultSet results = statement.executeQuery();
				authenticated = results.next();
			}
			catch (SQLException e) {
				log.error("Error connecting to database.");
			}

			if (authenticated) {
				session.setAttribute("username", username);
				response.sendRedirect("/login?status=authenticated");
			}
			else {
				response.sendRedirect("/login?status=invalid");
			}
		}
		// otherwise, the user left a blank field.
		else {
			response.sendRedirect("/login?status=error");
			return;
		}
	}
}