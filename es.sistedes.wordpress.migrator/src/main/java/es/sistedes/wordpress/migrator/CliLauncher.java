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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.PrivateKey;
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
import org.apache.commons.lang3.StringUtils;

import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.Util;

/**
 * CLI invocator
 * 
 * @author agomez
 *
 */
public class CliLauncher {

	private static final Logger LOGGER = Logger.getLogger(CliLauncher.class.getName());

	private static final String INPUT = "i";
	private static final String INPUT_LONG = "input";
	private static final String CONFERENCES = "c";
	private static final String CONFERENCES_LONG = "conferences";
	private static final String START_YEAR = "s";
	private static final String START_YEAR_LONG = "start-year";
	private static final String END_YEAR = "e";
	private static final String END_YEAR_LONG = "end-year";
	private static final String OUTPUT = "o";
	private static final String OUTPUT_LONG = "output";
	private static final String USER = "u";
	private static final String USER_LONG = "user";
	private static final String PASSWORD = "p";
	private static final String PASSWORD_LONG = "password";
	private static final String WAITING_TIME = "w";
	private static final String WAITING_TIME_LONG = "waiting-time";
	private static final String HANDLE_PREFIX = "h";
	private static final String HANDLE_PREFIX_LONG = "handle-prefix";
	private static final String HANDLE_PRIVATE_KEY_FILE = "k";
	private static final String HANDLE_PRIVATE_KEY_FILE_LONG = "handle-key-file";
	private static final String HANDLE_PRIVATE_KEY_PASSWORD = "x";
	private static final String HANDLE_PRIVATE_KEY_PASSWORD_LONG = "handle-password";
	private static final String DRY_RUN = "d";
	private static final String DRY_RUN_LONG = "dry-run";
	private static final String INTERACTIVE = "t";
	private static final String INTERACTIVE_LONG = "interactive";
	private static final String MIGRATE_DOCS = "m";
	private static final String MIGRATE_DOCS_LONG = "migrate-docs";

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

			URL input = new URL(commandLine.getOptionValue(INPUT));
			URL output = new URL(commandLine.getOptionValue(OUTPUT));
			String user = commandLine.getOptionValue(USER);
			String password = commandLine.getOptionValue(PASSWORD);
			String prefix = commandLine.getOptionValue(HANDLE_PREFIX);
			PublicKeyAuthenticationInfo auth = getAuth(commandLine.getOptionValue(HANDLE_PREFIX), 
					commandLine.getOptionValue(HANDLE_PRIVATE_KEY_FILE), 
					commandLine.getOptionValue(HANDLE_PRIVATE_KEY_PASSWORD));
			
			Migrator migrator = new Migrator(input, output, user, password, prefix, auth);

			if (commandLine.hasOption(WAITING_TIME)) {
				DelayedStreamOpener.setDelay(Integer.parseInt(commandLine.getOptionValue(WAITING_TIME)));
			}
			
			if (commandLine.hasOption(START_YEAR)) {
				migrator.putOption(Migrator.Options.START_YEAR, Integer.parseInt(commandLine.getOptionValue(START_YEAR)));
			}

			if (commandLine.hasOption(END_YEAR)) {
				migrator.putOption(Migrator.Options.END_YEAR, Integer.parseInt(commandLine.getOptionValue(END_YEAR)));
			}
			
			if (commandLine.hasOption(CONFERENCES)) {
				migrator.putOption(Migrator.Options.CONFERENCES, commandLine.getOptionValues(CONFERENCES));
			}

			if (commandLine.hasOption(DRY_RUN)) {
				migrator.putOption(Migrator.Options.DRY_RUN, commandLine.hasOption(DRY_RUN));
			}
			
			if (commandLine.hasOption(INTERACTIVE)) {
				migrator.putOption(Migrator.Options.INTERACTIVE, commandLine.hasOption(INTERACTIVE));
			}

			if (commandLine.hasOption(MIGRATE_DOCS)) {
				migrator.putOption(Migrator.Options.MIGRATE_DOCUMENTS, commandLine.hasOption(MIGRATE_DOCS));
			}
			
