package es.sistedes.wordpress.migrator.wpmodel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class Conference extends Library {

	private transient List<Edition> editions;

	public String getAcronym() {
		Matcher matcher = Pattern.compile(".*\\(\\b(?<acro>[A-Z]+)\\b\\).*").matcher(getTitle());
		if (matcher.matches()) {
			return matcher.group("acro");
		} else {
			throw new RuntimeException(String.format("Unable to determine acronym for conference '%s'", getTitle()));
		}
	}
	
	public List<Edition> getEditions() throws IOException {
		if (editions == null) {
			try {
				URL url = new URL(getCollectionUrl() + String.format(WorpressEndpoints.LIBRARY_PARENT_QUERY, getId()));
				this.editions = Collections.unmodifiableList(Arrays.asList(new Gson().fromJson(new InputStreamReader(DelayedStreamOpener.open(url)), Edition[].class)));
			} catch (MalformedURLException e) {
				// Should not happen...
				new RuntimeException(e);
			}
		}
		return editions;
	}
	
	public List<Edition> getEditions(Comparator<Edition> comparator) throws IOException {
		return getEditions().stream().sorted(comparator).collect(Collectors.toList());
	}
}
