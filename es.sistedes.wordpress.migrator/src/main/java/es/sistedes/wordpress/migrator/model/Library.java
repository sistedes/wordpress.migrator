package es.sistedes.wordpress.migrator.model;

import java.util.Map;

public class Library {

	// BEGIN: JSON fields
	private String id;
	private Map<String, String> title; 
	private Map<String, Map<String, String>[]> _links;
	// END: JSON fields

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title.get("rendered");
	}

	public String getCollectionUrl() {
		return _links.get("collection")[0].get("href");
	}


}
