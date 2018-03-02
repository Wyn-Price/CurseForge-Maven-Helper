package com.wynprice.curseforgemaven;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

public class Gui extends Application
{
	
	public static Text actiontarget = new Text();
	public static TextArea fakeURL = new TextArea();

	
	@Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Curseforge API helper + v" + Main.version);
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Text scenetitle = new Text("Curseforge thing");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.TOP_CENTER);
        hbBtn.getChildren().add(scenetitle);
        grid.add(hbBtn, 0, 0, 2, 1);

        Label userName = new Label("URL:");
        grid.add(userName, 0, 1);

        TextField userTextField = new TextField();
        userTextField.setPrefWidth(300f);
        grid.add(userTextField, 1, 1);
        
        Button button = new Button();
        button.setText("Go");
        grid.add(button, 2, 1);
        
        actiontarget = new Text();
		actiontarget.setFill(Color.FIREBRICK);
        grid.add(actiontarget, 1, 2);
        
        fakeURL = new TextArea();
		fakeURL.setStyle("-fx-text-fill: #f40000;");

        
        HBox fakeUrlBtn = new HBox(10);
        fakeUrlBtn.setAlignment(Pos.TOP_CENTER);
        fakeUrlBtn.getChildren().add(fakeURL);
        fakeUrlBtn.setPrefWidth(467);
        
        fakeUrlBtn.setPrefHeight(500d);
        grid.add(fakeUrlBtn, 0, 3, 3, 1);
        
        button.setOnAction((event) -> {
            Main.run(userTextField.getText());
        });
        
        Scene scene = new Scene(grid, 700, 500);

        scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if(key.getCode()==KeyCode.ENTER) {
                Main.run(userTextField.getText());
            }
        });
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
