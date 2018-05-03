package com.wynprice.curseforgemaven;

import java.util.ArrayList;
import java.util.HashMap;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class OptionalDownloadGui extends Application{

	private final HashMap<String, CheckBox> downloadmap = new HashMap<>();
	
	private ArrayList<String> optionsToDownload;
	
	private final String name;
	
	public OptionalDownloadGui(String name, ArrayList<String> downloads) {
		for(String down : downloads) {
			CheckBox button = new CheckBox(down);
			button.setSelected(true);
			downloadmap.put(down, button);
		}
		this.name = name;
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		VBox vb = new VBox();
        VBox box = new VBox();
		ScrollPane sp = new ScrollPane();
		vb.setPadding(new Insets(10));
		Scene scene = new Scene(box, 500, 300);
		stage.setScene(scene);
		stage.setTitle("Download Optional Libs for " + name);
        box.getChildren().addAll(sp);
        box.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(sp, Priority.ALWAYS);
 
        for(String s : downloadmap.keySet()) {
        	vb.getChildren().add(downloadmap.get(s));
        }
        
        sp.setVmax(440);
        sp.setPrefSize(115, 150);
        sp.setContent(vb);
        
        Button out = new Button("Continue");
        out.setOnAction((event) -> {
        	runResults();
        	stage.close();
        });
        box.getChildren().add(out);
        out.setPrefWidth(Double.MAX_VALUE);
		stage.setOnCloseRequest((handle) -> runResults());
		scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if(key.getCode()==KeyCode.ENTER) {
            	runResults();
            	stage.close();
            }
        });
		stage.show();
	}
	
	private void runResults() {
		ArrayList<String> options = new ArrayList<>();
		for(String key : downloadmap.keySet()) {
			if(downloadmap.get(key).isSelected()) {
				options.add(key);
			}
		}
		this.optionsToDownload = options;
	}
	
	synchronized public ArrayList<String> getOptionsToDownload() {
		return optionsToDownload;
	}

}
