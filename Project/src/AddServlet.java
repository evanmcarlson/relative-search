import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet responsible for displaying add context page. When a valid URL is
 * submitted on this page, initializes a web crawler on that URL and adds data
 * to the inverted index.
 * 
 * @see SearchServer
 * @author evancarlson
 */
public class AddServlet extends HttpServlet {

	/** A logger specifically for this class. */
	private static final Logger log = LogManager.getLogger(AddServlet.class);

	/** ID used for serialization, which we are not using. */
	private static final long serialVersionUID = 1L;

	/** Title of web page. */
	private static final String TITLE = "Add Context";

	/** The inverted index to search */
	ThreadSafeInvertedIndex index;

	/** The work queue to handle tasks */
	WorkQueue queue;

	/** The limit of URL to crawl; used by the web crawler */
	int limit;

	/**
	 * Initalizes an AddServlet with necessary objects and variables.
	 * 
	 * @param index the inverted index to search
	 * @param queue the work queue to use
	 * @param limit the max number of URLs to crawl
	 */
	public AddServlet(ThreadSafeInvertedIndex index, WorkQueue queue, int limit) {
		super();
		this.index = index;
		this.queue = queue;
		this.limit = limit;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		log.info("AddServlet ID " + this.hashCode() + " handling GET request.");

		String url = null;

		if (request.getParameter("url") != null) {
			// read from form field
			url = request.getParameter("url");
			// sanitize url to protect against XSS
			url = StringEscapeUtils.escapeHtml4(url); // escape anything that looks like script
			// add to the index using a WebCrawler
			URL seed = new URL(url);
			WebCrawler crawler = new WebCrawler(index, queue, limit);
			crawler.crawl(seed);
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
		out.printf("		<script defer src=\"https://use.fontawesome.com/releases/v5.3.1/js/all.js\"></script>%n");
		out.printf("	</head>%n");

		out.printf("	<body>%n");
		out.printf("		<section class=\"hero is-primary is-bold\" style=\"height:100vh; text-align:center;\">%n");
		out.printf("	  		<div class=\"hero-body\">%n");
		out.printf("	    		<div class=\"container\" style=\"padding-top:20vh;\">%n");
		out.printf("	      			<h1 class=\"title\">%n");
		out.printf("	        		Add Context%n");
		out.printf("	      			</h1>%n");
		out.printf("	      			<h2 class=\"subtitle\">%n");
		out.printf("					<i class=\"fas fa-plus\"></i>%n");
		out.printf(
				"						&nbsp;The web is a big place.</h2>%n<h2 class=\"subtitle\">Add a URL to search only on sites closely related to it.");
		out.printf("	      			</h2>%n");

		// the search form
		out.printf("					<form method=\"get\" action=\"/add\"%n>");
		out.printf("						<div class=\"field has-addons has-addons-centered\">%n");
		out.printf("							<div class=\"control\">%n");
		out.printf("									<p><input class=\"input\" type=\"text\"  name=\"url\">%n");
		out.printf("			  				</div>%n");
		out.printf(" 							<div class=\"control\">%n");
		out.printf(
				"			    					<button class=\"button is-primary\" type=\"submit\">Add</button>%n");
		out.printf(" 							</div>%n");
		out.printf("						</div>%n");
		out.printf("					</form>%n");

		if (url != null) {
			out.printf("				<h3>Added %s to your context.</h3>%n", url);
		}

		out.printf(
				"					<p>View the <a href=\"/locations\" style=\"text-decoration:underline;\">sites</a> or <a href=\"/index\" style=\"text-decoration:underline;\">words</a> in your context.</p>%n");
		out.printf("					<p>Back to <a href=\"/\" style=\"text-decoration:underline;\">home</a>.%n");
		out.printf("	    		</div>%n");
		out.printf("	    	</div>%n");

		// thread info
		out.printf("			<div style=\"padding:30px;\">%n");
		out.printf("	    		<p>%n");
		out.printf("	      		This request was handled by thread %s.%n", Thread.currentThread().getName());
		out.printf("	    		</p>%n");
		out.printf("			</div>%n");

		out.printf("		</section>%n");
		out.printf("	</body>%n");
		out.printf("</html>%n");
		out.close();
		response.setStatus(HttpServletResponse.SC_OK);
	}
}