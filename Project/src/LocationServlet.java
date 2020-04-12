import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet responsible for displaying the index locations & counts.
 * 
 * @see SearchServer
 * @author evancarlson
 */
public class LocationServlet extends HttpServlet {

	/** A logger specifically for this class. */
	private static final Logger log = LogManager.getLogger(LocationServlet.class);

	/** ID used for serialization, which we are not using. */
	private static final long serialVersionUID = 1L;

	/** The title to use for this webpage. */
	private static final String TITLE = "Show Sites";

	/* The location to count map to display */
	Map<String, Integer> locationMap;

	/**
	 * Initalizes a LocationServlet with a location to count map.
	 * 
	 * @param locationMap the map to display
	 */
	public LocationServlet(Map<String, Integer> locationMap) {
		super();
		this.locationMap = locationMap;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		log.info("LocationServlet ID " + this.hashCode() + " handling GET request.");

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
		out.printf("		<section class=\"hero is-primary is-bold\">%n");
		out.printf("			<div class=\"hero-body\"\">%n");
		out.printf("				<div class=\"container\" style=\"width:90vw; display:inline-block;\">%n");

		// the text
		out.printf("					<div style=\"width:auto; float:left;\">%n");
		out.printf("	      				<h1 class=\"title\">%n");
		out.printf("	        			<a href=\"/\">Show Sites</a>%n");
		out.printf("	      				</h1>%n");
		out.printf("	      				<h2 class=\"subtitle\">%n");
		out.printf("						<i class=\"fas fa-search\"></i>%n");
		out.printf("						&nbsp;Viewing sites in your index%n");
		out.printf("	      				</h2>%n");
		out.printf("					</div>%n");

		// the search form
		out.printf("					<div style=\"width:auto; float:right;\">%n");
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
		out.printf("					</div>%n");

		out.printf("				</div>%n");
		out.printf("			</div>%n");
		out.printf("		</section>%n");

		out.printf("		<section class=\"section\">%n");
		out.printf("			<div class=\"container\">%n");

		// output the inverted index

		// for each key in the index:

		for (var entry : locationMap.entrySet()) {
			String location = entry.getKey();
			Integer count = entry.getValue();
			out.printf("			<ul>");
			out.printf("				<li><a href=\"%s\">%s</a> - %d words</li>", location, location, count);
			out.printf("			</ul>");
		}

		out.printf("			</div>%n");
		out.printf("		</section>%n");

		out.printf("		<footer class=\"footer\">%n");
		out.printf("	  		<div class=\"content has-text-centered\">%n");
		out.printf("	    		<p>%n");
		out.printf("				This request was handled by thread %s.%n", Thread.currentThread().getName());
		out.printf("				</p>%n");
		out.printf("	  		</div>%n");
		out.printf("		</footer>%n");

		out.printf("	</body>%n");
		out.printf("</html>%n");
		out.close();
		response.setStatus(HttpServletResponse.SC_OK);
	}
}