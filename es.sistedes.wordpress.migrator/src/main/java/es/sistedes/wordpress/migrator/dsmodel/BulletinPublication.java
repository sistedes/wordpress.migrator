package es.sistedes.wordpress.migrator.dsmodel;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.sistedes.wordpress.migrator.wpmodel.Author;
import es.sistedes.wordpress.migrator.wpmodel.Bulletin;
import es.sistedes.wordpress.migrator.wpmodel.Document.License;
import es.sistedes.wordpress.migrator.wpmodel.Seminar;

public class BulletinPublication extends Publication {

	private transient static final Logger LOGGER = LoggerFactory.getLogger(BulletinPublication.class);
	private transient static final String VIDEO_CACHE_DIR = "videocache";
	
	public BulletinPublication(String title, String _abstract, List<String> keywords, List<Author> authors, String uri, String licenseName, Date date) {
		super(title, _abstract, keywords, authors, uri, licenseName, date);
		setType(Type.BULLETIN.getName());
	}

	public static BulletinPublication from(Collection collection, Bulletin bulletin) {
		File file = getPdfFile(bulletin.getHandle());
		try {
			if (file != null && !file.exists()) {
				FileUtils.copyInputStreamToFile((InputStream) new URL(bulletin.getDocumentUrl()).getContent(), file);
			}
		} catch (Exception e) {
			LOGGER.error("Unable to retrieve PDF file for "  + bulletin.getLink());
		}
		BulletinPublication publication = new BulletinPublication(
				bulletin.getTitle(),
				bulletin.getDescription(),
				Collections.emptyList(),
				Collections.emptyList(),
				bulletin.getHandleUri(), 
				License.CC_BY_NC_ND.getName(),
				bulletin.getDate());
		publication.setIsPartOf("Boletines Sistedes");
		return publication;
	}
	
	public static BulletinPublication from(Collection collection, Seminar seminar) {
		File file = getDirectoryFile(seminar.getHandle());
		if (!file.exists()) {
			LOGGER.error("Resources folder for seminar "  + seminar.getTitle() + " does not exist");
		}
		BulletinPublication publication = new BulletinPublication(
				seminar.getTitle(),
				seminar.getSummary(),
				Collections.emptyList(),
				seminar.getAuthors(),
				seminar.getHandleUri(), 
				License.CC_BY_NC_ND.getName(),
				seminar.getDate());
		publication.setIsPartOf("Seminarios Sistedes");
		publication.metadata.setSistedesBio(seminar.getBio());
		return publication;
	}
	
	public File[] getFiles() {
		File directoryFile = getDirectoryFile(getSistedesHandle());
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
