package es.sistedes.wordpress.migrator.wpmodel;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Media {

	// BEGIN: JSON fields
	private String id;
	private String link;
	private Map<String, String> title;
	private String source_url;
	// END: JSON fields

	public String getId() {
		return id;
	}

	public String getTitle() {
		return StringUtils.trimToNull(title.get("rendered"));
	}
	
	public String getLink() {
		return link;
	}

	public String getAttachmentUrl() {
		return source_url;
	}
}
