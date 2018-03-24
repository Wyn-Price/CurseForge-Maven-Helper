package com.wynprice.curseforgemaven;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GradleProperties extends HashMap<String, List<String>>{
	
	private static final long serialVersionUID = 1L;
	
	public void put(String key, String value) {
		ArrayList<String> list = new ArrayList<>();
		list.add(value);
		super.put(key, list);
	}
	
	public void putQuote(String key, String value) {
		this.put(key, '"' + value + '"');
	}
	
	public List<String> getRemovedQuotes(Object key) {
		List<String> ret = new ArrayList<>();
		List<String> list = this.get(key);
		for(int i = 0; i < list.size(); i++) {
			String s = list.get(i);
			if(s.startsWith("\"") && s.endsWith("\"")) {
				s = s.substring(1, s.length() - 1);
			} 
			ret.add(s);
		}
		return ret;
	}
}
