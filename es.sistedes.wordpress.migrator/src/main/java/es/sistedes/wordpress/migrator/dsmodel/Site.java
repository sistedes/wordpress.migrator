package es.sistedes.wordpress.migrator.dsmodel;

public class Site extends AbstractDSpaceEntity {

	public String getHandlePrefix() {
		return handle.split("/")[0];
	}

	public String getUri() {
		return "https://hdl.handle.net/" + handle;
	}
	
	public String getBaseUri() {
		return 	"https://hdl.handle.net/" + getHandlePrefix();
	}
}