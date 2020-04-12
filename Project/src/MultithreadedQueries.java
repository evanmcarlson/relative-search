import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * A class for working with query files using multithreading.
 * 
 * @author evancarlson
 *
 */
public class MultithreadedQueries implements QueriesInterface {

	/**
	 * The inverted index to search.
	 */
	private final ThreadSafeInvertedIndex index;

	/** The work queue to use */
	private final WorkQueue queue;

	/**
	 * Maps query strings to sorted search results.
	 */
	private final SortedMap<String, ArrayList<InvertedIndex.SearchResult>> resultMap;

	/**
	 * Initializes a multithreaded query processor.
	 * 
	 * @param index the inverted index to search
	 * @param queue the work queue to use
	 */
	public MultithreadedQueries(ThreadSafeInvertedIndex index, WorkQueue queue) {
		// super(index);
		this.index = index;
		this.queue = queue;
		this.resultMap = new TreeMap<>();
	}

	/**
	 * Processes queries multithreaded using a Worker class that adds Runnable
	 * parsers on each file to a work queue.
	 * 
	 * @param file  the file of search queries separated by line
	 * @param exact {@true} if exact search should be performed
	 * @throws IOException
	 */
	public void processQueries(Path file, boolean exact) throws IOException {
		QueriesInterface.super.processQueries(file, exact);
		queue.finish();
	}

	public void processQuery(String line, boolean exact) {
		queue.execute(new Searcher(line, exact));
	}

	/**
	 * Cleans a query string into a stemmed set of words.
	 * 
	 * @param uncleaned a user-input query string
	 * @return a set of the cleaned and stemmed words from the string
	 */
	public static Set<String> cleanQuery(String uncleaned) {
		// clean the query
		String[] cleanedWords = TextParser.parse(uncleaned);
		// return if there are no valid words in the query
		if (cleanedWords.length == 0) {
			return new TreeSet<String>();
		}
		// create a stemmer
		Stemmer stemmer = new SnowballStemmer(InvertedIndexBuilder.DEFAULT);
		// initialize an empty set of sorted and unique stems from a query
		TreeSet<String> cleaned = new TreeSet<>();
		// stem and add each word from the query
		for (String word : cleanedWords) {
			word = stemmer.stem(word).toString();
			cleaned.add(word);
		}

		return cleaned;
	}

	/**
	 * should be private? Searches the inverted index from a query and prepares
	 * search results.
	 */
	class Searcher implements Runnable {
		/** The search query as a line */
		String line;

		/** The search specifier. True if exact, false if partial */
		boolean exact;

		/**
		 * Initializes a searcher given a query as a string and a search specifier.
		 *
		 * @param line  the search query as a string
		 * @param exact {@code true} if exact search should be performed
		 */
		public Searcher(String line, boolean exact) {
			this.line = line;
			this.exact = exact;
		}

		@Override
		public void run() {
			// split and clean the words in the query
			String[] cleanedWords = TextParser.parse(line);
			// return if there are no valid words in the query
			if (cleanedWords.length == 0) {
				return;
			}
			// create one stemmer per query
			Stemmer stemmer = new SnowballStemmer(InvertedIndexBuilder.DEFAULT);
			// initialize an empty set of sorted and unique stems from a query
			TreeSet<String> query = new TreeSet<>();
			// stem and add each word from the query
			for (String word : cleanedWords) {
				word = stemmer.stem(word).toString();
				query.add(word);
			}

			String joined = String.join(" ", query);

			// if the query already has results, return
			synchronized (resultMap) {
				if (resultMap.containsKey(joined)) {
					return;
				}
			}

			// search for results
			ArrayList<InvertedIndex.SearchResult> results = index.search(query, exact);

			// add the query and its results to the resultMap
			synchronized (resultMap) {
				resultMap.put(joined, results);
			}
		}
	}

	/**
	 * Writes the resultMap in JSON format to a file.
	 * 
	 * @param path the file to write to
	 * @throws IOException
	 */
	public void writeJSON(Path path) throws IOException {
		synchronized (resultMap) {
			JSONWriter.writeResults(resultMap, path);
		}
	}
}