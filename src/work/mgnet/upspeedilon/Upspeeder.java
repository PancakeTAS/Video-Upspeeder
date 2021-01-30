package work.mgnet.upspeedilon;

import java.io.File;
import java.io.IOException;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSlider;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class Upspeeder extends Application {

	public static DoubleProperty progress;
	public static DoubleProperty bitrate;
	public static int framerate = 60;
	public static int skippingFrames = 0;
	
	public static StringProperty icodec;
	public static StringProperty ilength;
	public static StringProperty ibitrate;
	public static StringProperty iquality;
	
	public static StringProperty patho;
	public static StringProperty lengtho;
	public static StringProperty estfilesizeo;
	
	public static File inputfile;
	public static FrameGrab grab;
	
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		VBox root = new VBox(getSplitPane(getInputPane(primaryStage), getSettingsPane(), getOutputPane()), getHBoxPane());
		primaryStage.setScene(new Scene(root, 900, 600));
		primaryStage.setTitle("Upspeeder by Pfannekuchen");
		primaryStage.show();
	}
	
	// Top Sections
	
	private Pane getInputPane(Stage primaryStage) {
		AnchorPane inputPane = new AnchorPane();
		
		Label inputLabel = new Label("Input");
		inputLabel.setAlignment(Pos.CENTER);
		inputLabel.setLayoutX(14);
		inputLabel.setLayoutY(14);
		inputLabel.setFont(new Font("System", 18));
		inputLabel.setStyle("-fx-text-fill: #9f9f9f");
		inputLabel.setMinWidth(60);
		inputLabel.setTextAlignment(TextAlignment.CENTER);
		inputLabel.setWrapText(false);
		
		Label codec = new Label("Codec: <Unknown>");
		codec.setLayoutX(30);
		codec.setLayoutY(245);
		codec.setPrefSize(173, 27);
		icodec = codec.textProperty();
		
		Label length = new Label("Length: <Unknown>");
		length.setLayoutX(30);
		length.setLayoutY(272);
		length.setPrefSize(173, 27);
		ilength = length.textProperty();
		
		Label bitrate = new Label("Bitrate: <Unknown>");
		bitrate.setLayoutX(30);
		bitrate.setLayoutY(299);
		bitrate.setPrefSize(173, 27);
		ibitrate = bitrate.textProperty();
		
		Label resolution = new Label("Quality: <Unknown>");
		resolution.setLayoutX(30);
		resolution.setLayoutY(326);
		resolution.setPrefSize(173, 27);
		iquality = resolution.textProperty();
		
		JFXButton loadFileButton = new JFXButton("Load File");
		loadFileButton.setPrefSize(180, 47);
		loadFileButton.setLayoutX(21);
		loadFileButton.setStyle("-fx-background-color: #5b7784; -fx-text-fill: white");
		loadFileButton.setLayoutY(163);
		loadFileButton.setFont(Font.loadFont(Upspeeder.class.getResourceAsStream("helv.ttf"), 14));
		loadFileButton.setMnemonicParsing(false);
		loadFileButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				 FileChooser chooser = new FileChooser();
				 chooser.setTitle("Select Input File");
				 chooser.getExtensionFilters().add(new ExtensionFilter("Video", "*.mp4", "*.mov", "*.flv", "*.mkv"));
				 inputfile = chooser.showOpenDialog(primaryStage);
				 
				 loadFileData();
				 
			}
		});
		
		inputPane.getChildren().addAll(inputLabel, loadFileButton, codec, length, bitrate, resolution);
		return inputPane;
	}
	
	private ScrollPane getSettingsPane() {
		AnchorPane stP = new AnchorPane();
		ScrollPane settingsPane = new ScrollPane(stP);
		
		Label title = new Label("Settings");
		title.setAlignment(Pos.CENTER);
		title.setLayoutX(14);
		title.setLayoutY(14);
		title.setFont(new Font("System", 18));
		title.setStyle("-fx-text-fill: #9f9f9f");
		title.setMinWidth(60);
		title.setTextAlignment(TextAlignment.CENTER);
		title.setWrapText(false);
		
		JFXSlider bitrateSlider = new JFXSlider(0, 50000, 20000);
		bitrate = bitrateSlider.valueProperty();
		bitrateSlider.setLayoutX(112);
		bitrateSlider.setLayoutY(507);
		bitrateSlider.setPrefSize(229, 24);
		
		Label bitrLbl = new Label("Output Bitrate:");
		bitrLbl.setLayoutX(14);
		bitrLbl.setLayoutY(510);
		bitrLbl.setPrefSize(98, 18);
		
		Label cbitrateL = new Label("20000 Kbps");
		cbitrateL.setLayoutX(347);
		cbitrateL.setLayoutY(510);
		cbitrateL.setPrefSize(89, 18);
		bitrate.addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				cbitrateL.setText(newValue.intValue() > 20000 ? (((int) (newValue.intValue() / 1000)) + " Mbps") : newValue.intValue() + " Kbps");
			}
		});
		
		Label tickrateLbl = new Label("Tickrate: ");
		tickrateLbl.setPrefSize(57, 18);
		tickrateLbl.setLayoutX(50);
		tickrateLbl.setTextAlignment(TextAlignment.RIGHT);
		tickrateLbl.setLayoutY(478);
		
		TextField tickrate = new TextField("20");
		tickrate.setPrefSize(70, 26);
		tickrate.setLayoutX(112);
		tickrate.setLayoutY(474);
		
		Label ctb = new Label("Skipping 0 frames.");
		ctb.setPrefSize(220, 18);
		ctb.setLayoutX(196);
		ctb.setTextAlignment(TextAlignment.RIGHT);
		ctb.setLayoutY(478);
		
		tickrate.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				try {
					int tickrate = Integer.parseInt(newValue);
					float gamespeed = tickrate / 20F;
					skippingFrames = (int) (float) (1f / gamespeed);
					skippingFrames *= (framerate / 20);
					//indowsFileAtt attr = Files.readAttributes(inputfile, BasicFileAttributes.class);
					//ystem.out.println(attr.);
					ctb.setText("Skipping " + skippingFrames + " frames.");
				} catch (Exception e) {
					
				}
			}
		});
		
		stP.getChildren().addAll(title, bitrateSlider, bitrLbl, cbitrateL, tickrateLbl, tickrate, ctb);
		return settingsPane;
	}
	
	private Pane getOutputPane() {
		AnchorPane outputPane = new AnchorPane();
		
		Label outputLabel = new Label("Output");
		outputLabel.setAlignment(Pos.CENTER);
		outputLabel.setLayoutX(14);
		outputLabel.setLayoutY(14);
		outputLabel.setFont(new Font("System", 18));
		outputLabel.setStyle("-fx-text-fill: #9f9f9f");
		outputLabel.setMinWidth(60);
		outputLabel.setTextAlignment(TextAlignment.CENTER);
		outputLabel.setWrapText(false);
		
		JFXButton outputFileButton = new JFXButton("Output File");
		outputFileButton.setPrefSize(180, 47);
		outputFileButton.setLayoutX(20);
		outputFileButton.setStyle("-fx-background-color: #5b7784; -fx-text-fill: white");
		outputFileButton.setLayoutY(155);
		outputFileButton.setFont(Font.loadFont(Upspeeder.class.getResourceAsStream("helv.ttf"), 14));
		outputFileButton.setMnemonicParsing(false);
		
		Label length = new Label("Length: <Unknown>");
		length.setLayoutX(30);
		length.setLayoutY(272);
		length.setPrefSize(173, 27);
		lengtho = length.textProperty();
		
		Label path = new Label("Path: Videos");
		path.setLayoutX(30);
		path.setLayoutY(299);
		path.setPrefSize(173, 27);
		patho = path.textProperty();
		
		Label est = new Label("Est: <Unknown>");
		est.setLayoutX(30);
		est.setLayoutY(245);
		est.setPrefSize(173, 27);
		estfilesizeo = est.textProperty();
		
		outputPane.getChildren().addAll(outputLabel, outputFileButton, length, path, est);
		return outputPane;
	}
	
	// Root Section
	
	private SplitPane getSplitPane(Pane inputPane, ScrollPane settingsPane, Pane outputPane) {
		SplitPane splitPane = new SplitPane(inputPane, settingsPane, outputPane);
		splitPane.setPrefSize(900, 580);
		splitPane.setDividerPositions(.25, .75);
		splitPane.setFocusTraversable(true);
		return splitPane;
	}
	
	private Pane getHBoxPane() {
		ProgressBar progress = new ProgressBar(0.0);
		progress.setPrefHeight(20);
		progress.setPrefWidth(900);
		Upspeeder.progress = progress.progressProperty();
		HBox progressPane = new HBox(new Pane(progress));
		progressPane.setAlignment(Pos.CENTER_LEFT);
		progressPane.setSpacing(5.0);
		return progressPane;
	}

	// Actual Stuff
	
	public void loadFileData() {
		try {
			grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(inputfile));
			iquality.setValue("Quality: " + grab.getMediaInfo().getDim().getWidth() + "x" + grab.getMediaInfo().getDim().getHeight());
		} catch (IOException | JCodecException e) {
			e.printStackTrace();
		}
	}
	
}
