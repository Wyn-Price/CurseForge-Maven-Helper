package com.wynprice.curseforgemaven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

public class GradleFileEditor {
	
	/**
	 * Used to edit the gradle file
	 * @param file the {@code build.gradle} file
	 * @param resultList the list of results to append to the gradle file
	 */
	public static void editFile(File file, ArrayList<CurseResult> resultList) {
		if(!file.exists()) {
			Gui.actiontarget.setText("Unable to find gradle file at: " + file.getAbsolutePath());
			return;
		}
		
		String readFile;
		try {
			readFile = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			handleException(e);
			return;
		}	
		
		GradleFileBlock overBlock = new GradleFileBlock(null, "", "");
		
		GradleFileBlock currentBlock = overBlock;
		for(char c : readFile.toCharArray()) {
			if(c == '{') {
				String[] sarray = currentBlock.content.split(" ");
				String[] nSArray = sarray[sarray.length - 1].split("\n");
				int spaces = 0;
				char[] aChar = currentBlock.content.toCharArray();
				for(int i = aChar.length; i > 0; i--) {
					char c1 = aChar[i - 1];
					if(c1 != ' ') {
						break;
					} else {
						spaces ++;
					}
				}
				String name = nSArray[nSArray.length - 1];
				String trimmedName = name.replace("\t", "").trim();
				currentBlock.content = currentBlock.content.substring(0, currentBlock.content.length() - name.length() - spaces);
				currentBlock.content += "<!wyn!" + trimmedName + ">";
				currentBlock = new GradleFileBlock(currentBlock, name, trimmedName);
			} else if(c == '}') {
				currentBlock = currentBlock.parent;
			} else {
				currentBlock.content += c;
			}
		}	
		
	}
	
	private static class GradleFileBlock {
		public final ArrayList<GradleFileBlock> children = new ArrayList<>();
		public String content = "";
		public final GradleFileBlock parent;
		public final String name;
		public final String blockName;
				
		public GradleFileBlock(GradleFileBlock parent, String blockName, String name) {

			this.parent = parent;
			this.name = name;
			this.blockName = blockName;
			if(this.parent != null) {
				this.parent.children.add(this);
			}
		}
		
		public GradleFileBlock getChild(String childName) {
			for(GradleFileBlock block : children) {
				if(block.name.equalsIgnoreCase(childName)) {
					return block;
				}
			}
			return null;
		}
		
		@Override
		public String toString() {
			String ret = content;
			String[] aString = content.split("<!wyn!");
			for(int i = 1; i < aString.length; i++) {
				ret = ret.replace("<!wyn!" + aString[i].split(">")[0] + ">", String.valueOf(getChild(aString[i].split(">")[0]).toString()));
			}
			if(parent != null) {
				ret = blockName + " {" + ret + "}";
			}
			
			return ret;
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
