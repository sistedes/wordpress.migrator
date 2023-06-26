package es.sistedes.wordpress.migrator.wpmodel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class DocumentsLibrary extends Library {
	
	private transient List<Seminar> seminars;
	private transient List<Bulletin> bulletins;

	public String getLibraryName() {
		return "Archivo documental de Sistedes";
	}
	
	public String getDescription() {
		return "Sistedes pone a disposición de sus miembros y simpatizantes su "
				+ "archivo documental, en el que se incluyen todo tipo de publicaciones (boletines de prensa, seminarios, "
				+ "documentos, informes, etc.) que puedan ser de interés para las comunidades de Ingeniería del Software, "
				+ "Bases de Datos y  Tecnologías de Desarrollo de Software.";
	}
	
	public List<Seminar> getSeminars() throws IOException {
		// For the seminars, no endpoint is provided in the API, so we scrap the 
		// raw HTML instead as a quick and dirty solution...
		if (seminars == null) {
			seminars = new ArrayList<>();
			try {
				URL url = new URL(getBaseURL() + "/seminario/seminarios-sistedes/");
				String page = IOUtils.toString(new InputStreamReader(DelayedStreamOpener.open(url), "UTF-8"));
				String listPattern = "<a .*? href=.*?>Seminarios SISTEDES</a>\\s<ul.*?>(.*?)</ul>"; 
				Matcher listMatcher = Pattern.compile(listPattern, Pattern.MULTILINE | Pattern.DOTALL).matcher(page);
				if (listMatcher.find()) {
					String list = listMatcher.group(1);
					String itemPattern = "<li>\\s*<a .*?href=\"(\\S+)\">.*?</li>";
					Matcher itemMatcher = Pattern.compile(itemPattern, Pattern.MULTILINE | Pattern.DOTALL).matcher(list);
					while (itemMatcher.find()) {
						seminars.add(new Seminar(new URL(itemMatcher.group(1).trim())));
					}
				}
			} catch (MalformedURLException e) {
				// Should not happen...
				new RuntimeException(e);
			}
		}
		return seminars;
	}
	
	public List<Bulletin> getBulletins() throws IOException {
		// For the bulletins, no endpoint is provided in the API, so we scrap the 
		// raw HTML instead as a quick and dirty solution...
		if (bulletins == null) {
			bulletins = new ArrayList<>();
			try {
				URL url = new URL(getBaseURL() + "/boletin/boletines-de-prensa/");
				String page = IOUtils.toString(new InputStreamReader(DelayedStreamOpener.open(url), "UTF-8"));
				String listPattern = "<a .*? href=.*?>Boletines de Prensa</a>\\s<ul.*?>(.*?)</ul>\\s*</li>\\s*</ul>"; 
				Matcher listMatcher = Pattern.compile(listPattern, Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS).matcher(page);
				if (listMatcher.find()) {
					String list = listMatcher.group(1);
					String itemPattern = "<li>\\s*<a .*?href=\"(\\S+boletin/boletines-de-prensa/20\\S+/\\S+)\".*?>Boletín n.*?</a></li>";
					Matcher itemMatcher = Pattern.compile(itemPattern, Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS).matcher(list);
					while (itemMatcher.find()) {
						bulletins.add(new Bulletin(new URL(itemMatcher.group(1).trim())));
					}
				}
			} catch (MalformedURLException e) {
				// Should not happen...
				new RuntimeException(e);
			}
		}
		return bulletins;
	}
	
	public List<Bulletin> getBulletins(Comparator<Bulletin> comparator) throws IOException {
		return getBulletins().stream().sorted(comparator).collect(Collectors.toList());
	}
	
	public List<Seminar> getSeminars(Comparator<Seminar> comparator) throws IOException {
		return getSeminars().stream().sorted(comparator).collect(Collectors.toList());
	}
	
	private URL getBaseURL() throws MalformedURLException {
		URL url = new URL(getCollectionUrl());
		return new URL(url.getProtocol(), url.getHost(), url.getPort(), "");
	}
	
}
