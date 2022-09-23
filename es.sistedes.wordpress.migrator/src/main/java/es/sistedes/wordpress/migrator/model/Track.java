package es.sistedes.wordpress.migrator.model;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class Track extends Library {
	
	// BEGIN: JSON fields
	private Map<String, String> articulos;
	// END: JSON fields

	private transient List<Article> articles;

	public List<Article> getArticles() throws IOException {
		if (articles == null) {
			this.articles = new ArrayList<>();
			try {
				for (String id : articulos.values()) {
					URL url = new URL(id);
					this.articles.add(new Gson().fromJson(new InputStreamReader(DelayedStreamOpener.open(url)), Article.class));
				}
			} catch (MalformedURLException e) {
				// Should not happen...
				new RuntimeException(e);
			}
		}
		articles = Collections.unmodifiableList(articles);
		return articles;
	}
}
