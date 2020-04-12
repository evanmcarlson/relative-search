import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A data structure that maps keys to appearance locations to position indices.
 * At depth, the inverted index has three levels: Key -> Location -> Indices
 * 
 * @author evancarlson
 *
 */
public class InvertedIndex {

	/**
	 * The inverted index data structure. Maps a key to the location(s) it is found,
	 * each of which maps to a set of position indices.
	 */
	private final TreeMap<String, TreeMap<String, TreeSet<Integer>>> index;

	/**
	 * A data structure that maps each location present in the inverted index to its
	 * total word count.
	 */
	private final TreeMap<String, Integer> locationMap;

	/**
	 * Initializes an empty inverted index and an empty word-count map.
	 */
	public InvertedIndex() {
		index = new TreeMap<String, TreeMap<String, TreeSet<Integer>>>();
		locationMap = new TreeMap<String, Integer>();
	}

	/**
	 * Adds all elements from another inverted index.
	 * 
	 * @param other the other index to add from
	 */
	public void addAll(InvertedIndex other) {
		for (String word : other.index.keySet()) {
			// if the word is not in the index .. try using putIfAbsent?
			if (this.index.containsKey(word) == false) {
				this.index.put(word, other.index.get(word));
			}
			// the word is already in the index
			else {
				// iterate through its location -> position map
				for (var entry : other.index.get(word).entrySet()) {
					String location = entry.getKey();
					// if the location is already in the index, too
					if (this.index.get(word).containsKey(location)) {
						// use the built-in add method to add all of the position indices
						this.index.get(word).get(location).addAll(entry.getValue());
					}
					// otherwise, the location needs to be added
					else {
						this.index.get(word).put(entry.getKey(), entry.getValue());
					}
				}
			}
		}

		for (String location : other.locationMap.keySet()) {
			// if the location's word count is not in the map, add it
			locationMap.putIfAbsent(location, other.locationMap.get(location));
			// put the max of the two values
			if (locationMap.get(location) < other.locationMap.get(location)) {
				locationMap.put(location, other.locationMap.get(location));
			}
		}
	}

	/**
	 * Adds a position for the given key and location in the inverted index. If the
	 * key or location do not exist in the index, they will be added first.
	 * 
	 * @param key      the key to add to the inverted index
	 * @param location the location that contains the word
	 * @param position an index in respect to the total word-count of the location
	 *                 indicating where the key appears in the location
	 * 
	 */
	public void add(String key, String location, int position) {
		assert position > 0;
		/*
		 * if the key does not exist in the index, add it if the location does not exist
		 * for the word, add it add the position of appearance to the location's indices
		 * set
		 */
		index.putIfAbsent(key, new TreeMap<String, TreeSet<Integer>>());
		index.get(key).putIfAbsent(location, new TreeSet<Integer>());
		index.get(key).get(location).add(position);
		/*
		 * if the file doesn't exist in our record of files, add it if the current
		 * position is higher than the previously recorded position, replace it with the
		 * higher position
		 */
		locationMap.putIfAbsent(location, position);
		if (locationMap.get(location) < position) {
			locationMap.put(location, position);
		}
	}

	/**
	 * Checks if a key exists in the inverted index.
	 * 
	 * @param key the key to check exists
	 * @return {@code true} if the key exists in the inverted index
	 */
	public boolean hasKey(String key) {
		return index.containsKey(key);
	}

	/**
	 * Checks if a location exists for the given key in the inverted index.
	 * 
	 * @param key      the key in the inverted index
	 * @param location the location in the inverted index
	 * @return {@code true} if the location exists for the given key
	 * 
	 * @see #hasKey(String)
	 */
	public boolean hasLocation(String key, String location) {
		if (hasKey(key)) {
			return index.get(key).containsKey(location);
		}
		return false;
	}

	/**
	 * Checks if a position exists for the given key and location in the inverted
	 * index.
	 * 
	 * @param key      the key in the inverted index
	 * @param location the location in the index
	 * @param position the position to check exists
	 * @return {@code true} if the position exists for the given key and location
	 * 
	 * @see #hasLocation(String, String)
	 */
	public boolean hasPosition(String key, String location, int position) {
		if (hasLocation(key, location)) {
			return index.get(key).get(location).contains(position); // integer.valueof() ?
		}
		return false;
	}

