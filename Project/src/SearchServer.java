import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * A class responsible for configuring and running an embedded jetty server.
 * 
 * @author evancarlson
 */
public class SearchServer {

	/** A logger specifically for this class. */
	private static final Logger log = LogManager.getLogger(SearchServer.class);

	/** The end of transmission signal; used for server shutdown */
	private static final String EOT = "stop";

	/** The inverted index */
	private final ThreadSafeInvertedIndex index;

	/** The port to connect the server to */
	private final int port;

	/** The WorkQueue to handle tasks */
	private final WorkQueue queue;

	/** The number of URLs to crawl when adding to index */
	private final int limit;

	/**
	 * Initializes a server with necessary information.
	 * 
	 * @param index an inverted index
	 * @param queue a WorkQueue
	 * @param limit the number of URLs to crawl
	 * @param port  the port to connect to
	 */
	public SearchServer(ThreadSafeInvertedIndex index, WorkQueue queue, int limit, int port) {
		this.index = index;
		this.queue = queue;
		this.limit = limit;
		this.port = port;
	}

	/**
	 * Starts and configures a jetty web server instance.
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {

		Server server = new Server(port);

		// open a connection to the database
		String properties = "database.properties";
		DatabaseConnector connector = new DatabaseConnector(properties);

		// if the database cannot connect, do not start the server
		if (!connector.testConnection()) {
			return;
		}

		// type of handler that supports sessions
		ServletContextHandler servletContext = null;

		// turn on sessions and set context
		servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		servletContext.setContextPath("/");

		// add servlet mappings to the handler
		servletContext.addServlet(MainServlet.class, "/");
		servletContext.addServlet(new ServletHolder(new SearchServlet(index)), "/results");
		servletContext.addServlet(new ServletHolder(new AddServlet(index, queue, limit)), "/add");
		servletContext.addServlet(new ServletHolder(new IndexServlet(index)), "/index");
		servletContext.addServlet(new ServletHolder(new LocationServlet(index.getLocationToCountMap())), "/locations");
		servletContext.addServlet(new ServletHolder(new CreateServlet(connector)), "/create");
		servletContext.addServlet(new ServletHolder(new LoginServlet(connector)), "/login");
		servletContext.addServlet(LogoutServlet.class, "/logout");
		// temporary fix, while debugging ShutdownHandler...
		servletContext.addServlet(new ServletHolder(new ShutdownServlet(server)), "/shutdown");

		// default handler for favicon.ico requests
		DefaultHandler defaultHandler = new DefaultHandler();
		defaultHandler.setServeIcon(true);
		ContextHandler defaultContext = new ContextHandler("/favicon.ico");
		defaultContext.setHandler(defaultHandler);

		// handler for server shutdown
//		@SuppressWarnings("deprecation")
//		ShutdownHandler shutdownHandler = new ShutdownHandler(server, "stop");
//		ContextHandler shutdownContext = new ContextHandler("/shutdown");
//		shutdownContext.setHandler(shutdownHandler);

		// setup handler order
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { defaultContext, servletContext });

		// setup jetty server
		server.setHandler(handlers);
		server.start();
		server.join();
	}

	/**
	 * The servlet class responsible for displaying the main search page.
	 * 
	 * @see SearchServer
	 */
	public static class MainServlet extends HttpServlet {

		/** ID used for serialization, which we are not using. */
		private static final long serialVersionUID = 1L;

		/** Title of web page. */
		private static final String TITLE = "Simple Search Engine";

		/**
		 * Displays the main search page
		 */
		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

			log.info("MainServlet ID " + this.hashCode() + " handling GET request.");

			// tracks user state
			HttpSession session = request.getSession(true);
			String username = null;

			// try to get user info
			try {
				username = (String) session.getAttribute("username");
				// sanitize; we are putting the name in the HTML
				username = StringEscapeUtils.escapeHtml4(username);
			}
			catch (Exception e) {
				log.debug(e);
			}

			// form HTML
			response.setContentType("text/html");
			PrintWriter out = response.getWriter();

