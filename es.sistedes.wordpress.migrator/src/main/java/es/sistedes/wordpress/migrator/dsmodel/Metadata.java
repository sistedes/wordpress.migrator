package es.sistedes.wordpress.migrator.dsmodel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.annotations.SerializedName;

public class Metadata {
	
	public volatile static DateFormat DATE_FORMAT_SIMPLE_W_HOUR = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	public volatile static DateFormat DATE_FORMAT_SIMPLE = new SimpleDateFormat("yyyy-MM-dd");
	public volatile static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:00'Z'");
	
	// BEGIN: JSON fields
	@SerializedName("dc.title")
	private List<MetadataEntry> titles = new ArrayList<>();

	@SerializedName("dc.identifier.uri")
	private List<MetadataEntry> uris = new ArrayList<>();

	@SerializedName("dc.description")
	private List<MetadataEntry> descriptions = new ArrayList<>();
	
	@SerializedName("dc.description.abstract")
	private List<MetadataEntry> abstracts = new ArrayList<>();
	
	@SerializedName("dc.description.tableofcontents")
	private List<MetadataEntry> tocs = new ArrayList<>();

	@SerializedName("dc.relation.ispartof")
	private List<MetadataEntry> isPartOf = new ArrayList<>();

	@SerializedName("dc.subject")
	private List<MetadataEntry> subjects = new ArrayList<>();
	
	@SerializedName("dc.rights")
	private List<MetadataEntry> rights = new ArrayList<>();
	
	@SerializedName("dc.rights.license")
	private List<MetadataEntry> licenses = new ArrayList<>();

	@SerializedName("dc.rights.uri")
	private List<MetadataEntry> rightsUris = new ArrayList<>();
	
	@SerializedName("dc.date.available")
	private List<MetadataEntry> datesAvailable = new ArrayList<>();
	
	@SerializedName("dc.date.issued")
	private List<MetadataEntry> datesIssued = new ArrayList<>();
	
	@SerializedName("dc.relation.isformatof")
	private List<MetadataEntry> isFormatOf = new ArrayList<>();

	@SerializedName("dspace.entity.type")
	private List<MetadataEntry> type = new ArrayList<>();
	
	@SerializedName("person.givenName")
	private List<MetadataEntry> givenNames = new ArrayList<>();
	
	@SerializedName("person.familyName")
	private List<MetadataEntry> familyNames = new ArrayList<>();
	
	@SerializedName("person.name.variant")
	private List<MetadataEntry> nameVariants = new ArrayList<>();
	
	@SerializedName("person.affiliation.name")
	private List<MetadataEntry> affiliations = new ArrayList<>();

	@SerializedName("person.email")
	private List<MetadataEntry> emails = new ArrayList<>();
	
	@SerializedName("bds.contributor.author")
	private List<MetadataEntry> bdsAuthors = new ArrayList<>();
	
	@SerializedName("bds.contributor.email")
	private List<MetadataEntry> bdsEmails = new ArrayList<>();
	
	@SerializedName("bds.contributor.affiliation")
	private List<MetadataEntry> bdsAffiliations = new ArrayList<>();

	@SerializedName("bds.contributor.bio")
	private List<MetadataEntry> bdsBio = new ArrayList<>();
	
	@SerializedName("bds.conference.name")
	private List<MetadataEntry> bdsConferenceNames = new ArrayList<>();

	@SerializedName("bds.edition.name")
	private List<MetadataEntry> bdsEditionNames = new ArrayList<>();

	@SerializedName("bds.edition.year")
	private List<MetadataEntry> bdsEditionYears = new ArrayList<>();

	@SerializedName("bds.edition.date")
	private List<MetadataEntry> bdsEditionDates = new ArrayList<>();

	@SerializedName("bds.edition.location")
	private List<MetadataEntry> bdsEditionLocations = new ArrayList<>();

	@SerializedName("bds.proceedings.editors")
	private List<MetadataEntry> bdsProceedingsEditors = new ArrayList<>();

