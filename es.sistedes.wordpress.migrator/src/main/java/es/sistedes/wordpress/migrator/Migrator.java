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
import java.io.PrintStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import es.sistedes.wordpress.migrator.model.Article;
import es.sistedes.wordpress.migrator.model.Author;
import es.sistedes.wordpress.migrator.model.BDSistedes;
import es.sistedes.wordpress.migrator.model.Conference;
import es.sistedes.wordpress.migrator.model.ConferencesLibrary;
import es.sistedes.wordpress.migrator.model.Edition;
import es.sistedes.wordpress.migrator.model.Track;

/**
 * A {@link Migrator} class that extracts data from the Sistedes Digital Library
 * hosted at the given <code>url</code> and serializes it in a new format by
 * calling the {@link #serialize(OutputStream)} method.
 * 
 * @author agomez
 *
 */
public class Migrator {

	private URL url;
	private Map<MigratorOptions, Object> options;

	public Migrator(URL url) {
		this.url = url;
		this.options = new HashMap<MigratorOptions, Object>();
	}

	/**
	 * Changes the {@link MigratorOptions}
	 * 
	 * @param options
	 */
	public void changeOptions(Map<MigratorOptions, Object> options) {
		this.options = options;
	}

	/**
	 * Sets a new {@link MigratorOptions}
	 * 
	 * @param key
	 * @param value
	 */
	public void putOption(MigratorOptions key, Object value) {
		this.options.put(key, value);
	}

	/**
	 * Removes the given {@link MigratorOptions}
	 * 
	 * @param key
	 */
	public void removeOption(MigratorOptions key) {
		this.options.remove(key);
	}

	/**
	 * Converts the data available in the library hosted at {@link #url}, and
	 * serializes it in the give {@link OutputStream}. The filters specified in the
	 * {@link Migrator#options} are applied to limit the result.
	 * 
	 * @throws MigrationException If any error occurs, check
	 *                            {@link MigrationException#getCause()} to figure
	 *                            out the exact cause
	 */
	public synchronized void serialize(OutputStream output) throws MigrationException {

		PrintStream out = new PrintStream(output);

		try {
			BDSistedes bdSistedes = new BDSistedes(url);
			ConferencesLibrary conferencesLibrary = bdSistedes.getConferencesLibrary();
			
			// NOTE: We use for loops instead of streams since the getters may throw exceptions
			for (Conference conference : conferencesLibrary.getConferences((c1, c2) -> StringUtils.compare(c1.getTitle(), c2.getTitle()))) {
				if (getConferences().isEmpty() || getConferences().contains(conference.getAcronym())) {
					out.println(conference.getTitle());
					for (Edition edition : conference.getEditions((e1, e2) -> StringUtils.compare(e1.getTitle(), e2.getTitle()))) {
						if (edition.getYear() >= getStartYear() && edition.getYear() <= getEndYear()) {
							out.println("  * " + edition.getTitle());
							if (!edition.getTracks().isEmpty()) {
								for (Track track : edition.getTracks()) {
									out.println("    * " + track.getTitle());
									for (Article article : track.getArticles()) {
										out.println(
											"      - " + article.getTitle() 
											+ "\n        "
											+ article.getAuthors().stream().map(Author::toString).collect(Collectors.joining("; ")));
									}
								}
							} else {
								for (Article article : edition.getArticles()) {
									out.println(
										"    - " + article.getTitle()
										+ "\n      "
										+ article.getAuthors().stream().map(Author::toString).collect(Collectors.joining("; ")));
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			throw new MigrationException(e);
		}
	}
	
	private int getStartYear() {
		return (int) options.getOrDefault(MigratorOptions.START_YEAR, Integer.MIN_VALUE);
	}

	private int getEndYear() {
		return (int) options.getOrDefault(MigratorOptions.START_YEAR, Integer.MAX_VALUE);
	}

	private List<String> getConferences() {
		return Arrays.asList((String[]) options.getOrDefault(MigratorOptions.CONFERENCES, new String[] {}));
	}
	
}
