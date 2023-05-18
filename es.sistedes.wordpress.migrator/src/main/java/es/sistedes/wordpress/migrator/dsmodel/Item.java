package es.sistedes.wordpress.migrator.dsmodel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.google.gson.Gson;

public class Item extends DSpaceEntity {

	// BEGIN: JSON fields
	// Do not delete: these properties are mandatory in
	// order to generate a valid JSON representation of
	// the Item
	protected boolean inArchive = true;
	protected boolean discoverable = true;
	// END: JSON fields
	
	public enum Type {
		AUTHOR("Autor"),
		PAPER("Artículo"),
		ABSTRACT("Resumen"),
		BULLETIN("Boletín"),
		SEMINAR("Seminario"),
		PRELIMINARS("Preliminares");
		
		
		private String name;
		
		Type(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
	}

	protected String getType() {
		return this.metadata.getType();
	}

	protected void setType(String type) {
		this.metadata.setType(type);
	}
	
	public static Item fromHttpEntity(HttpEntity entity) throws ParseException, IOException {
		return new Gson().fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), Item.class);
	}
	
	public void setInArchive(boolean inArchive) {
		this.inArchive = inArchive;
	}

	public void setDiscoverable(boolean discoverable) {
		this.discoverable = discoverable;
	}
}
