package es.sistedes.wordpress.migrator.wpmodel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;

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
					this.articles.add(new Gson().fromJson(
							StringEscapeUtils.unescapeXml(
									IOUtils.toString(DelayedStreamOpener.open(url), StandardCharsets.UTF_8)), Article.class));
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
