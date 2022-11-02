package es.sistedes.wordpress.migrator.dsmodel;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.google.gson.Gson;

public class DSpaceEntity {
	
	// BEGIN: JSON fields
	protected String id;
	protected String uuid;
	protected String name;
	protected String handle;
	protected Metadata metadata = new Metadata(); 
	// END: JSON fields

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getUri() {
		return this.metadata.getUri();
	}
	
	public void setUri(String uri) {
		this.metadata.setUri(uri);
	}
	
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getHandle() {
		return handle;
	}
	
	public void setHandle(String handle) {
		this.handle = handle;
	}
	
	public String toJson() {
		return new Gson().toJson(this);
	}
	
	public HttpEntity toHttpEntity() {
		return new StringEntity(toJson(), ContentType.APPLICATION_JSON);
	}
	
}
