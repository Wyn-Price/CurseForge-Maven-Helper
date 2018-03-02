package com.wynprice.curseforgemaven;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class Main 
{
	
	public static final String version = "0.1.0";
	
	public static void main(String[] args)
	{
		try
		{
			Gui.launch(Gui.class, args);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void run(String url)
	{
		Gui.actiontarget.setText("");
		
		Gui.fakeURL.setText("");
		new Thread(() -> 
		{
			try {
				long millis = System.currentTimeMillis();

				ArrayList<CurseResult> resultList = actuallyRun(url, new ArrayList<>());
				if(!resultList.isEmpty()) {
					String output = "";
					for(CurseResult result : resultList) {
						output += "URL: " + result.getURL() + "\nGradle: " + result.getGradle() + "\n\n";
					}
					Gui.fakeURL.setText(output);
				}
				Gui.actiontarget.setText("Finished in " + (System.currentTimeMillis() - millis) + "ms");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}, "Curseforge Thread").start();;
	}
	
	private static ArrayList<CurseResult> actuallyRun(String url, ArrayList<CurseResult> list)  throws Exception
	{
		String[] splitUrl = url.split("/");
		if(splitUrl.length != 7 || !splitUrl[0].equals("https:") || !splitUrl[2].equals("minecraft.curseforge.com") || !splitUrl[3].equals("projects") || !splitUrl[5].equals("files") || !splitUrl[6].matches("\\d+")) {
			if(url.length() > 40)
				url = url.substring(0, 20) + "..." + url.substring(url.length() - 20, url.length());
			Gui.actiontarget.setText("Invalid URL: " + url);
			Gui.fakeURL.setText("Format: https://minecraft.curseforge.com/projects/examplemod/files/12345");
			return list;
		}
		
		Gui.actiontarget.setText("Resolving File - " + splitUrl[4]);
		
		String projectSlug = splitUrl[4];
		String urlRead = readURL(url);
		
		if(urlRead.contains("<h5>Required Library</h5>")) { //Contains Required files
			String[] libList = urlRead.split("<h5>Required Library</h5>")[1].split("<ul>")[1].split("</ul>")[0].split("<li class=\"project-tag\">");
			int times = 1;
			for(String lib : libList) {
				if(lib.split("<a href=\"").length > 1) {
					Gui.actiontarget.setText("Resolving Dependencies (" + times++ + "/" + (libList.length - 1) + ") - " + lib.split("<div class=\"project-tag-name overflow-tip\">")[1].split("<span>")[1].split("</span>")[0]);
					getLatestURL("https://minecraft.curseforge.com" + lib.split("<a href=\"")[1].split("\">")[0], urlRead.split("<h4>Supported Minecraft")[1].split("<ul>")[1].split("</ul>")[0].split("<li>")[1].split("</li>")[0], list);
				}
			}
		}

		String mavenArtifiactRaw = urlRead.split("<div class=\"info-data overflow-tip\">")[1].split("</div>")[0];
		mavenArtifiactRaw = mavenArtifiactRaw.substring(0, mavenArtifiactRaw.length() - 4);
		String[] splitArtifiact = mavenArtifiactRaw.split("-");
		String version = splitArtifiact[splitArtifiact.length - 2];
		String[] splitArtifiactNonVersion = new String[splitArtifiact.length - 2];
		String[] splitArtifactsVersion = new String[2];
		for(int i = 0; i < splitArtifiact.length; i++)
			if(i < splitArtifiact.length - 2)
				splitArtifiactNonVersion[i] = splitArtifiact[i];
			else
				splitArtifactsVersion[i - splitArtifiact.length + 2] = splitArtifiact[i];
				
		String mavenArtifiact = String.join("-", splitArtifiactNonVersion);
		if(splitArtifiact.length == 2) {
			mavenArtifiact = splitArtifiact[0];
			version = splitArtifiact[1];
		}
		
		String mavenClassifier = "";
		for(String artifact : mavenArtifiactRaw.split("-"))
			if(!artifact.equals(version) && !artifact.equals(mavenArtifiact))
				mavenClassifier += artifact + ":";
		if(mavenClassifier.length() > 0) {
			mavenClassifier = mavenClassifier.substring(0, mavenClassifier.length() - 1);
		}
		list.add(new CurseResult("https://minecraft.curseforge.com/api/maven/" + String.join("/", projectSlug, mavenArtifiact, version, mavenArtifiactRaw) + ".jar", projectSlug + ":" + String.join(":", mavenArtifiact, version) + (mavenClassifier.isEmpty() ? "" : ":") + mavenClassifier));
		return list;
	}
	
	private static void getLatestURL(String projectURL, String MCVersion, ArrayList<CurseResult> list) throws Exception {
		String urlRead = readURL(projectURL + "/files");
		String[] urlReadLibs = urlRead.split("<tr class=\"project-file-list-item\">");
		for(int i = 1; i < urlReadLibs.length; i++) {
			if(urlReadLibs[i].split("<span class=\"version-label\">")[1].split("</span>")[0].equals(MCVersion)) {
				actuallyRun("https://minecraft.curseforge.com" + urlReadLibs[i].split("<a class=\"overflow-tip twitch-link\" href=\"")[1].split("\"")[0], list);
				break;
			}
		}
	}
	
	private static String readURL(String url) throws Exception {
		InputStream urlStream = new URL(url).openStream();
		String urlRead = "";
		int len = urlStream.read();
		while(len != -1) {
			urlRead += (char)len;
			len = urlStream.read();
		}
		return urlRead;
	}
}