			out.printf("<!DOCTYPE html>%n");
			out.printf("<html>%n");
			out.printf("	<head>%n");
			out.printf("		<meta charset=\"utf-8\">%n");
			out.printf("		<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">%n");
			out.printf("		<title>%s</title>%n", TITLE);
			out.printf(
					"			<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bulma@0.8.0/css/bulma.min.css\">%n");
			out.printf(
					"		<script defer src=\"https://use.fontawesome.com/releases/v5.3.1/js/all.js\"></script>%n");
			out.printf("	</head>%n");
			out.printf("	<body>%n");
			out.printf(
					"		<section class=\"hero is-primary is-bold\" style=\"height:100vh; text-align:center;\">%n");
			out.printf("	  		<div class=\"hero-body\">%n");

			// if user IS NOT logged in, display the login button
			if (username == null) {
				out.printf(
						"				<form method=\"get\" action=\"/login\" style=\"text-align:right; margin-right:30px;\">");

				out.printf(
						"			    	<button class=\"button is-primary\" type=\"submit\">Sign in</button>%n");

				out.printf("			</form>%n");
			}
			// if user IS logged in, display the account
			else {
				out.printf("			<details style=\"text-align:right;\">");
				out.printf("				<summary style=\"outline:none;\">%s</summary>", username);
				out.printf("				<a href=\"/logout\">Log out</a>");
				out.printf("			</details>");
			}

			out.printf("	    		<div class=\"container\" style=\"width:100vw; padding-top:20vh;\">%n");
			out.printf("	      			<h1 class=\"title\">Relative Search</h1>%n");
			out.printf(
					"	      			<h2 class=\"subtitle\"><i class=\"fas fa-search\"></i>&nbsp;A search engine for contextual information retrieval</h2>%n");

			// the search form
			out.printf("					<form method=\"get\" action=\"/results\"%n>");
			out.printf("						<div class=\"field has-addons has-addons-centered\">%n");
			out.printf("							<div class=\"control\">%n");
			out.printf("									<p><input class=\"input\" type=\"text\"  name=\"url\">%n");
			out.printf("			  				</div>%n");
			out.printf(" 							<div class=\"control\">%n");
			out.printf(
					"			    					<button class=\"button is-primary\" type=\"submit\">Search</button>%n");
			out.printf(" 							</div>%n");
			out.printf("						</div>%n");
			out.printf("					</form>%n");

			out.printf(
					"						<p>or, <a href=\"/add\" style=\"text-decoration:underline;\">add to your context</a>.");
			out.printf("	    		</div>%n");
			out.printf("			</div>%n");

			// thread info
			out.printf("			<div style=\"padding:30px;\">%n");
			out.printf("				<p>This request was handled by thread %s.</p>%n",
					Thread.currentThread().getName());
			out.printf("			</div>%n");

			out.printf("		</section>%n");
			out.printf("	</body>%n");
			out.printf("</html>%n");
			out.close();
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	/**
	 * Servlet responsible for shutting the server down gracefully. Intended for
	 * admin use only.
	 * 
	 * @see SearchServer
	 */
	public class ShutdownServlet extends HttpServlet {

		/** A logger specifically for this class. */
		private final Logger log = LogManager.getLogger(ShutdownServlet.class);

		/** ID used for serialization, which we are not using. */
		private static final long serialVersionUID = 1L;

		/** Title of web page. */
		private static final String TITLE = "Site Shutdown";

		/** The server to shut down */
		private Server server;

		/**
		 * Initializes the servlet with a server.
		 * 
		 * @param server the server to shut down
		 */
		public ShutdownServlet(Server server) {
			this.server = server;
		}

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

			log.info("ShutdownServlet ID " + this.hashCode() + " handling GET request.");

			response.setStatus(202);
			PrintWriter out = response.getWriter();
			response.setContentType("text/html");

			if (request.getParameter("password") != null) {
				String password = request.getParameter("password");
				// sanitize query to protect against XSS
				password = StringEscapeUtils.escapeHtml4(password); // escape anything that looks like script
				if (password.equals(EOT)) {
					out.printf("<p>Authenticated; shutting down.</p>");

					try {
						// Stop the server.
						new Thread() {
							@Override
							public void run() {
								try {
									log.info("Shutting down Jetty...");
									server.stop();
									log.info("Jetty is shut down.");
								}
								catch (Exception ex) {
									log.error("Error when stopping Jetty: " + ex.getMessage(), ex);
								}
							}
						}.start();
					}
					catch (Exception ex) {
						log.error("Unable to stop Jetty: " + ex);
					}
					return;
				}
			}

			out.printf("<!DOCTYPE html>%n");
			out.printf("<html>%n");
			out.printf("	<head>%n");
			out.printf("		<meta charset=\"utf-8\">%n");
			out.printf("		<title>%s</title>%n", TITLE);
			out.printf("	</head>%n");
			out.printf("%n");
			out.printf("	<body>%n");

			// the search form
			out.printf("		<form method=\"get\" action=\"/shutdown\">");
			out.printf("			<div class=\"control\" style=\"display:inline-block; padding:8px\">%n");
			out.printf(
					"				<p><input type=\"text\"  name=\"password\" style=\"line-height:1.75em\" maxlength=\"50\" placeholder=\"Admin password\" size=\"50\">");
			out.printf("			</div>%n");

			out.printf("			<button class=\"button is-primary\" type=\"submit\">Submit</button>%n");
			out.printf("			</div>%n");
			out.printf("		</form>%n");
			out.printf("	</body>%n");
			out.printf("</html>%n");
			response.flushBuffer();
			out.close();
		}
	}
}