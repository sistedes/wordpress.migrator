package es.sistedes.wordpress.migrator.dsmodel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.wpmodel.Article;
import es.sistedes.wordpress.migrator.wpmodel.Author;

public class Item extends AbstractDSpaceEntity {

	// BEGIN: JSON fields
	protected boolean inArchive = true;
	protected boolean discoverable = true;
	// END: JSON fields
	
	public static Item from(Article article) {
		return new Item(article.getTitle(), article.getAbstract(),  article.getKeywords(), article.getAuthors(), article.getHandle());
	}

	public Item(String title, String description, List<String> keywords, List<Author> authors, String uri) {
		this.name = title;
		this.metadata.setTitle(title);
		this.metadata.setDescription(description);
		this.metadata.setUri(uri);
		authors.forEach(a -> this.metadata.addAuthor(a.getLastName() + ", " + a.getFirstName()));
	}
	
	public static Item fromHttpEntity(HttpEntity entity) throws ParseException, IOException {
		return new Gson().fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), Item.class);
	}
}
