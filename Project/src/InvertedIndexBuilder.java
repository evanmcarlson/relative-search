import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Builds an inverted index by parsing, stemming, and adding data to an index
 * from a file.
 * 
 * @author evancarlson
 */
public class InvertedIndexBuilder {

	/** The default stemmer algorithm used by this class. */
	public static final SnowballStemmer.ALGORITHM DEFAULT = SnowballStemmer.ALGORITHM.ENGLISH;

	/** The inverted index to build/add data to */
	private final InvertedIndex index;

	/**
	 * Instantiates a new builder.
	 * 
	 * @param index the inverted index to build
	 */
	public InvertedIndexBuilder(InvertedIndex index) {
		this.index = index;
	}

	/**
	 * Builds an InvertedIndex from a path.
	 * 
	 * @param inPath the input path to build inverted index from
	 * @throws IOException
	 */
	public void build(Path inPath) throws IOException {
		List<Path> locations = FileTraverser.getTextFiles(inPath);
		for (Path path : locations) {
			parse(path);
		}
	}

	/**
	 * Convenience method for parse(Path, InvertedIndex)
	 * 
	 * @param path a path to parse and extract data from
	 * @throws IOException
	 */
	public void parse(Path path) throws IOException {
		parse(path, this.index);
	}

	/**
	 * Parses a text file and adds each unique stemmed word from the file into the
	 * inverted index with the corresponding file path and appearance location(s).
	 * Also records the total word count of each file that is parsed.
	 * 
	 * @param file  the file to parse and extract data from
	 * @param index the inverted index to add data to
	 * @throws IOException
	 * 
	 * @see TextParser#clean(String)
	 * @see Stemmer#stem(CharSequence)
	 */
	public static void parse(Path file, InvertedIndex index) throws IOException {
		Stemmer stemmer = new SnowballStemmer(DEFAULT);
		// starting position
		int pos = 0;
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);) {
			String line = null;
			String location = file.toString();
			while ((line = reader.readLine()) != null) {
				// split by white space and clean
				String[] words = TextParser.parse(line);
				// stem and add data to inverted index
				for (String word : words) {
					word = stemmer.stem(word).toString();
					++pos;
					index.add(word, location, pos);
				}
			}
		}
	}
}