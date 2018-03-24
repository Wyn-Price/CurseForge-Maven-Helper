package com.wynprice.curseforgemaven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to read/write to the "build.gradle" file
 * @author Wyn Price
 *
 */
public class GradleFileBlock {
	/**
	 * All Children registered to this block
	 */
	private final ArrayList<GradleFileBlock> children = new ArrayList<>();
	
	/**
	 * The order of the properties
	 */
	private final ArrayList<String> keyOrder = new ArrayList<>();
	
	/**
	 * The content of this block
	 */
	private String content = "";
	/**
	 * The parent of this block, or null if there is none
	 */
	private final GradleFileBlock parent;
	/**
	 * The name of this block
	 */
	private final String name;
			
	/**
	 * The index of the block. Is practically how many parents this block has.
	 */
	private final int index;
	
	private GradleProperties properties;

	private boolean hasInit;
	
	/**
	 * Load a GradleFileBlock from a file
	 * @param file The file to use 
	 * @return The GradleFileBlock
	 * @throws IOException if an I/O error occurs reading from the stream
	 */
	public static GradleFileBlock loadFromFile(File file) throws IOException {
		GradleFileBlock currentBlock = new GradleFileBlock(null, "");
		for(char c : new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).toCharArray()) {
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
				currentBlock = new GradleFileBlock(currentBlock, trimmedName);
			} else if(c == '}') {
				currentBlock = currentBlock.parent;
			} else {
				currentBlock.content += c;
			}
		}	
		currentBlock.initProperties();
		return currentBlock;
	}
	
	public GradleFileBlock(GradleFileBlock parent, String name) {
		this.parent = parent;
		this.name = name;
		if(parent != null) {
			index = parent.index + 1;
			parent.addChild(this);
		} else {
			index = 0;
		}
		this.properties = new GradleProperties();
	}
	
	/**
	 * Get the list of children with the same name from this block. 
	 * @param childName The name of the blocks to look for
	 * @return A list containing all the blocks, or an empty list if none could be found
	 */
	public ArrayList<GradleFileBlock> getChildren(String childName) {
		ArrayList<GradleFileBlock> retList = new ArrayList<>();
		for(GradleFileBlock block : children) {
			if(block.name.equalsIgnoreCase(childName)) {
				retList.add(block);
			}
		}
		return retList;
	}
	
	/**
	 * Gets the list of children by name from this block. If none could be found, a new block is created.
	 * @param childName The childs name
	 * @return A list containing all the blocks, or a new list if none could be found
	 */
	public ArrayList<GradleFileBlock> getOrCreateChildren(String childName) {
		ArrayList<GradleFileBlock> retList = getChildren(childName);
		if(retList.isEmpty()) {
			GradleFileBlock ret = new GradleFileBlock(this, childName);
			retList.add(ret);
		}
		return retList;
	}
	
	/**
	 * Get the properties of the content of this block. 
	 * <b>If the property is a function, it will be put as the key, with an empty list
	 * @return A copy of the properties
	 */
	public GradleProperties getProperties()  {
		return properties;
	}
	
	@Override
	public String toString() {
		String content = ""; 
		GradleProperties props = getProperties();
		ArrayList<String> keyOrder = new ArrayList<>(this.keyOrder);
		for(String key : props.keySet()) {
			if(!keyOrder.contains(key)) {
				keyOrder.add(key);
			}
		}
		for(String key : keyOrder) {
			List<String> values = props.get(key);
			if(values.isEmpty()) {
				content += getTabs() + key;
				continue;
			}
			for(String value : values) {
				content += getTabs() + key + " " + value + "\n";
			}
		}
		String ret = content;
		String[] aString = content.split("<!wyn!");
		for(int i = 1; i < aString.length; i++) {
			int code = Integer.valueOf(aString[i].split(">")[0]);
			for(GradleFileBlock block : children) {
				if(block.hashCode() == code) {
					ret = ret.replace("<!wyn!" + aString[i].split(">")[0] + ">", block.toString());
					break;
				}
			}
		}
		if(parent != null) {
			ret = name + " {\n" + ret + parent.getTabs() + "}";
		}
		
		return ret;
	}

	private void initProperties() {
		GradleProperties props = new GradleProperties();
		for(String line : content.split("\n")) {
			line = line.trim();
			if(line.isEmpty()) {
				continue;
			}
			String key = "";
			String value = "";
			boolean hasFoundMarker = false;
			for(char c : line.toCharArray()) {
				if(c == ' ' || c == '=') {
					hasFoundMarker = true;
				}
				if(hasFoundMarker) {
					value += c;
				} else {
					key += c;
				}
			}
			
			if(!hasFoundMarker && !(line.startsWith("<!wyn!") && line.endsWith(">"))) {
				keyOrder.add(line);
				props.put(line, new ArrayList<>());
			}
			
			key = key.trim();
			value = value.trim();
			keyOrder.remove(key);
			keyOrder.add(key);
			if(!props.containsKey(key)) {
				props.put(key, new ArrayList<>());
			}
			
			props.get(key).add(value);
		}
		this.properties = props;
		hasInit = true;
		children.forEach(child -> child.initProperties());
	}
	
	private String getTabs() {
		String tabs = "";
		for(int i = 0; i < index; i++) {
			tabs += "\t";
		}
		return tabs;
	}
	
	private void addChild(GradleFileBlock child) {
		this.children.add(child);
		String name = "<!wyn!" + child.hashCode() + ">";
		if(hasInit) {
			getProperties().put(name, new ArrayList<>());
			keyOrder.add(name);
			getProperties().put("\n", new ArrayList<>());
			keyOrder.add("\n");
			
		} else {
			this.content += name;

		}
	}
}
