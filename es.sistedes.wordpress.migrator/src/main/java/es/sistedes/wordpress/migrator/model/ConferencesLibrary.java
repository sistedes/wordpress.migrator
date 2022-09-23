package es.sistedes.wordpress.migrator.model;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class ConferencesLibrary extends Library {

	private transient List<Conference> conferences;

	public List<Conference> getConferences() throws IOException {
		if (conferences == null) {
			try {
				URL url = new URL(getCollectionUrl() + String.format(Endpoints.LIBRARY_PARENT_QUERY, getId()));
				this.conferences = Collections
						.unmodifiableList(Arrays.asList(new Gson().fromJson(new InputStreamReader(DelayedStreamOpener.open(url)), Conference[].class)));
			} catch (MalformedURLException e) {
				// Should not happen...
				new RuntimeException(e);
			}
		}
		return conferences;
	}
	public List<Conference> getConferences(Comparator<Conference> comparator) throws IOException {
		return getConferences().stream().sorted(comparator).collect(Collectors.toList());
	}
}
