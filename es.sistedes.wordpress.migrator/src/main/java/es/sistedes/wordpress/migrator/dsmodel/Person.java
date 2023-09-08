package es.sistedes.wordpress.migrator.dsmodel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.google.gson.Gson;

import es.sistedes.wordpress.migrator.wpmodel.Author;

public class Person extends Item {

	public static Person fromAuthor(Author author) {
		return new Person(author.getFirstName(), author.getLastName(), author.getAffiliation(), StringUtils.contains(author.getEmail(), "@") ? author.getEmail() : null);
	}
	
	public Person(String name, String surname, String affiliation, String email) {
		setGivenName(name);
		setFamilyName(surname);
		addAffiliation(affiliation);
		addEmail(email);
		setType(Type.AUTHOR.getName());
	}
	
	public String getGivenName() {
		return this.metadata.getPersonGivenName();
	}
	
	public void setGivenName(String name) {
		this.metadata.setPersonGivenName(name);
	}
	
	public String getFamilyName() {
		return this.metadata.getPersonFamilyName();
	}
	
	public void setFamilyName(String surname) {
		this.metadata.setPersonFamilyName(surname);
	}
	
	public String getFullName() {
		return getGivenName() + " " + getFamilyName();
	}
	
	public List<String> getNameVariants() {
		return this.metadata.getPersonNameVariants();
	}
	
	public List<String> getAffiliations() {
		return this.metadata.getPersonAffiliations();
	}
	
	public void setAffiliations(List<String> affiliations) {
		this.metadata.setPersonAffiliations(affiliations);
	}
	
	public void addAffiliation(String affiliation) {
		this.metadata.addPersonAffiliation(affiliation);
	}
	
	public List<String> getEmails() {
		return this.metadata.getPersonEmails();
	}
	
	public void setEmails(List<String> emails) {
		this.metadata.setPersonEmails(emails.stream().map(e -> StringUtils.toRootLowerCase(e)).collect(Collectors.toList()));
	}
	
	public void addEmail(String email) {
		this.metadata.addPersonEmail(StringUtils.toRootLowerCase(email));
	}
	
	
	public static Person fromHttpEntity(HttpEntity entity) throws ParseException, IOException {
		return new Gson().fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), Person.class);
	}
}
