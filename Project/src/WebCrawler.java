import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * A multithreaded web crawler using a work queue to build an inverted index
 * from a seed URL
 * 
 * @author evancarlson
 *
 */
public class WebCrawler extends InvertedIndexBuilder {

	/** A logger specifically for this class. */
	private static final Logger log = LogManager.getLogger(WebCrawler.class);

	/** The inverted index to build/add data to */
	private final InvertedIndex index;

	/** The work queue to use */
	private final WorkQueue queue;

	/** The max number of URLs to parse */
	private int limit;

	/** The set of URLs that have been parsed */
	private Set<URL> visited;

	/**
	 * Initializes a web crawler object.
	 * 
	 * @param index the inverted index to add to
	 * @param queue the work queue to use
	 * @param limit the number of URLs to parse
	 */
	public WebCrawler(InvertedIndex index, WorkQueue queue, int limit) {
		super(index);
		this.index = index;
		this.queue = queue;
		this.limit = limit;
		this.visited = new HashSet<>(limit);
	}

	/**
	 * Crawls the web, starting at a seed URL.
	 * 
	 * @param seed the URL to begin crawling from
	 * @throws IOException
	 */
	public void crawl(URL seed) throws IOException {
		System.out.println("Crawling from " + seed.toString() + "...");
		visited.add(seed); // TODO Changed here
		queue.execute(new Worker(seed));
		queue.finish();
		System.out.println("Crawled " + String.valueOf(limit) + " links.");
	}

	/**
	 * Parses HTML from a URL and adds the plain text data into an inverted index.
	 * This method is accessed by multiple threads.
	 * 
	 * @param url   the URL to parse
	 * @param index the inverted index to add to
	 * @throws IOException
	 */
	public void parse(URL url, InvertedIndex index) throws IOException {

		String html;
		html = HtmlFetcher.fetch(url, 3); // fetch html, allow up to three redirects

		if (html == null) {
			return;
		}

		html = HtmlCleaner.stripBlockElements(html); // strip html block elements
		ArrayList<URL> links = LinkParser.listLinks(url, html);

		for (URL link : links) {
			// if the link is unique and we are still crawling, start a new worker.
			// otherwise, break the loop
			boolean unique = false;
			synchronized (visited) {
				unique = visited.add(link);
				if (visited.size() > limit) { // TODO Changed here
					break;
				}
			}
			if (unique) {
				queue.execute(new Worker(link));
			}
		}

		html = HtmlCleaner.stripTags(html); // strip html tags
		html = HtmlCleaner.stripEntities(html); // strip html entities

		// clean, parse and stem text to populate the inverted index
		Stemmer stemmer = new SnowballStemmer(InvertedIndexBuilder.DEFAULT);
		// starting position
		int pos = 0;
		String location = url.toString();
		// split by white space and clean
		String[] words = TextParser.parse(html);
		// stem and add data to inverted index
		for (String word : words) {
			word = stemmer.stem(word).toString();
			++pos;
			index.add(word, location, pos);
		}
	}

	/**
	 * A worker class that parses URLs and adds data from eligible sites to the
	 * inverted index.
	 */
	private class Worker implements Runnable {

		/** The URL to parse */
		private final URL url;

		/**
		 * Initializes a worker class by cleaning the URL parameter.
		 * 
		 * @param url the URL to begin crawling from
		 */
		private Worker(URL url) {
			this.url = LinkParser.clean(url);
		}

		@Override
		public void run() {
			try {
				InvertedIndex local = new InvertedIndex();
				parse(url, local);

				synchronized (index) {
					index.addAll(local);
				}
			}
			catch (IOException io) {
				log.error("Error parsing ", url);
			}
		}
	}
}