import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet responsible for displaying search results.
 * 
 * @see SearchServer
 * @author evancarlson
 */
public class SearchServlet extends HttpServlet {

	/** A logger specifically for this class. */
	private static final Logger log = LogManager.getLogger(SearchServlet.class);

	/** ID used for serialization, which we are not using. */
	private static final long serialVersionUID = 1L;

	/** The title to use for this webpage. */
	private static final String TITLE = "Search Results";

	/* The inverted index to search */
	ThreadSafeInvertedIndex index;

	/**
	 * Initializes a SearchServlet with an index.
	 * 
	 * @param index the inverted index to search
	 */
	public SearchServlet(ThreadSafeInvertedIndex index) {
		super();
		this.index = index;
	}

	/**
	 * Displays some text, a search form, and clickable search results.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		log.info("MessageServlet ID " + this.hashCode() + " handling GET request.");

		// store initial start time
		Instant start = Instant.now();

		String query;
		if (request.getParameter("url") == null) {
			response.sendRedirect("/");
			return;
		}
		else {
			// read from form field
			query = request.getParameter("url");
		}

		// sanitize query to protect against XSS
		query = StringEscapeUtils.escapeHtml4(query);

		// clean the query
		Set<String> cleanedQuery = MultithreadedQueries.cleanQuery(query);

		// search for results
		ArrayList<InvertedIndex.SearchResult> results = index.partialSearch(cleanedQuery);

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
		out.printf("	      				<h1 class=\"title\"><a href=\"/\">Simple Search Engine</a></h1>%n");
		out.printf(
				"	      				<h2 class=\"subtitle\"><i class=\"fas fa-search\"></i>&nbsp;Searching for %s",
				query);
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

		// calculate time elapsed and output
		Duration elapsed = Duration.between(start, Instant.now());
		double seconds = (double) elapsed.toMillis() / Duration.ofSeconds(1).toMillis();

		// the results
		if (results.isEmpty()) {
			out.printf("			<h2 class=\"title\"><i class=\"fas fa-times\"></i> No results found.</h2>%n");
		}
		else {
			out.printf(
					"				<h2 class=\"title\"><i class=\"fas fa-check\"></i> Found %d results in %f seconds.</h2>%n",
					results.size(), seconds);

			// How can I retrieve the title and content-length efficiently? That is,
			// without having to fetch the headers and HTML for each result, as these
			// operations have already happened at some point.

			out.printf("<ol>%n");
//			Pattern regex = Pattern.compile("<title>(.*?)</title>");
			for (InvertedIndex.SearchResult result : results) {
				// the url as a string
				String resultUrl = result.getName();
				double score = result.getScore();
				int count = result.getCount();

				// inefficient approach:

				// fetch the html to retrieve the page title
//				String html = HtmlFetcher.fetch(resultUrl);
//				Matcher matcher = regex.matcher(html);
//				String title = matcher.group(0);
				// retrieve word count
//				Integer wordCount = index.getLocationToCountMap().get(resultUrl);
				// fetch the url headers to retrieve content length
//				Map<String, List<String>> headers = HttpsFetcher.fetch(resultUrl);
//				List<String> lengthValues = headers.get("Content-Length");
//				String contentLength = lengthValues.get(0);

				out.printf(
						"				<li><a href=\"%s\">%s</a><p style=\"size: 10px; background-color:#eee;\"> Score: %f | Count: %d</p></li>",
						resultUrl, resultUrl, score, count);
			}
			out.printf("			</ol>%n");
		}
		out.printf("			</div>%n");
		out.printf("		</section>%n");

		// thread info
		out.printf("		<footer class=\"footer\">%n");
		out.printf("	  		<div class=\"content has-text-centered\">%n");
		out.printf("				<p>This request was handled by thread %s.</p>%n", Thread.currentThread().getName());
		out.printf("			</div>%n");
		out.printf("		</footer>%n");

		out.printf("	</body>%n");
		out.printf("</html>%n");
		out.close();
		response.setStatus(HttpServletResponse.SC_OK);
	}
}