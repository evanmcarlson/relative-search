import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A thread-safe version of {@link InvertedIndex} using a read/write lock.
 * 
 * @author evancarlson
 *
 */
public class ThreadSafeInvertedIndex extends InvertedIndex {

	/**
	 * The lock used to protect concurrent access to the underlying set.
	 */
	private final ReadWriteLock lock;

	/**
	 * Initializes a thread-safe inverted index.
	 */
	public ThreadSafeInvertedIndex() {
		super();
		lock = new ReadWriteLock();
	}

	@Override
	public void addAll(InvertedIndex other) {
		lock.writeLock().lock();
		try {
			super.addAll(other);
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void add(String key, String location, int position) {
		lock.writeLock().lock();
		try {
			super.add(key, location, position);
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public boolean hasKey(String key) {
		lock.readLock().lock();
		try {
			return super.hasKey(key);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public boolean hasLocation(String key, String location) {
		lock.readLock().lock();
		try {
			return super.hasLocation(key, location);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public boolean hasPosition(String key, String location, int position) {
		lock.readLock().lock();
		try {
			return super.hasPosition(key, location, position);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public int numKeys() {
		lock.readLock().lock();
		try {
			return super.numKeys();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public int numLocations(String key) {
		lock.readLock().lock();
		try {
			return super.numLocations(key);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public int numPositions(String key, String location) {
		lock.readLock().lock();
		try {
			return super.numPositions(key, location);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<String> getKeys() {
		lock.readLock().lock();
		try {
			return super.getKeys();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<String> getLocations(String key) {
		lock.readLock().lock();
		try {
			return super.getLocations(key);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<Integer> getPositions(String key, String location) {
		lock.readLock().lock();
		try {
			return super.getPositions(key, location);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Map<String, Integer> getLocationToCountMap() {
		lock.readLock().lock();
		try {
			return super.getLocationToCountMap();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public ArrayList<SearchResult> exactSearch(Collection<? extends String> query) {
		lock.readLock().lock();
		try {
			return super.exactSearch(query);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public ArrayList<SearchResult> partialSearch(Collection<? extends String> query) {
		lock.readLock().lock();
		try {
			return super.partialSearch(query);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void writeIndex(Path path) throws IOException {
		lock.readLock().lock();
		try {
			super.writeIndex(path);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public String toString() {
		lock.readLock().lock();
		try {
			return super.toString();
		}
		finally {
			lock.readLock().unlock();
		}
	}
}