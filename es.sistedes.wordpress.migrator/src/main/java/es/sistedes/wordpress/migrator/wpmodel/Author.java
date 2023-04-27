package es.sistedes.wordpress.migrator.wpmodel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class Author {
	
	private static final Logger LOGGER = Logger.getLogger(Author.class.getCanonicalName());
	private static final String MALE_NAMES_FILE = "/male-names.csv";
	private static final String FEMALE_NAMES_FILE = "/female-names.csv";
	private static final String SURNAMES_FILE = "/surnames.csv";
	private static final String EXCEPTIONS_FILE = "exceptions.txt";
	private static SortedMap<String, Integer> names = new TreeMap<>();
	private static SortedMap<String, Integer> surnames = new TreeMap<>();
	private static SortedMap<String, String[]> exceptions = new TreeMap<>();

	static {
		loadNames();
		loadSurnames();
		loadExceptions();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> saveExceptions()));
	}
	
	private static void loadNames() {
		for (String file : new String [] { MALE_NAMES_FILE, FEMALE_NAMES_FILE}) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(Author.class.getResourceAsStream(file)))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.trim().startsWith("#")) continue;
					String[] fields = line.split(",");
					if (fields.length >= 2) {
						names.putIfAbsent(fields[0].trim(), Integer.valueOf(fields[1].trim()));
					}
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, String.format("Unable to load '%s' into names registry", file.replaceFirst("/", "")));
			}
		}
	}

	private static void loadSurnames() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Author.class.getResourceAsStream(SURNAMES_FILE)))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().startsWith("#")) continue;
				String[] fields = line.split(",");
				if (fields.length >= 2) {
					surnames.put(fields[0].trim(), Integer.valueOf(fields[1].trim()));
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, String.format("Unable to load '%s' into surnames registry", SURNAMES_FILE.replaceFirst("/", "")));
		}
	}
	
	private static void loadExceptions() {
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(EXCEPTIONS_FILE)))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split(",");
				if (fields.length == 4) {
					exceptions.put(fields[0].trim(), new String[] {fields[1].trim(), fields[2].trim(), fields[3].trim()});
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to load exceptions registry");
		}
	}
	
	private static void saveExceptions() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(EXCEPTIONS_FILE)))) {
			for (Entry<String, String[]> entry : exceptions.entrySet()) {
				writer.write(String.format("%s,%s,%s,%s\n", entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]));
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to save exceptions registry");
		}
	}
	
	
	private String firstName;
	private String lastName;
	private String email;
	private String affiliation;
	
	public Author(String fullname, String email, String affiliation) {
		this.email = StringUtils.trimToNull(email);
		this.affiliation = StringUtils.trimToNull(affiliation);
		parseName(fullname);
	}

	private void parseName(String fullname) {
		fullname = fullname.trim().replaceAll("\\.", ". ").replaceAll("\\s+", " ").replaceAll("–", "-");
		String[] exceptionFields = exceptions.get(fullname);
		if (exceptionFields != null && StringUtils.isNotBlank(exceptionFields[0]) && StringUtils.isNotBlank(exceptionFields[1])) {
			firstName = exceptionFields[0];
			lastName = exceptionFields[1];
		} else {
			String[] fields = fullname.split(" ");
			if (fields.length == 0) {
				throw new IllegalArgumentException("Can't parse empty name");
			} else if (fields.length == 1) {
				throw new IllegalArgumentException(String.format("Unexpected format for full name '%s': more than one word expected", fullname));
			} else if (fields.length == 2) {
				firstName = fields[0];
				lastName = fields[1];
			} else {
				int surnameStartsAt = 1;
				// We skip the first word since we consider it will always be a Name
				for (int i = 1; i < fields.length; i++) {
					surnameStartsAt = i;
					if (fields[i].contains(".")) {
						// Initials always are part of the name, keep looping...
						continue;
					} else if (fields[i].contains("-")) {
						// Composed words always denote a surname
						break;
					} else if (StringUtils.equalsIgnoreCase("de", fields[i])
							|| StringUtils.equalsIgnoreCase("y", fields[i])
							|| StringUtils.equalsIgnoreCase("van", fields[i])) {
						// These particles typically denote a surname
						break;
					} else {
						String cleanField = StringUtils.stripAccents(fields[i]).toUpperCase();
						if (names.containsKey(StringUtils.stripAccents(
								StringUtils.join(ArrayUtils.subarray(fields, 0, i + 1), " ")).toUpperCase()
									.replace("MA ", "MARIA ")
									.replace(" DEL", "")
								)
							) {
							// May be a composed name...
							// We remove the "del" particle (e.g., Maria del Carmen) to do the comparison 
							// since such names are registered without it (i.e. MARIA CARMEN)
							// Also, sometimes "Maria" is abbreviated as "Ma". We consider safe 
							// to do the replacement since "Ma" is not a common Spanish name
							continue;
						} else if (names.getOrDefault(cleanField, 0) > surnames.getOrDefault(cleanField, 0) ) {
							// The word is more common as a name than as a surname
							continue;
						} else {
							break;
						}
					}
				}
				if (StringUtils.equalsIgnoreCase(fields[surnameStartsAt-1], "del")) {
					// Backtrack:
					// If the particle before the surname was a "DEL", 
					// then it was not part of a composed name
					surnameStartsAt--;
				}
				firstName = StringUtils.join(ArrayUtils.subarray(fields, 0, surnameStartsAt), " ");
				lastName = StringUtils.join(ArrayUtils.subarray(fields, surnameStartsAt, fields.length), " ");
			}
			exceptions.put(fullname, new String[] { StringUtils.defaultIfBlank(firstName, fullname), StringUtils.defaultIfBlank(lastName, ""), email });
		}
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public String getFullName() {
		return firstName + " " + lastName;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getAffiliation() {
		return affiliation;
	}
	
	@Override
	public String toString() {
		return StringUtils.defaultIfBlank(lastName, "<empty>")  + ", " + StringUtils.defaultIfBlank(firstName, "<empty>");
	}
}
