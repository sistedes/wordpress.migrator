package es.sistedes.wordpress.migrator.dsmodel;

public class DublinCoreBasic {

	// BEGIN: JSON fields
	@SuppressWarnings("unused")
	private String value;
	
    @SuppressWarnings("unused")
	private String language;
    
    @SuppressWarnings("unused")
	private String authority;
    
    @SuppressWarnings("unused")
	private Integer confidence;

    @SuppressWarnings("unused")
    private Integer place;
    // END: JSON fields
    
    public DublinCoreBasic(String value) {
    	this.value = value;
    	this.confidence = -1;
    }
    
    public DublinCoreBasic(String value, Integer place) {
    	this.value = value;
    	this.confidence = -1;
    	this.place = place;
    }
    
    public String getValue() {
		return value;
	}
}
