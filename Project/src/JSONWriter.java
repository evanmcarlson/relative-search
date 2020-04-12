import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Outputs inverted index in "pretty" JSON format where newlines are used to
 * separate elements and nested elements are indented.
 *
 * Warning: This class is not thread-safe. If multiple threads access this class
 * concurrently, access must be synchronized externally.
 *
 * @author evancarlson
 */
public class JSONWriter {

	/**
	 * Writes the elements as a pretty JSON array.
	 * 
	 * In the use-case of an inverted index, this function takes in the ArrayList of
	 * locations where a stem is found within a file and writes them as a pretty
	 * JSON array.
	 *
	 * @param elements the elements to write
	 * @param writer   the writer to use
	 * @param level    the initial indent level
	 * @throws IOException
	 * 
	 * @see #arrayEntry(Integer, Writer, int)
	 * @see #indent(String, Writer, int)
	 */
	public static void asArray(Collection<Integer> elements, Writer writer, int level) throws IOException {
		writer.write('[');
		var iterator = elements.iterator();
		// if there's a first element, write it
		if (iterator.hasNext()) {
			arrayEntry(iterator.next(), writer, level + 1);
		}
		// for all subsequent entries, write a comma and the next entry
		while (iterator.hasNext()) {
			writer.write(",");
			arrayEntry(iterator.next(), writer, level + 1);
		}
		writer.write('\n');
		indent("]", writer, level - 1);
	}

