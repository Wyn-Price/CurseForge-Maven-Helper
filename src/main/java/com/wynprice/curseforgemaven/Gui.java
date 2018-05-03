package com.wynprice.curseforgemaven;

import java.io.File;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * The GUI class
 * @author Wyn Price
 *
 */
public class Gui extends Application
{
	
	public static Text actiontarget = new Text();
	public static TextArea fakeURL = new TextArea();
	public static CheckBox useOptional = new CheckBox();
	
	@Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Curseforge Maven Helper v" + Main.version + " by Wyn Price");
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        
        Text scenetitle = new Text("Curseforge Maven Helper");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.TOP_CENTER);
        hbBtn.getChildren().add(scenetitle);
        grid.add(hbBtn, 0, 0, 2, 1);

        Label userName = new Label("URL:");
        grid.add(userName, 0, 1);

        TextField userTextField = new TextField();
        userTextField.setPrefWidth(400d);
        grid.add(userTextField, 1, 1);
        
        Label gradleFile = new Label("Gradle File:");
        grid.add(gradleFile, 0, 2);
        
        TextField fileTextField = new TextField();
        fileTextField.setPrefWidth(400d);
        grid.add(fileTextField, 1, 2);
        
        Button button = new Button();
        button.setText("Go");
        HBox hbbutton = new HBox(10);
        hbbutton.setAlignment(Pos.TOP_RIGHT);
        hbbutton.getChildren().add(button);
        grid.add(hbbutton, 2, 1, 1, 1);
        
        useOptional = new CheckBox("Include Optional Libraries");
        HBox hbradio = new HBox(10);
        hbradio.getChildren().add(useOptional);
        grid.add(hbradio, 1, 3);
                
        actiontarget = new Text();
		actiontarget.setFill(Color.FIREBRICK);
        grid.add(actiontarget, 1, 4);
        
        fakeURL = new TextArea();
		fakeURL.setStyle("-fx-text-fill: #f40000;");

        
        HBox fakeUrlBtn = new HBox(10);
        fakeUrlBtn.setAlignment(Pos.TOP_CENTER);
        fakeUrlBtn.getChildren().add(fakeURL);
        
        fakeUrlBtn.setPrefHeight(500d);
        grid.add(fakeUrlBtn, 0, 5, 3, 1);
        
        button.setOnAction((event) -> {
            Main.run(userTextField.getText(), new File(fileTextField.getText()));
        });
                
        Scene scene = new Scene(grid, 700, 700);

        scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if(key.getCode()==KeyCode.ENTER) {
                Main.run(userTextField.getText(), new File(fileTextField.getText()));
            }
        });
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
