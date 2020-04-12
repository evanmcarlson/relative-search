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
 * Servlet responsible for displaying account creation page and handling
 * database validation/entry.
 * 
 * @see SearchServer
 * @author evancarlson
 *
 */
public class CreateServlet extends HttpServlet {

	/** A logger specifically for this class. */
	private static final Logger log = LogManager.getLogger(CreateServlet.class);

	/** ID used for serialization, which we are not using. */
	private static final long serialVersionUID = 1L;

	/** Title of web page. */
	private static final String TITLE = "Create Account";

	/** The JDBC to a user database */
	private final DatabaseConnector connector;

	/**
	 * @param connector the connector to a SQL user database
	 */
	public CreateServlet(DatabaseConnector connector) {
		this.connector = connector;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		log.info("CreateServlet ID " + this.hashCode() + " handling GET request.");

		String successful = null;

		if (request.getParameter("success") != null) {
			// get login status
			String success = request.getParameter("success");
			// sanitize it
			success = StringEscapeUtils.escapeHtml4(success);

			if (success.equals("true")) {
				successful = success;
			}
			if (success.equals("false")) {
				successful = success;
			}
			if (success.contentEquals("invalid")) {
				successful = success;
			}
		}

		// form HTML
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();

		out.printf("<!DOCTYPE html>%n");
		out.printf("<html>%n");
		out.printf("	<head>%n");
		out.printf("		<meta charset=\"utf-8\">%n");
		out.printf("		<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">%n");
		// if successful, give meta tag to redirect in 3 seconds
		if (successful != null) {
			if (successful.equals("true")) {
				out.printf("<meta http-equiv=\"refresh\" content=\"3;url=/\"/>");
			}
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
		out.printf("	      			<h1 class=\"title\">%n");
		out.printf("	        		Create an account%n");
		out.printf("	      			</h1>%n");
		out.printf("	      			<h2 class=\"subtitle\">%n");
		out.printf("					<i class=\"fas fa-plus\"></i>%n");
		out.printf("					&nbsp;Thank you for supporting our platform!</h2>%n");

		// the account creation form
		out.printf(
				"						<form method=\"post\" action=\"/create\" style=\"max-width:600px; margin-left:auto; margin-right:auto;\"%n>");
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
		out.printf(
				"			    				<button class=\"button is-primary\" type=\"submit\">Log in</button>%n");
		out.printf(" 							</div>%n");
		out.printf("						</div>%n");
		out.printf("					</form>%n");

		if (successful != null) {
			if (successful.equals("true")) {
				out.printf("			<h3>Account created! Redirecting in 3 seconds...</h3>");
			}
			if (successful.equals("false")) {
				out.printf("			<h3>An account with that username already exists.</h3>");
			}
			if (successful.equals("invalid")) {
				out.printf("			<h3>Please fill both forms.</h3>");
			}
		}

		out.printf("					<p>Back to <a href=\"/\" style=\"text-decoration:underline;\">home</a>.");
		out.printf("	    		</div>%n");
		out.printf("	    	</div>%n");

		// thread info
		out.printf("			<div style=\"padding:30px;\">%n");
		out.printf("	    		<p>%n");
		out.printf("	      		This request was handled by thread %s.%n", Thread.currentThread().getName());
		out.printf("	    		</p>%n");
		out.printf("	  		</div>%n");

		out.printf("		</section>%n");
		out.printf("	</body>%n");
		out.printf("</html>%n");
		out.close();
		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		log.info("CreateServlet ID " + this.hashCode() + " handling POST request.");

		HttpSession session = request.getSession(true);
		response.setStatus(HttpServletResponse.SC_OK);

		boolean successful = false;

		// get input
		String username = request.getParameter("username");
		String password = request.getParameter("userpass");

		if (!username.isBlank() && !password.isBlank()) {
			// sanitize it
			username = StringEscapeUtils.escapeHtml4(username);
			password = StringEscapeUtils.escapeHtml4(password);

			// connect to the database
			try (Connection db = connector.getConnection()) {
				// safely perform a SQL search query
				PreparedStatement select = db.prepareStatement("SELECT * FROM users WHERE username=? AND password=?");
				select.setString(1, username);
				select.setString(2, password);

				ResultSet results = select.executeQuery();
				if (results.next()) {
					// someone else has that username
					successful = false;
				}
				else {
					// safely create a new row in the user database
					PreparedStatement insert = db.prepareStatement("INSERT INTO users(username, password) VALUES(?,?)");
					insert.setString(1, username);
					insert.setString(2, password);
					insert.execute();
					successful = true;
					session.setAttribute("username", username);
				}
			}
			catch (SQLException e) {
				log.error("Error connecting to database.");
			}
		}
		// otherwise, the user left a blank field.
		else {
			response.sendRedirect("/create?success=invalid");
			return;
		}
		// send the appropriate get request
		if (successful) {
			response.sendRedirect("/create?success=true");
		}
		else {
			response.sendRedirect("/create?success=false");
		}
	}
}