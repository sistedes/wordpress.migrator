package es.sistedes.wordpress.migrator.dsmodel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.annotations.SerializedName;

public class Metadata {
	
	private volatile static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T00:00:00Z'");
	
	// BEGIN: JSON fields
	@SerializedName("dc.title")
	private List<DublinCoreBasic> titles = new ArrayList<>();

	@SerializedName("dc.identifier.uri")
	private List<DublinCoreBasic> uris = new ArrayList<>();

	@SerializedName("dc.description")
	private List<DublinCoreBasic> descriptions = new ArrayList<>();
	
	@SerializedName("dc.description.abstract")
	private List<DublinCoreBasic> abstracts = new ArrayList<>();

	@SerializedName("dc.description.toc")
	private List<DublinCoreBasic> tocs = new ArrayList<>();

	@SerializedName("dc.contributor.author")
	private List<DublinCoreBasic> authors = new ArrayList<>();
	
	@SerializedName("dc.rights")
	private List<DublinCoreBasic> rights = new ArrayList<>();

	@SerializedName("dc.date.accessioned")
	private List<DublinCoreBasic> datesAccessioned = new ArrayList<>();

	@SerializedName("dc.date.available")
	private List<DublinCoreBasic> datesAvailable = new ArrayList<>();
	// END: JSON fields
	
	
	public void setTitle(String title) {
		if (StringUtils.isBlank(title)) return;
		titles.clear();
		titles.add(new DublinCoreBasic(title));
	}

	public void setUri(String uri) {
		if (StringUtils.isBlank(uri)) return;
		uris.clear();
		uris.add(new DublinCoreBasic(uri));
	}

	public void setDescription(String description) {
		if (StringUtils.isBlank(description)) return;
		descriptions.clear();
		descriptions.add(new DublinCoreBasic(description));
	}
	
	public void setAbstract(String abs) {
		if (StringUtils.isBlank(abs)) return;
		abstracts.clear();
		abstracts.add(new DublinCoreBasic(abs));
	}

	public void setToc(String toc) {
		if (StringUtils.isBlank(toc)) return;
		tocs.clear();
		tocs.add(new DublinCoreBasic(toc));
	}

	public void addAuthor(String author) {
		authors.add(new DublinCoreBasic(author, authors.size()));
	}
	
	public void setRights(String rights) {
		if (StringUtils.isBlank(rights)) return;
		this.rights.clear();
		this.rights.add(new DublinCoreBasic(rights));
	}
	
	public void setDate(Date date) {
		if (date == null) return;
		this.datesAccessioned.clear();
		this.datesAvailable.clear();
		this.datesAccessioned.add(new DublinCoreBasic(DATE_FORMAT.format(date)));
		this.datesAvailable.add(new DublinCoreBasic(DATE_FORMAT.format(date)));
	}
	
	public String getAbstract() {
		return abstracts.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public String getTitle() {
		return titles.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public String getUri() {
		return uris.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public String getDescription() {
		return descriptions.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public String getToc() {
		return tocs.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public String getRights() {
		return rights.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public Date getDate() {
		return datesAvailable.stream().findFirst().map(d ->  {
			try { return DATE_FORMAT.parse(d.getValue()); } catch (ParseException e) { throw new RuntimeException(e); }
		}).orElse(null);
	}
	
}
