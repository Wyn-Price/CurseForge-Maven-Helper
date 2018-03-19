package com.wynprice.curseforgemaven;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * The main calculation class.
 * @author Wyn Price
 *
 */
public class Main 
{
	
	public static final String version = "0.2.0";
	
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
	
	private static boolean useOptional = false;
	
	/**
	 * Used to run {@link #calculate(String, ArrayList)} in cleaner way, and to output the results.
	 * @param url The projects file url to use
	 */
	public static void run(String url)
	{
		
		useOptional = Gui.useOptional.isSelected();
		
		Gui.actiontarget.setText("");
		
		Gui.fakeURL.setText("");
		new Thread(() -> 
		{
			try {
				long millis = System.currentTimeMillis();

				ArrayList<CurseResult> resultList = calculate(url, new ArrayList<>());
				if(!resultList.isEmpty()) {
					String urlOutput = "URL:\n";
					String forgeGradleOutput = "Gradle:\n";
					for(CurseResult result : resultList) {
						urlOutput += result.getURL() + "\n";
						forgeGradleOutput += "deobfCompile \"" + result.getGradle() + "\"\n";
					}
					Gui.fakeURL.setText(urlOutput + "\n\n" + forgeGradleOutput);
				}
				Gui.actiontarget.setText("Finished in " + (System.currentTimeMillis() - millis) + "ms");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}, "Curseforge Thread").start();;
	}
	
	/**
	 * The Main calculation method. 
	 * @param url The projects file url to use.
	 * @param list The list of which to add the results to. Will also be returned
	 * @return {@code list}
	 */
	public static ArrayList<CurseResult> calculate(String url, ArrayList<CurseResult> list)  throws Exception
	{
		for(CurseResult result : list) {
			if(result.getGradle().split(":")[0].equalsIgnoreCase(url.split("/")[4])) {
				return list;
			}
		}
		String[] splitUrl = url.split("/");
		if(splitUrl.length != 7 || !splitUrl[0].equals("https:") || !splitUrl[2].equals("minecraft.curseforge.com") || !splitUrl[3].equals("projects") || !splitUrl[5].equals("files") || !splitUrl[6].matches("\\d+")) {
			if(url.length() > 40) {
				url = url.substring(0, 20) + "..." + url.substring(url.length() - 20, url.length());
			}
			Gui.actiontarget.setText("Invalid URL: " + url);
			Gui.fakeURL.setText("Format: https://minecraft.curseforge.com/projects/examplemod/files/12345");
			return list;
		}
		
		Gui.actiontarget.setText("Resolving File - " + splitUrl[4]);
		
		String projectSlug = splitUrl[4];
		String urlRead = readURL(url, true);
		
		downloadLibraries(urlRead, "Required Library", "Dependencies", list);
		downloadLibraries(urlRead, "Include", "Dependencies", list);		
		if(useOptional) {
			downloadLibraries(urlRead, "Optional Library", "Optional Library", list);
		}

		String mavenArtifiactRaw = urlRead.split("<div class=\"info-data overflow-tip\">")[1].split("</div>")[0];
		mavenArtifiactRaw = mavenArtifiactRaw.substring(0, mavenArtifiactRaw.length() - 4);
		if(!mavenArtifiactRaw.endsWith("-dev")) {
			String[] devValues = getMavenValues(projectSlug, mavenArtifiactRaw + "-dev");
			try
			{
				readURL("https://minecraft.curseforge.com/api/maven/" + String.join("/", projectSlug, devValues[0], devValues[1], mavenArtifiactRaw + "-dev") + ".jar", false);
				Gui.actiontarget.setText("Resolving File - " + splitUrl[4] + " - Dev Version");
				mavenArtifiactRaw += "-dev";
			}
			catch (Exception e) 
			{
				;
			}
		}
		
		String[] values = getMavenValues(projectSlug, mavenArtifiactRaw);
		list.add(new CurseResult(
				"https://minecraft.curseforge.com/api/maven/" + String.join("/", projectSlug, values[0], values[1], mavenArtifiactRaw) + ".jar",
				projectSlug + ":" + String.join(":", values[0], values[1]) + (values[2].isEmpty() ? "" : ":") + values[2]));
		return list;
	}
	
	/**
	 * Gets the Related Project files and downloades them
	 * @param urlRead The read URL
	 * @param splitterText the Name of the project type (Required Library, Embedded Library, stuff like that). <b>THIS MUST BE WHAT IS DISPLAYED ON CURSEFORGE</b>
	 * @param guiDisplay The text to display to the GUI
	 * @param list The list of results
	 */
	private static void downloadLibraries(String urlRead, String splitterText, String guiDisplay, ArrayList<CurseResult> list) throws Exception {
		if(urlRead.contains("<h5>" + splitterText + "</h5>")) {
			String[] libList = urlRead.split("<h5>" + splitterText + "</h5>")[1].split("<ul>")[1].split("</ul>")[0].split("<li class=\"project-tag\">");
			int times = 1;
			for(String lib : libList) {
				if(lib.split("<a href=\"").length > 1) {
					Gui.actiontarget.setText("Resolving " + guiDisplay + " (" + times++ + "/" + (libList.length - 1) + ") - " + lib.split("<div class=\"project-tag-name overflow-tip\">")[1].split("<span>")[1].split("</span>")[0]);
					addLatestToList("https://minecraft.curseforge.com" + lib.split("<a href=\"")[1].split("\">")[0], urlRead.split("<h4>Supported Minecraft")[1].split("<ul>")[1].split("</ul>")[0].split("<li>")[1].split("</li>")[0], list, 0);
				}
			}
		}
	}
	
	/**
	 * Gets the maven values from the file name and projectSlug
	 * <br>{
	 * <br>&nbsp&nbsp&nbspmavenArtifiact,
	 * <br>&nbsp&nbsp&nbspversion,
	 * <br>&nbsp&nbsp&nbspmavenClassifier
	 * <br>}
	 */
	private static String[] getMavenValues(String projectSlug, String fileName) {
		String[] splitArtifiact = fileName.split("-");
		String version = splitArtifiact[splitArtifiact.length - 2];
		String[] splitArtifiactNonVersion = new String[splitArtifiact.length - 2];
		for(int i = 0; i < splitArtifiact.length; i++) {
			if(i < splitArtifiact.length - 2) {
				splitArtifiactNonVersion[i] = splitArtifiact[i];
			}
		}
				
		String mavenArtifiact = String.join("-", splitArtifiactNonVersion);
		if(splitArtifiact.length == 2) {
			mavenArtifiact = splitArtifiact[0];
			version = splitArtifiact[1];
		}
		
		String mavenClassifier = "";
		for(String artifact : fileName.split("-")) {
			if(!artifact.equals(version) && !mavenArtifiact.contains(artifact)) {
				mavenClassifier += artifact + ":";
			}
		}
		if(mavenClassifier.length() > 0) {
			mavenClassifier = mavenClassifier.substring(0, mavenClassifier.length() - 1);
		}
		return new String[] {mavenArtifiact, version, mavenClassifier};
	}
	
	/**
	 * Used to get the latest file from a curseforge project, of a particular minecraft version, then add it to the {@code list}
	 * @param projectURL The projects url page. This should be the homepage, for example {@link https://minecraft.curseforge.com/projects/secretroomsmod}
	 * @param MCVersion The minecraft version to use to get the latest version
	 * @param list A list to add the result to
	 * @param page The files page to track. If you're calling this, the page should be 0 or 1. 
	 */
	private static void addLatestToList(String projectURL, String MCVersion, ArrayList<CurseResult> list, int page) throws Exception {
		String urlRead = readURL(projectURL + "/files?page=" + page, true);
		if(urlRead.split("<span class=\"b-pagination-item s-active active\">").length > 1 && Integer.valueOf(urlRead.split("<span class=\"b-pagination-item s-active active\">")[1].split("</span>")[0]) < page) {
			return;
		}
		String[] urlReadLibs = urlRead.split("<tr class=\"project-file-list-item\">");
		for(int i = 1; i < urlReadLibs.length; i++) {
			if(urlReadLibs[i].split("<span class=\"version-label\">")[1].split("</span>")[0].equals(MCVersion)) {
				calculate("https://minecraft.curseforge.com" + urlReadLibs[i].split("<a class=\"overflow-tip twitch-link\" href=\"")[1].split("\"")[0], list);
				return;
			}
		}
		addLatestToList(projectURL, MCVersion, list, page++);
	}
	
	/**
	 * Used to get the data a URL holds.
	 * @param url The url to uses
	 * @param simulate Should the program <u>actually</u> download the file
	 * @return the data that the url points to. Usally a webpage
	 */
	private static String readURL(String url, boolean simulate) throws Exception {
		InputStream urlStream = new URL(url).openStream();
		String urlRead = "";
		int len = urlStream.read();
		if(simulate) {
			while(len != -1) {
				urlRead += (char)len;
				len = urlStream.read();
			}
		}
		return urlRead;
	}
}
