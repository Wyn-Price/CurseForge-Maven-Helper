package com.wynprice.curseforgemaven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * The main calculation class.
 * @author Wyn Price
 *
 */
public class Main 
{
	
	public static final String version = "0.3.0";
	
	public static void main(String[] args)
	{
		try {
			Gui.launch(Gui.class, args);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static boolean useOptional = false;
	
	private static ArrayList<CurseResult> prevList = new ArrayList<>();
	
	/**
	 * Used to run {@link #calculate(String, ArrayList)} in cleaner way, and to output the results.
	 * @param url The projects file url to use
	 * @param file the build.gradle file to edit, if any
	 */
	public static void run(String url, File file)
	{
		
		useOptional = Gui.useOptional.isSelected();
		
		Gui.actiontarget.setText("");
		
		Gui.fakeURL.setText("");
		Thread thread = new Thread(() -> 
		{
			try {
				long millis = System.currentTimeMillis();
				String[] splitUrl = url.split("/");
		        if(splitUrl.length != 7 || !splitUrl[0].equals("https:") || !splitUrl[2].equals("minecraft.curseforge.com") || !splitUrl[3].equals("projects") || !splitUrl[5].equals("files") || !splitUrl[6].matches("\\d+")) {
		        	if(!prevList.isEmpty()) {
		            	if(file.exists() && file.getName().equals("build.gradle")) {
		            		GradleFileEditor.editFile(file, prevList);
		            	}
		            }
		        }
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
				if(!resultList.isEmpty()) {
					prevList = resultList;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, "Curseforge Thread");
				
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
     * Used to calculate the gradle and downloads from a url
     * @param url The URL
     * @param list The list of current
     * @return {@code list}
     */
    public static ArrayList<CurseResult> calculate(String url, ArrayList<CurseResult> list) throws IOException
    {
        for(CurseResult result : list) {
            if(result.getGradle().split(":")[0].equalsIgnoreCase(url.split("/")[4])) {
                return list;
            }
        }
        String[] splitUrl = url.split("/");
        if(splitUrl.length != 7 || !splitUrl[0].equals("https:") || !splitUrl[2].equals("minecraft.curseforge.com") || !splitUrl[3].equals("projects") || !splitUrl[5].equals("files") || !splitUrl[6].matches("\\d+")) {
        	if(!prevList.isEmpty()) {
            	File file = new File(url);
            	if(file.exists() && file.getName().equals("build.gradle")) {
            		GradleFileEditor.editFile(file, prevList);
            	}
            }
        	if(url.length() > 40) {
                url = url.substring(0, 20) + "..." + url.substring(url.length() - 20, url.length());
            }
            Gui.actiontarget.setText("Invalid URL: " + url);
			Gui.fakeURL.setText("Format: https://minecraft.curseforge.com/projects/examplemod/files/12345");
            return list;
        }
        
        Gui.actiontarget.setText("Resolving File - `" + splitUrl[4] + "`");
        
        String projectSlug = splitUrl[4];
        Document urlRead = getDocument(url);
        downloadLibraries(urlRead, "Required Library", "Dependencies", list);
        downloadLibraries(urlRead, "Include", "Dependencies", list);        
        if(useOptional) {
        	downloadLibrariesWithGui(urlRead, "Optional Library", "Optional Library", list);
        }

        String mavenArtifiactRaw = urlRead.select("div.info-data").get(0).html();
        mavenArtifiactRaw = mavenArtifiactRaw.substring(0, mavenArtifiactRaw.length() - 4);
        if(!mavenArtifiactRaw.endsWith("-dev")) {
            String[] devValues = getMavenValues(projectSlug, mavenArtifiactRaw + "-dev");
            try
            {
                getDocument("https://minecraft.curseforge.com/api/maven/" + String.join("/", projectSlug, devValues[0], devValues[1], mavenArtifiactRaw + "-dev") + ".jar");
                Gui.actiontarget.setText("Resolving File - `" + splitUrl[4] + "` - Dev Version");
                mavenArtifiactRaw += "-dev";
            }
            catch (Exception e) 
            {
                //Dont worry if exception is thrown here, this is just to check if the dev version exists. If it dosnt then just continue with the normal version
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
    private static void downloadLibraries(Document urlRead, String splitterText, String guiDisplay, ArrayList<CurseResult> list) throws IOException {
    	Elements outerElements = urlRead.select("h5:containsOwn(" + splitterText + ")");
    	if(!outerElements.isEmpty()) {
    		Elements elementList = urlRead.select("h5:containsOwn(" + splitterText + ") + ul").select("a");
    		int times = 1;
    		String mcVersion = urlRead.select("h4:containsOwn(Supported Minecraft) + ul").select("li").html().split("\n")[0];
    		for(Element element : elementList) {
    			addLatestToList("https://minecraft.curseforge.com" + element.attr("href"), mcVersion, guiDisplay + " (" + times++ + "/" + elementList.size()  + ") - `" + element.select("div.project-tag-name").select("span").html() + "`", list, 0, -1);
    		}
    	}
    }
    
    private static void downloadLibrariesWithGui(Document urlRead, String splitterText, String guiDisplay, ArrayList<CurseResult> list) throws IOException {
    	Elements outerElements = urlRead.select("h5:containsOwn(" + splitterText + ")");
    	if(!outerElements.isEmpty()) {
    		Elements elementList = urlRead.select("h5:containsOwn(" + splitterText + ") + ul").select("a");
    		int times = 1;
    		String mcVersion = urlRead.select("h4:containsOwn(Supported Minecraft) + ul").select("li").html().split("\n")[0];
    		ArrayList<String> slist = new ArrayList<>();
    		for(Element element : elementList) {
    			slist.add(element.select("div.project-tag-name").select("span").html());
    		}
    		String fileName = urlRead.select("div.info-data").get(0).html();
    		final OptionalDownloadGui gui = new OptionalDownloadGui(fileName, slist);
    		Platform.runLater(()->{
                Stage secondStage = new Stage();
                try {
    				gui.start(secondStage);
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
                secondStage.show();
             });
    		
    		Gui.actiontarget.setText("Awaiting results from gui for " + fileName);
    		
    		while(gui.getOptionsToDownload() == null) {
    			;
    		}
    		
    		Gui.actiontarget.setText("Processing results from gui for " + fileName);

    		for(Element element : elementList) {
    			String elementName = element.select("div.project-tag-name").select("span").html();
    			if(gui.getOptionsToDownload().contains(elementName)) {
        			addLatestToList("https://minecraft.curseforge.com" + element.attr("href"), mcVersion, guiDisplay + " (" + times++ + "/" + gui.getOptionsToDownload().size()  + ") - `" + element.select("div.project-tag-name").select("span").html() + "`", list, 0, -1);
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
     * @param guiMessage The message used for the GUI
     * @param list A list to add the result to
     * @param page The files page to track. If you're calling this, the page should be 0 or 1. 
     * @param maxpages The max amount of pages. Should be -1 if calling this
     */
    private static void addLatestToList(String projectURL, String MCVersion, String guiMessage, ArrayList<CurseResult> list, int page, int maxpages) throws IOException {
        if(page == 0) {
            page = 1;
        }
        if(projectURL.split("/")[4].matches("\\d+")) {
            try {
                URLConnection con = new URL(projectURL).openConnection(); //Used to convert the project id to project slug
                con.connect();
                InputStream is = con.getInputStream();
                projectURL = con.getURL().toString();
                is.close();
            } catch (IOException e) {
            	throw e;
            }
        }
        
        
        Gui.actiontarget.setText("Resolving " + guiMessage + (maxpages < 0 ? "" : ". Page " + page + "/" + maxpages));
        
        Document urlRead = getDocument(projectURL + "/files?page=" + page);
        Elements pageNumberElements = urlRead.select("ul.b-pagination-list, .paging-list, .j-tablesorter-pager, .j-listing-pagination").select("li.b-pagination-item");
        try {
        	if(pageNumberElements.size() >= 2) {
            	maxpages = Integer.valueOf(pageNumberElements.get(pageNumberElements.size() - 2).select("a").html());
        	}
        } catch (NumberFormatException e) {
        	
		}
        Elements pageElement = urlRead.select("span.b-pagination-item").select("span.s-active");
        if(!pageElement.isEmpty()) {
            try {
                if(Integer.valueOf(pageElement.html()) < page) {
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Enable to parse value of " + pageElement.html() + " to an int. Returning search on page " + page);
                return;
            }
        }

        Elements libElements = urlRead.select("tr.project-file-list-item");

        for(Element element : libElements) {
            boolean isCorrectVersion = MCVersion.equalsIgnoreCase(element.select("span.version-label").html()); 
            if(!isCorrectVersion) {
                for(String version : element.select("span.additional-versions").attr("title").replace("<div>", "").split("</div>")) { //Get the list of extra hidden versions
                    if(MCVersion.equalsIgnoreCase(version)) {
                        isCorrectVersion = true;
                        break;
                    }
                }

            }
            if(isCorrectVersion) {
                calculate("https://minecraft.curseforge.com" + element.select("a.twitch-link").attr("href"), list);
                return;
            }
        }
        
        addLatestToList(projectURL, MCVersion, guiMessage, list, page+=1, maxpages);
    }
    
    @SuppressWarnings("static-access")
	private static Document getDocument(String url) throws IOException {
        Document ret = null;
        while (ret == null) {
            try {
                ret = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1").get();
            } catch (SocketTimeoutException e) {
                System.out.println("Caught timeout loading URL: " + url);
                System.out.println("Retrying in 5 seconds...");
                try {
					Thread.currentThread().sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
            } 
        }
        return ret;
    }

}
