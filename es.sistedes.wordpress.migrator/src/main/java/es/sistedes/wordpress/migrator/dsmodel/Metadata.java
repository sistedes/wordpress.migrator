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
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.DataHolder;

public class Metadata {
	
	private volatile static DataHolder HTML_2_MD_OPTIONS = FlexmarkHtmlConverter.builder()
													.set(FlexmarkHtmlConverter.TYPOGRAPHIC_QUOTES, false)
													.set(FlexmarkHtmlConverter.TYPOGRAPHIC_SMARTS, false)
													.set(FlexmarkHtmlConverter.SKIP_CHAR_ESCAPE, true);
	
	public volatile static DateFormat DATE_FORMAT_SIMPLE_W_HOUR = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	public volatile static DateFormat DATE_FORMAT_SIMPLE = new SimpleDateFormat("yyyy-MM-dd");
	public volatile static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:00'Z'");
	
	// BEGIN: JSON fields
	@SerializedName("dc.title")
	private List<MetadataEntry> titles = new ArrayList<>();

	@SerializedName("dc.identifier.sistedes")
	private List<MetadataEntry> sistedesIdentifiers = new ArrayList<>();

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
	
	@SerializedName("dc.date.accessioned")
	private List<MetadataEntry> datesAccessioned = new ArrayList<>();
	
	@SerializedName("dc.date.available")
	private List<MetadataEntry> datesAvailable = new ArrayList<>();
	
	@SerializedName("dc.date.issued")
	private List<MetadataEntry> datesIssued = new ArrayList<>();
	
	@SerializedName("dc.publisher")
	private List<MetadataEntry> publishers = new ArrayList<>();
	
	@SerializedName("dc.relation.isformatof")
	private List<MetadataEntry> isFormatOf = new ArrayList<>();

	@SerializedName("dspace.entity.type")
	private List<MetadataEntry> type = new ArrayList<>();
	
	@SerializedName("person.givenName")
	private List<MetadataEntry> personGivenNames = new ArrayList<>();
	
	@SerializedName("person.familyName")
	private List<MetadataEntry> personFamilyNames = new ArrayList<>();
	
	@SerializedName("person.name.variant")
	private List<MetadataEntry> personNameVariants = new ArrayList<>();
	
	@SerializedName("person.affiliation.name")
	private List<MetadataEntry> personAffiliations = new ArrayList<>();

	@SerializedName("person.email")
	private List<MetadataEntry> personEmails = new ArrayList<>();
	
	@SerializedName("dc.contributor.signature")
	private List<MetadataEntry> contributorsSignatures = new ArrayList<>();
	
	@SerializedName("dc.contributor.email")
	private List<MetadataEntry> contributorsEmails = new ArrayList<>();
	
	@SerializedName("dc.contributor.affiliation")
	private List<MetadataEntry> contributorsAffiliations = new ArrayList<>();

	@SerializedName("dc.contributor.bio")
	private List<MetadataEntry> contributorsBios = new ArrayList<>();
	
	@SerializedName("bs.conference.name")
	private List<MetadataEntry> bsConferenceNames = new ArrayList<>();

	@SerializedName("bs.conference.acronym")
	private List<MetadataEntry> bsConferenceAcronyms = new ArrayList<>();
	
	@SerializedName("bs.edition.name")
	private List<MetadataEntry> bsEditionNames = new ArrayList<>();

	@SerializedName("bs.edition.date")
	private List<MetadataEntry> bsEditionDates = new ArrayList<>();

	@SerializedName("bs.edition.location")
	private List<MetadataEntry> bsEditionLocations = new ArrayList<>();

	@SerializedName("bs.proceedings.editor")
	private List<MetadataEntry> bsProceedingsEditors = new ArrayList<>();

