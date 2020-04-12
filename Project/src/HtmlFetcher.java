import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

//import org.apache.commons.validator.routines.UrlValidator;

/**
 * A specialized version of {@link HttpsFetcher} that follows redirects and
 * returns HTML content if possible.
 *
 * @see HttpsFetcher
 */
public class HtmlFetcher {

	/**
	 * Returns {@code true} if and only if there is a "Content-Type" header and the
	 * first value of that header starts with the value "text/html"
	 * (case-insensitive).
	 *
	 * @param headers the HTTP/1.1 headers to parse
	 * @return {@code true} if the headers indicate the content type is HTML
	 */
	public static boolean isHtml(Map<String, List<String>> headers) {
		if (headers.get("Content-Type") != null) {
			if (headers.get("Content-Type").get(0).toLowerCase().startsWith("text/html")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parses the HTTP status code from the provided HTTP headers, assuming the
	 * status line is stored under the {@code null} key.
	 *
	 * @param headers the HTTP/1.1 headers to parse
	 * @return the HTTP status code or -1 if unable to parse for any reasons
	 */
	public static int getStatusCode(Map<String, List<String>> headers) {
		try {
			String[] info = headers.get(null).get(0).split(" ");
			return Integer.parseInt(info[1]);
		}
		catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Returns {@code true} if and only if the HTTP status code is between 300 and
	 * 399 (inclusive) and there is a "Location" header with at least one value.
	 *
	 * @param headers the HTTP/1.1 headers to parse
	 * @return {@code true} if the headers indicate the content type is HTML
	 */
	public static boolean isRedirect(Map<String, List<String>> headers) {
		// null=[HTTP/1.1 301 Moved Permanently]
		int status = getStatusCode(headers);
		if (300 <= status && status <= 399) {
			if (headers.get("Location").get(0) != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Fetches the resource at the URL using HTTP/1.1 and sockets. If the status
	 * code is 200 and the content type is HTML, returns the HTML as a single
	 * string. If the status code is a valid redirect, will follow that redirect if
	 * the number of redirects is greater than 0. Otherwise, returns {@code null}.
	 *
	 * @param url       the url to fetch
	 * @param redirects the number of times to follow redirects
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 * @throws IOException
	 *
	 * @see HttpsFetcher#openConnection(URL)
	 * @see HttpsFetcher#printGetRequest(PrintWriter, URL)
	 * @see HttpsFetcher#getHeaderFields(BufferedReader)
	 * @see HttpsFetcher#getContent(BufferedReader)
	 *
	 * @see String#join(CharSequence, CharSequence...)
	 *
	 * @see #isHtml(Map)
	 * @see #isRedirect(Map)
	 */
	public static String fetch(URL url, int redirects) throws IOException {
		try (Socket socket = HttpsFetcher.openConnection(url);
				PrintWriter request = new PrintWriter(socket.getOutputStream());
				InputStreamReader input = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
				BufferedReader response = new BufferedReader(input);) {
			HttpsFetcher.printGetRequest(request, url);
			Map<String, List<String>> headers = HttpsFetcher.getHeaderFields(response);
			if (isRedirect(headers) && redirects > 0) {
				return fetch(new URL(headers.get("Location").get(0)), redirects - 1);
			}
			else if (getStatusCode(headers) == 200 && isHtml(headers)) {
				return getHtml(response);
			}
			return null;
		}
	}

	/**
	 * Converts the {@link String} url into a {@link URL} object and then calls
	 * {@link #fetch(URL, int)}.
	 *
	 * @param url       the url to fetch
	 * @param redirects the number of times to follow redirects
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 * @throws IOException
	 *
	 * @see #fetch(URL, int)
	 */
	public static String fetch(String url, int redirects) throws IOException {
		try {
			return fetch(new URL(url), redirects);
		}
		catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Converts the {@link String} url into a {@link URL} object and then calls
	 * {@link #fetch(URL, int)} with 0 redirects.
	 *
	 * @param url the url to fetch
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 * @throws IOException
	 *
	 * @see #fetch(URL, int)
	 */
	public static String fetch(String url) throws IOException {
		return fetch(url, 0);
	}

	/**
	 * Calls {@link #fetch(URL, int)} with 0 redirects.
	 *
	 * @param url the url to fetch
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 * @throws IOException
	 */
	public static String fetch(URL url) throws IOException {
		return fetch(url, 0);
	}

	/**
	 * Gets the html from a socket. Whether this output includes headers depends how
	 * the socket connection has already been used.
	 *
	 * @param response the reader created from a socket connection
	 * @return a string of the URL's HTML
	 * @throws IOException
	 */
	public static String getHtml(BufferedReader response) throws IOException {
		List<String> lines = HttpsFetcher.getContent(response);
		return String.join("\n", lines);
	}

	/**
	 * Checks if a URL is valid and can be correctly parsed.
	 * 
	 * @param url the URL to check
	 * @return {@code true} if the URL is valid
	 */
	public static boolean isValid(String url) {
		// UrlValidator urlValidator = new UrlValidator();
		// return urlValidator.isValid(url);
		return url != null && !url.isBlank();
	}
}