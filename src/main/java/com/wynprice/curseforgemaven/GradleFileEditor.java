package com.wynprice.curseforgemaven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GradleFileEditor {
	
	/**
	 * Used to edit the gradle file
	 * @param file the {@code build.gradle} file
	 * @param resultList the list of results to append to the gradle file
	 */
	public static void editFile(File file, ArrayList<CurseResult> resultList) {
		GradleFileBlock overBlock;
		try {
			overBlock = GradleFileBlock.loadFromFile(file);
		} catch (IOException e) {
			handleException(e);
			return;
		}
		
		mavenapplication:
		{
			overBlock.getOrCreateChildren("repositories").get(0).getProperties();
			for(GradleFileBlock block : overBlock.getOrCreateChildren("repositories").get(0).getChildren("maven")) {
				GradleProperties props = block.getProperties();
				if(props.containsKey("url")) {
					for(String url : props.get("url")) {
						if(url.contains("minecraft.curseforge.com/api/maven")) {
							break mavenapplication;
						}
					}
				}
			}
			GradleFileBlock mavenblock = new GradleFileBlock(overBlock.getOrCreateChildren("repositories").get(0), "maven");
			GradleProperties props = mavenblock.getProperties();
			props.putQuote("name", "curseforge maven");
			props.putQuote("url", "https://minecraft.curseforge.com/api/maven");
		}
		List<String> set = new ArrayList<>();
		GradleFileBlock dep = overBlock.getOrCreateChildren("dependencies").get(0);	
		ArrayList<CurseResult> usedResults = new ArrayList<>();
		outer:
		for(String singleDep : dep.getProperties().getRemovedQuotes("deobfCompile")) {
			for(CurseResult result : resultList) {
				if(result.getGradle().split(":")[0].equals(singleDep.split(":")[0])) {
					usedResults.add(result);
					set.add(result.getGradle());
					continue outer;
				}
			}
			set.add(singleDep);
		}
		
		for(CurseResult result : resultList) {
			if(!usedResults.contains(result)) {
				set.add(result.getGradle());
			}
		}
		
		List<String> currentList = dep.getProperties().get("deobfCompile");
		currentList.clear();
		for(String singleDep : set) {
			currentList.add("\"" + singleDep + "\"");
		}		
	}
	
	
	private static void handleException(Exception e) {
		Gui.actiontarget.setText("Error" + e.getMessage());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps;
		try {
			ps = new PrintStream(baos, true, "utf-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return;
		}
		e.printStackTrace(ps);
		String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		ps.close();
		Gui.fakeURL.setText(content);
	}
}
