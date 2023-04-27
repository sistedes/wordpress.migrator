package es.sistedes.wordpress.migrator.wpmodel;

public abstract class Document {
	
	public static enum License {
		CC_BY("CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/"),
		CC_BY_NC("CC BY-NC 4.0", "https://creativecommons.org/licenses/by-nc/4.0/"),
		CC_BY_NC_SA("CC BY-NC-SA 4.0", "https://creativecommons.org/licenses/by-nc-sa/4.0/"),
		CC_BY_NC_ND("CC BY-NC-ND 4.0", "https://creativecommons.org/licenses/by-nc-nd/4.0/"),
		CC_BY_SA("CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/"),
		CC_BY_ND("CC BY-ND 4.0", "https://creativecommons.org/licenses/by-nd/4.0/"),
		RESTRICTED("Restringida", "N/A"),
		PUBLISHED("Ya Publicado", "N/A"),
		UNKNOWN("Unknown", "N/A");
		
		private String name;
		private String url;
		
		private License(String name, String url){
			this.name = name;
			this.url = url;
		}
		
		public String getName() {
			return name;
		}

		public String getUrl() {
			return url;
		}
		
		public static License from(String longName) {
			// We include here some hardcoded strings, because we 
			// can find a lot of shit in the current Wordpress Library
			// which supposedly means CC-BY
			if ("CreativeCommons".equals(longName) || "1".equals(longName)) {
				return License.CC_BY;
			}
			for (License license : License.values()) {
	            if (license.name.equals(longName)) {
	                return license;
	            }
	        }
			return UNKNOWN;
		}
	}
	
	public abstract String getHandle();
	
	public abstract String getHandleUri();
	
	public abstract String getDocumentUrl();
	
	public abstract String getTitle();
	
	public abstract String getDescription();

	public abstract String getAbstract();
	
}