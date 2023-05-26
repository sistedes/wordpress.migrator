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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class Edition extends Track {
	
	private transient final static Logger logger = LoggerFactory.getLogger(Edition.class);
	
	// BEGIN: JSON fields
	protected Date date; 
	// END: JSON fields
	
	private transient Conference conference;
	private transient List<Track> tracks;
	
	public Date getDate() {
		return date;
	}
	
	public void setConference(Conference conference) {
		this.conference = conference;
	}
	
	public Conference getConference() {
		return conference;
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
	
	public String getName() {
		String text = excerpt.get("rendered").replaceAll(" ", " "); // Replace ASCII 255 (non-breaking space) by regular space;
		Matcher matcher = Pattern.compile("Actas de las (.*? \\(.*?\\))").matcher(text);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			throw new RuntimeException("Unable to get edition name for " + getTitle());
		}
	}
	
	public List<Track> getTracks() throws IOException {
		if (tracks == null) {
			try {
				URL url = new URL(getCollectionUrl() + String.format(WorpressEndpoints.LIBRARY_PARENT_QUERY, getId()));
				this.tracks = Collections.unmodifiableList(Arrays.asList(new Gson().fromJson(
						StringEscapeUtils.unescapeXml(IOUtils.toString(DelayedStreamOpener.open(url), StandardCharsets.UTF_8)), Track[].class)));
				this.tracks.forEach(t -> t.setEdition(this));
			} catch (MalformedURLException e) {
				// Should not happen...
				new RuntimeException(e);
			}
		}
		return tracks;
	}
	
	@Override
	public Edition getEdition() {
		return this;
	}
	
	@Override
	public void setEdition(Edition edition) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<Article> getArticles() throws IOException {
		List<Article> articles = new ArrayList<>(super.getArticles());
		for (Track track : getTracks()) {
			articles.addAll(track.getArticles());
		}
		return Collections.unmodifiableList(articles);
	}

	public String getLocation() {
		String text = excerpt.get("rendered").replaceAll(" ", " "); // Replace ASCII 255 (non-breaking space) by regular space;
		Matcher matcher = Pattern.compile("\\(" + conference.getAcronym() + "\\s*\\d+\\)\\.(.*?),").matcher(text);
		if (matcher.find()) {
			return matcher.group(1).trim();
		} else {
			throw new RuntimeException("Unable to get edition location for " + getTitle());
		}
	}

	public String getProceedingsName() {
		String text = excerpt.get("rendered").replaceAll(" ", " "); // Replace ASCII 255 (non-breaking space) by regular space;
		Matcher matcher = Pattern.compile("(Actas de las .*? \\(.*?\\))").matcher(text);
		if (matcher.find()) {
			return matcher.group(1).trim();
		} else {
			throw new RuntimeException("Unable to get edition proceedings for " + getTitle());
		}
	}

	public List<String> getEditors() {
		List<String> editors = new ArrayList<>();
		String text = excerpt.get("rendered").replaceAll(" ", " "); // replace non-breaking spaces
		Matcher matcher = Pattern.compile("<p>(?<ed1>.+?)\\s+(y\\s+(?<ed2>.+?)\\s+)?\\([Ee]ds?\\.\\)").matcher(text);
		if (matcher.find()) {
			if (matcher.group("ed1") != null) editors.add(matcher.group("ed1"));
			if (matcher.group("ed2") != null) editors.add(matcher.group("ed2"));
			return editors;
		} else {
			throw new RuntimeException("Unable to get edition proceedings for " + getTitle());
		}
	}
	
	@Override
	public String getDescription() {
		String description = super.getDescription();
		description = description.replaceAll("<p>&nbsp;</p>", "").trim();
		description = Pattern.compile(
				"<p>\\s*<em>(?:.*?)\\(Eds?\\.\\), Actas de(?:.*?)</em>\\s*</p>", Pattern.DOTALL)
				.matcher(description).replaceAll("").trim();
		description = Pattern.compile(
				"<p>\\s*<i>(?:.*?)\\(Eds?\\.\\), Actas de(?:.*?)</i>\\s*</p>", Pattern.DOTALL)
				.matcher(description).replaceAll("").trim();
		description = Pattern.compile(
				"<ul>\\s*<li>(?:<a href=\"https?://biblioteca.sistedes.es\\S+\">.*?</li>)+\\s*</ul>", Pattern.DOTALL)
				.matcher(description).replaceAll("").trim();
		description = Pattern.compile(
				"<!-- \\[begin:sub_tracks_conferencia\\] -->(?:.*?)<!-- \\[end:sub_tracks_conferencia\\] -->", Pattern.DOTALL)
				.matcher(description).replaceAll("").trim();
		description = Pattern.compile(
				"<!-- \\[begin:cita-conferencia\\] -->(?:.*?)<!-- \\[end:cita-conferencia\\] -->", Pattern.DOTALL)
				.matcher(description).replaceAll("").trim();
		description = Pattern.compile(
				" A continuación se detalla el contenido de las actas:", Pattern.DOTALL)
				.matcher(description).replaceAll("").trim();
		description = Pattern.compile(
				"<ol\\s+id=\"articulo_a_conferencia_list\">(?:.*?)</ol>", Pattern.DOTALL)
				.matcher(description).replaceAll("").trim();
		description = description.replaceAll("<hr ?/>", "").trim();
		return description;
	}
}