	/**
	 * Retrieves the number of keys in the inverted index.
	 * 
	 * @return int the number of keys in the inverted index
	 */
	public int numKeys() {
		return getKeys().size();
	}

	/**
	 * Retrieves the number of appearance locations given a key in the inverted
	 * index.
	 * 
	 * @param key the key in the inverted index
	 * @return int the number of locations where the key appears
	 */
	public int numLocations(String key) {
		return getLocations(key).size();
	}

	/**
	 * Retrieves the number of position indices given a key and a location in the
	 * inverted index. This resembles the number of times the key occurs in the
	 * location.
	 * 
	 * @param key      the key in the inverted index
	 * @param location the location in the inverted index
	 * @return int the number of times the key occurs in the location
	 */
	public int numPositions(String key, String location) {
		return getPositions(key, location).size();
	}

	/**
	 * Retrieves the keys in the inverted index.
	 * 
	 * @return {@code Set<String>} an immutable set of the inverted index's keys
	 */
	public Set<String> getKeys() {
		if (index.size() > 0) {
			return Collections.unmodifiableSet(index.keySet());
		}
		return Collections.emptySet();
	}

	/**
	 * Retrieves the set of locations given a key in the inverted index. If the key
	 * does not exist, will return an empty set.
	 * 
	 * @param key the key in the inverted index
	 * @return {@code Set<String>} an immutable set of locations where the key
	 *         appears
	 */
	public Set<String> getLocations(String key) {
		if (hasKey(key)) {
			return Collections.unmodifiableSet(index.get(key).keySet());
		}
		return Collections.emptySet();
	}

	/**
	 * Retrieves the set of sorted position indices given a key and location in the
	 * inverted index. If the key or location do not exist, will return an empty
	 * set.
	 * 
	 * @param key      a key in the inverted index
	 * @param location a location in the inverted index
	 * @return {@code Set<Integer>} an immutable set of the positions where the key
	 *         appears in the location
	 */
	public Set<Integer> getPositions(String key, String location) {
		if (hasLocation(key, location)) {
			return Collections.unmodifiableSet(index.get(key).get(location));
		}
		return Collections.emptySet();
	}

	/**
	 * Retrieves the locationMap of the index; that is, a map of locations in the
	 * inverted index to their total word counts.
	 * 
	 * @return {@code Map<String, Integer>} an immutable map of locations to their
	 *         word counts.
	 */
	public Map<String, Integer> getLocationToCountMap() {
		if (locationMap.size() > 0) {
			return Collections.unmodifiableMap(locationMap);
		}
		return Collections.emptyMap();
	}

	/**
	 * Searches the inverted index.
	 * 
	 * @param query a collection of words to search for
	 * @param exact true if exact search, false if partial search
	 * @return a list of search results from the index created for the query
	 */
	public ArrayList<SearchResult> search(Collection<? extends String> query, boolean exact) {
		if (exact) {
			return exactSearch(query);
		}
		else {
			return partialSearch(query);
		}
	}

