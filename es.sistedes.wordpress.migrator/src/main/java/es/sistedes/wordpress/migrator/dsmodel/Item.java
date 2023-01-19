package es.sistedes.wordpress.migrator.dsmodel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.MessageFormat;
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
import es.sistedes.wordpress.migrator.wpmodel.Author;

public class Item extends DSpaceEntity {

	private transient static final Logger logger = LoggerFactory.getLogger(Item.class);
	private transient static final String PDF_CACHE_DIR = "pdfcache";
	
	// BEGIN: JSON fields
	protected boolean inArchive = true;
	protected boolean discoverable = true;
	// END: JSON fields
	
	
	public static Item from(Collection collection, Article article) {
		File file = getFile(article);
		try {
			if (file != null && !file.exists()) {
				FileUtils.copyInputStreamToFile((InputStream) new URL(article.getDocumentUrl()).getContent(), file);
			}
		} catch (Exception e) {
			logger.error("Unable to retrieve PDF file for "  + article.getLink());
		}
		return new Item(article.getTitle(), article.getAbstract(),  article.getKeywords(), article.getAuthors(), article.getHandleUri(), collection.getDate());
	}

	public static File getFile(Article article) {
		String handle = article.getHandle();
		if (handle == null) {
			return null;
		} else {
			return Paths.get(PDF_CACHE_DIR, handle.replaceAll("/", "-") + ".pdf").toFile();
		}
	}

	public Item(String title, String description, List<String> keywords, List<Author> authors, String uri, Date date) {
		this.name = title;
		this.handle = StringUtils.replace(uri, "http://hdl.handle.net", "");
		this.metadata.setTitle(title);
		this.metadata.setDescription(description);
		this.metadata.setUri(uri);
		this.metadata.setDate(date);
		keywords.forEach(k -> this.metadata.addSubject(k));
		authors.forEach(a -> this.metadata.addAuthor(a.getLastName() + ", " + a.getFirstName()));
		authors.forEach(a -> { 
			if (StringUtils.isNotBlank(a.getEmail())) { 
					this.metadata.addEmail(a.getEmail());
				} else {
					logger.warn(MessageFormat.format("Missing e-mail for ''{0} {1}'' in paper at ''{2}''", a.getFirstName(), a.getLastName(), uri));
					this.metadata.addEmail("unknown@invalid");
				}
			});
		authors.forEach(a -> { 
			if (StringUtils.isNotBlank(a.getAffiliation())) { 
				this.metadata.addInstitution(a.getAffiliation());
			} else {
				logger.warn(MessageFormat.format("Missing affiliation for ''{0} {1}'' in paper at ''{2}''", a.getFirstName(), a.getLastName(), uri));
				this.metadata.addInstitution("Unknown Affiliation");
			}
		});
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
