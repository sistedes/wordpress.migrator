package es.sistedes.wordpress.migrator.dsmodel;

public class MetadataEntry {

	// BEGIN: JSON fields
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
    
    public MetadataEntry(String value) {
    	this.value = value;
    	this.confidence = -1;
    }
    
    public MetadataEntry(String value, Integer place) {
    	this.value = value;
    	this.confidence = -1;
    	this.place = place;
    }
    
    public String getValue() {
		return value;
	}
}
