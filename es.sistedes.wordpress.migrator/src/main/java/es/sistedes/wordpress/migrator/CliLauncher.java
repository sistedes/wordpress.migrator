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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

/**
 * CLI invocator
 * 
 * @author agomez
 *
 */
public class CliLauncher {

	private static final Logger LOGGER = Logger.getLogger(CliLauncher.class.getName());

	private static final String URL = "u";
	private static final String URL_LONG = "url";
	private static final String CONFERENCES = "c";
	private static final String CONFERENCES_LONG = "conferences";
	private static final String START_YEAR = "s";
	private static final String START_YEAR_LONG = "start-year";
	private static final String END_YEAR = "e";
	private static final String END_YEAR_LONG = "end-year";
	private static final String OUTPUT = "o";
	private static final String OUTPUT_LONG = "output";
	private static final String DELAY = "d";
	private static final String DELAY_LONG = "delay-long";

	private static final Options options = new Options();

	static {
		configureOptions(options);
	}

	public static void main(String[] args) {
		try {
			run(args);
		} catch (Throwable t) {
			if (t instanceof RuntimeException || t instanceof Error) {
				// Log unexpected unchecked exception
				LOGGER.log(Level.SEVERE, t.toString(), t);
			}
			System.exit(ReturnCodes.ERROR.getReturnCode());
		}
	}

	/**
	 * Runs the {@link CliLauncher}
	 * 
	 * @param args
	 * @throws Exception
	 */
	private static void run(String[] args) throws Exception {
		try {
			CommandLine commandLine = null;

			try {
				CommandLineParser parser = new DefaultParser();
				commandLine = parser.parse(options, args);
			} catch (ParseException e) {
				printError(e.getLocalizedMessage());
				printHelp();
				throw e;
			}

			Migrator migrator = new Migrator(new java.net.URL(commandLine.getOptionValue(URL)));

			if (commandLine.hasOption(DELAY)) {
				DelayedStreamOpener.setDelay(Integer.parseInt(commandLine.getOptionValue(DELAY)));
			}
			
			if (commandLine.hasOption(START_YEAR)) {
				migrator.putOption(MigratorOptions.START_YEAR, Integer.parseInt(commandLine.getOptionValue(START_YEAR)));
			}

			if (commandLine.hasOption(END_YEAR)) {
				migrator.putOption(MigratorOptions.END_YEAR, Integer.parseInt(commandLine.getOptionValue(END_YEAR)));
			}
			
			if (commandLine.hasOption(CONFERENCES)) {
				migrator.putOption(MigratorOptions.CONFERENCES, commandLine.getOptionValues(CONFERENCES));
			}
			
			OutputStream output = System.out;
			if (commandLine.hasOption(OUTPUT)) {
				output = new FileOutputStream(new File(commandLine.getOptionValue(OUTPUT)));
			} 

			try {
				migrator.serialize(output);
			} finally {
				IOUtils.closeQuietly(output);
			}
		} catch (MigrationException | FileNotFoundException e) {
			printError("ERROR: " + e.getLocalizedMessage());
			throw e;
		}
	}

	/**
	 * Prints the help about the command-line options
	 */
	private static void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(new OptionComarator<Option>());
		formatter.printHelp("java -jar <this-file.jar>", options, true);
	}

	/**
	 * Prints the <code>message</code> {@link String} in the standard error
	 * 
	 * @param message
	 *            The message
	 */
	private static void printError(String message) {
		System.err.println(message);
	}

	/**
	 * Configures the command-line {@link Options}
	 * 
	 * @param options
	 *            The {@link Options}
	 */

	private static void configureOptions(Options options) {
		// @formatter:off
		Option hostOpt = Option
				.builder(URL)
				.longOpt(URL_LONG)
				.argName("base url")
				.desc("Base URL of the Wordpress SISTEDES Digital Library")
				.numberOfArgs(1)
				.required()
				.build();

		Option startYearOpt = Option
				.builder(START_YEAR)
				.longOpt(START_YEAR_LONG)
				.argName("start-year")
				.desc("Consider only editions celebrated after the specified year including it (optional, start from the oldest by default)")
				.numberOfArgs(1)
				.build();

		Option endYearOpt = Option
				.builder(END_YEAR)
				.longOpt(END_YEAR_LONG)
				.argName("end-year")
				.desc("Consider only editions celebrated before the specified year including it (optional, end at the lastest by default)")
				.numberOfArgs(1)
				.build();
		
		Option conferencesOpt = Option
				.builder(CONFERENCES)
				.longOpt(CONFERENCES_LONG)
				.argName("conference")
				.desc("Limit the migration to the specified conferences (optional, process all conferences by default)")
				.numberOfArgs(Option.UNLIMITED_VALUES)
				.build();
		
		Option outputOpt = Option
				.builder(OUTPUT)
				.longOpt(OUTPUT_LONG)
				.argName("output file")
				.desc("The output file (optional, stdout will be used if no file is specified)")
				.numberOfArgs(1)
				.build();
		
		Option delayOpt = Option
				.builder(DELAY)
				.longOpt(DELAY_LONG)
				.argName("delay")
				.desc("Time to wait (in ms) between connections to the Sistedes Digital Library to avoid flooding it (optional, no delay if not set)")
				.numberOfArgs(1)
				.build();
		// @formatter:on

		options.addOption(hostOpt);
		options.addOption(startYearOpt);
		options.addOption(endYearOpt);
		options.addOption(conferencesOpt);
		options.addOption(outputOpt);
		options.addOption(delayOpt);
	}

	/**
	 * Comparator to always give the command line options in the same order
	 * 
	 * @author agomez
	 *
	 * @param <T>
	 */
	private static class OptionComarator<T extends Option> implements Comparator<T> {
		private static final String OPTS_ORDER = "ucseod";

		@Override
		public int compare(T o1, T o2) {
			return OPTS_ORDER.indexOf(o1.getOpt()) - OPTS_ORDER.indexOf(o2.getOpt());
		}
	}

}
