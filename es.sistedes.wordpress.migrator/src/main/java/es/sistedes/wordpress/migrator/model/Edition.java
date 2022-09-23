package es.sistedes.wordpress.migrator.model;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class Edition extends Track {
	
	private transient List<Track> tracks;
	
	public int getYear() {
		Matcher matcher = Pattern.compile(".*\\b(?<year>(?:199|20[012])[0-9])\\b.*").matcher(getTitle());
		if (matcher.matches()) {
			return Integer.valueOf(matcher.group("year"));
		} else {
			throw new RuntimeException(String.format("Unable to determine year for conference edition '%s'", getTitle()));
		}
	}
	
	public List<Track> getTracks() throws IOException {
		if (tracks == null) {
			try {
				URL url = new URL(getCollectionUrl() + String.format(Endpoints.LIBRARY_PARENT_QUERY, getId()));
				this.tracks = Collections.unmodifiableList(Arrays.asList(new Gson().fromJson(new InputStreamReader(DelayedStreamOpener.open(url)), Track[].class)));
			} catch (MalformedURLException e) {
				// Should not happen...
				new RuntimeException(e);
			}
		}
		return tracks;
	}
	
	@Override
	public List<Article> getArticles() throws IOException {
		List<Article> articles = new ArrayList<>(super.getArticles());
		for (Track track : getTracks()) {
			articles.addAll(track.getArticles());
		}
		return Collections.unmodifiableList(articles);
	}
}
