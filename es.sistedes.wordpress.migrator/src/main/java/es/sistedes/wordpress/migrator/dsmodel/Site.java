package es.sistedes.wordpress.migrator.dsmodel;

public class Site extends DSpaceEntity {

	private String getHandlePrefix() {
		return getHandle().split("/")[0];
	}

	public String getUri() {
		return "https://hdl.handle.net/" + getHandle();
	}
	
	public String getBaseUri() {
		return 	"https://hdl.handle.net/" + getHandlePrefix();
	}
}