	/**
	 * Writes the elements as a pretty JSON array to a file.
	 *
	 * @param elements the elements to write
	 * @param path     the file path to use
	 * @throws IOException
	 *
	 * @see #asArray(Collection, Writer, int)
	 */
	public static void asArray(Collection<Integer> elements, Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			asArray(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON array.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see #asArray(Collection, Writer, int)
	 */
	public static String asArray(Collection<Integer> elements) {
		try {
			StringWriter writer = new StringWriter();
			asArray(elements, writer, 0);
			return writer.toString();
		}
		catch (IOException e) {
			return null;
		}
	}

	/**
	 * Writes a single element in a pretty JSON array; that is, an element on its
	 * own line and indented a specified amount of times.
	 * 
	 * @param number the number to write in the array
	 * @param writer the writer to use
	 * @param level  the indent level
	 * @throws IOException
	 * 
	 * @see #indent(Integer, Writer, int)
	 */
	public static void arrayEntry(Integer number, Writer writer, int level) throws IOException {
		writer.write('\n');
		indent(number, writer, level);
	}

	/**
	 * Writes the elements as a pretty JSON object; that is, one object per line
	 * indented a specified amount of times where an object is a string formatted
	 * '"key": value'.
	 * 
	 * @param entries a {@code Map<String, Integer>} that in this case represents
	 *                the inverted index's fileMap
	 * @param writer  the writer to use
	 * @param level   the indent level
	 * @throws IOException
	 * 
	 * @see #writeEntry(Entry, Writer, int)
	 * @see #indent(String, Writer, int)
	 * 
	 */
	public static void asObject(Map<String, Integer> entries, Writer writer, int level) throws IOException {
		writer.write('{');
		var iterator = entries.entrySet().iterator();
		// if there's a first element, write it
		if (iterator.hasNext()) {
			writeEntry(iterator.next(), writer, level);
		}
		// for all subsequent entries, write a comma and the next entry
		while (iterator.hasNext()) {
			writer.write(",");
			writeEntry(iterator.next(), writer, level);
		}
		writer.write('\n');
		indent("}", writer, level - 1);
	}

	/**
	 * Writes a single entry in a pretty JSON object; that is '"key": value'.
	 * 
	 * @param entry  a {@code Map.Entry<String, Integer>} that in this case
	 *               represents an entry in the inverted index's fileMap.
	 * @param writer the writer to use
	 * @param level  the indent level
	 * @throws IOException
	 * 
	 * @see #writeKey(String, Writer, int)
	 */
	public static void writeEntry(Entry<String, Integer> entry, Writer writer, int level) throws IOException {
		// write "key: ", where key is the entry's key
		writeKey(entry.getKey(), writer, level);
		// write the entry's value
		writer.write(entry.getValue().toString());
	}

	/**
	 * Writes the entries as a pretty nested JSON object; that is,
	 * 
	 * { "key": [ value_one, value_two, value_three ] }
	 * 
	 * The purpose of this function in this context is to iterate through the
	 * appearance files of a particular stem in an inverted index data structure,
	 * write each file as the key of a pretty JSON object, and write the
	 * corresponding location array as the value.
	 * 
	 * @param elements the elements of the index to write
	 * @param writer   the writer to use
	 * @param level    the initial indent level
	 * @throws IOException
	 * 
	 * @see #writeNestedEntry(Entry, Writer, int)
	 * @see #indent(String, Writer, int)
	 */
	public static void asNestedObject(Map<String, ? extends Collection<Integer>> elements, Writer writer, int level) throws IOException {
		writer.write('{');
		// create an iterator over the second the level of the index
		var iterator = elements.entrySet().iterator();
		// if there's a first element, write it and its corresponding
		if (iterator.hasNext()) {
			writeNestedEntry(iterator.next(), writer, level);
		}
		// for all subsequent entries, write a comma and the next level of the index
		while (iterator.hasNext()) {
			writer.write(",");
			writeNestedEntry(iterator.next(), writer, level);
		}
		writer.write('\n');
		indent("}", writer, level - 1);
	}

	/**
	 * Writes a file path and its corresponding location array as a pretty nested
	 * JSON array.
	 * 
	 * @param entry  an entry from the inverted index that maps a file name to its
	 *               set of locations
	 * @param writer the writer to use
	 * @param level  the indent level
	 * @throws IOException
	 * 
	 * @see #writeKey(String, Writer, int)
	 * @see #asArray(Collection, Writer, int)
	 */
	public static void writeNestedEntry(Entry<String, ? extends Collection<Integer>> entry, Writer writer, int level) throws IOException {
		writeKey(entry.getKey(), writer, level);
		asArray(entry.getValue(), writer, level + 1);
	}

	/**
	 * Writes the results for one search query as a pretty nested JSON objects.
	 * 
	 * @param results an {@code ArrayList<SearchResult>} that represents results for
	 *                a search query
	 * @param writer  the writer to use
	 * @param level   the initial indent level
	 * @throws IOException
	 */
	public static void asResults(ArrayList<InvertedIndex.SearchResult> results, Writer writer, int level) throws IOException {
		writer.write('[');
		// create an iterator over the search results
		var iterator = results.iterator();
		// if there's a first result, write it and its snippet
		if (iterator.hasNext()) {
			writeResultEntry(iterator.next(), writer, level);
		}
		// for all subsequent results, write a comma and the next result
		while (iterator.hasNext()) {
			writer.write(",");
			writeResultEntry(iterator.next(), writer, level);
		}
		writer.write('\n');
		indent("]", writer, level - 1);
	}

	/**
	 * Writes a single search result as a JSON object.
	 * 
	 * @param result a single search result
	 * @param writer the writer to use
	 * @param level  the indent level
	 * @throws IOException
	 */
	public static void writeResultEntry(InvertedIndex.SearchResult result, Writer writer, int level) throws IOException {
		writer.write('\n');
		indent("{", writer, level);
		writeKey("where", writer, level + 1);
		quote(result.getName(), writer);
		writer.write(",");
		writeKey("count", writer, level + 1);
		writer.write(Integer.toString(result.getCount()));
		writer.write(",");
		writeKey("score", writer, level + 1);
		String formattedScore = String.format("%.8f", result.getScore());
		writer.write(formattedScore);
		writer.write('\n');
		indent("}", writer, level);
	}

	/**
	 * Writes only the key of a pretty JSON object; that is, '"key": '.
	 * 
	 * @param key    the key as a string
	 * @param writer the writer to use
	 * @param level  the indent level
	 * @throws IOException
	 * 
	 * @see #quote(String, Writer, int)
	 */
	public static void writeKey(String key, Writer writer, int level) throws IOException {
		writer.write('\n');
		quote(key, writer, level);
		writer.write(": ");
	}

	/**
	 * Writes the {@code \t} tab symbol by the number of times specified.
	 *
	 * @param writer the writer to use
	 * @param times  the number of times to write a tab symbol
	 * @throws IOException
	 */
	public static void indent(Writer writer, int times) throws IOException {
		for (int i = 0; i < times; i++) {
			writer.write('\t');
		}
	}

	/**
	 * Indents and then writes the element.
	 *
	 * @param element the element to write
	 * @param writer  the writer to use
	 * @param times   the number of times to indent
	 * @throws IOException
	 *
	 * @see #indent(String, Writer, int)
	 * @see #indent(Writer, int)
	 */
	public static void indent(Integer element, Writer writer, int times) throws IOException {
		indent(element.toString(), writer, times);
	}

	/**
	 * Indents and then writes the element.
	 *
	 * @param element the element to write
	 * @param writer  the writer to use
	 * @param times   the number of times to indent
	 * @throws IOException
	 *
	 * @see #indent(Writer, int)
	 */
	public static void indent(String element, Writer writer, int times) throws IOException {
		indent(writer, times);
		writer.write(element);
	}

	/**
	 * Writes the element surrounded by {@code " "} quotation marks.
	 *
	 * @param element the element to write
	 * @param writer  the writer to use
	 * @throws IOException
	 */
	public static void quote(String element, Writer writer) throws IOException {
		writer.write('"');
		writer.write(element);
		writer.write('"');
	}

	/**
	 * Indents and then writes the element surrounded by {@code " "} quotation
	 * marks.
	 *
	 * @param element the element to write
	 * @param writer  the writer to use
	 * @param times   the number of times to indent
	 * @throws IOException
	 *
	 * @see #indent(Writer, int)
	 * @see #quote(String, Writer)
	 */
	public static void quote(String element, Writer writer, int times) throws IOException {
		indent(writer, times);
		quote(element, writer);
	}

	/**
	 * Writes the inverted index in JSON format to a file.
	 * 
	 * @param index   the inverted index to output
	 * @param outPath the file to write to
	 * @throws IOException
	 * 
	 * @see #writeKey(String, Writer, int)
	 * @see #asNestedObject(Map, Writer, int)
	 */
	public static void writeIndex(Map<String, TreeMap<String, TreeSet<Integer>>> index, Path outPath) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
			writer.write("{");
			var iterator = index.keySet().iterator(); // an iterator on the index's keys
			// if there's a first stem, write it. Then, write it's appearance files and
			// their corresponding location arrays as a nested JSON object
			if (iterator.hasNext()) {
				String key = iterator.next();
				writeKey(key, writer, 0);
				asNestedObject(index.get(key), writer, 1);
			}
			// for all subsequent stems, write the stem AND a comma. Then, write their
			// appearance files and their corresponding location arrays as a nested JSON
			// object
			while (iterator.hasNext()) {
				writer.write(",");
				String key = iterator.next();
				writeKey(key, writer, 0);
				asNestedObject(index.get(key), writer, 1);
			}
			writer.write('\n');
			writer.write("}");
		}
	}

