package es.sistedes.wordpress.migrator.dsmodel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.wpmodel.Track;

public class Collection extends AbstractDSpaceEntity {

	private final static List<String> PARTICLES = Arrays.asList(new String[] { "a", "de", "en", "por", "y", "la", "el", "ya"});
	
	public static Collection from(Community community, Track track, Date date) {
		String title = StringUtils.stripAccents(track.getTitle());
		title = title.replaceAll("[^\\w\\s]", "");
		String[] words = title.split("\\s+");
		String suffix = Arrays.asList(words).stream().filter(w -> !PARTICLES.contains(w)).map(w -> w.toUpperCase().substring(0, 1)).collect(Collectors.joining());
		return new Collection(track.getTitle(), track.getDescription(), community.getUri(), suffix, date);
	}

	public Collection(String title, String description, String baseUri, String suffix, Date date) {
		this.name = title;
		this.metadata.setTitle(title);
		this.metadata.setDescription(description);
		this.metadata.setUri(baseUri + "/" + suffix);
		this.metadata.setDate(date);
	}
	
	public static Collection fromHttpEntity(HttpEntity entity) throws ParseException, IOException {
		return new Gson().fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), Collection.class);
	}
	
	public Date getDate() {
		return this.metadata.getDate();
	}
}
