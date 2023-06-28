package es.sistedes.wordpress.migrator.dsmodel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import es.sistedes.wordpress.migrator.wpmodel.Author;
import es.sistedes.wordpress.migrator.wpmodel.Document.License;
import es.sistedes.wordpress.migrator.wpmodel.Track;

public class PreliminarsPublication extends Publication {

	private transient static final Logger LOGGER = LoggerFactory.getLogger(PreliminarsPublication.class);
	
	private transient String contents;
	
	public PreliminarsPublication(String title, String _abstract, List<String> keywords, List<Author> authors, String sistedesIdentifier, String licenseName, Date date, String contents) {
		super(title, _abstract, keywords, authors, sistedesIdentifier, licenseName, date);
		this.contents = contents;
		setType(Type.PRELIMINARS.getName());
		if (!getFile().exists()) {
			createFile();
		}
	}

	public static PreliminarsPublication from(Collection collection, Track track) {
		String cleanContent = track.getDescription().replaceAll("<p>&nbsp;</p>", "").trim();
		cleanContent = cleanContent.replaceAll("&nbsp;"," ").trim();
		cleanContent = cleanContent.replaceAll("[  ]+"," ").trim();
		cleanContent = Pattern.compile("\\s*style=\".*?\"", Pattern.DOTALL).matcher(cleanContent).replaceAll("").trim();
		cleanContent = Pattern.compile("<(.+?)>\\s*</\\1>\\s*", Pattern.DOTALL).matcher(cleanContent).replaceAll("").trim();
		cleanContent = Pattern.compile("<(:?[Hh]\\d)>(.*?)</\\1>", Pattern.DOTALL).matcher(cleanContent).replaceAll("<h2>$2</h2>").trim();
		cleanContent = Pattern.compile("<[Pp]>\\s*<[Uu]>(.*?)</[Uu]>\\s*</[Pp]>", Pattern.DOTALL).matcher(cleanContent).replaceAll("<h2>$1</h2>").trim();
		cleanContent = Pattern.compile("<[Pp]>\\s*<(:?STRONG|strong)>(.*?)</(:?STRONG|strong)>\\s*</[Pp]>", Pattern.DOTALL).matcher(cleanContent).replaceAll("<h2>$2</h2>").trim();
		cleanContent = Pattern.compile("<[Uu]>(.*?)</[Uu]>", Pattern.DOTALL).matcher(cleanContent).replaceAll("$1").trim();
		cleanContent = Pattern.compile("^\\s*$", Pattern.MULTILINE).matcher(cleanContent).replaceAll("").trim();

		Matcher matcher;
		String suffix;
		if (!track.getTitle().contains(" ")) {
			suffix = StringUtils.stripAccents(track.getTitle()).toUpperCase();
		} else if ((matcher = Pattern.compile("([A-Z]+): .*").matcher(track.getTitle())).matches()) {
			suffix = matcher.group(1);
		} else if ((matcher = Pattern.compile("([\\w ]+): .*").matcher(track.getTitle())).matches()) {
			String[] words = matcher.group(1).replaceAll("[^\\w\\s]", "").split("\\s+");
			suffix = Arrays.asList(words).stream().filter(w -> !PARTICLES.contains(w)).map(w -> w.toUpperCase().substring(0, 1)).collect(Collectors.joining());
		} else {
			String[] words = StringUtils.stripAccents(track.getTitle()).replaceAll("[^\\w\\s]", "").split("\\s+");
			suffix = Arrays.asList(words).stream().filter(w -> !PARTICLES.contains(w)).map(w -> w.toUpperCase().substring(0, 1)).collect(Collectors.joining());
		}
		
		String title = track.getTitle().replaceFirst("^\\d+\\.\\W+", "");
		String _abstract = "";
		if (track.getTitle().matches(".*[Cc]omit[eé]\\s+[Dd]e\\s+[Pp]rograma.*")) {
			_abstract = "Comité de programa de las " + track.getEdition().getName() + ".";
		} else if (track.getTitle().matches(".*[Cc]omit[eé]s?.*")) {
			_abstract = "Comités de las " + track.getEdition().getName() + ".";
		} else if (track.getTitle().matches(".*[Pp]reliminares.*")) {
			_abstract = "Prefacio de las " + track.getEdition().getName() + ".";
			title = "Prefacio";
		} else if (track.getTitle().matches(".*[Pp]refacio.*")) {
			_abstract = "Prefacio de las " + track.getEdition().getName() + ".";
		} else if (track.getTitle().matches(".*[Cc]onferencia\\s+[Ii]nvitada.*")) {
			_abstract = "Conferencia invitada en las " + track.getEdition().getName() + " por el " + title.split(":")[1].trim() + ".";
		} else if (track.getTitle().matches(".*[Cc]harla\\s+[Ii]nvitada.*")) {
			_abstract = "Conferencia invitada \"" + title.split(":")[0].trim() + "\" en las " + track.getEdition().getName() + ".";
		} else if (track.getTitle().matches(".*[Kk]eynote.*")) {
			_abstract = "Conferencia invitada \"" + title.split(":")[0].trim() + "\" en las " + track.getEdition().getName() + ".";
		} else if (track.getTitle().matches(".*[IÍií]ndice.*")) {
			_abstract = "Índice de las " + track.getEdition().getProceedingsName() + ".";
		} else if (track.getTitle().matches(".*[Tt]utorial.*:.*")) {
			_abstract = "Tutorial \"" + title.split(":")[1].trim() + "\" en las " + track.getEdition().getName() + ".";
		}
		
		final PreliminarsPublication publication = new PreliminarsPublication(
				title,
				_abstract,
				null,
				null,
				collection.getSistedesIdentifier() + "/" + suffix, License.CC_BY_NC_ND.getName(),
				track.getEdition().getDate(),
				cleanContent);
		publication.setIsPartOf(track.getEdition().getProceedingsName());
		publication.setProvenance(("Automatically imported from " + track.getLink() + " on " + ZonedDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneId.of("GMT"))).replace("[GMT]", " (GMT)"));
		publication.metadata.setSistedesConferenceName(track.getEdition().getConference().getTitle());
		publication.metadata.setSistedesConferenceAcronym(track.getEdition().getConference().getAcronym());
		publication.metadata.setSistedesEditionName(track.getEdition().getName());
		publication.metadata.setSistedesEditionDate(new SimpleDateFormat("yyyy-MM-dd").format(
				track.getEdition().getDate()));
		publication.metadata.setSistedesEditionLocation(track.getEdition().getLocation());
		publication.metadata.setSistedesProceedingsName(track.getEdition().getProceedingsName());
		publication.metadata.setSistedesProceedingsEditor(track.getEdition().getEditors());
		publication.metadata.setPublisher("Sistedes");
		return publication;
	}
	
	private String getContents() {
		return this.contents;
	}
	
	private void createFile() {
		StringBuilder builder = new StringBuilder();
		builder.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
		builder.append("<html>\n");
		builder.append("<head>\n");
		builder.append("  <title>" + getTitle() + "</title>\n");
		builder.append("</head>\n");
		builder.append("<body>\n");
		builder.append("  <h1>" + getTitle() + "</h1>\n");
		builder.append(Pattern.compile("^(.*?)$", Pattern.MULTILINE).matcher(getContents()).replaceAll("  $1") + "\n");
		builder.append("</body>\n");
		builder.append("</html>\n");
		
		Document document = Jsoup.parse(builder.toString());
		document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
		
		
		{
			File file = new File(FilenameUtils.removeExtension(getFile().getPath()) + ".html");
			try (FileWriter writer = new FileWriter(file)) {
				writer.write(builder.toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		{
			File file = getFile();
			try (OutputStream outputStream = new FileOutputStream(file)) {
			    ITextRenderer renderer = new ITextRenderer();
			    SharedContext sharedContext = renderer.getSharedContext();
			    sharedContext.setPrint(true);
			    sharedContext.setInteractive(false);
			    renderer.setDocumentFromString(document.html());
			    renderer.layout();
			    renderer.createPDF(outputStream);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public File[] getFiles() {
		return new File[] { 
			new File(FilenameUtils.removeExtension(getFile().getPath()) + ".html"),
			getFile()
		};
	}
}
