package es.sistedes.wordpress.migrator.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Article {

	// BEGIN: JSON fields
	private String id;
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