	@SerializedName("bs.proceedings.name")
	private List<MetadataEntry> bsProceedingsNames = new ArrayList<>();
	// END: JSON fields
	
	
	public String getTitle() {
		return titles.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setTitle(String title) {
		if (StringUtils.isBlank(title)) return;
		this.titles.clear();
		this.titles.add(new MetadataEntry(title));
	}

	public String getSistedesIdentifier() {
		return sistedesIdentifiers.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}

	public void setSistedesIdentifier(String id) {
		if (StringUtils.isBlank(id)) return;
		this.sistedesIdentifiers.clear();
		this.sistedesIdentifiers.add(new MetadataEntry(id));
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
		this.abstracts.add(new MetadataEntry(
//				abs
				FlexmarkHtmlConverter.builder(HTML_2_MD_OPTIONS).build().convert(abs)
				));
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
	
	public String getPublisher() {
		return publishers.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setPublisher(String publisher) {
		if (StringUtils.isBlank(publisher)) return;
		this.publishers.clear();
		this.publishers.add(new MetadataEntry(publisher));
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

	@SuppressWarnings("deprecation")
	public void setDate(Date date) {
		if (date == null) return;
		this.datesAvailable.clear();
		this.datesIssued.clear();
		this.datesAccessioned.clear();
		this.datesAvailable.add(new MetadataEntry(DATE_FORMAT.format(date)));
		this.datesAccessioned.add(new MetadataEntry(DATE_FORMAT.format(date)));
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

	public String getPersonGivenName() {
		return personGivenNames.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setPersonGivenName(String givenName) {
		if (StringUtils.isBlank(givenName)) return;
		this.personGivenNames.clear();
		this.personGivenNames.add(new MetadataEntry(givenName));
	}
	
	public String getPersonFamilyName() {
		return personFamilyNames.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setPersonFamilyName(String familyName) {
		if (StringUtils.isBlank(familyName)) return;
		this.personFamilyNames.clear();
		this.personFamilyNames.add(new MetadataEntry(familyName));
	}
	
	public List<String> getPersonNameVariants() {
		return personNameVariants.stream().map(e -> e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void setPersonNameVariants(List<String> nameVariants) {
		if (nameVariants == null) return;
		this.personNameVariants.clear();
		for (int i = 0; i < nameVariants.size(); i++) {
			this.personNameVariants.add(new MetadataEntry(nameVariants.get(i), i + 1));
		}
	}

	public List<String> getPersonAffiliations() {
		return personAffiliations.stream().map(a ->  a.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void addPersonAffiliation(String affiliation) {
		if (StringUtils.isBlank(affiliation)) return;
		this.personAffiliations.add(new MetadataEntry(affiliation));
	}
	
	public void setPersonAffiliations(List<String> affiliations) {
		if (this.personAffiliations == null) return;
		this.personAffiliations.clear();
		for (int i = 0; i < affiliations.size(); i++) {
			this.personAffiliations.add(new MetadataEntry(affiliations.get(i), i + 1));
		}
	}
	
	public List<String> getPersonEmails() {
		return personEmails.stream().map(e ->  e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void addPersonEmail(String email) {
		if (StringUtils.isBlank(email)) return;
		this.personEmails.add(new MetadataEntry(email));
	}
	
	public void setPersonEmails(List<String> emails) {
		if (this.personEmails == null) return;
		this.personEmails.clear();
		for (int i = 0; i < emails.size(); i++) {
			this.personEmails.add(new MetadataEntry(emails.get(i), i + 1));
		}
	}
	
	public List<String> getContributorsSignatures() {
		return contributorsSignatures.stream().map(e -> e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void setContributorsSignatures(List<String> authors) {
		if (authors == null) return;
		this.contributorsSignatures.clear();
		for (int i = 0; i < authors.size(); i++) {
			this.contributorsSignatures.add(new MetadataEntry(authors.get(i), i + 1));
		}
	}
	
	public List<String> getContributorsEmails() {
		return contributorsEmails.stream().map(e -> e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void setContributorsEmails(List<String> emails) {
		if (emails == null) return;
		this.contributorsEmails.clear();
		for (int i = 0; i < emails.size(); i++) {
			this.contributorsEmails.add(new MetadataEntry(emails.get(i), i + 1));
		}
	}
	
	public List<String> getContributorsAffiliations() {
		return contributorsAffiliations.stream().map(e -> e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void setContributorsAffiliations(List<String> affiliations) {
		if (affiliations == null) return;
		this.contributorsAffiliations.clear();
		for (int i = 0; i < affiliations.size(); i++) {
			this.contributorsAffiliations.add(new MetadataEntry(affiliations.get(i), i + 1));
		}
	}
	
	public String getContributorBio() {
		return contributorsBios.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setContributorBio(String bio) {
		if (StringUtils.isBlank(bio)) return;
		this.contributorsBios.clear();
		this.contributorsBios.add(new MetadataEntry(
//				bio
				FlexmarkHtmlConverter.builder(HTML_2_MD_OPTIONS).build().convert(bio)
				));
	}
	
	public String getSistedesConferenceName() {
		return bsConferenceNames.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesConferenceName(String conferenceName) {
		if (StringUtils.isBlank(conferenceName)) return;
		this.bsConferenceNames.clear();
		this.bsConferenceNames.add(new MetadataEntry(conferenceName));
	}
	
	public String getSistedesConferenceAcronym() {
		return bsConferenceAcronyms.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesConferenceAcronym(String conferenceAcronym) {
		if (StringUtils.isBlank(conferenceAcronym)) return;
		this.bsConferenceAcronyms.clear();
		this.bsConferenceAcronyms.add(new MetadataEntry(conferenceAcronym));
	}
	
	public String getSistedesEditionName() {
		return bsEditionNames.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesEditionName(String editionName) {
		if (StringUtils.isBlank(editionName)) return;
		this.bsEditionNames.clear();
		this.bsEditionNames.add(new MetadataEntry(editionName));
	}

	public String getSistedesEditionDate() {
		return bsEditionDates.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesEditionDate(String editionDate) {
		if (StringUtils.isBlank(editionDate)) return;
		this.bsEditionDates.clear();
		this.bsEditionDates.add(new MetadataEntry(editionDate));
	}
	
	public String getSistedesEditionLocation() {
		return bsEditionLocations.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesEditionLocation(String editionLocation) {
		if (StringUtils.isBlank(editionLocation)) return;
		this.bsEditionLocations.clear();
		this.bsEditionLocations.add(new MetadataEntry(editionLocation));
	}
	
	public List<String> getSistedesProceedingsEditor() {
		return bsProceedingsEditors.stream().map(e -> e.getValue()).collect(Collectors.toUnmodifiableList());
	}
	
	public void setSistedesProceedingsEditor(List<String> proceedingsEditors) {
		if (proceedingsEditors == null) return;
		this.bsProceedingsEditors.clear();
		for (int i = 0; i < proceedingsEditors.size(); i++) {
			this.bsProceedingsEditors.add(new MetadataEntry(proceedingsEditors.get(i), i + 1));
		}
	}
	
	public String getSistedesProceedingsName() {
		return bsProceedingsNames.stream().findFirst().map(e ->  e.getValue()).orElse(null);
	}
	
	public void setSistedesProceedingsName(String ProceedingsName) {
		if (StringUtils.isBlank(ProceedingsName)) return;
		this.bsProceedingsNames.clear();
		this.bsProceedingsNames.add(new MetadataEntry(ProceedingsName));
	}
}