	/**
	 * Performs an exact search on the inverted index, such that any word in the
	 * index that exactly matches a provided query word is returned.
	 * 
	 * @param query a list of clean and stemmed words from a search query
	 * @return an {@code ArrayList<SearchResult>} where SearchResults are sorted by
	 *         word frequency
	 */
	public ArrayList<SearchResult> exactSearch(Collection<? extends String> query) {
		// initialize an empty list of search results
		ArrayList<SearchResult> results = new ArrayList<>();
		// initialize an empty map used for an easy location -> result lookup
		Map<String, SearchResult> lookup = new HashMap<>();

		for (String word : query) {
			// if the word is present in the index
			if (index.get(word) != null) {
				searchHelper(word, lookup, results);
			}
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * Performs a partial search on the inverted index, such that any word in the
	 * index that *starts with* a provided query word is returned.
	 * 
	 * @param query a list of clean and stemmed words from a search query
	 * @return an {@code ArrayList<SearchResult>} where SearchResults are sorted by
	 *         word frequency
	 */
	public ArrayList<SearchResult> partialSearch(Collection<? extends String> query) {
		// initialize an empty list of search results
		ArrayList<SearchResult> results = new ArrayList<>();
		// initialize an empty map used for an easy location -> result lookup
		Map<String, SearchResult> lookup = new HashMap<>();

		for (String word : query) {
			for (String key : index.tailMap(word).keySet()) {
				// once a word no longer starts with the prefix, exit
				if (!key.startsWith(word)) {
					break;
				}
				searchHelper(key, lookup, results);
			}
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * Helps the search methods by creating new search results and updating existing
	 * search results.
	 * 
	 * @param key     a key in the index
	 * @param lookup  a location -> result map used to check if search results exist
	 * @param results the result list to add to
	 */
	private void searchHelper(String key, Map<String, SearchResult> lookup, List<SearchResult> results) {
		for (String location : index.get(key).keySet()) { // getLocations(key) is just a little extra logic
			if (lookup.containsKey(location)) {
				lookup.get(location).update(key);
			}
			else {
				SearchResult result = new SearchResult(location, key);
				lookup.put(location, result);
				results.add(result);
			}
		}
	}

	/**
	 * Writes the inverted index as a pretty JSON file to the provided path.
	 * 
	 * @param path the file to write to
	 * @throws IOException
	 */
	public void writeIndex(Path path) throws IOException {
		JSONWriter.writeIndex(index, path);
	}

	/**
	 * Returns the string representation of the inverted index.
	 * 
	 * @return the inverted index as a {@code String}.
	 */
	@Override
	public String toString() {
		return JSONWriter.writeIndex(index);
	}

	/**
	 * 
	 * A non-static inner class that stores a single search result.
	 *
	 */
	public class SearchResult implements Comparable<SearchResult> {
		/**
		 * Name of the location a query word appears
		 */
		private final String location;

		/**
		 * The amount of times any of the query words appear in the location
		 */
		private int queryCount;
		/**
		 * The score of the search result used for sorting, calculated by query
		 * frequency: total query matches in location / total words in locations
		 */
		private double score;

		/**
		 * Initializes a search result from the inverted index.
		 * 
		 * @param location the location a word is found
		 * @param word     a word in the index
		 */
		public SearchResult(String location, String word) {
			this.location = location;
			update(word);
		}

		/**
		 * Retrieves the location of the search result.
		 * 
		 * @return the location of the search result as a {@code String}
		 */
		public String getName() {
			return location;
		}

		/**
		 * Retrieves the number of times the query word(s) appears in the search result.
		 * 
		 * @return int the number of times the query appears in the result
		 */
		public int getCount() {
			return queryCount;
		}

		/**
		 * Retrieves the score of the search result; used for sorting results.
		 * 
		 * @return {@code double} the score of the search result
		 */
		public double getScore() {
			return score;
		}

		/**
		 * Sets the queryCount of the search result; the amount of times any of the
		 * query words appear in the location. Also updates the result's score.
		 * 
		 * @param word the word to locate and update in the index
		 */
		private void update(String word) {
			this.queryCount += index.get(word).get(this.location).size();
			this.score = Double.valueOf(queryCount) / Double.valueOf(locationMap.get(location));
		}

		@Override
		public int compareTo(SearchResult other) {
			if (Double.compare(other.score, this.score) != 0) {
				// sort by score in descending order
				return Double.compare(other.score, this.score);
			}
			else {
				// if scores are equal, sort by query count in descending order
				if (Integer.compare(other.queryCount, this.queryCount) != 0) {
					return Integer.compare(other.queryCount, this.queryCount);
				}
				else {
					// if the score and query count are equal, sort by location alphabetically
					return this.location.compareToIgnoreCase(other.location);
				}
			}
		}

		@Override
		public String toString() {
			String string = this.location + "|" + Double.toString(this.score) + "|" + Integer.toString(this.queryCount);
			return string;
		}
	}
}