package es.sistedes.wordpress.migrator.wpmodel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class BDSistedes {

	private transient DocumentsLibrary documentsLibrary;
	private transient ConferencesLibrary conferencesLibrary;

	public BDSistedes(URL baseUrl) throws IOException {
		URL url = new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(),
				WorpressEndpoints.LIBRARY_ENDPOINT + String.format(WorpressEndpoints.LIBRARY_PARENT_QUERY, "0"));
		
		List<Library> libraries = Arrays.asList(new Gson().fromJson(new InputStreamReader(DelayedStreamOpener.open(url)), Library[].class));
		
		this.documentsLibrary = new Gson().fromJson(
				new Gson().toJson(libraries.stream().filter(l -> StringUtils.containsIgnoreCase(l.getTitle(), "documentos")).findFirst().orElseThrow()),
				DocumentsLibrary.class);
		this.conferencesLibrary = new Gson().fromJson(
				new Gson().toJson(libraries.stream().filter(l -> StringUtils.containsIgnoreCase(l.getTitle(), "conferencias")).findFirst().orElseThrow()),
				ConferencesLibrary.class);
	}

	public Library getDocumentsLibrary() {
		return documentsLibrary;
	}

	public ConferencesLibrary getConferencesLibrary() throws IOException {
		return conferencesLibrary;
	}
}