			migrator.migrate();
			
		} catch (MigrationException e) {
			printError("ERROR: " + e.getLocalizedMessage());
			throw e;
		}
	}
	
	private static PublicKeyAuthenticationInfo getAuth(String prefix, String filename, String password) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try (FileInputStream fs = new FileInputStream(new File(filename))) {
            byte buf[] = new byte[1024];
            int r;
            while ((r = fs.read(buf)) >= 0)
                bout.write(buf, 0, r);
        }
        byte key[] = bout.toByteArray();

        PrivateKey privkey = null;
        if (Util.requiresSecretKey(key) && StringUtils.isBlank(password)) {
            throw new MigrationException("Private key in " + filename + " requires a password");
        }
        key = Util.decrypt(key, password.getBytes());
        privkey = Util.getPrivateKeyFromBytes(key, 0);
        return new PublicKeyAuthenticationInfo(Util.encodeString("0.NA/" + prefix), 300, privkey);
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
		Option inputOpt = Option
				.builder(INPUT)
				.longOpt(INPUT_LONG)
				.argName("input-url")
				.desc("Base URL of the Wordpress Sistedes Digital Library to read")
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
		
		Option waitingOpt = Option
				.builder(WAITING_TIME)
				.longOpt(WAITING_TIME_LONG)
				.argName("delay")
				.desc("Time to wait (in ms) between connections to the Sistedes Digital Library to avoid flooding it (optional, no delay if not set)")
				.numberOfArgs(1)
				.build();

		Option outputOpt = Option
				.builder(OUTPUT)
				.longOpt(OUTPUT_LONG)
				.argName("output-url")
				.desc("Base URL of the DSpace Sistedes Digital Library to write")
				.numberOfArgs(1)
				.required()
				.build();

		Option userOpt = Option
				.builder(USER)
				.longOpt(USER_LONG)
				.argName("user")
				.desc("User of the DSpace Sistedes Digital Library with write privileges")
				.numberOfArgs(1)
				.required()
				.build();

		Option passwordOpt = Option
				.builder(PASSWORD)
				.longOpt(PASSWORD_LONG)
				.argName("password")
				.desc("Password of the user")
				.numberOfArgs(1)
				.required()
				.build();
		
		Option handlePrefixOpt = Option
				.builder(HANDLE_PREFIX)
				.longOpt(HANDLE_PREFIX_LONG)
				.argName("prefix")
				.desc("Prefix of the Handle registry to update")
				.numberOfArgs(1)
				.required()
				.build();
		
		Option handlePrivateKeyFileOpt = Option
				.builder(HANDLE_PRIVATE_KEY_FILE)
				.longOpt(HANDLE_PRIVATE_KEY_FILE_LONG)
				.argName("key")
				.desc("File with the private key to athenticate in the Handle system")
				.numberOfArgs(1)
				.required()
				.build();
		
		Option handlePrivateKeyPasswordOpt = Option
				.builder(HANDLE_PRIVATE_KEY_PASSWORD)
				.longOpt(HANDLE_PRIVATE_KEY_PASSWORD_LONG)
				.argName("key")
				.desc("Password to decypt the Handle key file")
				.numberOfArgs(1)
				.build();
		
		Option dryRunOpt = Option
				.builder(DRY_RUN)
				.longOpt(DRY_RUN_LONG)
				.desc("Do not perform any changes in the target DSpace instance")
				.numberOfArgs(0)
				.build();

		Option interactiveOpt = Option
				.builder(INTERACTIVE)
				.longOpt(INTERACTIVE_LONG)
				.desc("Ask interactively when there is uncertainty when matching authors")
				.numberOfArgs(0)
				.build();
		
		Option migrateDocsOpt = Option
				.builder(MIGRATE_DOCS)
				.longOpt(MIGRATE_DOCS_LONG)
				.desc("Also migrate the Sistedes documents to the target DSpace instance")
				.numberOfArgs(0)
				.build();
		// @formatter:on

		options.addOption(inputOpt);
		options.addOption(startYearOpt);
		options.addOption(endYearOpt);
		options.addOption(conferencesOpt);
		options.addOption(waitingOpt);
		options.addOption(outputOpt);
		options.addOption(userOpt);
		options.addOption(passwordOpt);
		options.addOption(handlePrefixOpt);
		options.addOption(handlePrivateKeyFileOpt);
		options.addOption(handlePrivateKeyPasswordOpt);
		options.addOption(dryRunOpt);
		options.addOption(interactiveOpt);
		options.addOption(migrateDocsOpt);
	}

	/**
	 * Comparator to always give the command line options in the same order
	 * 
	 * @author agomez
	 *
	 * @param <T>
	 */
	private static class OptionComarator<T extends Option> implements Comparator<T> {
		private static final String OPTS_ORDER = "icseoupwhkxmtd";

		@Override
		public int compare(T o1, T o2) {
			return OPTS_ORDER.indexOf(o1.getOpt()) - OPTS_ORDER.indexOf(o2.getOpt());
		}
	}

}
