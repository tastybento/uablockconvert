package com.wasteofplastic.uablockconverter;

import java.util.HashMap;

@SuppressWarnings("serial")
public class CaseInsensitiveMap extends HashMap<String, Players> {

    @Override
    public Players put(String key, Players value) {
	return super.put(key.toLowerCase(), value);
    }

    // not @Override because that would require the key parameter to be of type Object
    public Players get(String key) {
	return super.get(key.toLowerCase());
    }
}