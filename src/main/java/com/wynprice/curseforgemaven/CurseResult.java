package com.wynprice.curseforgemaven;

/**
 * A holder class for the results. Holds the URL and the Gradle.
 * @author Wyn Price
 *
 */
public class CurseResult 
{
	private final String URL;
	private final String gradle;
	
	public CurseResult(String URL, String gradle) 
	{
		this.URL = URL;
		this.gradle = gradle;
	}
	
	public String getGradle() {
		return gradle;
	}
	
	public String getURL() {
		return URL;
	}
	
	@Override
	public String toString() {
		return "URL:" + URL + "\nGRADLE: " + gradle + "\n";
	}
}
