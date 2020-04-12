import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A simple interface for query processing classes.
 * 
 * @author evancarlson
 *
 */
public interface QueriesInterface {

	/**
	 * Default method that executes query searches from a file.
	 * 
	 * @param file  the file of search queries separated by line
	 * @param exact {@true} if exact search should be performed
	 * @throws IOException
	 */
	public default void processQueries(Path file, boolean exact) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);) {
			String line = null;
			// parses each line of the input file of search queries
			while ((line = reader.readLine()) != null) {
				processQuery(line, exact);
			}
		}
	}

	/**
	 * Processes a single search query into unique and stemmed words, then searches
	 * the inverted index and saves the results in resultMap.
	 * 
	 * @param line  the search query to parse and search
	 * @param exact defines whether to perform an exact or partial search
	 */
	public void processQuery(String line, boolean exact);

	/**
	 * Writes the resultMap in JSON format to a file.
	 * 
	 * @param path the file to write to
	 * @throws IOException
	 */
	public void writeJSON(Path path) throws IOException;
}
