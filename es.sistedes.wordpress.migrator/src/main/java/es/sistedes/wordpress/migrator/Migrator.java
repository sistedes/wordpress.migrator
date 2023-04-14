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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
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
import com.google.gson.JsonObject;

import es.sistedes.wordpress.migrator.dsmodel.Collection;
import es.sistedes.wordpress.migrator.dsmodel.Community;
import es.sistedes.wordpress.migrator.dsmodel.DSpaceEntity;
import es.sistedes.wordpress.migrator.dsmodel.Item;
import es.sistedes.wordpress.migrator.dsmodel.Site;
import es.sistedes.wordpress.migrator.wpmodel.Article;
import es.sistedes.wordpress.migrator.wpmodel.Article.License;
import es.sistedes.wordpress.migrator.wpmodel.Author;
import es.sistedes.wordpress.migrator.wpmodel.BDSistedes;
import es.sistedes.wordpress.migrator.wpmodel.Conference;
import es.sistedes.wordpress.migrator.wpmodel.ConferencesLibrary;
import es.sistedes.wordpress.migrator.wpmodel.Edition;
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

	private static final String API_ENDPOINT = "/api";
	private static final String AUTHN_LOGIN_ENDPOINT = API_ENDPOINT + "/authn/login";
	private static final String SITES_ENDPOINT = API_ENDPOINT + "/core/sites";
	private static final String TOP_COMMUNITIES_ENDPOINT = API_ENDPOINT + "/core/communities/search/top";
	private static final String COMMUNITIES_ENDPOINT = API_ENDPOINT + "/core/communities";
	private static final String COLLECTIONS_ENDPOINT = API_ENDPOINT + "/core/collections";
	private static final String ITEMS_ENDPOINT = API_ENDPOINT + "/core/items";
	private static final String ITEM_BUNDLES_ENDPOINT = API_ENDPOINT + "/core/items/%s/bundles";
	private static final String BUNDLES_BITSTREAMS_ENDPOINT = API_ENDPOINT + "/core/bundles/%s/bitstreams";
	private static final String RESOURCE_POLICIES_ENDPOINT = API_ENDPOINT + "/authz/resourcepolicies";
	private static final String RESOURCE_POLICIES_SEARCH_ENDPOINT = RESOURCE_POLICIES_ENDPOINT + "/search/resource?uuid=%s";
	private static final String METADATAFIELDS_ENDPOINT = API_ENDPOINT + "/core/metadatafields";

	private static final String COMMUNITIES_SUFFIX = "/communities/";
	private static final String COLLECTIONS_SUFFIX = "/collections/";
	private static final String ITEMS_SUFFIX = "/items/";
	
	
	
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
		HANDLE_PREFIX,
		HANDLE_AUTH,
		DRY_RUN
		// @formatter:on
	}

	private URL input;
	private URL output;
	private String user;
	private String password;
	private URL frontend;
	private Map<Options, Object> options;
	private HttpClientBuilder httpClientBuilder;

	private String xsrfToken;
	private String jwtToken;

	private Site site;

	public Migrator(URL input, URL output, String user, String password, URL frontend) {
		this.input = input;
		this.output = output;
		this.user = user;
		this.password = password;
		this.frontend = frontend;
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

			try {
				addCustomMetadataRefinements();
			} catch (Exception e) {
				logger.error("Unable to add custom metadata refinements", e);
			}
			
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
							if (edition.getArticles().isEmpty()) {
								logger.warn("[!EDITION] '" + edition.getTitle() + "' has no papers! Skipping!");
								continue;
							}
							Community childCommunity = createCommunity(community, edition);
							if (edition.getTracks().isEmpty()) {
								logger.warn("[!EDITION] '" + edition.getTitle() + "' has no tracks! Creating a dummy one...");
								logger.info("[>TRACK] Starting migration of " + edition.getTitle() + " (" + edition.getArticles().size() + " papers)");
								Collection collection = createCollection(childCommunity, edition, edition.getDate());
								for (Article article : edition.getArticles()) {
									logger.debug("[-PAPER] Migrating '" + article.getTitle() + "'. "
											+ article.getAuthors().stream().map(Author::toString).collect(Collectors.joining("; ")));
									createItem(collection, article);
								}
								logger.info("[<TRACK] Migration of '" + edition.getTitle() + "' finished");
							} else {
								for (Track track : edition.getTracks()) {
									logger.info("[>TRACK] Starting migration of " + track.getTitle() + " (" + track.getArticles().size() + " papers)");
									Collection collection = createCollection(childCommunity, track, edition.getDate());
									for (Article article : track.getArticles()) {
										logger.debug("[-PAPER] Migrating '" + article.getTitle() + "'. "
												+ article.getAuthors().stream().map(Author::toString).collect(Collectors.joining("; ")));
										createItem(collection, article);
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
			return bdSistedes;
		} catch (Exception e) {
			throw new MigrationException(e);
		}
	}

	private Community createCommunity(Site site, Conference conference) throws MigrationException, IOException, ParseException {
		
		Community community = Community.from(site, conference);
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				
				HttpGet get = new HttpGet(output + TOP_COMMUNITIES_ENDPOINT);
				get.setHeader(X_XSRF_TOKEN, xsrfToken);
				get.setHeader(AUTHORIZATION_TOKEN, jwtToken);
				
				try (CloseableHttpResponse response = client.execute(get)) {
					if (response.getCode() != HttpStatus.SC_OK) {
						throw new MigrationException(MessageFormat.format("Unable to obtain Communities from ''{0}''. HTTP request returned code {1}: {2}",
								output, response.getCode(), community.toJson()));
					}
					if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
						xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
					}
					String string = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
					CommunitiesResponse communitiesResponse = new Gson().fromJson(string, CommunitiesResponse.class);
					if (communitiesResponse._embedded != null) {
						List<Community> communities = communitiesResponse._embedded.communities;
						Optional<Community> result = communities.stream().filter(c -> StringUtils.equals(c.getName(), conference.getTitle())).findFirst();
						if (result.isPresent()) {
							return result.get();
						}
					}
				}
			}
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
			try {
				setHandle(community.getHandle(), frontend + COMMUNITIES_SUFFIX + community.getId(), getHandlePrefix(), getHandleAuth());
			} catch (Exception e) {
				throw new MigrationException(e);
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
			try {
				setHandle(community.getHandle(), frontend + COMMUNITIES_SUFFIX + community.getId(), getHandlePrefix(), getHandleAuth());
			} catch (Exception e) {
				throw new MigrationException(e);
			}
		}
		return community;
	}

	private Collection createCollection(Community parent, Track track, Date date) throws MigrationException, IOException, ParseException, URISyntaxException {
		
		Collection collection = Collection.from(parent, track, date);
		
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
			try {
				setHandle(collection.getHandle(), frontend + COLLECTIONS_SUFFIX + collection.getId(), getHandlePrefix(), getHandleAuth());
			} catch (Exception e) {
				throw new MigrationException(e);
			}
		}
		return collection;
	}
	
	private Item createItem(Collection parent, Article article) throws MigrationException, IOException, ParseException, URISyntaxException {
		
		Item item = Item.from(parent, article);
		
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				
				{
					URIBuilder builder = new URIBuilder(output + ITEMS_ENDPOINT);
			        builder.setParameter("owningCollection", parent.getId());
					
					HttpPost post = new HttpPost(builder.build());
					post.setEntity(item.toHttpEntity());
					post.setHeader(X_XSRF_TOKEN, xsrfToken);
					post.setHeader(AUTHORIZATION_TOKEN, jwtToken);
	
					try (CloseableHttpResponse response = client.execute(post)) {
						if (response.getCode() == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
							throw new MigrationException(MessageFormat.format("Unable to create Item from ''{0}''. HTTP request returned code {1}: {2}",
									article, response.getCode(), item.toJson()));
						} else if (response.getCode() != HttpStatus.SC_CREATED) {
							throw new MigrationException(MessageFormat.format("Unable to create Item from ''{0}''. HTTP request returned code {1}.",
									article, response.getCode()));
						}
						item = Item.fromHttpEntity(response.getEntity());
						if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
							xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
						}
					}
				}
				// Now that the item has been already created, rewrite it
				{
					item.setDate(parent.getDate());
					item.setUri(article.getHandleUri());
	
					HttpPut put = new HttpPut(output + ITEMS_ENDPOINT + "/" + item.getId());
					put.setHeader(X_XSRF_TOKEN, xsrfToken);
					put.setHeader(AUTHORIZATION_TOKEN, jwtToken);
					put.setEntity(item.toHttpEntity());
					
					try (CloseableHttpResponse response = client.execute(put)) {
						if (response.getCode() == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
							throw new MigrationException(MessageFormat.format("Unable to create Collection from ''{0}''. HTTP request returned code {1}: {2}",
									article, response.getCode(), item.toJson()));
						} else if (response.getCode() != HttpStatus.SC_OK) {
							throw new MigrationException(MessageFormat.format("Unable to create Collection from ''{0}''. HTTP request returned code {1}.",
									article, response.getCode()));
						}
						item = Item.fromHttpEntity(response.getEntity());
						if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
							xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
						}
					}
				}
				
				// Now, create a bundle... 
				if (article.getHandle() == null) {
					logger.info("Article '" + article.getLink() + "' does not have a handle! Skipping file upload...");
				} else {
					File file = Item.getFile(article);
					if (file.exists()) {
						DSpaceEntity bundle = new DSpaceEntity();
						bundle.setName("ORIGINAL");
						{
							HttpPost post = new HttpPost(output + String.format(ITEM_BUNDLES_ENDPOINT, item.getId()));
							post.setHeader(X_XSRF_TOKEN, xsrfToken);
							post.setHeader(AUTHORIZATION_TOKEN, jwtToken);
							post.setEntity(new StringEntity(new Gson().toJson(bundle), ContentType.APPLICATION_JSON));
							
							try (CloseableHttpResponse response = client.execute(post)) {
								if (response.getCode() != HttpStatus.SC_CREATED) {
									throw new MigrationException(MessageFormat.format("Unable to create bundles for ''{0}''. HTTP request returned code {1}.",
											article, response.getCode()));
								}
								bundle = new Gson().fromJson(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), DSpaceEntity.class);
								if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
									xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
								}
							}
						}
						
						Identifiable uploadedFile = null;
						// Upload a file...
						{
							HttpPost post = new HttpPost(output + String.format(BUNDLES_BITSTREAMS_ENDPOINT, bundle.getUuid()));
							post.setHeader(X_XSRF_TOKEN, xsrfToken);
							post.setHeader(AUTHORIZATION_TOKEN, jwtToken);
							
							MultipartEntityBuilder builder = MultipartEntityBuilder.create();
							builder.addBinaryBody("file", file, ContentType.APPLICATION_PDF, file.getName());
							post.setEntity(builder.build());
							
							try (CloseableHttpResponse response = client.execute(post)) {
								if (response.getCode() != HttpStatus.SC_CREATED) {
									throw new MigrationException(MessageFormat.format("Unable to upload file for ''{0}''. HTTP request returned code {1}.",
											article.getTitle(), response.getCode()));
								}
								String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
								uploadedFile = new Gson().fromJson(json, Identifiable.class);
								if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
									xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
								}
							}
						}
						
						// And if the item is restricted, remove the resource policy
						// (which at this point, should be a single one: allow anonymous read access)
						// Upload a file...
						if (uploadedFile != null && License.from(article.getLicense()) == License.RESTRICTED) {
							Identifiable policy = null;
							{
								HttpGet get = new HttpGet(output + String.format(RESOURCE_POLICIES_SEARCH_ENDPOINT, uploadedFile.id));
								get.setHeader(X_XSRF_TOKEN, xsrfToken);
								get.setHeader(AUTHORIZATION_TOKEN, jwtToken);
								
								try (CloseableHttpResponse response = client.execute(get)) {
									if (response.getCode() != HttpStatus.SC_OK) {
										throw new MigrationException(MessageFormat.format("Unable to get policies file for ''{0}''. HTTP request returned code {1}.",
												uploadedFile.id, response.getCode()));
									}
									String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
									policy = new Gson().fromJson(json, ResourcePoliciesResponse.class)._embedded.resourcepolicies.get(0);
	
									if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
										xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
									}
								}
							}
							if (policy != null){
								HttpDelete delete = new HttpDelete(output + RESOURCE_POLICIES_ENDPOINT + "/" + policy.id);
								delete.setHeader(X_XSRF_TOKEN, xsrfToken);
								delete.setHeader(AUTHORIZATION_TOKEN, jwtToken);
		
								try (CloseableHttpResponse response = client.execute(delete)) {
									if (response.getCode() != HttpStatus.SC_NO_CONTENT) {
										throw new MigrationException(MessageFormat.format("Unable to delete policy ''{0}''. HTTP request returned code {1}.",
												policy.id, response.getCode()));
									}
									EntityUtils.consume(response.getEntity());
									if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
										xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
									}
								}
							}
						}
					}
				}
			}
			try {
				setHandle(new URL(item.getUri()).getPath().replaceFirst("/", ""), 
						frontend + ITEMS_SUFFIX + item.getId(), getHandlePrefix(), getHandleAuth());
			} catch (Exception e) {
				throw new MigrationException(e);
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
	
	private void addCustomMetadataRefinements() throws MigrationException, IOException, URISyntaxException {
		if (!isDryRun()) {
			try (CloseableHttpClient client = httpClientBuilder.build()) {
				
				URIBuilder builder = new URIBuilder(output + METADATAFIELDS_ENDPOINT);
		        builder.setParameter("schemaId", "1");
				
				HttpPost post = new HttpPost(builder.build());
				
				List<JsonObject> objs = new ArrayList<>();
				objs.add(new JsonObject());
				objs.get(0).addProperty("element", "contributor");
				objs.get(0).addProperty("qualifier", "email");
				objs.get(0).addProperty("scopeNote", "When contributors are persons, use primarily to specify their e-mails.");

				objs.add(new JsonObject());
				objs.get(1).addProperty("element", "contributor");
				objs.get(1).addProperty("qualifier", "institution");
				objs.get(1).addProperty("scopeNote", "Use for institutions that contributed to this item. May be used to specify affiliations.");

				for (JsonObject obj : objs) {
					post.setEntity(new StringEntity(obj.toString(), ContentType.APPLICATION_JSON));
					post.setHeader(X_XSRF_TOKEN, xsrfToken);
					post.setHeader(AUTHORIZATION_TOKEN, jwtToken);
	
					try (CloseableHttpResponse response = client.execute(post)) {
						if (response.getCode() != HttpStatus.SC_CREATED) {
							MigrationException me = new MigrationException(MessageFormat.format("Unable to create metadata element from ''{0}''. HTTP request returned code {1}.",
									obj.toString(), response.getCode()));
							logger.error(me.getMessage());
						}
						EntityUtils.consume(response.getEntity());
						if (response.getFirstHeader(DSPACE_XSRF_TOKEN) != null) {
							xsrfToken = response.getFirstHeader(DSPACE_XSRF_TOKEN).getValue();
						}
					}
				}
			}
		}
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

	private PublicKeyAuthenticationInfo getHandleAuth() {
		return (PublicKeyAuthenticationInfo) options.get(Options.HANDLE_AUTH);
	}

	private String getHandlePrefix() {
		return (String) options.get(Options.HANDLE_PREFIX);
	}
	
	private boolean isDryRun() {
		return (boolean) options.getOrDefault(Options.DRY_RUN, false);
	}
	
	private static void setHandle(String handle, String url, String prefix, PublicKeyAuthenticationInfo auth) throws HandleException, MigrationException  {
		HandleResolver resolver = new HandleResolver();
        
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        
        boolean found = false;
        {
        	ResolutionRequest request = new ResolutionRequest(Util.encodeString(handle), null, null, null);
        	request.authoritative = true;
        	AbstractResponse response = resolver.processRequest(request);
        	found = (response.responseCode == AbstractMessage.RC_SUCCESS);
        }

        HandleValue urlVal = new HandleValue(1, Util.encodeString("URL"), Util.encodeString(url), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false);

        AbstractRequest request = null;
        if (found) {
        	request = new ModifyValueRequest(Util.encodeString(handle), urlVal, auth);
        	request.authoritative = true;
        } else {
        	AdminRecord adminRecord = new AdminRecord(Util.encodeString("0.NA/" + prefix), 300, true, true, true, true, true, true, true, true, true, true, true, true);
        	HandleValue[] values = {
        			urlVal,
        			new HandleValue(100, Util.encodeString("HS_ADMIN"), Encoder.encodeAdminRecord(adminRecord), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false) 
        	};
            request = new CreateHandleRequest(Util.encodeString(handle), values, auth);
        }
        AbstractResponse response = resolver.processRequest(request);
        if (response.responseCode != AbstractMessage.RC_SUCCESS) {
        	throw new MigrationException("Unable to create / update URL for handle " + handle);
        }
	}
}
