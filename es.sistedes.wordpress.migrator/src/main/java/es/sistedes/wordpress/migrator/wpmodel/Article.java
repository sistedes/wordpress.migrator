package es.sistedes.wordpress.migrator.wpmodel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class Article extends Document {

	private static class Term {
		private String name;
	}
	
	private transient static final Logger LOGGER = LoggerFactory.getLogger(Article.class);
	private transient String proceedings;
	
	// BEGIN: JSON fields
	private String id;
	private String link;
	private Map<String, String> title;
	private Map<String, Map<String, String>[]> _links;
	private Map<String, String> content;
	private Map<String, String> excerpt;
	private Map<String, String> metadata;
	// END: JSON fields

	private transient Track track;
	private transient List<Author> authors;
	private transient String cleanTitle;

	public Track getTrack() {
		return track;
	}
	
	public void setTrack(Track track) {
		this.track = track;
	}
	
	public String getId() {
		return id;
	}

	public String getTitle() {
		if (cleanTitle != null) return cleanTitle;
		String rawTitle = StringUtils.trimToNull(title.get("rendered"));
		Matcher matcher;
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\((:?[Tt]ool\\s*)[Dd]demo(:?straci[oó]n)?\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\([Tt]utorial\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\([Ww]ork in [Pp]rogress\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\([Tt]rabajo en [Pp]rogreso\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\([Tt]rabajo [Oo]riginal\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\([Rr]esumen\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\([Aa]bstract\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\([Ss]ummary\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\([Ee]xtended [Aa]bstract\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\(RELEVANTE YA PUBLICADO\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\(YA PUBLICADO\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\(YA PUBLICADO\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\(Trabajo ya publicado\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\([Aa]rtículo [Rr]elevante\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^«?(.*?)»?\\s*\\(Trabajo de alto nivel\\)\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^\\([Aa]rtículo [Rr]elevante\\)\\s*«?(.*?)»?\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^\\s*[Tt]rabajo [Rr]elevante\\s*\\W*\\s*«?(.*?)»?\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^\\([Tt]rabajo [Rr]elevante\\)\\s*«?(.*?)»?\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^ART[IÍ]CULO RELEVANTE:\\s*«?(.*?)»?\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		matcher = Pattern.compile("^[Ee]xtended [Aa]bstract of\\s*«?(.*?)»?\\s*$").matcher(rawTitle);
		if (matcher.matches()) {
			LOGGER.info(MessageFormat.format("[TITLE] ''{0}'' -> ''{1}''", rawTitle, matcher.group(1)));
			cleanTitle = matcher.group(1);
			return cleanTitle;
		}
		LOGGER.info(MessageFormat.format("[TITLE] Unchanged: ''{0}''", rawTitle));
		cleanTitle = rawTitle;
		return cleanTitle;
	}

	public String getLink() {
		return link;
	}
	
	public String getCollectionUrl() {
		return StringUtils.trimToNull(_links.get("collection")[0].get("href"));
	}

	public String getContent() {
		return StringUtils.trimToNull(content.get("rendered"));
	}

	public String getDescription() {
		return StringUtils.trimToNull(excerpt.get("rendered"));
	}

	public String getAbstract() {
		return StringUtils.trimToNull(metadata.get("abstract"));
	}

	public String getLicense() {
		return StringUtils.trimToNull(metadata.get("autorizacion"));
	}
	
	public List<String> getKeywords() {
		List<String> result;
		try {
			String termsUrl = StringUtils.trimToNull(_links.get("wp:term")[0].get("href"));
			Term[] terms = new Gson().fromJson(new InputStreamReader(DelayedStreamOpener.open(new URL(termsUrl))), Term[].class);
			result = Arrays.asList(terms).stream()
					.map(t -> StringUtils.trim(t.name))
					.map(k -> WordUtils.capitalize(k))
					.map(k -> RegExUtils.replacePattern(k, "^\"(.*)\"$", "$1")) // Cleanup quoted strings
					.map(k -> RegExUtils.replacePattern(k, "^(.*)\\.$", "$1")) // Cleanup string finishing with a dot
					.collect(Collectors.toUnmodifiableList());
		} catch (Exception e) {
			LOGGER.error(MessageFormat.format("Unable to retrieve keywords for article ''{0}''", link));
			result = Arrays.asList(new String[] {});
		}
		if (result.isEmpty()) {
			LOGGER.warn(MessageFormat.format("Article ''{0}'' has no keywords", link));
		}
		return result;
	}
	
	public String getHandle() {
		return StringUtils.trimToNull(metadata.get("handle"));
	}

	public String getHandleUri() {
		return "https://hdl.handle.net/" + StringUtils.trimToNull(metadata.get("handle"));
	}

	public String getProceedings() {
		return proceedings;
	}
	
	public void setProceedings(String proceedings) {
		this.proceedings = proceedings;
	}
	
	public String getDocumentUrl() {
		try {
			URL postUrl = new URL(link);
			if (StringUtils.isNotBlank(metadata.get("paper_pdf_file"))) {
				return new URL(postUrl.getProtocol(), postUrl.getHost(), postUrl.getPort(), "/submissions/" + StringUtils.trimToNull(metadata.get("paper_pdf_file"))).toString();
			} else if (StringUtils.isNotBlank(metadata.get("paper_pdf"))) {
				String mediaId = StringUtils.trimToNull(metadata.get("paper_pdf"));
				URL mediaUrl = new URL(postUrl.getProtocol(), postUrl.getHost(), postUrl.getPort(), "/wp-json/wp/v2/media/" + mediaId);
				Media media = new Gson().fromJson(new InputStreamReader(DelayedStreamOpener.open(mediaUrl)), Media.class);
				return media.getAttachmentUrl();
			} else return null;
		} catch (IOException | JsonSyntaxException | JsonIOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<Author> getAuthors() {
		if (authors == null) {
			authors = new ArrayList<>();
			for (int i = 1; i <= 8; i++) {
				if (StringUtils.isNotBlank(metadata.get("author_name_" + i))) {
					authors.add(new Author(
							StringUtils.trimToNull(metadata.get("author_name_" + i)), 
							StringUtils.trimToNull(metadata.get("author_email_" + i)), 
							StringUtils.trimToNull(metadata.get("author_univ_" + i))));
				}
			}
			authors = Collections.unmodifiableList(authors);
		}
		return authors;
	}
}
