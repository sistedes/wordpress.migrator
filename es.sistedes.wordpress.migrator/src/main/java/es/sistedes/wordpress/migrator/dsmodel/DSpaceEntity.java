package es.sistedes.wordpress.migrator.dsmodel;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DSpaceEntity {
	
	protected final static transient List<String> PARTICLES = Arrays.asList(new String[] {
			"a", "de", "del", "en", "por", "y", "o", "u", "e", "la", "el", "lo", "las",
			"los", "con", "desde", "hacia", "para", "por", "ya", "and", "on", "of" });
	
	// BEGIN: JSON fields
	protected String uuid;
	protected String name;
	protected String handle;
	protected Metadata metadata = new Metadata(); 
	// END: JSON fields

	public String getUuid() {
		return uuid;
	}
	
	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}
	
	public String getHandle() {
		return handle;
	}
	
	public String getHandleUrl() {
		return "https://hdl.handle.net/" + handle;
	}

	public String getSistedesIdentifier() {
		return this.metadata.getSistedesIdentifier();
	}
	
	public String getSistedesUri() {
		return "https?://hdl.handle.net/" + this.metadata.getSistedesIdentifier();
	}
	
	public void setSistedesIdentifier(String id) {
		this.metadata.setSistedesIdentifier(id);
	}
	
	public String getTitle() {
		return this.metadata.getTitle();
	}
	
	public void setTitle(String title) {
		this.metadata.setTitle(title);
	}
	
	public String getDescription() {
		return this.metadata.getDescription();
	}
	
	public void setDescription(String html) {
		this.metadata.setDescription(html);
	}
	
	public String getProvenance() {
		return this.metadata.getProvenance();
	}
	
	public void setProvenance(String provenance) {
		this.metadata.setProvenance(provenance);
	}
	
	public String getAbstract() {
		return this.metadata.getAbstract();
	}
	
	public void setAbstract(String _abstract) {
		this.metadata.setAbstract(_abstract);
	}
	
	public String getRights() {
		return this.metadata.getRights();
	}
	
	public void setRights(String html) {
		this.metadata.setRights(html);
	}
	
	public String toJson() {
		JsonObject obj = (JsonObject) new Gson().toJsonTree(this);
		// Let's clean the empty metadata properties...
		JsonObject metadataObj = (JsonObject) obj.get("metadata");
		Iterator<Entry<String, JsonElement>> it = metadataObj.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, JsonElement> entry = it.next();
			JsonArray elt = (JsonArray) entry.getValue();
			if (elt.isEmpty()) {
				it.remove();
			}
		}
		return obj.toString();
	}
	
	public HttpEntity toHttpEntity() {
		return new StringEntity(toJson(), ContentType.APPLICATION_JSON);
	}
}
