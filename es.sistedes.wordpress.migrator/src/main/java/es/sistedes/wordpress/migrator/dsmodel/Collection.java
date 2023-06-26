package es.sistedes.wordpress.migrator.dsmodel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.dsmodel.Item.Type;
import es.sistedes.wordpress.migrator.wpmodel.Track;

public class Collection extends DSpaceEntity {

	public static Collection from(Community community, Track track, Date date) {
		String title = track.getTitle().replaceFirst("Sesi.n\\s+\\d+\\W+", "");
		title = title.replaceFirst("^\\d+\\.\\W+", "");
		title = title.replaceFirst("^[A-Z]+:\\W+", "");
		title = title.replaceFirst("^Track [A-Z]+\\W+", "");
		Matcher matcher;
		String suffix;
		if (!title.contains(" ")) {
			suffix = StringUtils.stripAccents(title).toUpperCase();
		} else if ((matcher = Pattern.compile("Track ([A-Z]+) .*").matcher(track.getTitle())).matches()) {
			suffix = matcher.group(1);
		} else if ((matcher = Pattern.compile("([A-Z]+): .*").matcher(track.getTitle())).matches()) {
			suffix = matcher.group(1);
		} else if ((matcher = Pattern.compile("([\\w ]+): .*").matcher(track.getTitle())).matches()) {
			String[] words = matcher.group(1).replaceAll("[^\\w\\s]", "").split("\\s+");
			suffix = Arrays.asList(words).stream().filter(w -> !PARTICLES.contains(w)).map(w -> w.toUpperCase().substring(0, 1)).collect(Collectors.joining());
		} else {
			String[] words = StringUtils.stripAccents(title).replaceAll("[^\\w\\s]", "").split("\\s+");
			suffix = Arrays.asList(words).stream().filter(w -> !PARTICLES.contains(w)).map(w -> w.toUpperCase().substring(0, 1)).collect(Collectors.joining());
		}
		String _abstract = "Artículos en la categoría " + title + " publicados en las " + track.getEdition().getProceedingsName() + ".";
		String description = "Artículos en la categoría <em>" + title + "</em> publicados en las <em>" + track.getEdition().getProceedingsName() + "</em>.";
		return new Collection(title, _abstract, description, community.getSistedesIdentifier() + "/" + suffix, date);
	}

	public Collection(String title, String _abstract, String description, String sistedesId, Date date, Type entityType) {
		this(title, _abstract, description, sistedesId, date);
		this.metadata.setType(entityType.getName());
	}
	
	public Collection(String title, String _abstract, String description, String sistedesId, Date date) {
		setTitle(title);
		setAbstract(_abstract);
		setDescription(description);
		setSistedesIdentifier(sistedesId);
		setDate(date);
	}

	public Date getDate() {
		return this.metadata.getDate();
	}

	protected void setDate(Date date) {
		this.metadata.setDate(date);
	}

	public static Collection fromHttpEntity(HttpEntity entity) throws ParseException, IOException {
		return new Gson().fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), Collection.class);
	}
}
