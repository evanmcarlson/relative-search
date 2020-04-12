import java.io.IOException;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Builds and inverted index using multithreading.
 * 
 * @author evancarlson
 *
 */
public class MultithreadedInvertedIndexBuilder extends InvertedIndexBuilder {

	/** A logger specifically for this class. */
	private static final Logger log = LogManager.getLogger(MultithreadedInvertedIndexBuilder.class);

	/** The inverted index to build/add data to */
	private final ThreadSafeInvertedIndex index;

	/** The work queue to use */
	private final WorkQueue queue;

	/**
	 * Initializes a multithreaded inverted index builder.
	 * 
	 * @param index the index to add to
	 * @param queue the work queue to use
	 */
	public MultithreadedInvertedIndexBuilder(ThreadSafeInvertedIndex index, WorkQueue queue) {
		super(index);
		this.index = index;
		this.queue = queue;
	}

	/**
	 * Builds an InvertedIndex from the given path using multithreading.
	 * 
	 * @param inPath the input path to build inverted index from
	 * @throws IOException
	 */
	@Override
	public void build(Path inPath) throws IOException {
		super.build(inPath);
		queue.finish();
	}

	@Override
	public void parse(Path path) throws IOException {
		queue.execute(new Worker(path));
	}

	/**
	 * Worker class that adds a location's data to the index
	 */
	private class Worker implements Runnable {
		/** The location to extract data from */
		private final Path location;

		/**
		 * Initializes a worker with a location
		 * 
		 * @param location the location to be added to the index
		 */
		private Worker(Path location) {
			this.location = location;
		}

		@Override
		public void run() {
			try {

				InvertedIndex local = new InvertedIndex();
				parse(location, local);
				index.addAll(local);
			}
			catch (IOException e) {
				log.debug("error parsing ", location);
			}
		}
	}
}