import java.util.ConcurrentModificationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author evancarlson
 *
 */
public class ReadWriteLock {
	/** A logger specifically for this class. */
	private static final Logger log = LogManager.getLogger(ReadWriteLock.class);

	/** The lock used for reading. */
	private final SimpleLockInterface readerLock;

	/** The lock used for writing. */
	private final SimpleLockInterface writerLock;

	/** The lock used for synchronizing data within this class. */
	private final Object lock;

	/** Integer that tracks the number of active readers */
	private int readers;

	/** Integer that tracks the number of active writers */
	private int writers;

	/**
	 * Initializes a new simple read/write lock.
	 */
	public ReadWriteLock() {
		readerLock = new ReadLock();
		writerLock = new WriteLock();
		lock = new Object();
		readers = 0;
		writers = 0;
	}

	/**
	 * Returns the reader lock.
	 *
	 * @return the reader lock
	 */
	public SimpleLockInterface readLock() {
		return readerLock;
	}

	/**
	 * Returns the writer lock.
	 *
	 * @return the writer lock
	 */
	public SimpleLockInterface writeLock() {
		return writerLock;
	}

	/**
	 * Determines whether the thread running this code and the other thread are in
	 * fact the same thread.
	 *
	 * @param other the other thread to compare
	 * @return true if the thread running this code and the other thread are not
	 *         null and have the same ID
	 *
	 * @see Thread#getId()
	 * @see Thread#currentThread()
	 */
	public static boolean sameThread(Thread other) {
		return other != null && other.getId() == Thread.currentThread().getId();
	}

	/**
	 * Used to maintain simultaneous read operations.
	 */
	private class ReadLock implements SimpleLockInterface {

		/**
		 * Will wait until there are no active writers in the system, and then will
		 * increase the number of active readers.
		 */
		@Override
		public void lock() {
			synchronized (lock) {
				while (writers > 0) {
					try {
						lock.wait();
					}
					catch (InterruptedException e) {
						log.debug("Thread was interrupted");
						Thread.currentThread().interrupt();
					}
				}
				readers++;
			}
		}

		/**
		 * Will decrease the number of active readers, and notify any waiting threads if
		 * necessary.
		 */
		@Override
		public void unlock() {
			synchronized (lock) {
				readers--;
				if (readers == 0) {
					lock.notifyAll();
				}
			}
		}

	}

	/**
	 * Used to maintain exclusive write operations.
	 */
	private class WriteLock implements SimpleLockInterface {

		/** The thread that currently holds the write lock */
		private Thread holding = null;

		/**
		 * Will wait until there are no active readers or writers in the system, and
		 * then will increase the number of active writers and update which thread holds
		 * the write lock.
		 */
		@Override
		public void lock() {
			synchronized (lock) {
				while (readers > 0 || writers > 0) { // wait until safe to write
					try {
						lock.wait();
					}
					catch (InterruptedException e) {
						log.debug("Thread was interrupted");
						Thread.currentThread().interrupt();
					}
				}
				writers++; // update number of active writers
				holding = Thread.currentThread(); // update which thread holds the write lock
			}
		}

		/**
		 * Will decrease the number of active writers, and notify any waiting threads if
		 * necessary. If unlock is called by a thread that does not hold the lock, then
		 * a {@link ConcurrentModificationException} is thrown.
		 *
		 * @see #sameThread(Thread)
		 *
		 * @throws ConcurrentModificationException if unlock is called without
		 *                                         previously calling lock or if unlock
		 *                                         is called by a thread that does not
		 *                                         hold the write lock
		 */
		@Override
		public void unlock() throws ConcurrentModificationException {
			synchronized (lock) {
				if (holding == null || !sameThread(holding)) {
					log.debug("Wrong thread is calling WriteLock.unlock() or lock() was never called.");
					throw new ConcurrentModificationException();
				}

				writers--; // update the number of active writers
				holding = null;

				lock.notifyAll();
			}
		}
	}
}