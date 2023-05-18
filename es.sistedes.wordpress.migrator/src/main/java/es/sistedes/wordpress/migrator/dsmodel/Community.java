package es.sistedes.wordpress.migrator.dsmodel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.wpmodel.Conference;
import es.sistedes.wordpress.migrator.wpmodel.DocumentsLibrary;
import es.sistedes.wordpress.migrator.wpmodel.Edition;

public class Community extends DSpaceEntity {

	private static final String BIBTEX_TAG = "<p>Ver la referencia en formato <a href=\"#\"  class=\"citaBibtex\">Bibtex</a></p>";
	
	public static Community from(Site site, DocumentsLibrary library) {
		return new Community(library.getLibraryName(), library.getDescription(), library.getDescription(),
				site.getBaseUri().replaceAll("https?://hdl.handle.net/", "") + "/SISTEDES");
	}
	
	public static Community from(Site site, Conference conference) {
		String description = conference.getDescription();
		String _abstract = null;
		try {
			String paragraph = description.substring(description.indexOf("<p>") + 3, description.indexOf("</p>"));
			_abstract = paragraph.replaceAll("<[^>]+/?>", "");
		} catch (IndexOutOfBoundsException e) {
			// Ignore if we can't extract the first paragraph using substrings...
		};
		return new Community(conference.getTitle(), description, _abstract,
				site.getBaseUri().replaceAll("https?://hdl.handle.net/", "") + "/" + conference.getAcronym());
	}
	
	public static Community from(Community community, Edition edition) {
		String description = edition.getDescription();
		String _abstract = null;
		if (edition.getDescription().contains(BIBTEX_TAG)) {
			description = description.substring(0, description.indexOf(BIBTEX_TAG)).trim();
		} else {
			description = edition.getDescription();
		}
		try {
			String paragraph = description.substring(description.indexOf("<p>") + 3, description.indexOf("</p>"));
			_abstract = paragraph.replaceAll("<[^>]+/?>", "");
		} catch (IndexOutOfBoundsException e) {
			// Ignore if we can't extract the first paragraph using substrings...
		};
		return new Community(edition.getTitle(), description, _abstract, community.getSistedesIdentifier() + "/" + String.valueOf(edition.getYear()));
	}
	
	private Community(String title, String description, String _abstract, String sistedesId) {
		setTitle(title); 
		setDescription(description);
		setAbstract(_abstract);
		setSistedesIdentifier(sistedesId);
	}
	
	public static Community fromHttpEntity(HttpEntity entity) throws ParseException, IOException {
		return new Gson().fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), Community.class);
	}

}
