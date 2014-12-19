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
	if (super.containsKey(key)) {
	    return super.get(key);
	}
	return super.get(key.toLowerCase());
    }
    
    public boolean containsKey(String key) {
	if (super.containsKey(key) || super.containsKey(key.toLowerCase())) {
	    return true;
	}
	return false;
    }
}