	/**
	 * Writes the inverted index in JSON format to a string.
	 * 
	 * @param index the inverted index
	 * @return the inverted index as a string
	 */
	public static String writeIndex(Map<String, TreeMap<String, TreeSet<Integer>>> index) {
		try {
			StringWriter writer = new StringWriter();
			writer.write("{");
			var iterator = index.keySet().iterator(); // an iterator on the index's keys
			// if there's a first stem, write it. Then, write it's appearance files and
			// their corresponding location arrays as a nested JSON object
			if (iterator.hasNext()) {
				String key = iterator.next();
				writeKey(key, writer, 0);
				asNestedObject(index.get(key), writer, 1);
			}
			// for all subsequent stems, write the stem AND a comma. Then, write their
			// appearance files and their corresponding location arrays as a nested JSON
			// object
			while (iterator.hasNext()) {
				writer.write(",");
				String key = iterator.next();
				writeKey(key, writer, 0);
				asNestedObject(index.get(key), writer, 1);
			}
			writer.write('\n');
			writer.write("}");
			return writer.toString();
		}
		catch (IOException e) {
			return null;
		}
	}

	/**
	 * Writes each file its word-count as a pretty JSON object to the provided path.
	 * 
	 * @param entries the inverted index's fileMap
	 * @param outPath the path to write output to
	 * @throws IOException
	 * 
	 * @see #asObject(Map, Writer, int)
	 */
	public static void writeCounts(Map<String, Integer> entries, Path outPath) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
			asObject(entries, writer, 1);
		}
	}

	/**
	 * Writes query strings and their corresponding search results in JSON format to
	 * a file.
	 * 
	 * @param resultMap a map from a query string to its search results
	 * @param path      the file to write to
	 * @throws IOException
	 */
	public static void writeResults(SortedMap<String, ArrayList<InvertedIndex.SearchResult>> resultMap, Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writer.write("{");
			// an iterator on the query strings
			var iterator = resultMap.keySet().iterator();
			// if there's a first query, write it. Then, write it's search results as a
			// nested JSON object.
			if (iterator.hasNext()) {
				String key = iterator.next();
				writeKey(key, writer, 1);
				asResults(resultMap.get(key), writer, 2);
			}
			// for all subsequent queries, write the query AND a comma. Then, write its
			// search results as a nested JSON object.
			while (iterator.hasNext()) {
				writer.write(",");
				String key = iterator.next();
				writeKey(key, writer, 1);
				asResults(resultMap.get(key), writer, 2);
			}
			writer.write('\n');
			writer.write("}");
		}
	}
}