package es.sistedes.wordpress.migrator.wpmodel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class Article {

	// BEGIN: JSON fields
	private String id;
	private String link;
	private Map<String, String> title;
	private Map<String, Map<String, String>[]> _links;
	private Map<String, String> content;
	private Map<String, String> excerpt;
	private Map<String, String> metadata;
	// END: JSON fields

	private transient List<Author> authors;

	public String getId() {
		return id;
	}

	public String getTitle() {
		return StringUtils.trimToNull(title.get("rendered"));
	}

	public String getLink() {
		return link;
	}
	
	public String getCollectionUrl() {
		return StringUtils.trimToNull(_links.get("collection")[0].get("href"));
	}

	public String getContent() {
		return StringUtils.trimToNull(content.get("rendered"));
	}

	public String getExcerpt() {
		return StringUtils.trimToNull(excerpt.get("rendered"));
	}

	public String getAbstract() {
		return StringUtils.trimToNull(metadata.get("abstract"));
	}

	public List<String> getKeywords() {
		return Arrays.asList(metadata.get("keywords") != null ? metadata.get("keywords").split(",") : new String[] {});
	}
	
	public String getHandle() {
		return StringUtils.trimToNull(metadata.get("handle"));
	}

	public String getHandleUri() {
		return "https://hdl.handle.net/" + StringUtils.trimToNull(metadata.get("handle"));
	}

	public String getDocumentUrl() {
		try {
			URL postUrl = new URL(link);
			if (StringUtils.isNotBlank(metadata.get("paper_pdf_file"))) {
				return new URL(postUrl.getProtocol(), postUrl.getHost(), postUrl.getPort(), "/submissions/" + StringUtils.trimToNull(metadata.get("paper_pdf_file"))).toString();
			} else if (StringUtils.isNotBlank(metadata.get("paper_pdf"))) {
				String mediaId = StringUtils.trimToNull(metadata.get("paper_pdf"));
				URL mediaUrl = new URL(postUrl.getProtocol(), postUrl.getHost(), postUrl.getPort(), "/wp-json/wp/v2/media/" + mediaId);
				Media media = new Gson().fromJson(new InputStreamReader(DelayedStreamOpener.open(mediaUrl)), Media.class);
				return media.getAttachmentUrl();
			} else return null;
		} catch (IOException | JsonSyntaxException | JsonIOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<Author> getAuthors() {
		if (authors == null) {
			authors = new ArrayList<>();
			for (int i = 1; i <= 8; i++) {
				if (StringUtils.isNotBlank(metadata.get("author_name_" + i))) {
					authors.add(new Author(
							StringUtils.trimToNull(metadata.get("author_name_" + i)), 
							StringUtils.trimToNull(metadata.get("author_email_" + i)), 
							StringUtils.trimToNull(metadata.get("author_univ_" + i))));
				}
			}
			authors = Collections.unmodifiableList(authors);
		}
		return authors;
	}
}
