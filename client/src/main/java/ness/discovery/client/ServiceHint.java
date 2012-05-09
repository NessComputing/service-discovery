package ness.discovery.client;

/**
 * Opaque directive to the {@link ServiceLocator} which affects the
 * service discovery process.
 *
 * @author steven
 */
public class ServiceHint {
	public static final String VERSION_HINT = "Version";
	public static final String QUALIFIER_HINT = "Qualifier";
	public static final String CONSISTENTHASH_HINT = "ConsistentHash";
	private final String name;
	private final String value;
    
	ServiceHint(String name, String value) {
    	this.name = name;
    	this.value = value;
    }

	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
	
    /*
     * Factory methods
     */
	
    /**
     * Select a service with a particular qualifier
     */
    public static ServiceHint withQualifier(String qualifier) {
        return new ServiceHint(QUALIFIER_HINT, qualifier);
    }

    public static ServiceHint withVersion(int version) {
        return new ServiceHint(VERSION_HINT, String.valueOf(version));
    }
    
    /** Selects a service that serves the requested hash key.
     * 
     * @param hashKey (example: user id, or another consistent identifier)
     * @return
     */
    public static ServiceHint servesKey(String hashKey) {
    	return new ServiceHint(CONSISTENTHASH_HINT, hashKey);
    }
}
