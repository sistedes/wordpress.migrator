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
	
	private volatile static DateFormat DATE_FORMAT_SIMPLE = new SimpleDateFormat("yyyy-MM-dd");
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

	@SerializedName("dc.subject")
	private List<DublinCoreBasic> subjects = new ArrayList<>();
	
	@SerializedName("dc.contributor.author")
	private List<DublinCoreBasic> authors = new ArrayList<>();
	
	@SerializedName("dc.contributor.email")
	private List<DublinCoreBasic> emails = new ArrayList<>();
	
	@SerializedName("dc.contributor.institution")
	private List<DublinCoreBasic> institutions = new ArrayList<>();
	
	@SerializedName("dc.rights.license")
	private List<DublinCoreBasic> licenses = new ArrayList<>();

	@SerializedName("dc.rights.uri")
	private List<DublinCoreBasic> rightsUris = new ArrayList<>();
	
	@SerializedName("dc.date.available")
	private List<DublinCoreBasic> datesAvailable = new ArrayList<>();
	
	@SerializedName("dc.date.issued")
	private List<DublinCoreBasic> datesIssued = new ArrayList<>();
	
	@SerializedName("dc.relation.isformatof")
	private List<DublinCoreBasic> isFormatOf = new ArrayList<>();
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
	
	public void addSubject(String subject) {
		subjects.add(new DublinCoreBasic(subject, subjects.size()));
	}
	
	public void setAbstract(String abs) {
		if (StringUtils.isBlank(abs)) return;
		abstracts.clear();
		abstracts.add(new DublinCoreBasic(abs));
	}

	public void addAuthor(String author) {
		authors.add(new DublinCoreBasic(author, authors.size()));
	}
	
	public void addEmail(String email) {
		emails.add(new DublinCoreBasic(email, emails.size()));
	}
	
	public void addInstitution(String institution) {
		institutions.add(new DublinCoreBasic(institution, institutions.size()));
	}
	
	public void setLicense(String license) {
		if (StringUtils.isBlank(license)) return;
		licenses.clear();
		licenses.add(new DublinCoreBasic(license));
	}

	public void addLicense(String license) {
		if (StringUtils.isBlank(license)) return;
		licenses.add(new DublinCoreBasic(license));
	}
	
	public void setRightsUri(String rightsUri) {
		if (StringUtils.isBlank(rightsUri)) return;
		rightsUris.clear();
		rightsUris.add(new DublinCoreBasic(rightsUri));
	}
	
	public void setDate(Date date) {
		if (date == null) return;
		this.datesAvailable.clear();
		this.datesIssued.clear();
		this.datesAvailable.add(new DublinCoreBasic(DATE_FORMAT.format(date)));
		this.datesIssued.add(new DublinCoreBasic(DATE_FORMAT_SIMPLE.format(date)));
	}

	public void setIsFormatOf(String isFormatOf) {
		if (StringUtils.isBlank(isFormatOf)) return;
		this.isFormatOf.clear();
		this.isFormatOf.add(new DublinCoreBasic(isFormatOf));
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

	public String getRights() {
		return licenses.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public Date getDate() {
		return datesAvailable.stream().findFirst().map(d ->  {
			try { return DATE_FORMAT.parse(d.getValue()); } catch (ParseException e) { throw new RuntimeException(e); }
		}).orElse(null);
	}

	public String getIsFormatOf() {
		return isFormatOf.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
}
