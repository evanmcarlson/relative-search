import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class responsible for running this project based on the provided command-line
 * arguments.
 *
 * @author evancarlson
 */
public class Driver {

	/** A logger specifically for this class. */
	private static final Logger log = LogManager.getLogger(Driver.class);

	/**
	 * Initializes the classes necessary based on the provided command-line
	 * arguments. This includes the argument parser itself to parse command-line
	 * arguments, an inverted index and its builder, and the configuration of JSON
	 * output.
	 *
	 * @param args flag/value pairs used to start this program
	 */
	public static void main(String[] args) {

		// parse command line arguments
		ArgumentParser parser = new ArgumentParser(args);

		// if there are no arguments or no valid flags provided, end the program
		if (parser.numFlags() == 0) {
			System.out.println("Error: No valid arguments provided.");
			log.debug("No valid arguments provided");
			return;
		}

		// the inverted index
		InvertedIndex index;

		// the inverted index builder
		InvertedIndexBuilder builder;

		// an interface for the class that will process queries
		QueriesInterface queries;

		// the queue that handles tasks
		WorkQueue queue = null;

		SearchServer server = null;

		// true if the program should use multithreading
		boolean multithreaded = parser.hasFlag("-threads") || parser.hasFlag("-url") || parser.hasFlag("-port");

		if (multithreaded) {
			// retrieve and set thread input
			final int threads;
			String threadInput = parser.getString("-threads");
			// check that the user input is an int greater than 0
			if (TextParser.isInt(threadInput) && Integer.parseInt(parser.getString("-threads")) > 0) {
				// set thread count
				threads = Integer.parseInt(parser.getString("-threads"));
			}
			// otherwise, input was not valid and defaults to five threads
			else {
				threads = 5;
				System.out.println("Defaulted to 5 threads.");
			}
			// create a work queue
			queue = new WorkQueue(threads);
			// create a thread safe inverted index
			ThreadSafeInvertedIndex threadSafe = new ThreadSafeInvertedIndex();
			index = threadSafe;

			// determine method of building - from files or web pages?
			if (parser.hasFlag("-url") && parser.hasValue("-url")) {
				int limit;

				// if they included a limit
				if (parser.hasFlag("-limit")) {
					String input = parser.getString("-limit");
					// validate it and set it
					if (TextParser.isInt(input) && Integer.parseInt(input) > 0) {
						limit = Integer.parseInt(input);
					}
					// otherwise, set it to a default of fifty
					else {
						limit = 50;
					}
				}
				// default to a limit of fifty to avoid an infinite crawl
				else {
					limit = 50;
				}
				WebCrawler crawler = new WebCrawler(index, queue, limit);
				// crawl the web starting at the seed link and add to index
				builder = crawler;
				int port;
				String inputPort = parser.getString("-port");
				if (TextParser.isInt(inputPort)) {
					port = Integer.parseInt(inputPort);
				}
				else {
					port = 8080;
				}
				server = new SearchServer(threadSafe, queue, limit, port);
			}
			else {
				// create a multithreaded inverted index builder
				builder = new MultithreadedInvertedIndexBuilder(threadSafe, queue);
			}
			// create a multithreaded query processor
			queries = new MultithreadedQueries(threadSafe, queue);
		}
		else {
			// create a simple inverted index
			index = new InvertedIndex();
			// create an inverted index builder
			builder = new InvertedIndexBuilder(index);
			// create a query processor
			queries = new Queries(index);
		}

		// process other command line arguments

		// if building from web pages
		if (parser.hasFlag("-url") && parser.hasValue("-url")) {
			URL seed = null;
			// grab the input seed url
			try {
				seed = new URL(parser.getString("-url"));
				WebCrawler crawler = (WebCrawler) builder;
				crawler.crawl(seed);
			}
			catch (Exception e) {
				log.debug("Invalid URL: ", parser.getString("-url"));
			}
		}

		// if building from files
		if (parser.hasFlag("-path") && parser.hasValue("-path")) {
			// retrieve provided input path
			Path inPath = parser.getPath("-path");
			try {
				builder.build(inPath);
			}
			catch (IOException e) {
				System.out.println("Error building inverted index.");
				log.debug("Error building InvertedIndex from path: ", inPath);
			}
		}

		// if there is an index output flag
		if (parser.hasFlag("-index")) {
			// retrieve provided output path or default to index.json
			Path outPath = parser.getPath("-index", Path.of("index.json"));
			try {
				// print inverted index as pretty JSON file
				index.writeIndex(outPath);
			}
			catch (IOException e) {
				System.out.println("Error writing index as JSON to " + outPath.toString());
				log.debug("Error outputting index JSON to: ", outPath);
			}
		}

		// if there is a count output flag
		if (parser.hasFlag("-counts")) {
			// retrieve provided output path or default to counts.json
			Path outPath = parser.getPath("-counts", Path.of("counts.json"));
			try {
				// print files and word-count as pretty JSON objects
				JSONWriter.writeCounts(index.getLocationToCountMap(), outPath);
			}
			catch (IOException e) {
				System.out.println("Error outputting word counts.");
				log.debug("Error outputting count JSON to: ", outPath);
			}
		}

		// if there is a query input flag
		if (parser.hasFlag("-query") && parser.hasValue("-query")) {
			// retrieve provided input path
			Path inPath = parser.getPath("-query");
			try {
				boolean exact = parser.hasFlag("-exact");
				queries.processQueries(inPath, exact);
			}
			catch (IOException e) {
				System.out.println("There was a problem reading queries from " + inPath);
				log.debug("Error processing queries from", inPath);
			}
		}

		// if there is a results output flag
		if (parser.hasFlag("-results")) {
			// retrieve provided output path or default to results.json
			Path outPath = parser.getPath("-results", Path.of("results.json"));
			try {
				// print queries and results as a pretty JSON file
				queries.writeJSON(outPath);
			}
			catch (IOException e) {
				System.out.println("There was a problem writing search results to " + outPath);
				log.debug("Error writing search results to ", outPath);
			}
		}

		// launch search engine!
		if (server != null) {
			try {
				server.start();
			}
			catch (Exception e) {
				log.debug("Error starting jetty server");
			}
		}

		// gracefully shutdown queue
		if (queue != null) {
			queue.shutdown();
			log.info("WorkQueue is shut down.");
		}
	}
}