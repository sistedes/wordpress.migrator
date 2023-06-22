/*******************************************************************************
* Copyright (c) 2016 Sistedes
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Abel Gómez - initial API and implementation
*******************************************************************************/

package es.sistedes.wordpress.migrator;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import es.sistedes.wordpress.migrator.dsmodel.BulletinPublication;
import es.sistedes.wordpress.migrator.dsmodel.Bundle;
import es.sistedes.wordpress.migrator.dsmodel.Collection;
import es.sistedes.wordpress.migrator.dsmodel.Community;
import es.sistedes.wordpress.migrator.dsmodel.Item;
import es.sistedes.wordpress.migrator.dsmodel.Item.Type;
import es.sistedes.wordpress.migrator.dsmodel.Person;
import es.sistedes.wordpress.migrator.dsmodel.PreliminarsPublication;
import es.sistedes.wordpress.migrator.dsmodel.Publication;
import es.sistedes.wordpress.migrator.dsmodel.SeminarPublication;
import es.sistedes.wordpress.migrator.dsmodel.Site;
import es.sistedes.wordpress.migrator.wpmodel.Article;
import es.sistedes.wordpress.migrator.wpmodel.Author;
import es.sistedes.wordpress.migrator.wpmodel.BDSistedes;
import es.sistedes.wordpress.migrator.wpmodel.Bulletin;
import es.sistedes.wordpress.migrator.wpmodel.Conference;
import es.sistedes.wordpress.migrator.wpmodel.Document.License;
import es.sistedes.wordpress.migrator.wpmodel.DocumentsLibrary;
import es.sistedes.wordpress.migrator.wpmodel.Edition;
import es.sistedes.wordpress.migrator.wpmodel.Seminar;
import es.sistedes.wordpress.migrator.wpmodel.Track;
import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractRequest;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ModifyValueRequest;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.ResolutionRequest;
import net.handle.hdllib.Util;

/**
 * A {@link Migrator} class that extracts data from the Sistedes Digital Library
 * hosted at the given <code>url</code> and serializes it in a new format by
 * calling the {@link #serialize(OutputStream)} method.
 * 
 * @author agomez
 *
 */
public class Migrator {
	
	final static Logger logger = LoggerFactory.getLogger(Migrator.class);

	private final static String DSPACE_XSRF_TOKEN = "DSPACE-XSRF-TOKEN";
	private final static String X_XSRF_TOKEN = "X-XSRF-TOKEN";
	private final static String AUTHORIZATION_TOKEN = "Authorization";

	private final static String ORIGINAL_BUNDLE = "ORIGINAL";
	private final static String OTHER_BUNDLE = "OTHER";

	private static final String API_ENDPOINT = "/api";
	private static final String AUTHN_LOGIN_ENDPOINT = API_ENDPOINT + "/authn/login";
	private static final String SITES_ENDPOINT = API_ENDPOINT + "/core/sites";
	private static final String TOP_COMMUNITIES_ENDPOINT = API_ENDPOINT + "/core/communities/search/top";
	private static final String COMMUNITIES_ENDPOINT = API_ENDPOINT + "/core/communities";
	private static final String COMMUNITIES_COLLECTIONS_ENDPOINT = API_ENDPOINT + "/core/communities/%s/collections";
	private static final String COLLECTIONS_ENDPOINT = API_ENDPOINT + "/core/collections";
	private static final String COLLECTIONS_ITEMT_TEMPLATE_ENDPOINT = API_ENDPOINT + "/core/collections/%s/itemtemplate";
	private static final String ITEMS_ENDPOINT = API_ENDPOINT + "/core/items";
	private static final String ITEM_BUNDLES_ENDPOINT = API_ENDPOINT + "/core/items/%s/bundles";
	private static final String BUNDLES_BITSTREAMS_ENDPOINT = API_ENDPOINT + "/core/bundles/%s/bitstreams";
	private static final String RESOURCE_POLICIES_ENDPOINT = API_ENDPOINT + "/authz/resourcepolicies";
	private static final String RESOURCE_POLICIES_SEARCH_ENDPOINT = RESOURCE_POLICIES_ENDPOINT + "/search/resource?uuid=%s";
	private static final String AUTHOR_PAPER_RELATIONSHIP_ENDPOINT = API_ENDPOINT + "/core/relationships?relationshipType=1";
	private static final String AUTHOR_ABSTRACT_RELATIONSHIP_ENDPOINT = API_ENDPOINT + "/core/relationships?relationshipType=2";
	private static final String AUTHOR_SEMINAR_RELATIONSHIP_ENDPOINT = API_ENDPOINT + "/core/relationships?relationshipType=3";
	private static final String DISCOVER_SEARCH_OBJECTS_ENDPOINT = API_ENDPOINT + "/discover/search/objects";

	private class Identifiable {
		String id;
	}
	
	private class SitesResponse {
		private class Embedded {
			private List<Site> sites;
		}

		private Embedded _embedded;
	}

	private class CommunitiesResponse {
		private class Embedded {
			private List<Community> communities;
		}
		
		private Embedded _embedded;
	}

	private class CollectionsResponse {
		private class Embedded {
			private List<Collection> collections;
		}
		
		private Embedded _embedded;
	}

	private class ResourcePoliciesResponse {
		private class Embedded {
			private List<Identifiable> resourcepolicies;
		}
		
		private Embedded _embedded;
	}
	
	/**
	 * Options controlling the migration process
	 * 
	 * @author agomez
	 *
	 */
	public enum Options {
		// @formatter:off
		CONFERENCES,
		START_YEAR,
		END_YEAR,
		DRY_RUN,
		INTERACTIVE,
		MIGRATE_DOCUMENTS,
		// @formatter:on
	}

	private URL input;
	private URL output;
	private String user;
	private String password;
	private String prefix;
	private DSpaceAuth dspaceAuth;
	private PublicKeyAuthenticationInfo auth;
	private Map<Options, Object> options;
	private HttpClientBuilder httpClientBuilder;


	private Site site;
	
	private class DSpaceAuth {
		private URL url;
		private String xsrfToken;
		private String jwtToken;
		private Date lastIssed;

		public DSpaceAuth(URL url) {
			this.url = url;
		}
		
		public String getJwtToken() {
			return jwtToken;
		}
		
		public String getXsrfToken() {
			if (Calendar.getInstance().getTime().toInstant().getEpochSecond() - lastIssed.toInstant().getEpochSecond() > 25 * 60) {
				// Refresh JWT token every 25 minutes
				logger.info("[!AUTH] Refreshing JWT token...");
				refreshJwtToken();
			}
			return xsrfToken;
		}

		public void updateXsrfToken(String xsrfToken) {
			this.xsrfToken = xsrfToken; 
		}
		
		public void login() {
			this.lastIssed = Calendar.getInstance().getTime();
			try (CloseableHttpClient client = httpClientBuilder.build()) {

				HttpGet get = new HttpGet(url + API_ENDPOINT);
				try (CloseableHttpResponse response = client.execute(get)) {
					EntityUtils.consume(response.getEntity());
					xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
				}

				List<NameValuePair> data = new ArrayList<>();
				data.add(new BasicNameValuePair("user", user));
				data.add(new BasicNameValuePair("password", password));

				HttpPost post = new HttpPost(url + AUTHN_LOGIN_ENDPOINT);
				post.setEntity(new UrlEncodedFormEntity(data));
				post.setHeader(X_XSRF_TOKEN, xsrfToken);

				try (CloseableHttpResponse response = client.execute(post)) {
					if (response.getCode() != HttpStatus.SC_OK) {
						throw new MigrationException();
					}
					EntityUtils.consume(response.getEntity());
					xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
					jwtToken = response.getFirstHeader(AUTHORIZATION_TOKEN).getValue();
				}
			} catch (Exception e) {
				throw new RuntimeException(MessageFormat.format("Unable to log in ''{0}''", url));
			}
		}
		
