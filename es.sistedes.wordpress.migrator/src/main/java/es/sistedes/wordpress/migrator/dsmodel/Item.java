package es.sistedes.wordpress.migrator.dsmodel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.wpmodel.Article;
import es.sistedes.wordpress.migrator.wpmodel.Article.License;
import es.sistedes.wordpress.migrator.wpmodel.Author;
import es.sistedes.wordpress.migrator.wpmodel.Bulletin;

public class Item extends DSpaceEntity {

	private transient static final Logger logger = LoggerFactory.getLogger(Item.class);
	private transient static final String PDF_CACHE_DIR = "pdfcache";
	
	// BEGIN: JSON fields
	protected boolean inArchive = true;
	protected boolean discoverable = true;
	// END: JSON fields
	
	
	public static Item from(Collection collection, Article article) {
		File file = getFile(article.getHandle());
		try {
			if (file != null && !file.exists()) {
				FileUtils.copyInputStreamToFile((InputStream) new URL(article.getDocumentUrl()).getContent(), file);
			}
		} catch (Exception e) {
			logger.error("Unable to retrieve PDF file for "  + article.getLink());
		}
		return new Item(article.getTitle(), article.getAbstract(),  article.getKeywords(), article.getAuthors(), article.getHandleUri(), article.getLicense(), collection.getDate());
	}

	public static Item from(Collection collection, Bulletin bulletin) {
		File file = getFile(bulletin.getHandle());
		try {
			if (file != null && !file.exists()) {
				FileUtils.copyInputStreamToFile((InputStream) new URL(bulletin.getDocumentUrl()).getContent(), file);
			}
		} catch (Exception e) {
			logger.error("Unable to retrieve PDF file for "  + bulletin.getLink());
		}
		Author sistedes = new Author("Sistedes Sistedes", null, null);
		return new Item(bulletin.getTitle(), bulletin.getDescription(), Collections.emptyList(),
				Arrays.asList(new Author[] { sistedes }), bulletin.getHandle(), 
				License.CC_BY_NC_ND.getName(), bulletin.getDate());
	}

	public static File getFile(String handle) {
		if (handle == null) {
			return null;
		} else {
			return Paths.get(PDF_CACHE_DIR, handle.replaceAll("/", "-") + ".pdf").toFile();
		}
	}

	public Item(String title, String description, List<String> keywords, List<Author> authors, String uri, String license, Date date) {
		// Added just in case it is possible to detect relevant and already published papers...
		// Not sure if applying it, seem to be corner cases...
		//
		// if (title.contains("(") || title.contains("(")) {
		// 	logger.info(MessageFormat.format("Paper ''{0}'' has parentheses in its title. Its license is ''{1}''.", title, license));
		// }
		
		this.name = title;
		this.handle = StringUtils.replace(uri, "http://hdl.handle.net", "");
		this.metadata.setTitle(title);
		this.metadata.setDescription(description);
		this.metadata.setUri(uri);
		this.metadata.setDate(date);
		keywords.forEach(k -> this.metadata.addSubject(k));
		authors.forEach(a -> this.metadata.addAuthor(
				// "Sistedes Sistedes" is a special reserved author name used in Bulletins
				"Sistedes".equals(a.getFirstName()) && "Sistedes".equals(a.getLastName()) ? 
						a.getFirstName() : a.getLastName() + ", " + a.getFirstName()));
		
		boolean noEmails = authors.stream().allMatch(a -> StringUtils.isBlank(a.getEmail()));
		boolean allEmails = authors.stream().allMatch(a -> StringUtils.isNotBlank(a.getEmail()));
		
		if (noEmails) {
			logger.warn(MessageFormat.format("Authors in paper at ''{0}'' have no e-mails", uri));
		} else if (allEmails) {
			authors.forEach(a -> this.metadata.addEmail(a.getEmail()) );
		} else {
			logger.warn(MessageFormat.format("Only some authors in paper at ''{0}'' have e-mails, adding dummy addresses for those missing...", uri));
			authors.forEach(a -> this.metadata.addEmail(StringUtils.defaultIfBlank(a.getEmail(), "unknown@invalid")));
		}
		authors.forEach(a -> { 
			if (StringUtils.isNotBlank(a.getAffiliation())) { 
				this.metadata.addInstitution(a.getAffiliation());
			} else if ("Sistedes".equals(a.getFirstName()) && "Sistedes".equals(a.getLastName())) {
				// Do nothing
			}else {
				logger.warn(MessageFormat.format("Missing affiliation for ''{0} {1}'' in paper at ''{2}''", a.getFirstName(), a.getLastName(), uri));
				this.metadata.addInstitution("Unknown Affiliation");
			}
		});
		if (License.from(license) == License.CC_BY) {
			this.metadata.setLicense("CC BY 4.0");
			this.metadata.setRightsUri("https://creativecommons.org/licenses/by/4.0/");
		} else if (License.from(license) == License.PUBLISHED) {
			this.metadata.setLicense("CC BY-NC-ND 4.0");
			this.metadata.setRightsUri("https://creativecommons.org/licenses/by-nc-nd/4.0/");
			this.metadata.setIsFormatOf("Already published paper. See document contents for DOI.");
		} else if (License.from(license) == License.RESTRICTED) {
			this.metadata.setLicense("All rights reserved to their respective owners");
			this.metadata.addLicense("Todos los derechos reservados a sus respectivos propietarios");
			logger.warn(MessageFormat.format("Restricted license for paper at ''{1}''", license, uri));
		} else if (License.from(license) == License.CC_BY_NC_ND) {
			this.metadata.setLicense("CC BY-NC-ND 4.0");
			this.metadata.setRightsUri("https://creativecommons.org/licenses/by-nc-nd/4.0/");
		} else {
			logger.warn(MessageFormat.format("Unexpected license type (''{0}'') for paper at ''{1}''", license, uri));
		}
	}
	
	public void setUri(String uri) {
		this.metadata.setUri(uri);
	}

	public void setDate(Date date) {
		this.metadata.setDate(date);
	}
	
	public static Item fromHttpEntity(HttpEntity entity) throws ParseException, IOException {
		return new Gson().fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), Item.class);
	}
}
