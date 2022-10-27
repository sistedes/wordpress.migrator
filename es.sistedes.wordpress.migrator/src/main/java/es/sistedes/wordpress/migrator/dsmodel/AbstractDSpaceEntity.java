package es.sistedes.wordpress.migrator.dsmodel;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.google.gson.Gson;

public abstract class AbstractDSpaceEntity {
	
	// BEGIN: JSON fields
	protected String id;
	protected String name;
	protected String handle;
	protected Metadata metadata = new Metadata(); 
	// END: JSON fields

	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getUri() {
		return this.metadata.getUri();
	}
	
	public String toJson() {
		return new Gson().toJson(this);
	}
	
	public HttpEntity toHttpEntity() {
		return new StringEntity(toJson(), ContentType.APPLICATION_JSON);
	}
	
}
