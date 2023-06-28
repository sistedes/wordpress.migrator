package es.sistedes.wordpress.migrator.wpmodel;

import java.util.Map;

public class Library {

	// BEGIN: JSON fields
	protected String id;
	protected String link;
	protected Map<String, String> title; 
	protected Map<String, String> content; 
	protected Map<String, Map<String, String>[]> _links;
	protected Map<String, String> excerpt;
	// END: JSON fields

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title.get("rendered");
	}
	
	public String getDescription() {
		return content.get("rendered");
	}

	public String getCollectionUrl() {
		return _links.get("collection")[0].get("href");
	}
	
	public String getLink() {
		return link;
	}
}
