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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.dsmodel.Collection;
import es.sistedes.wordpress.migrator.dsmodel.Community;
import es.sistedes.wordpress.migrator.dsmodel.Item;
import es.sistedes.wordpress.migrator.dsmodel.Site;
import es.sistedes.wordpress.migrator.wpmodel.Article;
import es.sistedes.wordpress.migrator.wpmodel.Author;
import es.sistedes.wordpress.migrator.wpmodel.BDSistedes;
import es.sistedes.wordpress.migrator.wpmodel.Conference;
import es.sistedes.wordpress.migrator.wpmodel.ConferencesLibrary;
import es.sistedes.wordpress.migrator.wpmodel.Edition;
import es.sistedes.wordpress.migrator.wpmodel.Track;

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

	private static final String API_ENDPOINT = "/api";
	private static final String AUTHN_LOGIN_ENDPOINT = API_ENDPOINT + "/authn/login";
	private static final String SITES_ENDPOINT = API_ENDPOINT + "/core/sites";
	private static final String COMMUNITIES_ENDPOINT = API_ENDPOINT + "/core/communities";
	private static final String COLLECTIONS_ENDPOINT = API_ENDPOINT + "/core/collections";
	private static final String ITEMS_ENDPOINT = API_ENDPOINT + "/core/items";

	private class Response {
		private class Embedded {
			private List<Site> sites;
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
		DRY_RUN
		// @formatter:on
	}

	private URL input;
	private URL output;
	private String user;
	private String password;
	private Map<Options, Object> options;
	private HttpClientBuilder httpClientBuilder;

	private String xsrfToken;
	private String jwtToken;

	private Site site;

	public Migrator(URL input, URL output, String user, String password) {
		this.input = input;
		this.output = output;
		this.user = user;
		this.password = password;
		this.options = new HashMap<Options, Object>();
		this.httpClientBuilder = HttpClients.custom().setDefaultCookieStore(new BasicCookieStore());

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
	 * @return a BDSistedes
	 * 
	 * @throws MigrationException If any error occurs, check
	 *                            {@link MigrationException#getCause()} to figure
	 *                            out the exact cause
	 */
	public synchronized BDSistedes crawl() throws MigrationException {
		try {
			login();

			BDSistedes bdSistedes = new BDSistedes(input);
			ConferencesLibrary conferencesLibrary = bdSistedes.getConferencesLibrary();

			// NOTE: We use for loops instead of streams since the getters may throw
			// exceptions
			for (Conference conference : conferencesLibrary.getConferences((c1, c2) -> StringUtils.compare(c1.getTitle(), c2.getTitle()))) {
				if (getConferences().isEmpty() || getConferences().contains(conference.getAcronym())) {
					logger.info("[>CONFERENCE] Starting migration of '" + conference.getTitle() + "'");
					Community community = createCommunity(getSite(), conference);
					for (Edition edition : conference.getEditions((e1, e2) -> StringUtils.compare(e1.getTitle(), e2.getTitle()))) {
						if (edition.getYear() >= getStartYear() && edition.getYear() <= getEndYear()) {
							logger.info("[>EDITION] Starting migration of '"  + edition.getTitle() + "'");
							Community childCommunity = createCommunity(community, edition);
							if (edition.getTracks().isEmpty()) {
								logger.error("[!EDITION] '" + edition.getTitle() + "' has not tracks! Skipping...");
								continue;
							}
							for (Track track : edition.getTracks()) {
								logger.debug("[>TRACK] Starting migration of " + track.getTitle());
								Collection collection = createCollection(childCommunity, track);
								for (Article article : track.getArticles()) {
									logger.debug("[-PAPER] Migrating '" + article.getTitle() + "'. "
											+ article.getAuthors().stream().map(Author::toString).collect(Collectors.joining("; ")));
									createItem(collection, article);
								}
								logger.debug("[<TRACK] Migration of '" + track.getTitle() + "' finished");
							}
							logger.info("[<EDITION] Migration of '"  + edition.getTitle() + "' finished");
						} else {
							logger.info("[!EDITION] Skipping '"  + edition.getTitle() + "'...");
						}
					}
					logger.info("[<CONFERENCE] Migration of '" + conference.getTitle() + "' finished");
				}
			}
			return bdSistedes;
		} catch (Exception e) {
			throw new MigrationException(e);
		}
	}

	private Community createCommunity(Site site, Conference conference) throws MigrationException, IOException, ParseException {
		
		Community community = Community.from(site, conference);
		
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {

				HttpPost post = new HttpPost(output + COMMUNITIES_ENDPOINT);
				post.setEntity(community.toHttpEntity());
				post.setHeader(X_XSRF_TOKEN, xsrfToken);
				post.setHeader(AUTHORIZATION_TOKEN, jwtToken);

				try (CloseableHttpResponse response = client.execute(post)) {
					if (response.getCode() == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
						throw new MigrationException(MessageFormat.format("Unable to create Community from ''{0}''. HTTP request returned code {1}: {2}",
								conference, response.getCode(), community.toJson()));
					} else if (response.getCode() != HttpStatus.SC_CREATED) {
						throw new MigrationException(MessageFormat.format("Unable to create Community from ''{0}''. HTTP request returned code {1}.",
								conference, response.getCode()));
					}
					community = Community.fromHttpEntity(response.getEntity());
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
					}
				}
			}
		}
		return community;
	}
	
	private Community createCommunity(Community parent, Edition edition) throws MigrationException, IOException, ParseException, URISyntaxException {
		
		Community community = Community.from(parent, edition);
		
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				
				URIBuilder builder = new URIBuilder(output + COMMUNITIES_ENDPOINT);
		        builder.setParameter("parent", parent.getId());
				
				HttpPost post = new HttpPost(builder.build());
				post.setEntity(community.toHttpEntity());
				post.setHeader(X_XSRF_TOKEN, xsrfToken);
				post.setHeader(AUTHORIZATION_TOKEN, jwtToken);
				
				try (CloseableHttpResponse response = client.execute(post)) {
					if (response.getCode() == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
						throw new MigrationException(MessageFormat.format("Unable to create Community from ''{0}''. HTTP request returned code {1}: {2}",
								edition, response.getCode(), community.toJson()));
					} else if (response.getCode() != HttpStatus.SC_CREATED) {
						throw new MigrationException(MessageFormat.format("Unable to create Community from ''{0}''. HTTP request returned code {1}.",
								edition, response.getCode()));
					}
					community = Community.fromHttpEntity(response.getEntity());
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
					}
				}
			}
		}
		return community;
	}

	private Collection createCollection(Community parent, Track track) throws MigrationException, IOException, ParseException, URISyntaxException {
		
		Collection collection = Collection.from(parent, track);
		
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				
				URIBuilder builder = new URIBuilder(output + COLLECTIONS_ENDPOINT);
		        builder.setParameter("parent", parent.getId());
				
				HttpPost post = new HttpPost(builder.build());
				post.setEntity(collection.toHttpEntity());
				post.setHeader(X_XSRF_TOKEN, xsrfToken);
				post.setHeader(AUTHORIZATION_TOKEN, jwtToken);

				try (CloseableHttpResponse response = client.execute(post)) {
					if (response.getCode() == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
						throw new MigrationException(MessageFormat.format("Unable to create Collection from ''{0}''. HTTP request returned code {1}: {2}",
								track, response.getCode(), collection.toJson()));
					} else if (response.getCode() != HttpStatus.SC_CREATED) {
						throw new MigrationException(MessageFormat.format("Unable to create Collection from ''{0}''. HTTP request returned code {1}.",
								track, response.getCode()));
					}
					collection = Collection.fromHttpEntity(response.getEntity());
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
					}
				}
			}
		}
		return collection;
	}
	
	private Item createItem(Collection parent, Article article) throws MigrationException, IOException, ParseException, URISyntaxException {
		
		Item item = Item.from(article);
		
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				
				URIBuilder builder = new URIBuilder(output + ITEMS_ENDPOINT);
		        builder.setParameter("owningCollection", parent.getId());
				
				HttpPost post = new HttpPost(builder.build());
				post.setEntity(item.toHttpEntity());
				post.setHeader(X_XSRF_TOKEN, xsrfToken);
				post.setHeader(AUTHORIZATION_TOKEN, jwtToken);

				try (CloseableHttpResponse response = client.execute(post)) {
					if (response.getCode() == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
						throw new MigrationException(MessageFormat.format("Unable to create Collection from ''{0}''. HTTP request returned code {1}: {2}",
								article, response.getCode(), item.toJson()));
					} else if (response.getCode() != HttpStatus.SC_CREATED) {
						throw new MigrationException(MessageFormat.format("Unable to create Collection from ''{0}''. HTTP request returned code {1}.",
								article, response.getCode()));
					}
					item = Item.fromHttpEntity(response.getEntity());
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
					}
				}
			}
		}
		return item;
	}

	private void login() throws MigrationException, IOException {

		try (CloseableHttpClient client = httpClientBuilder.build()) {

			HttpGet get = new HttpGet(output + API_ENDPOINT);
			try (CloseableHttpResponse response = client.execute(get)) {
				EntityUtils.consume(response.getEntity());
				xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
			}

			List<NameValuePair> data = new ArrayList<>();
			data.add(new BasicNameValuePair("user", user));
			data.add(new BasicNameValuePair("password", password));

			HttpPost post = new HttpPost(output + AUTHN_LOGIN_ENDPOINT);
			post.setEntity(new UrlEncodedFormEntity(data));
			post.setHeader(X_XSRF_TOKEN, xsrfToken);

			try (CloseableHttpResponse response = client.execute(post)) {
				if (response.getCode() != HttpStatus.SC_OK) {
					throw new MigrationException(MessageFormat.format("Unable to log in ''{0}''", output));
				}
				EntityUtils.consume(response.getEntity());
				xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
				jwtToken = response.getFirstHeader(AUTHORIZATION_TOKEN).getValue();
			}
		}
	}

	private Site getSite() throws IOException, ParseException {
		if (site == null) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				HttpGet get = new HttpGet(output + SITES_ENDPOINT);
				try (CloseableHttpResponse response = client.execute(get)) {
					String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
					site = new Gson().fromJson(json, Response.class)._embedded.sites.get(0);
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
}
