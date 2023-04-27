package es.sistedes.wordpress.migrator.dsmodel;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.google.gson.Gson;

public class DSpaceEntity {
	
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

	public String getSistedesHandle() {
		return this.metadata.getUri().replaceAll("https?://hdl.handle.net/", "");
	}
	
	public String getUri() {
		return this.metadata.getUri();
	}
	
	public void setUri(String uri) {
		this.metadata.setUri(uri);
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
		return new Gson().toJson(this);
	}
	
	public HttpEntity toHttpEntity() {
		return new StringEntity(toJson(), ContentType.APPLICATION_JSON);
	}
}
