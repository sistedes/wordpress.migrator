package es.sistedes.wordpress.migrator.wpmodel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class Edition extends Track {
	
	// BEGIN: JSON fields
	private Date date; 
	// END: JSON fields
	
	private transient List<Track> tracks;
	
	public Date getDate() {
		return date;
	}
	
	public int getYear() {
		// Do not trust the parsed date from Wordpress post metadata.
		// It is only correctly set for newer conferences, and this
		// method is important to filter out which conferences are
		// processes
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
				URL url = new URL(getCollectionUrl() + String.format(WorpressEndpoints.LIBRARY_PARENT_QUERY, getId()));
				this.tracks = Collections.unmodifiableList(Arrays.asList(new Gson().fromJson(
						StringEscapeUtils.unescapeXml(IOUtils.toString(DelayedStreamOpener.open(url), StandardCharsets.UTF_8)), Track[].class)));
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
