package es.sistedes.wordpress.migrator.dsmodel;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.sistedes.wordpress.migrator.wpmodel.Author;
import es.sistedes.wordpress.migrator.wpmodel.Document.License;
import es.sistedes.wordpress.migrator.wpmodel.Seminar;

public class SeminarPublication extends Publication {

	private transient static final Logger LOGGER = LoggerFactory.getLogger(SeminarPublication.class);
	private transient static final String VIDEO_CACHE_DIR = "videocache";
	
	public SeminarPublication(String title, String _abstract, List<String> keywords, List<Author> authors, String uri, String licenseName, Date date) {
		super(title, _abstract, keywords, authors, uri, licenseName, date);
		setType(Type.SEMINAR.getName());
	}

	public static SeminarPublication from(Collection collection, Seminar seminar) {
		File file = getDirectoryFile(seminar.getHandle());
		if (!file.exists()) {
			LOGGER.error("Resources folder for seminar "  + seminar.getTitle() + " does not exist");
		}
		SeminarPublication publication = new SeminarPublication(
				seminar.getTitle(),
				seminar.getSummary(),
				Collections.emptyList(),
				seminar.getAuthors(),
				seminar.getHandle(), 
				License.CC_BY_NC_ND.getName(),
				seminar.getDate());
		publication.setIsPartOf("Seminarios Sistedes");
		publication.metadata.setContributorBio(seminar.getBio());
		publication.metadata.setPublisher("Sistedes");
		return publication;
	}
	
	public File[] getFiles() {
		File directoryFile = getDirectoryFile(getSistedesIdentifier());
		return directoryFile.exists() && directoryFile.isDirectory() ?  directoryFile.listFiles() : new File[] {};
	}
	
	private static File getDirectoryFile(String handle) {
		if (handle == null) {
			return null;
		} else {
			return Paths.get(VIDEO_CACHE_DIR, handle.replaceAll("/", "-")).toFile();
		}
	}
}
