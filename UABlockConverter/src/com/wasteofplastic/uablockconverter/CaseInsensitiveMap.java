package com.wasteofplastic.uablockconverter;

import java.util.HashMap;

/**
 * Stores a map in a way that can be retrieved in any way lower case or upper
 * @author tastybento
 *
 */
@SuppressWarnings("serial")
public class CaseInsensitiveMap extends HashMap<String, Players> {

    @Override
    public Players put(String key, Players value) {
	return super.put(key, value);
    }

    // not @Override because that would require the key parameter to be of type Object
    public Players get(String key) {
	// If the key supplied is actually correct in terms of case - this will be provided
	if (super.containsKey(key)) {
	    return super.get(key);
	}
	// If the case-sensitive one is not known, the all lower-case one will be provided
	return super.get(key.toLowerCase());
    }
    /*
    public boolean containsKey(String key) {
	if (super.containsKey(key) || super.containsKey(key.toLowerCase())) {
	    return true;
	}
	return false;
    }
    */
}