	@SerializedName("bds.proceedings.name")
	private List<MetadataEntry> bdsProceedingsNames = new ArrayList<>();
	// END: JSON fields
	
	
	public String getTitle() {
		return titles.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setTitle(String title) {
		if (StringUtils.isBlank(title)) return;
		this.titles.clear();
		this.titles.add(new MetadataEntry(title));
	}

	public String getUri() {
		return uris.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setUri(String uri) {
		if (StringUtils.isBlank(uri)) return;
		this.uris.clear();
		this.uris.add(new MetadataEntry(uri));
	}

	public String getDescription() {
		return descriptions.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setDescription(String description) {
		if (StringUtils.isBlank(description)) return;
		this.descriptions.clear();
		this.descriptions.add(new MetadataEntry(description));
	}
	
	public String getAbstract() {
		return abstracts.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setAbstract(String abs) {
		if (StringUtils.isBlank(abs)) return;
		this.abstracts.clear();
		this.abstracts.add(new MetadataEntry(abs));
	}
	
	public String getToc() {
		return tocs.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setToc(String toc) {
		if (StringUtils.isBlank(toc)) return;
		this.tocs.clear();
		this.tocs.add(new MetadataEntry(toc));
	}

	public String getIsPartOf() {
		return isPartOf.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setIsPartOf(String isPartOf) {
		if (StringUtils.isBlank(isPartOf)) return;
		this.isPartOf.clear();
		this.isPartOf.add(new MetadataEntry(isPartOf));
	}
	
	public List<String> getSubjects() {
		return subjects.stream().map(e -> e.getValue()).collect(Collectors.toUnmodifiableList());
	}

	public void setSubjects(List<String> subjects) {
		if (subjects == null) return;
		this.subjects.clear();
		for (int i = 0; i < subjects.size(); i++) {
			this.subjects.add(new MetadataEntry(subjects.get(i), i + 1));
		}
	}

	public String getRights() {
		return rights.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setRights(String rights) {
		if (StringUtils.isBlank(rights)) return;
		this.rights.clear();
		this.rights.add(new MetadataEntry(rights));
	}
	
	public String getLicense() {
		return licenses.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setLicense(String license) {
		if (StringUtils.isBlank(license)) return;
		this.licenses.clear();
		this.licenses.add(new MetadataEntry(license));
	}
	
	public String getRightsUri() {
		return rightsUris.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setRightsUri(String rightsUri) {
		if (StringUtils.isBlank(rightsUri)) return;
		this.rightsUris.clear();
		this.rightsUris.add(new MetadataEntry(rightsUri));
	}
	
	public Date getDate() {
		return datesAvailable.stream().findFirst().map(d ->  {
			try { return DATE_FORMAT.parse(d.getValue()); } catch (ParseException e) { throw new RuntimeException(e); }
		}).orElse(null);
	}

	public void setDate(Date date) {
		if (date == null) return;
		this.datesAvailable.clear();
		this.datesIssued.clear();
		this.datesAvailable.add(new MetadataEntry(DATE_FORMAT.format(date)));
		if (date.getHours() == 0 && date.getMinutes() == 0) {
			this.datesIssued.add(new MetadataEntry(DATE_FORMAT_SIMPLE.format(date)));
		} else {
			this.datesIssued.add(new MetadataEntry(DATE_FORMAT_SIMPLE_W_HOUR.format(date)));
		}
	}

	public String getIsFormatOf() {
		return isFormatOf.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setIsFormatOf(String isFormatOf) {
		if (StringUtils.isBlank(isFormatOf)) return;
		this.isFormatOf.clear();
		this.isFormatOf.add(new MetadataEntry(isFormatOf));
	}
	
	public String getType() {
		return type.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setType(String type) {
		if (StringUtils.isBlank(type)) return;
		this.type.clear();
		this.type.add(new MetadataEntry(type));
	}

	public String getGivenName() {
		return givenNames.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setGivenName(String givenName) {
		if (StringUtils.isBlank(givenName)) return;
		this.givenNames.clear();
		this.givenNames.add(new MetadataEntry(givenName));
	}
	
	public String getFamilyName() {
		return familyNames.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setFamilyName(String familyName) {
		if (StringUtils.isBlank(familyName)) return;
		this.familyNames.clear();
		this.familyNames.add(new MetadataEntry(familyName));
	}
	
	public List<String> getNameVariants() {
		return nameVariants.stream().map(e -> e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void setNameVariants(List<String> nameVariants) {
		if (nameVariants == null) return;
		this.nameVariants.clear();
		for (int i = 0; i < nameVariants.size(); i++) {
			this.nameVariants.add(new MetadataEntry(nameVariants.get(i), i + 1));
		}
	}

	public List<String> getAffiliations() {
		return affiliations.stream().map(a ->  a.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void addAffiliation(String affiliation) {
		if (StringUtils.isBlank(affiliation)) return;
		this.affiliations.add(new MetadataEntry(affiliation));
	}
	
	public void setAffiliations(List<String> affiliations) {
		if (this.affiliations == null) return;
		this.affiliations.clear();
		for (int i = 0; i < affiliations.size(); i++) {
			this.affiliations.add(new MetadataEntry(affiliations.get(i), i + 1));
		}
	}
	
	public List<String> getEmails() {
		return emails.stream().map(e ->  e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void addEmail(String email) {
		if (StringUtils.isBlank(email)) return;
		this.emails.add(new MetadataEntry(email));
	}
	
	public void setEmails(List<String> emails) {
		if (this.emails == null) return;
		this.emails.clear();
		for (int i = 0; i < emails.size(); i++) {
			this.emails.add(new MetadataEntry(emails.get(i), i + 1));
		}
	}
	
	public List<String> getSistedesAuthors() {
		return bdsAuthors.stream().map(e -> e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void setSistedesAuthors(List<String> authors) {
		if (authors == null) return;
		this.bdsAuthors.clear();
		for (int i = 0; i < authors.size(); i++) {
			this.bdsAuthors.add(new MetadataEntry(authors.get(i), i + 1));
		}
	}
	
	public List<String> getSistedesEmails() {
		return bdsEmails.stream().map(e -> e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void setSistedesEmails(List<String> emails) {
		if (emails == null) return;
		this.bdsEmails.clear();
		for (int i = 0; i < emails.size(); i++) {
			this.bdsEmails.add(new MetadataEntry(emails.get(i), i + 1));
		}
	}
	
	public List<String> getSistedesAffiliations() {
		return bdsAffiliations.stream().map(e -> e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void setSistedesAffiliations(List<String> affiliations) {
		if (affiliations == null) return;
		this.bdsAffiliations.clear();
		for (int i = 0; i < affiliations.size(); i++) {
			this.bdsAffiliations.add(new MetadataEntry(affiliations.get(i), i + 1));
		}
	}
	
	public String getSistedesBio() {
		return bdsBio.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesBio(String bio) {
		if (StringUtils.isBlank(bio)) return;
		this.bdsBio.clear();
		this.bdsBio.add(new MetadataEntry(bio));
	}
	
	public String getSistedesConferenceName() {
		return bdsConferenceNames.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesConferenceName(String conferenceName) {
		if (StringUtils.isBlank(conferenceName)) return;
		this.bdsConferenceNames.clear();
		this.bdsConferenceNames.add(new MetadataEntry(conferenceName));
	}
	
	public String getSistedesEditionName() {
		return bdsEditionNames.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesEditionName(String editionName) {
		if (StringUtils.isBlank(editionName)) return;
		this.bdsEditionNames.clear();
		this.bdsEditionNames.add(new MetadataEntry(editionName));
	}
	
	public String getSistedesEditionYear() {
		return bdsEditionYears.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesEditionYear(String editionYear) {
		if (StringUtils.isBlank(editionYear)) return;
		this.bdsEditionYears.clear();
		this.bdsEditionYears.add(new MetadataEntry(editionYear));
	}
	
	public String getSistedesEditionDate() {
		return bdsEditionDates.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesEditionDate(String editionDate) {
		if (StringUtils.isBlank(editionDate)) return;
		this.bdsEditionDates.clear();
		this.bdsEditionDates.add(new MetadataEntry(editionDate));
	}
	
	public String getSistedesEditionLocation() {
		return bdsEditionLocations.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesEditionLocation(String editionLocation) {
		if (StringUtils.isBlank(editionLocation)) return;
		this.bdsEditionLocations.clear();
		this.bdsEditionLocations.add(new MetadataEntry(editionLocation));
	}
	
	public String getSistedesProceedingsEditor() {
		return bdsProceedingsEditors.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesProceedingsEditor(String ProceedingsEditor) {
		if (StringUtils.isBlank(ProceedingsEditor)) return;
		this.bdsProceedingsEditors.clear();
		this.bdsProceedingsEditors.add(new MetadataEntry(ProceedingsEditor));
	}
	
	public String getSistedesProceedingsName() {
		return bdsProceedingsNames.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesProceedingsName(String ProceedingsName) {
		if (StringUtils.isBlank(ProceedingsName)) return;
		this.bdsProceedingsNames.clear();
		this.bdsProceedingsNames.add(new MetadataEntry(ProceedingsName));
	}
}
