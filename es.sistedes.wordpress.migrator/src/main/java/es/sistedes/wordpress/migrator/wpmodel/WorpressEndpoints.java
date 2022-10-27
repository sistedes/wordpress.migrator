package es.sistedes.wordpress.migrator.wpmodel;

final class WorpressEndpoints {
	
	static final String LIBRARY_ENDPOINT = "/wp-json/wp/v2/biblioteca";
	static final String LIBRARY_PARENT_QUERY = "?parent=%s";
	static final String ARTICLE_ENDPOINT = "/wp-json/wp/v2/articulo/%s";

	private WorpressEndpoints() {
	}
}
