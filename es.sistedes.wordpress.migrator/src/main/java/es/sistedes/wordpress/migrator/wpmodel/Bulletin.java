package es.sistedes.wordpress.migrator.wpmodel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.sistedes.wordpress.migrator.DelayedStreamOpener;

public class Bulletin {
	
	final static Logger logger = LoggerFactory.getLogger(Bulletin.class);
	
	private URL url;
	private String title;
	private String date;
	private String handle;
	private String documentUrl;
	
	public Bulletin(URL url) throws IOException  {
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
				this.title = StringUtils.capitalize(matcher.group(1).trim().replaceAll("\\. ", " - ").replaceAll("&#8211;", "-").toLowerCase());
			} else {
				logger.error("Unable to parse title from " + url);
			}
		}
		{
			String pattern = "<div .*?>\\s*<p>Boletín .*?</p>\\s*<p>Handle:\\s*<a href=\"https?://hdl.handle.net.*?>(.*?)</a>\\s*</p>\\s*<p>\\s*<a href=\\\"(.*?)\\\">Descargar</a>\\s*</p>\\s*</div>"; 
			Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS).matcher(page);
			if (matcher.find()) {
				this.handle = matcher.group(1).trim();
				this.documentUrl = matcher.group(2).trim();
			} else {
				logger.error("Unable to parse bulletin handle and URL from " + url);
			}
		}
	}
	
	public String getHandle() {
		return handle;
	}
	
	public String getHandleUri() {
		return "https://hdl.handle.net/" + StringUtils.trimToNull(handle);
	}
	
	public String getDocumentUrl() {
		return documentUrl;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getLink() {
		return url.toString();
	}
	
	public String getDescription() {
		DateFormat fmt = new SimpleDateFormat("MMMM 'de' yyyy", Locale.forLanguageTag("es"));
		return "Boletín de Sistedes. " + StringUtils.capitalize(fmt.format(getDate())) + ".";
	}
	
	public Date getDate() {
		DateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es"));
		try {
			// Note: the "de" particle not always appears, so we
			// directly remove it before trying to parse to avoid
			// having to try different SimpleDateFormats
			return fmt.parse(title.split(" - ")[1].replaceAll("de", ""));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	
}
