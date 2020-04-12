import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * This class is used for traversing directories and extracting valid text files
 * from given paths.
 * 
 * @author evancarlson
 *
 */
public class FileTraverser {

	/**
	 * Determines if a path points to a text file.
	 * 
	 * @param path the path to a file with an unknown extension
	 * @return {@code true} is the path is a reference to a text file
	 * 
	 * @see String#endsWith(String)
	 */
	public static boolean isTextFile(Path path) {
		String file = path.getFileName().toString().toLowerCase();
		return file.endsWith(".txt") || file.endsWith(".text");
	}

	/**
	 * Given an input path to a directory, this function will traverse the directory
	 * recursively and generate a list of text files. Given an input path of a file,
	 * will determine its readability by either adding it to readable or not.
	 * 
	 * @param path     a path to a relative directory or file
	 * @param readable the accumulator list of valid text files
	 * @return {@code ArrayList<Path>} the list of readable text files within the
	 *         scope of the path
	 * @throws IOException
	 * 
	 * @see #isTextFile(Path)
	 */
	public static ArrayList<Path> getTextFiles(Path path, ArrayList<Path> readable) throws IOException {
		// if the path is a directory, recursively traverse it
		if (Files.isDirectory(path)) {
			try (DirectoryStream<Path> files = Files.newDirectoryStream(path)) {
				for (Path file : files) {
					getTextFiles(file, readable);
				}
			}
		}
		else { // if the path is a valid text file, add it to the list
			if (isTextFile(path)) {
				readable.add(path);
			}
		}
		return readable;
	}

	/**
	 * Generates a list of readable files given a directory path. Determines
	 * readability given a file path by either adding it to readable or not.
	 * 
	 * @param path the input path to a file or directory to traverse
	 * @return {@code ArrayList<Path>} the result from the overloaded method, a list
	 *         of valid readable files after traversing
	 * @throws IOException
	 * 
	 * @see #getTextFiles(Path, ArrayList)
	 */
	public static ArrayList<Path> getTextFiles(Path path) throws IOException {
		ArrayList<Path> readable = new ArrayList<>();
		return getTextFiles(path, readable);
	}

	// maybe refactor these to return a set of paths instead of an arrayList?
}