		private void refreshJwtToken() {
			
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				
				HttpPost post = new HttpPost(output + AUTHN_LOGIN_ENDPOINT);
				post.setHeader(AUTHORIZATION_TOKEN, jwtToken);
				post.setHeader(X_XSRF_TOKEN, xsrfToken);
				
				try (CloseableHttpResponse response = client.execute(post)) {
					if (response.getCode() != HttpStatus.SC_OK) {
						throw new MigrationException();
					}
					EntityUtils.consume(response.getEntity());
					jwtToken = response.getFirstHeader(AUTHORIZATION_TOKEN).getValue();
					xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
					lastIssed = Calendar.getInstance().getTime();
				}
			} catch (Exception e) {
				throw new RuntimeException(MessageFormat.format("Unable to refresh JWT token on ''{0}''", url));
			}
		}
	}

	public Migrator(URL input, URL output, String user, String password, String prefix, PublicKeyAuthenticationInfo auth) {
		this.input = input;
		this.output = output;
		this.user = user;
		this.password = password;
		this.prefix = prefix;
		this.auth = auth;
		this.options = new HashMap<Options, Object>();
		this.httpClientBuilder = HttpClients.custom().setDefaultCookieStore(new BasicCookieStore());
		this.dspaceAuth = new DSpaceAuth(output);

	}

	/**
	 * Changes the {@link MigratorOptions}
	 * 
	 * @param options
	 */
	public void changeOptions(Map<Options, Object> options) {
		this.options = options;
	}

	/**
	 * Sets a new {@link MigratorOptions}
	 * 
	 * @param key
	 * @param value
	 */
	public void putOption(Options key, Object value) {
		this.options.put(key, value);
	}

	/**
	 * Removes the given {@link MigratorOptions}
	 * 
	 * @param key
	 */
	public void removeOption(Options key) {
		this.options.remove(key);
	}

	/**
	 * Migrates the data available in the library hosted at {@link #input} to
	 * {@link #outpu}. The filters specified in the {@link Migrator#options} are
	 * applied to limit the result.
	 * 
	 * @throws MigrationException If any error occurs, check
	 *                            {@link MigrationException#getCause()} to figure
	 *                            out the exact cause
	 */
	public synchronized void migrate() throws MigrationException {
		try {
			dspaceAuth.login();

			BDSistedes bdSistedes = new BDSistedes(input);
			
			// We need the Sistedes community to have a Global Authors Collection
			Community sistedesCommunity = findSistedesCommunity(bdSistedes);
			if (sistedesCommunity == null) {
				sistedesCommunity = createSistedesCommunity(bdSistedes);
			}
			
			Collection authorsCollection = findAuthorsCollection(sistedesCommunity);
			if (authorsCollection == null) {
				authorsCollection = createCollection(sistedesCommunity, 
					new Collection(
							"Autores",
							"Todos los autores que han contribuido a las jornadas Sistedes", 
							"Todos los autores que han contribuido a las jornadas Sistedes", 
							sistedesCommunity.getSistedesIdentifier() + "/AUTHORS",
							null,
							Type.AUTHOR));
			}

			if (isMigrateDocumentsEnabled()) {
				migrateSeminars(bdSistedes, authorsCollection);
				migrateBulletins(bdSistedes);
			}
			migrateConferences(bdSistedes, authorsCollection);
		} catch (Exception e) {
			throw new MigrationException(e);
		}
	}

	private void migrateConferences(BDSistedes bdSistedes, Collection authorsCollection) throws Exception {
		// NOTE: We use for loops instead of streams since the getters may throw
		// exceptions
		for (Conference conference : bdSistedes.getConferencesLibrary().getConferences((c1, c2) -> StringUtils.compare(c1.getTitle(), c2.getTitle()))) {
			if (getConferences().isEmpty() || getConferences().contains(conference.getAcronym())) {
				logger.info("[>CONFERENCE] Starting migration of '" + conference.getTitle() + "'");
				Community community = createConferenceCommunity(getSite(), conference);
				for (Edition edition : conference.getEditions((e1, e2) -> StringUtils.compare(e1.getTitle(), e2.getTitle()))) {
					if (edition.getYear() >= getStartYear() && edition.getYear() <= getEndYear()) {
						logger.info("[>EDITION] Starting migration of '"  + edition.getTitle() + "'");
						if (edition.getArticles().isEmpty()) {
							logger.warn("[!EDITION] '" + edition.getTitle() + "' has no papers! Skipping!");
							continue;
						}
						Community childCommunity = createEditionSubCommunity(community, edition);
						Collection preliminariesCollection = null;
						if (!edition.getTracks().isEmpty() && edition.getTracks().stream()
								.anyMatch(t -> {try { return t.getArticles().isEmpty(); } catch (IOException e) {}; return false;})) {
							preliminariesCollection = createPreliminariesCollection(childCommunity);
						}
						if (edition.getTracks().isEmpty()) {
							logger.warn("[!EDITION] '" + edition.getTitle() + "' has no tracks! Creating a dummy one...");
							logger.info("[>TRACK] Starting migration of " + edition.getTitle() + " (" + edition.getArticles().size() + " papers)");
							Collection publicationsCollection = createCollection(childCommunity, edition);
							for (Article article : edition.getArticles()) {
								logger.debug("[-PAPER] Migrating '" + article.getTitle() + "'. "
										+ article.getAuthors().stream().map(Author::toString).collect(Collectors.joining("; ")));
								createPublication(publicationsCollection, authorsCollection, article);
							}
							logger.info("[<TRACK] Migration of '" + edition.getTitle() + "' finished");
						} else {
							for (Track track : edition.getTracks()) {
								logger.info("[>TRACK] Starting migration of " + track.getTitle() + " (" + track.getArticles().size() + " papers)");
								if (track.getArticles().isEmpty()) {
									createPublication(preliminariesCollection, track);
								} else {
									Collection publicationsCollection = createCollection(childCommunity, track, edition.getDate());
									for (Article article : track.getArticles()) {
										logger.debug("[-PAPER] Migrating '" + article.getTitle() + "'. "
												+ article.getAuthors().stream().map(Author::toString).collect(Collectors.joining("; ")));
										article.setProceedings(edition.getName());
										createPublication(publicationsCollection, authorsCollection, article);
									}
								}
								logger.info("[<TRACK] Migration of '" + track.getTitle() + "' finished");
							}
						}
						logger.info("[<EDITION] Migration of '"  + edition.getTitle() + "' finished");
					} else {
						logger.info("[!EDITION] Skipping '"  + edition.getTitle() + "'...");
					}
				}
				logger.info("[<CONFERENCE] Migration of '" + conference.getTitle() + "' finished");
			}
		}
	}

	private void migrateBulletins(BDSistedes bdSistedes) throws Exception {
		Community sistedesCommunity = findSistedesCommunity(bdSistedes);
		if (sistedesCommunity == null) {
			sistedesCommunity = createSistedesCommunity(bdSistedes);
		}
		DocumentsLibrary documentsLibrary = bdSistedes.getDocumentsLibrary();
		if (documentsLibrary.getBulletins().isEmpty()) {
			return;
		}
		logger.info("[>Bulletin] Starting migration of Sistedes Bulletins");
		Collection bulletinsCollection = createBulletinsCollection(sistedesCommunity);
		for (Bulletin bulletin: documentsLibrary.getBulletins((b1, b2) -> b1.getDate().compareTo(b2.getDate()))) {
			logger.debug("[-BULLETIN] Migrating '" + bulletin.getTitle() + "'.");
			createPublication(bulletinsCollection, bulletin);
		}
		logger.info("[<Bulletin] Migration of Sistedes Bulletins finished");
	}

	private void migrateSeminars(BDSistedes bdSistedes, Collection authorsCollection) throws Exception {
		Community sistedesCommunity = findSistedesCommunity(bdSistedes);
		if (sistedesCommunity == null) {
			sistedesCommunity = createSistedesCommunity(bdSistedes);
		}
		DocumentsLibrary documentsLibrary = bdSistedes.getDocumentsLibrary();
		if (documentsLibrary.getSeminars().isEmpty()) {
			return;
		}
		logger.info("[>Seminar] Starting migration of Sistedes Seminars");
		Collection seminarsCollection = createSeminarsCollection(sistedesCommunity);
		for (Seminar seminar : documentsLibrary.getSeminars((s1, s2) -> s1.getDate().compareTo(s2.getDate()))) {
			logger.debug("[-BULLETIN] Migrating '" + seminar.getTitle() + "'.");
			createPublication(seminarsCollection, authorsCollection, seminar);
		}
		logger.info("[<Seminar] Migration of Sistedes Seminars finished");
	}
	
	private Community createConferenceCommunity(final Site site, final Conference conference) throws Exception {
		Community community = Community.from(site, conference);
		if (!isDryRun()) {
			Community found = findTopCommunity(community);
			if (found != null) {
				return found;
			}
			community = createCommunity(null, community);
		}
		return community;
	}

	private Community findSistedesCommunity(final BDSistedes bdSistedes) throws Exception {
		Community community = Community.from(getSite(), bdSistedes.getDocumentsLibrary());
		Community found = findTopCommunity(community);
		if (found != null) {
			return found;
		}
		return null;
	}
	
	private Collection findAuthorsCollection(final Community community) throws Exception {
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			{
				HttpGet get = new HttpGet(output + String.format(COMMUNITIES_COLLECTIONS_ENDPOINT, community.getUuid()));
				get.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
				get.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
				
				try (CloseableHttpResponse response = client.execute(get)) {
					if (response.getCode() != HttpStatus.SC_OK) {
						throw new MigrationException(
								MessageFormat.format("Unable to obtain Collections from ''{0}''. HTTP request returned code {1}: {2}",
								output, response.getCode(), community.toJson()));
					}
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
					}
					String string = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
					CollectionsResponse collectionsResponse = new Gson().fromJson(string, CollectionsResponse.class);
					if (collectionsResponse._embedded != null) {
						List<Collection> collections = collectionsResponse._embedded.collections;
						String title = "Autores";
						Optional<Collection> result = collections.stream().filter(c -> StringUtils.equals(c.getTitle(), title)).findFirst();
						if (result.isPresent()) {
							return result.get();
						}
					}
				}
			}
		}
		return null;
	}
	
	
	
	private Community createSistedesCommunity(final BDSistedes bdSistedes) throws MigrationException, IOException, ParseException {
		Community community = Community.from(getSite(), bdSistedes.getDocumentsLibrary());
		if (!isDryRun()) {
			community = createCommunity(null, community);
		}
		return community;
	}

	private Community findTopCommunity(final Community community) throws Exception {
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			// Check first if there's already a top level community...
			{
				HttpGet get = new HttpGet(output + TOP_COMMUNITIES_ENDPOINT);
				get.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
				get.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
				
				try (CloseableHttpResponse response = client.execute(get)) {
					if (response.getCode() != HttpStatus.SC_OK) {
						throw new MigrationException(
								MessageFormat.format("Unable to obtain Communities from ''{0}''. HTTP request returned code {1}: {2}",
								output, response.getCode(), community.toJson()));
					}
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
					}
					String string = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
					CommunitiesResponse communitiesResponse = new Gson().fromJson(string, CommunitiesResponse.class);
					if (communitiesResponse._embedded != null) {
						List<Community> communities = communitiesResponse._embedded.communities;
						String title = community.getTitle();
						Optional<Community> result = communities.stream().filter(c -> StringUtils.equals(c.getTitle(), title)).findFirst();
						if (result.isPresent()) {
							return result.get();
						}
					}
				}
			}
		}
		return null;
	}
	
	private Community createEditionSubCommunity(final Community parent, final Edition edition) throws MigrationException, IOException, ParseException, URISyntaxException {
		Community community = Community.from(parent, edition);
		if (!isDryRun()) {
			community = createCommunity(parent, community);
		}
		return community;
	}
	
	private Community createCommunity(final Community parent, final Community community) throws MigrationException {
		Community result = null;
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			URIBuilder builder = new URIBuilder(output + COMMUNITIES_ENDPOINT);
			if (parent != null) {
				builder.setParameter("parent", parent.getUuid());
			}
			HttpPost post = new HttpPost(builder.build());
			post.setEntity(community.toHttpEntity());
			post.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
			post.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
			
			try (CloseableHttpResponse response = client.execute(post)) {
				if (response.getCode() != HttpStatus.SC_CREATED) {
					throw new MigrationException(
							MessageFormat.format("Unable to create Community from ''{0}''. HTTP request returned code {1}.",
							community.getTitle(), response.getCode()));
				}
				result = Community.fromHttpEntity(response.getEntity());
				if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
					dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
				}
			}
			setHandle(community.getSistedesIdentifier(), result.getHandleUrl(), prefix, auth);
		} catch (Exception e) {
			throw new MigrationException(e);
		}
		return result;
	}
	
	private Collection createCollection(final Community parent, final Edition edition) throws MigrationException, IOException, ParseException, URISyntaxException {
		String name = "Artículos";
		String _abstract = "Artículos publicados en las " + edition.getProceedingsName() + ".";
		String description = _abstract;
		Collection collection = new Collection(name, _abstract, description, parent.getSistedesIdentifier() + "/" + edition.getYear(), edition.getDate());
		if (!isDryRun()) {
			collection = createCollection(parent, collection);
		}
		return collection;
	}

	private Collection createCollection(final Community parent, final Track track, final Date date) throws MigrationException, IOException, ParseException, URISyntaxException {
		Collection collection = Collection.from(parent, track, date);
		if (!isDryRun()) {
			collection = createCollection(parent, collection);
		}
		return collection;
	}
	
	private Collection createPreliminariesCollection(final Community parent) throws Exception {
		String name = "Preliminares";
		String _abstract = "Preliminares de las Actas de las " + parent.getName() + ". "
				+ "Los preliminares de las actas incluyen información adicional sobre las jornadas, "
				+ "tales como la presentación de las jornadas, los comités participantes, "
				+ "las conferencias invitadas, o los agradecimientos, entre otras secciones.";
		String description = _abstract;
		Collection collection = new Collection(name, _abstract, description, parent.getSistedesIdentifier() + "/PRELIMINARES", null);
		if (!isDryRun()) {
			collection = createCollection(parent, collection);
		}
		return collection;
	}
	
	private Collection createBulletinsCollection(final Community parent) throws Exception {
		String name = "Boletines de prensa";
		String _abstract = "El Boletín informativo de Sistedes es, en la actualidad, una publicación trimestral "
				+ "que recopila noticias recientes y relevantes, tanto para los socios como para todo aquel que "
				+ "pueda estar interesado en la Asociación. En los boletines se recogen tanto las noticias de "
				+ "los hechos acaecidos en el periodo correspondiente, como aquellas de interés que vayan a suceder "
				+ "próximamente";
		String description = "El Boletín informativo de Sistedes es, en la actualidad, una publicación trimestral "
				+ "que recopila noticias recientes y relevantes, tanto para los socios como para todo aquel que "
				+ "pueda estar interesado en la Asociación. En los boletines se recogen tanto las noticias de "
				+ "los hechos acaecidos en el periodo correspondiente, como aquellas de interés que vayan a suceder "
				+ "próximamente (congresos organizados por los socios, cursos, seminarios, tesis leídas, premios, etc.). "
				+ "Los Corresponsales de Sistedes colaboran con la directiva de la asociación proporcionando el "
				+ "contenido de los boletines. Si usted desea contribuir a los mismos, puede contactar con los "
				+ "editores enviando un mensaje de correo electrónico a la dirección "
				+ "<a href=\"mailto:noticias@sistedes.es\">noticias@sistedes.es</a>.";
		Collection collection = new Collection(name, _abstract, description, parent.getSistedesIdentifier() + "/BOLETINES", null, Type.BULLETIN);
		if (!isDryRun()) {
			collection = createCollection(parent, collection);
			BulletinPublication bulletinPublication = new BulletinPublication(null, null, null, null, null, License.CC_BY_NC_ND.getName(), null);
			bulletinPublication.setIsPartOf("Boletines Sistedes");
			bulletinPublication.setPublisher("Sistedes");
			bulletinPublication.setDiscoverable(false);
			bulletinPublication.setInArchive(false);
			createCollectionTemplate(collection, bulletinPublication);
		}
		return collection;
	}

	private Collection createSeminarsCollection(final Community parent) throws Exception {
		String name = "Seminarios Sistedes";
		String _abstract = "Los Seminarios Sistedes son charlas sobre temas de interés para nuestra comunidad científica,"
				+ " impartidos por destacados miembros de la misma y/o reconocidos expertos en las materias objeto de los mismos.";
		String description = "Los Seminarios Sistedes son charlas sobre temas de interés para nuestra comunidad científica,"
				+ " impartidos por destacados miembros de la misma y/o reconocidos expertos en las materias objeto de los mismos.";
		Collection collection = new Collection(name, _abstract, description, parent.getSistedesIdentifier() + "/SEMINARIOS", null, Type.SEMINAR);
		if (!isDryRun()) {
			collection = createCollection(parent, collection);
			SeminarPublication seminarPublication = new SeminarPublication(null, null, null, null, null, License.CC_BY_NC_ND.getName(), null);
			seminarPublication.setIsPartOf("Seminarios Sistedes");
			seminarPublication.setPublisher("Sistedes");
			seminarPublication.setDiscoverable(false);
			seminarPublication.setInArchive(false);
			createCollectionTemplate(collection, seminarPublication);
		}
		return collection;
	}
	
	private Collection createCollection(final Community parent, final Collection collection) throws MigrationException {
		Collection result = collection;
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				URIBuilder builder = new URIBuilder(output + COLLECTIONS_ENDPOINT);
		        builder.setParameter("parent", parent.getUuid());
				
				HttpPost post = new HttpPost(builder.build());
				post.setEntity(collection.toHttpEntity());
				post.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
				post.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
	
				try (CloseableHttpResponse response = client.execute(post)) {
					if (response.getCode() != HttpStatus.SC_CREATED) {
						throw new MigrationException(
								MessageFormat.format("Unable to create Collection for ''{0}''. HTTP request returned code {1}.",
								collection.getTitle(), response.getCode()));
					}
					result = Collection.fromHttpEntity(response.getEntity());
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
					}
				}
				setHandle(collection.getSistedesIdentifier(), result.getHandleUrl(), prefix, auth);
			} catch (Exception e) {
				throw new MigrationException(e);
			}
		}
		return result;
	}
	
	private Collection createCollectionTemplate(final Collection collection, Item item) throws Exception {
		Collection result = collection;
		// Theoretically, if these properties are false everything should work,
		// but for whatever reason, DSpace does not like them...
		JsonObject obj = (JsonObject) new Gson().toJsonTree(item);
		obj.remove("inArchive");
		obj.remove("discoverable");
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				URIBuilder builder = new URIBuilder(output + String.format(COLLECTIONS_ITEMT_TEMPLATE_ENDPOINT, collection.getUuid()));
				
				HttpPost post = new HttpPost(builder.build());
				post.setEntity(new StringEntity(obj.toString(), ContentType.APPLICATION_JSON));
				post.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
				post.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
	
				try (CloseableHttpResponse response = client.execute(post)) {
					if (response.getCode() != HttpStatus.SC_CREATED) {
						throw new MigrationException(
								MessageFormat.format("Unable to create Item template Collection for ''{0}''. HTTP request returned code {1}.",
								collection.getTitle(), response.getCode()));
					}
					result = Collection.fromHttpEntity(response.getEntity());
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
					}
				}
			}
		}
		return result;
	}

	private Publication createPublication(final Collection parent, final Track track) throws Exception {
		final PreliminarsPublication publication = PreliminarsPublication.from(parent, track);
		Publication result = null;
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				// First, create the publication....
				{
					result = createPublication(parent, publication);
				}

				// Second, create a bundle for the file...
				File[] files = publication.getFiles();
				if (files.length > 0) {
					attachBinaryFiles(ORIGINAL_BUNDLE, result,
							Arrays.asList(files).stream().filter(f -> f.getName().endsWith("pdf"))
							.collect(Collectors.toList()).toArray(s -> new File[s]));
					attachBinaryFiles(OTHER_BUNDLE, result,
							Arrays.asList(files).stream().filter(f -> f.getName().endsWith("html"))
							.collect(Collectors.toList()).toArray(s -> new File[s]));
				}
			} catch (Exception e) {
				throw new MigrationException(e);
			}
		}
		return result;
	}
	
	private Publication createPublication(final Collection parent, final Collection authorsCollection, Article article) throws Exception {
		final Publication publication = Publication.from(parent, article);
		Publication result = null;
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				// First, create the publication....
				{
					result = createPublication(parent, publication);
				}
				// Second, create the authors...
				{
					createAuthorsForPublication(authorsCollection, publication.getAuthors(), result);
				}
				// Third, create a bundle for the files...
				File file = publication.getFile();
				if (!file.exists()) {
					logger.info("Article '" + article.getLink() + "' does not have a file! Skipping file upload...");
				} else {
					Identifiable uploadedFile = attachBinaryFiles(ORIGINAL_BUNDLE, result, new File[] { file }).get(0);
	
					// And if the article is restricted, remove the resource policy
					// (which at this point, should be a single one: allow anonymous read access)
					// Upload a file...
					if (uploadedFile != null && License.from(article.getLicense()) == License.RESTRICTED) {
						Identifiable policy = null;
						{
							HttpGet get = new HttpGet(output + String.format(RESOURCE_POLICIES_SEARCH_ENDPOINT, uploadedFile.id));
							get.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
							get.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());

							try (CloseableHttpResponse response = client.execute(get)) {
								if (response.getCode() != HttpStatus.SC_OK) {
									throw new MigrationException(MessageFormat.format(
											"Unable to get policies file for ''{0}''. HTTP request returned code {1}.", uploadedFile.id, response.getCode()));
								}
								String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
								policy = new Gson().fromJson(json, ResourcePoliciesResponse.class)._embedded.resourcepolicies.get(0);

								if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
									dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
								}
							}
						}
						if (policy != null) {
							HttpDelete delete = new HttpDelete(output + RESOURCE_POLICIES_ENDPOINT + "/" + policy.id);
							delete.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
							delete.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());

							try (CloseableHttpResponse response = client.execute(delete)) {
								if (response.getCode() != HttpStatus.SC_NO_CONTENT) {
									throw new MigrationException(MessageFormat.format("Unable to delete policy ''{0}''. HTTP request returned code {1}.",
											policy.id, response.getCode()));
								}
								EntityUtils.consume(response.getEntity());
								if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
									dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
								}
							}
						}
					}
				}
			} catch (Exception e) {
				throw new MigrationException(e);
			}
		}
		return result;
	}

	private void createAuthorsForPublication(final Collection authorsCollection, final List<Author> authors, Publication result) throws Exception {
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				// First, create the authors...
				List<Person> personsInDSpace = new ArrayList<>();
				{
					for (Author author : authors) {
						Person person = findPersonFromAuthor(author);
						if (person == null) {
							person = createPersonFromAuthor(authorsCollection, author);
						} else {
							updatePersonFromAuthor(person, author);
						}
						personsInDSpace.add(person);
					}
				}
				// Second, link the authors with the publication
				{
					for (Person person : personsInDSpace) {
						String endpoint = result.isAbstract() ? AUTHOR_ABSTRACT_RELATIONSHIP_ENDPOINT : AUTHOR_PAPER_RELATIONSHIP_ENDPOINT;
						HttpPost post = new HttpPost(output + endpoint);
						post.setEntity(
								new StringEntity(MessageFormat.format("{0}{1}/{2} \n {0}{1}/{3}", 
									output, ITEMS_ENDPOINT, result.getUuid(), person.getUuid()),
								ContentType.create("text/uri-list")));
						post.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
						post.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
						
						try (CloseableHttpResponse response = client.execute(post)) {
							if (response.getCode() != HttpStatus.SC_CREATED) {
								throw new MigrationException(
										MessageFormat.format("Unable to create Relationship between ''{0}'' and ''{1}''. HTTP request returned code {2}.",
												person.getUuid(), result.getUuid(), response.getCode()));
							}
							EntityUtils.consume(response.getEntity());
							if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
								dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
							}
						}
					}
				}
			}
		}
	}
	
	private void createAuthorsForSeminarPublication(final Collection authorsCollection, final List<Author> authors, Publication result) throws Exception {
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				// First, create the authors...
				List<Person> personsInDSpace = new ArrayList<>();
				{
					for (Author author : authors) {
						Person person = findPersonFromAuthor(author);
						if (person == null) {
							person = createPersonFromAuthor(authorsCollection, author);
						} else {
							updatePersonFromAuthor(person, author);
						}
						personsInDSpace.add(person);
					}
				}
				// Second, link the authors with the publication
				{
					for (Person person : personsInDSpace) {
						HttpPost post = new HttpPost(output + AUTHOR_SEMINAR_RELATIONSHIP_ENDPOINT);
						post.setEntity(
								new StringEntity(MessageFormat.format("{0}{1}/{2} \n {0}{1}/{3}", 
										output, ITEMS_ENDPOINT, result.getUuid(), person.getUuid()),
										ContentType.create("text/uri-list")));
						post.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
						post.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
						
						try (CloseableHttpResponse response = client.execute(post)) {
							if (response.getCode() != HttpStatus.SC_CREATED) {
								throw new MigrationException(
										MessageFormat.format("Unable to create Relationship between ''{0}'' and ''{1}''. HTTP request returned code {2}.",
												person.getUuid(), result.getUuid(), response.getCode()));
							}
							EntityUtils.consume(response.getEntity());
							if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
								dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
							}
						}
					}
				}
			}
		}
	}
	
	private Person findPersonFromAuthor(Author author) throws MigrationException {
		Person result = null;
		List<Person> found = null;
		String messageTemplate = "";
		if (!isInteractive()) {
			if (author.getEmail() != null) {
				found = findPersons(author.getEmail());
				if (!found.isEmpty()) {
					for (String foundEmail : found.get(0).getEmails()) {
						// Try to match the Person using any of the saved e-mails...
						if (StringUtils.equalsIgnoreCase(author.getEmail(), foundEmail)) {
							// ... but make sure that the names have at least some similarity
							for (int i = 0; i < found.size(); i++) {
								String name1 = author.getFullName();
								String name2 = found.get(i).getFullName();
								Integer distance = new LevenshteinDistance().apply(name1, name2);
								float normalizedDistance = (float) distance / (float) Math.max(name1.length(), name2.length());
								if (normalizedDistance == 0) {
									messageTemplate = "[!PERSON] Exact match found for ''{0}, {1}'' with e-mail ''{2}'': ''{3}, {4} ({5})''";
									result = found.get(i);
									break;
								} else if (normalizedDistance < 0.1) {
									messageTemplate = "[!PERSON] Almost exact (< 0.1) match found for ''{0}, {1}'' with e-mail ''{2}'': ''{3}, {4} ({5})''";
									result = found.get(i);
									break;
								} else if (normalizedDistance < 0.3) {
									messageTemplate = "[!PERSON] Approximate match (< 0.3) found for ''{0}, {1}'' with e-mail ''{2}'': ''{3}, {4} ({5})''";
									result = found.get(i);
									break;
								} else if (normalizedDistance < 0.7) {
									messageTemplate = "[!PERSON] Approximate match (< 0.7) found for ''{0}, {1}'' with e-mail ''{2}'': ''{3}, {4} ({5})''";
									result = found.get(i);
									break;
								} else {
									messageTemplate = "[!PERSON] Approximate match (>= 0.7) found (but not assigning) for ''{0}, {1}'' with e-mail ''{2}'': ''{3}, {4} ({5})''";
									logger.info(MessageFormat.format(messageTemplate, author.getLastName(), author.getFirstName(), author.getEmail(),
											result != null ? result.getFamilyName() : "", result != null ? result.getGivenName() : "", result != null ? StringUtils.join(result.getEmails(), ", ") : ""));
									result = null;
									continue;
								}
							}
						} else {
							result = null;
						}
					}
				}
			}
			if (result == null) {
				// Try to match the Person using the name 
				found = findPersons(author.getFullName());
				Optional<Person> match = found.stream().filter(f -> StringUtils.equalsIgnoreCase(author.getFullName(), f.getFullName())).findAny();
				if (match.isPresent()) {
					messageTemplate = "[!PERSON] Exact match found for name ''{0}, {1}'' (''{2}''): ''{3}, {4} ({5})''";
					result = match.get();
				} else if
					(
						// Found 
						found.size() > 0 
						// and
						&&
						(
							// Full name match. Which needs:
							(
								// Exact surname match
								StringUtils.equalsIgnoreCase(
									author.getLastName().toLowerCase().replaceAll("[^\\w]+", "-"), 
									found.get(0).getFamilyName().toLowerCase().replaceAll("[^\\w]+", "-"))
								// and
								&& 
								// Name match, either by... 
								(
									// ... exact name match...
									StringUtils.equalsIgnoreCase(
										author.getFirstName().toLowerCase().replaceAll("[^\\w]+", "-"), 
										found.get(0).getGivenName().toLowerCase().replaceAll("[^\\w]+", "-"))
									// ... or...
									|| 
									// ... exact initial match (if any of the names is abbreviated)
									firstNameMatch(author.getFirstName(), found.get(0).getGivenName())
								)
							)
							// or
							||
							(
								// Name match
								StringUtils.equalsIgnoreCase(author.getFirstName(), found.get(0).getGivenName())
								// and
								&&
								// first surname match (if any of the names has to surnames)
								(
									(author.getLastName().split(" ").length == 1
									&& found.get(0).getFamilyName().split(" ").length == 2
									&& StringUtils.equalsIgnoreCase(author.getLastName().split(" ")[0], found.get(0).getFamilyName().split(" ")[0]))
								||
									(author.getLastName().split(" ").length == 2
									&& found.get(0).getFamilyName().split(" ").length == 1
									&& StringUtils.equalsIgnoreCase(author.getLastName().split(" ")[0], found.get(0).getFamilyName().split(" ")[0]))
								)
							)
						)
					) {
					messageTemplate = "[!PERSON] Approximate match found for name ''{0}, {1}'' (''{2}''): ''{3}, {4} ({5})''";
					result = found.get(0);
				}
			}
		} else {
			// Interactive way, try to find an exact match in author name
			found = findPersons(author.getFullName().replaceAll("\\W+", " "));
			if (found.isEmpty() && StringUtils.isNotBlank(author.getEmail())) {
				// If nothing is found, try using th e-mail...
				found = findPersons(author.getEmail());
			}
			if (found.isEmpty() && author.getLastName().split("[ -]+").length > 1) {
				// If nothing is found, try relaxing the surnames
				found = findPersons(author.getFirstName() + " " + author.getLastName().split("[ -]+")[0]);
			}
			// Try to do an exact match...
			Optional<Person> match = found.stream().filter(
					f -> StringUtils.equalsIgnoreCase(StringUtils.stripAccents(author.getFullName()), StringUtils.stripAccents(f.getFullName()))).findAny();
			if (match.isPresent()) {
				messageTemplate = "[!PERSON] Exact match found for name ''{0}, {1}'' (''{2}''): ''{3}, {4} ({5})''";
				result = match.get();
			} else if (!found.isEmpty()){
				// Try to match with any of the alternative names but making sure at least one e-mail matches
				for (Person person : found) {
					if (author.getEmail() != null
						&&
						person.getNameVariants().stream()
							.filter(v -> StringUtils.equalsIgnoreCase(
									StringUtils.stripAccents(author.getLastName() + ", " + author.getFirstName()), StringUtils.stripAccents(v))).findAny()
							.isPresent()
						&& 
						person.getEmails().stream()
							.filter(e -> StringUtils.equalsIgnoreCase(author.getEmail(), e)).findAny()
							.isPresent()) {
						messageTemplate = "[!PERSON] Exact match found for name ''{0}, {1}'' (''{2}''): ''{3}, {4} ({5})''";
						result = person;
						break;
					}
				}
				if (result == null) {
					// We were not lucky, ask the user....
					Integer selection;
					do {
						selection = null;
						System.out.println(MessageFormat.format("Unable to find an exact match for:\n    {0}, {1} <{2}> ({3})", 
								author.getLastName(), author.getFirstName(), author.getEmail(), author.getAffiliation()));
						System.out.println("Possible candidates are:");
						System.out.println("[0] None");
						for (int i = 0; i < found.size(); i++) {
							System.out.println(MessageFormat.format("[{0}] {1}, {2} <{3}> ({4})", 
									i+1, found.get(i).getFamilyName(), found.get(i).getGivenName(), 
									StringUtils.join(found.get(i).getEmails(), ", ") , StringUtils.join(found.get(i).getAffiliations(), ", ")));
						}
						Toolkit.getDefaultToolkit().beep();
						System.out.print(MessageFormat.format("Which one corresponds to {0}, {1}? ", 
								author.getLastName(), author.getFirstName()));
						BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
						try {
							selection = Integer.valueOf(reader.readLine());
						} catch (Exception e) {
							selection = null;
						}
					} while (selection == null || selection < 0 || selection > found.size());
					if (selection > 0) {
						result = found.get(selection - 1);
						messageTemplate = "[!PERSON] Manually set match for name ''{0}, {1}'' (''{2}''): ''{3}, {4} ({5})''";
					}
				}
			}

		}
		if (result == null) {
			messageTemplate = "[!PERSON] No match found for ''{0}, {1}'' (''{2}'')";
		}
		logger.info(MessageFormat.format(messageTemplate, author.getLastName(), author.getFirstName(), author.getEmail(),
				result != null ? result.getFamilyName() : "", result != null ? result.getGivenName() : "", result != null ? StringUtils.join(result.getEmails(), ", ") : ""));
		return result;
	}
	
	private boolean firstNameMatch(String name1, String name2) {
		String[] split1 = name1.toLowerCase().split(" ");		
		String[] split2 = name1.toLowerCase().split(" ");
		if (split1.length != split2.length) return false;
		for (int i = 0; i < split1.length; i++) {
			if (StringUtils.equals(split1[i], split2[i])) {
				// Names match, continue 
				continue;
			} else if ((split1[i].contains(".") || split2[i].contains("."))
					&& split1[i].charAt(0) == split2[i].charAt(0)) {
				// One of the names seems to be an abbreviation, check the initials...
				continue;
			} else {
				// No match found
				return false;
			}
		}
		return true;
	}
	
	private List<Person> findPersons(String query) throws  MigrationException {
		List<Person> result = new ArrayList<>();
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			URIBuilder builder = new URIBuilder(output + DISCOVER_SEARCH_OBJECTS_ENDPOINT);
			builder.setParameter("configuration", "administrativeView");
			builder.setParameter("dsoType", "item");
			builder.setParameter("sort", "score,DESC");
			builder.setParameter("f.entityType", Item.Type.AUTHOR.getName() + ",equals");
			builder.setParameter("query", query.replaceAll(":", "")); 	// In some cases, a URL is provided instead on an email.
																		// In such a case, remove the colon since the API fails to process it
			
			HttpGet get = new HttpGet(builder.build());
			get.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
			get.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
			
			try (CloseableHttpResponse response = client.execute(get)) {
				if (response.getCode() != HttpStatus.SC_OK) {
					throw new MigrationException(
							MessageFormat.format("Unable to create Author for query ''{0}''. HTTP request returned code {1}.",
									query, response.getCode()));
				}
				JsonObject object = new Gson().fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
				if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
					dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
				}
				try {
					JsonArray array = object
							.get("_embedded").getAsJsonObject()
							.get("searchResult").getAsJsonObject()
							.get("_embedded").getAsJsonObject()
							.get("objects").getAsJsonArray();
					for (int i = 0; i < array.size(); i++) {
						result.add(new Gson().fromJson(
								array.get(i).getAsJsonObject()
								.get("_embedded").getAsJsonObject()
								.get("indexableObject").toString(),
								Person.class));
					}
				} catch (Exception e) {
					// If result cannot be parsed, ignore...
				}
			}
		} catch (Exception e) {
			throw new MigrationException(e);
		}
		return result;
	}

	private void updatePersonFromAuthor(final Person personInDSpace, final Author author) throws  MigrationException {
		List<JsonObject> objs = new ArrayList<>();
		
		if ((
				// The e-mail was missing, adding it
				author.getEmail() != null && personInDSpace.getEmails().isEmpty()
			) || (
				// A new e-mail is found (maybe because a change in the affiliation
				// Add it
					author.getEmail() != null && !personInDSpace.getEmails().contains(StringUtils.toRootLowerCase(author.getEmail()))
			)) {
			JsonObject obj = new JsonObject();
			obj.addProperty("op", "add");
			obj.addProperty("path", "/metadata/person.email");
			obj.addProperty("value", author.getEmail());
			objs.add(obj);
			logger.info(MessageFormat.format(
					"[!PERSON UPDATE] Adding e-mail ''{0}'' to ''{1}'' (previous were ''{2}'')",
					author.getEmail(), personInDSpace.getFullName(), StringUtils.join(personInDSpace.getEmails(), ", ")));
		}
		
		if (author.getAffiliation() != null && personInDSpace.getAffiliations().isEmpty()) {
			// Affiliation was missing, adding it...
			JsonObject obj = new JsonObject();
			obj.addProperty("op", "add");
			obj.addProperty("path", "/metadata/person.affiliation.name");
			obj.addProperty("value", author.getAffiliation());
			objs.add(obj);
			logger.info(MessageFormat.format(
					"[!PERSON UPDATE] Adding affiliation ''{0}'' to ''{1}'' (previous was ''{2}'')",
					author.getAffiliation(), personInDSpace.getFullName(), null));
		} else {
			if (author.getAffiliation() != null && 
				personInDSpace.getAffiliations().stream()
				.allMatch(a -> new JaroWinklerSimilarity().apply(a, author.getAffiliation()) < 0.9)) {
					// Affiliations may be written in very different ways even when they are the same, so we use
					// and approximation to determine if we should add it or not
				JsonObject obj = new JsonObject();
				obj.addProperty("op", "add");
				obj.addProperty("path", "/metadata/person.affiliation.name");
				obj.addProperty("value", author.getAffiliation());
				objs.add(obj);
				logger.info(MessageFormat.format(
						"[!PERSON UPDATE] Adding affiliation ''{0}'' to ''{1}'' (previous were ''{2}'')",
						author.getAffiliation(), personInDSpace.getFullName(), StringUtils.join(personInDSpace.getAffiliations(), ", ")));
			}
		}
		
		// The names are not exactly the same, let's see if we should replace the name and add a variant
		if (!StringUtils.equals(personInDSpace.getFullName(), author.getFullName())) {
			if ((
					// It seems that the already saved name is shorter (maybe an abbreviation? only first surname?)
					// Let's update it and save the previous name as a variant
					author.getFullName().length() > personInDSpace.getFullName().length()
				) || (
					// It seems that the already saved name misses some accents
					// Let's update it...
					StringUtils.containsAny(author.getFullName(), "áéíóúàèìòùäëïöüâêîôû") && 
					!StringUtils.containsAny(personInDSpace.getFullName(), "áéíóúàèìòùäëïöüâêîôû")
				) || (
						// It seems that the already saved name has hyphens (maybe an "internationalized" variant?)
						// or dots (abbreviations?)
						// Let's update it to the "Spanish" custom (only if the new name is longer)...
						!StringUtils.containsAny(author.getFullName(), "-.") && 
						StringUtils.containsAny(personInDSpace.getFullName(), "-.") &&
						author.getFullName().length() >= personInDSpace.getFullName().length()
					)) {
				
				JsonObject obj1 = new JsonObject();
				obj1.addProperty("op", "replace");
				obj1.addProperty("path", "/metadata/person.givenName");
				obj1.addProperty("value", author.getFirstName());
				objs.add(obj1);
				JsonObject obj2 = new JsonObject();
				obj2.addProperty("op", "replace");
				obj2.addProperty("path", "/metadata/person.familyName");
				obj2.addProperty("value", author.getLastName());
				objs.add(obj2);
				JsonObject obj3 = new JsonObject();
				obj3.addProperty("op", "replace");
				obj3.addProperty("path", "/metadata/dc.title");
				obj3.addProperty("value", author.getLastName() + ", " + author.getFirstName());
				objs.add(obj3);
				JsonObject obj4 = new JsonObject();
				obj4.addProperty("op", "add");
				obj4.addProperty("path", "/metadata/person.name.variant");
				obj4.addProperty("value", personInDSpace.getFamilyName() + ", " + personInDSpace.getGivenName());
				objs.add(obj4);
				logger.info(MessageFormat.format(
						"[!PERSON UPDATE] Setting new visible name ''{0}'' (and marking ''{1}'' as a variant)",
						author.getFullName(), personInDSpace.getFullName()));
			} else {
				// The names are not exactly the same, but shouldn't replace the existing name
				// Let's add it as a variant if it does not exist yet
				if (!personInDSpace.getNameVariants().contains(author.getLastName() + ", " + author.getFirstName())) {
					JsonObject obj1 = new JsonObject();
					obj1.addProperty("op", "add");
					obj1.addProperty("path", "/metadata/person.name.variant");
					obj1.addProperty("value", author.getLastName() + ", " + author.getFirstName());
					objs.add(obj1);
					logger.info(MessageFormat.format(
							"[!PERSON UPDATE] Adding ''{0}'' as a variant for ''{1}''",
							author.getFullName(), personInDSpace.getFullName()));
				}

			}
		}

		if (objs.isEmpty()) {
			return;
		}
		
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			HttpPatch patch = new HttpPatch(output + ITEMS_ENDPOINT + "/" + personInDSpace.getUuid());
			patch.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
			patch.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
			patch.setEntity(new StringEntity(objs.toString(), ContentType.APPLICATION_JSON));
			
			try (CloseableHttpResponse response = client.execute(patch)) {
				if (response.getCode() != HttpStatus.SC_OK) {
					throw new MigrationException(
							MessageFormat.format("Unable to update Person ''{0}''. HTTP request returned code {1}.",
									personInDSpace.getUuid(), response.getCode()));
				}
				EntityUtils.consume(response.getEntity());
				if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
					dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
				}
			}
		} catch (Exception e) {
			throw new MigrationException(e);
		}
	}
	
	private Person createPersonFromAuthor(final Collection authorsCollection, final Author author) throws  MigrationException {
		Person result = null;
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			URIBuilder builder = new URIBuilder(output + ITEMS_ENDPOINT);
			builder.setParameter("owningCollection", authorsCollection.getUuid());
			
			HttpPost post = new HttpPost(builder.build());
			post.setEntity(Person.fromAuthor(author).toHttpEntity());
			post.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
			post.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
	
			try (CloseableHttpResponse response = client.execute(post)) {
				if (response.getCode() != HttpStatus.SC_CREATED) {
					throw new MigrationException(
							MessageFormat.format("Unable to create Person for ''{0}''. HTTP request returned code {1}.",
							author.getFullName(), response.getCode()));
				}
				result = Person.fromHttpEntity(response.getEntity());
				if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
					dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
				}
			}
		} catch (Exception e) {
			throw new MigrationException(e);
		}
		return result;
	}

	private Publication createPublication(final Collection parent, final Bulletin bulletin) throws Exception {
		Publication publication = BulletinPublication.from(parent, bulletin);
		if (!isDryRun()) {
			publication = createPublication(parent, publication);
			// Now, create a bundle... 
			File file = publication.getFile();
			if (!file.exists()) {
				logger.error("Bulletin '" + bulletin.getLink() + "' does not have a file! Skipping file upload...");
			} else {
				attachBinaryFiles(ORIGINAL_BUNDLE, publication, new File[] { file });
			}
		}
		return publication;
	}

	private Publication createPublication(final Collection parent, final Collection authorsCollection, final Seminar seminar) throws Exception {
		SeminarPublication publication = SeminarPublication.from(parent, seminar);
		Publication result = publication;
		if (!isDryRun()) {
			// Create the publication
			result = createPublication(parent, publication);
			// Create the authors
			createAuthorsForSeminarPublication(authorsCollection, publication.getAuthors(), result);
			// Now, create bundles... 
			final File[] files = publication.getFiles();
			if (files.length == 0) {
				logger.error("Seminar '" + seminar.getTitle() + "' does not have files! Skipping file upload...");
			} else {
				attachBinaryFiles(ORIGINAL_BUNDLE, result,
						Arrays.asList(files).stream().filter(f -> f.getName().endsWith("video.mp4"))
						.collect(Collectors.toList()).toArray(s -> new File[s]));
				attachBinaryFiles(OTHER_BUNDLE, result,
						Arrays.asList(files).stream().filter(f -> !f.getName().endsWith("video.mp4"))
						.collect(Collectors.toList()).toArray(s -> new File[s]));
			}
		}
		return result;
	}
	
	private Publication createPublication(final Collection parent, final Publication publication) throws  MigrationException {
		Publication result = null;
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			{
				URIBuilder builder = new URIBuilder(output + ITEMS_ENDPOINT);
			    builder.setParameter("owningCollection", parent.getUuid());
				
				HttpPost post = new HttpPost(builder.build());
				post.setEntity(publication.toHttpEntity());
				post.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
				post.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
	
				try (CloseableHttpResponse response = client.execute(post)) {
					if (response.getCode() != HttpStatus.SC_CREATED) {
						throw new MigrationException(MessageFormat.format("Unable to create Publication from ''{0}''. HTTP request returned code {1}.",
								publication, response.getCode()));
					}
					result = Publication.fromHttpEntity(response.getEntity());
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
					}
				}
			}
			// Now that the Publication has been already created, update some fields...
			{
				if (parent.getDate() != null) {
					// If parent.getDate() != we're processing a Track/Edition
					result.setDate(parent.getDate());
				} else {
					// If it's null, it's a bulletin or a Seminar, so let's overwrite again 
					// the date to avoid duplicating 'dc.date.available'
					result.setDate(publication.getDate());
				}
	
				HttpPut put = new HttpPut(output + ITEMS_ENDPOINT + "/" + result.getUuid());
				put.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
				put.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
				put.setEntity(result.toHttpEntity());
				
				try (CloseableHttpResponse response = client.execute(put)) {
					if (response.getCode() != HttpStatus.SC_OK) {
						throw new MigrationException(MessageFormat.format("Unable to update Publication from ''{0}''. HTTP request returned code {1}.",
								publication.getSistedesUri(), response.getCode()));
					}
					result = Publication.fromHttpEntity(response.getEntity());
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
					}
				}
			}
			setHandle(publication.getSistedesIdentifier(), result.getHandleUrl(), prefix, auth);
		} catch (Exception e) {
			throw new MigrationException(e);
		}
		return result;
	}
	
	private List<Identifiable> attachBinaryFiles(final String bundleName, final Publication publication, final File[] files) throws Exception {
		List<Identifiable> result = new ArrayList<>();
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			Bundle bundle = new Bundle(bundleName);
			// First create a Bundle...
			{
				logger.info(MessageFormat.format( "Creating Bundle for ''{0}''.", publication.getTitle()));
				HttpPost post = new HttpPost(output + String.format(ITEM_BUNDLES_ENDPOINT, publication.getUuid()));
				post.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
				post.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
				post.setEntity(new StringEntity(new Gson().toJson(bundle), ContentType.APPLICATION_JSON));

				try (CloseableHttpResponse response = client.execute(post)) {
					if (response.getCode() != HttpStatus.SC_CREATED) {
						throw new MigrationException(
								MessageFormat.format("Unable to create bundles for ''{0}''. HTTP request returned code {1}.",
										publication.getTitle(), response.getCode()));
					}
					bundle = new Gson().fromJson(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), Bundle.class);
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
					}
				}
			}
			
			// Afterwards, upload a file inside the bundle...
			{
				for (File file : files) {
					logger.info(MessageFormat.format( "Uploading ''{0}'' ({1,number,#.#} MB)...",
							file.getName(), FileUtils.sizeOfAsBigInteger(file).doubleValue() / (1024D * 1024D)));
					HttpPost post = new HttpPost(output + String.format(BUNDLES_BITSTREAMS_ENDPOINT, bundle.getUuid()));
					post.setHeader(X_XSRF_TOKEN, dspaceAuth.getXsrfToken());
					post.setHeader(AUTHORIZATION_TOKEN, dspaceAuth.getJwtToken());
					
					MultipartEntityBuilder builder = MultipartEntityBuilder.create();
					builder.addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName());
					post.setEntity(builder.build());
					
					try (CloseableHttpResponse response = client.execute(post)) {
						if (response.getCode() != HttpStatus.SC_CREATED) {
							throw new MigrationException(
									MessageFormat.format("Unable to upload file for ''{0}''. HTTP request returned code {1}.",
											publication.getTitle(), response.getCode()));
						}
						String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
						result.add(new Gson().fromJson(json, Identifiable.class));
						if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
							dspaceAuth.updateXsrfToken(response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue());
						}
					}
				}
			}
		}
		return result;
	}
	
	private Site getSite() throws IOException, ParseException {
		if (site == null) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				HttpGet get = new HttpGet(output + SITES_ENDPOINT);
				try (CloseableHttpResponse response = client.execute(get)) {
					String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
					site = new Gson().fromJson(json, SitesResponse.class)._embedded.sites.get(0);
				}
			}
		}
		return site;
	}

	private int getStartYear() {
		return (int) options.getOrDefault(Options.START_YEAR, Integer.MIN_VALUE);
	}

	private int getEndYear() {
		return (int) options.getOrDefault(Options.END_YEAR, Integer.MAX_VALUE);
	}

	private List<String> getConferences() {
		return Arrays.asList((String[]) options.getOrDefault(Options.CONFERENCES, new String[] {}));
	}
	
	private boolean isDryRun() {
		return (boolean) options.getOrDefault(Options.DRY_RUN, false);
	}

	private boolean isInteractive() {
		return (boolean) options.getOrDefault(Options.INTERACTIVE, false);
	}

	private boolean isMigrateDocumentsEnabled() {
		return (boolean) options.getOrDefault(Options.MIGRATE_DOCUMENTS, false);
	}
	
	private static void setHandle(String newHandle, String targetUrl, String prefix, PublicKeyAuthenticationInfo auth) throws HandleException, MigrationException  {
		logger.info("[>HDL] Handle created: '" + newHandle + "' -> '" + targetUrl + "'");

		HandleResolver resolver = new HandleResolver();
        
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        
        boolean found = false;
        {
        	ResolutionRequest request = new ResolutionRequest(Util.encodeString(newHandle), null, null, null);
        	request.authoritative = true;
        	AbstractResponse response = resolver.processRequest(request);
        	found = (response.responseCode == AbstractMessage.RC_SUCCESS);
        }

        HandleValue urlVal = new HandleValue(1, Util.encodeString("URL"), Util.encodeString(targetUrl), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false);

        AbstractRequest request = null;
        if (found) {
        	request = new ModifyValueRequest(Util.encodeString(newHandle), urlVal, auth);
        	request.authoritative = true;
        } else {
        	AdminRecord adminRecord = new AdminRecord(Util.encodeString("0.NA/" + prefix), 300, true, true, true, true, true, true, true, true, true, true, true, true);
        	HandleValue[] values = {
        			urlVal,
        			new HandleValue(100, Util.encodeString("HS_ADMIN"), Encoder.encodeAdminRecord(adminRecord), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false) 
        	};
            request = new CreateHandleRequest(Util.encodeString(newHandle), values, auth);
        }
        AbstractResponse response = resolver.processRequest(request);
        if (response.responseCode != AbstractMessage.RC_SUCCESS) {
        	throw new MigrationException("Unable to create / update URL for handle " + newHandle);
        }
	}
}
