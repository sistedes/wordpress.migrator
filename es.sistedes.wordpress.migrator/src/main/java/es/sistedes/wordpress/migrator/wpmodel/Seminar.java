package es.sistedes.wordpress.migrator.wpmodel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class Seminar {
	
	final static Logger logger = LoggerFactory.getLogger(Seminar.class);
	
	private URL url;
	private String title;
	private String summary;
	private List<Author> authors;
	private String bio;
	private Date date;
	private String handle;
	
	public Seminar(URL url) throws IOException  {
		// Again we have no API to easily retrieve this information
		// So let's go for the quick and dirty solution since this is a
		// one time migration, and we control the format of the source
		// HTML
		this.url = url;
		String page = IOUtils.toString(new InputStreamReader(DelayedStreamOpener.open(url), "UTF-8"));
		{
			String pattern = "<header.*?>\\s*<h1>(.*?)</h1>\\s*</header>"; 
			Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS).matcher(page);
			if (matcher.find()) {
				this.title = matcher.group(1).trim();
			} else {
				logger.error("Unable to parse title from " + url);
			}
		}
		{
			String pattern = "<div.*?>\\s*<h3>Resumen:</h3>(.*?)</div>"; 
			Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS).matcher(page);
			if (matcher.find()) {
				this.summary = matcher.group(1).trim();
			} else {
				logger.error("Unable to parse summary from " + url);
			}
		}
		{
			String pattern = "<div id=\"div-authors\".*?>\\s*<h3>Conferenciante:</h3>\\s*<ul>\\s*<p>\\s*<span.*?>(.*?)</span>\\s*</p>\\s*</ul>\\s*<!--.*?//-->\\s*(.*?)\\s*<h3>.*?</h3>\\s*<p>(.*?)</p>\\s*</div>"; 
			Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS).matcher(page);
			if (matcher.find()) {
				this.authors = extractAuthors(matcher.group(1).trim());
				this.bio = matcher.group(2).trim().replaceAll("\r\n", "").replaceAll("\\s*</p>\\s*", "</p>").replaceAll("\\s*(<br/?>)+\\s*", "</p>\n<p>");
				this.bio = bio.replaceAll("<p>\\(información no disponible\\)</p>", "");
				this.bio = bio.replaceAll("<(\\S+)>\\s*</\\1>", "");
				String textDate = matcher.group(3).trim();
				String format = "dd 'de' MMMM 'de' yyyy";
				if (textDate.endsWith(" h.")) {
					format = format + ", HH:mm 'h.'";
				}
				DateFormat fmt = new SimpleDateFormat(format, Locale.forLanguageTag("es"));
				try {
					this.date = fmt.parse(textDate);
				} catch (ParseException e1) {
					throw new RuntimeException(e1);
				}
			} else {
				logger.error("Unable to parse author and date information from " + url);
			}
		}
		{
			String pattern = "<div id=\"entry-pdf\".*?>\\s*<h3>Handle:</h3>\\s*<a href=\"http://hdl.handle.net.*?>(.*?)</a>\\s*</div>"; 
			Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS).matcher(page);
			if (matcher.find()) {
				this.handle = matcher.group(1).trim();
			} else {
				logger.error("Unable to parse seminar id from " + url);
			}
		}
	}
	
	private List<Author> extractAuthors(String authoringNote) {
		List<Author> result = new ArrayList<>();
		for (String s : authoringNote.split(",;")) {
			Matcher matcher = Pattern.compile("([^\\(]+)\\s*(?:\\((.*?)\\))?[,;]?", Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS).matcher(s.trim());
			while (matcher.find()) {
				if (StringUtils.contains("\"", matcher.group(2))) {
					logger.warn(MessageFormat.format("Affiliation of ''{0}'' has quotes in it (''{1}''), removing them", matcher.group(1), matcher.group(2)));
				}
				result.add(new Author(matcher.group(1), null,StringUtils.replace(matcher.group(2), "\"", "")));
			}
		}
		if (result.isEmpty()) {
			throw new RuntimeException("Seminars must have an author");
		}
		return result;
	}
	
	public String getTitle() {
		return title;
	}
	
	public List<Author> getAuthors() {
		return authors;
	}
	
	public Date getDate() {
		return date;
	}
	
	public String getSummary() {
		return summary;
	}
	
	public String getBio() {
		return bio;
	}
	
	public String getHandle() {
		return handle;
	}
	
	public String getHandleUri() {
		return "https://hdl.handle.net/" + StringUtils.trimToNull(getHandle());
	}
	
	public URL getUrl() {
		return url;
	}
}
