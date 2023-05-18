package es.sistedes.wordpress.migrator.dsmodel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.wpmodel.Article;
import es.sistedes.wordpress.migrator.wpmodel.Author;
import es.sistedes.wordpress.migrator.wpmodel.Document.License;

public class Publication extends Item {

	private transient static final Logger LOGGER = LoggerFactory.getLogger(Publication.class);
	private transient static final String PDF_CACHE_DIR = "pdfcache";
	private transient static final String PAPERS_DIR = "/articulos";
	private transient static final String ABSTRACTS_DIR = "/resumenes";

	private transient List<Author> authors = new ArrayList<>();
	
	public Publication(String title, String _abstract, List<String> keywords, List<Author> authors, String sistedesId, String licenseName, Date date) {
			setTitle(title);
			setAbstract(_abstract);
			setSistedesIdentifier(sistedesId);
			setDate(date);
			setKeywords(keywords);
			setLicense(License.from(licenseName), sistedesId);
			setAuthors(authors);
		}

	public static Publication from(Collection collection, Article article) {
		File file = getPdfFile(article.getHandle());
		try {
			if (file != null && !file.exists()) {
				FileUtils.copyInputStreamToFile((InputStream) new URL(article.getDocumentUrl()).getContent(), file);
			}
		} catch (Exception e) {
			LOGGER.error("Unable to retrieve PDF file for "  + article.getLink());
		}
		Publication publication = new Publication(
				article.getTitle(),
				article.getAbstract(),
				article.getKeywords(),
				article.getAuthors(),
				article.getHandle(),
				article.getLicense(),
				collection.getDate());
		publication.setType(publication.isAbstract() ? Type.ABSTRACT.getName() : Type.PAPER.getName());
		publication.setIsPartOf(article.getTrack().getEdition().getProceedingsName());
		publication.metadata.setSistedesConferenceName(article.getTrack().getEdition().getConference().getTitle());
		publication.metadata.setSistedesConferenceAcronym(article.getTrack().getEdition().getConference().getAcronym());
		publication.metadata.setSistedesEditionName(article.getTrack().getEdition().getName());
		publication.metadata.setSistedesEditionDate(new SimpleDateFormat("yyyy-MM-dd").format(
				article.getTrack().getEdition().getDate()));
		publication.metadata.setSistedesEditionLocation(article.getTrack().getEdition().getLocation());
		publication.metadata.setSistedesProceedingsName(article.getTrack().getEdition().getProceedingsName());
		publication.metadata.setSistedesProceedingsEditor(article.getTrack().getEdition().getEditors());
		publication.metadata.setPublisher("Sistedes");
		return publication;
	}

	@Override
	public void setTitle(String title) {
		this.name = title;
		super.setTitle(title);
	}
	
	public List<String> getKeywords() {
		return Collections.unmodifiableList(this.metadata.getSubjects());
	}
	
	public void setKeywords(List<String> keywords) {
		this.metadata.setSubjects(keywords);
	}
	
	public String getIsPartOf() {
		return this.metadata.getIsPartOf();
	}
	
	public void setIsPartOf(String isPartOf) {
		this.metadata.setIsPartOf(isPartOf);
	}
	
	public String getLicense() {
		return this.metadata.getLicense();
	}
	
	public void setLicense(String license) {
		this.metadata.setLicense(license);
	}

	public String getIsFormatOf() {
		return this.metadata.getIsFormatOf();
	}
	
	public void setIsFormatOf(String isFormatOF) {
		this.metadata.setIsFormatOf(isFormatOF);
	}

	public String getRightsUri() {
		return this.metadata.getRightsUri();
	}
	
	public void setRightsUri(String uri) {
		this.metadata.setRightsUri(uri);
	}

	public Date getDate() {
		return this.metadata.getDate();
	}

	public void setDate(Date date) {
		this.metadata.setDate(date);
	}
	
	
	public String getPublisher() {
		return this.metadata.getPublisher();
	}
	
	public void setPublisher(String publisher) {
		this.metadata.setPublisher(publisher);
	}
	
	
	
	public File getFile() {
		return getPdfFile(getSistedesIdentifier());
	}

	public boolean isAbstract() {
		return Paths.get(PDF_CACHE_DIR + ABSTRACTS_DIR, RegExUtils.replaceAll(getSistedesIdentifier(), "/", "-") + ".pdf").toFile().exists();
	}
	
	public void setAuthors(List<Author> authors) {
		if (authors == null) return;
		this.authors = new ArrayList<>(authors);
		this.metadata.setContributorsSignatures(authors.stream().map(a -> a.getLastName() + ", " + a.getFirstName()).collect(Collectors.toList()));
		if (authors.stream().anyMatch(a -> StringUtils.isNotBlank(a.getEmail()))) {
			this.metadata.setContributorsEmails(authors.stream().map(a -> StringUtils.defaultIfBlank(a.getEmail(), "")).collect(Collectors.toList()));
		}
		if (authors.stream().anyMatch(a -> StringUtils.isNotBlank(a.getAffiliation()))) {
			this.metadata.setContributorsAffiliations(authors.stream().map(a -> StringUtils.defaultIfBlank(a.getAffiliation(), "")).collect(Collectors.toList()));
		}
	}
	
	public List<Author> getAuthors() {
		return Collections.unmodifiableList(authors);
	}

	private void setLicense(License license, String uri) {
		switch (license) {
			case CC_BY:
			case CC_BY_NC_ND:
			case CC_BY_NC_SA:
				setLicense(license.getName());
				setRightsUri(license.getUrl());
				break;
			case PUBLISHED:
				setLicense(License.CC_BY_NC_ND.getName());
				setRightsUri(License.CC_BY_NC_ND.getUrl());
				setIsFormatOf("Already published paper. See document contents for DOI.");
				break;
			case RESTRICTED:
				setLicense("All rights reserved to their respective owners");
				LOGGER.warn(MessageFormat.format("Restricted license for paper at ''{1}''", license, uri));
				break;
			default:
				LOGGER.warn(MessageFormat.format("Unexpected license type (''{0}'') for paper at ''{1}''", license, uri));
		}
	}
	
	protected static File getPdfFile(String handle) {
		if (handle == null) {
			return null;
		} else if (Paths.get(PDF_CACHE_DIR + PAPERS_DIR, handle.replaceAll("/", "-") + ".pdf").toFile().exists()){
			return Paths.get(PDF_CACHE_DIR + PAPERS_DIR, handle.replaceAll("/", "-") + ".pdf").toFile();
		} else if (Paths.get(PDF_CACHE_DIR + ABSTRACTS_DIR, handle.replaceAll("/", "-") + ".pdf").toFile().exists()){
			return Paths.get(PDF_CACHE_DIR + ABSTRACTS_DIR, handle.replaceAll("/", "-") + ".pdf").toFile();
		} else {
			return Paths.get(PDF_CACHE_DIR, handle.replaceAll("/", "-") + ".pdf").toFile();
		}
	}
	
	
	public static Publication fromHttpEntity(HttpEntity entity) throws ParseException, IOException {
		return new Gson().fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), Publication.class);
	}
}
