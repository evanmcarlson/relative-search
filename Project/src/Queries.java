import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * A class used for working with query files.
 * 
 * @author evancarlson
 *
 */
public class Queries implements QueriesInterface {

	/** The inverted index to search */
	private final InvertedIndex index;

	/**
	 * Maps query strings to sorted search results.
	 */
	private final SortedMap<String, ArrayList<InvertedIndex.SearchResult>> resultMap;

	/**
	 * Initializes a query processor
	 * 
	 * @param index the inverted index the search
	 */
	public Queries(InvertedIndex index) {
		this.index = index;
		this.resultMap = new TreeMap<>();
	}

	/**
	 * Processes a single search query into unique and stemmed words, then searches
	 * the inverted index and saves the results in resultMap.
	 * 
	 * @param line  the search query to parse and search
	 * @param exact defines whether to perform an exact or partial search
	 */
	public void processQuery(String line, boolean exact) {
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
		if (resultMap.containsKey(joined)) {
			return;
		}

		// search for results
		ArrayList<InvertedIndex.SearchResult> results = index.search(query, exact);

		// add the query and its results to the resultMap
		resultMap.put(joined, results);
	}

	/**
	 * Writes the resultMap in JSON format to a file.
	 * 
	 * @param path the file to write to
	 * @throws IOException
	 */
	public void writeJSON(Path path) throws IOException {
		JSONWriter.writeResults(resultMap, path);
	}
}