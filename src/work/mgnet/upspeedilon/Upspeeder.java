package work.mgnet.upspeedilon;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.H264Utils2;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.tools.MainUtils;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSlider;
import com.sun.awt.AWTUtilities;

import javafx.application.Application;
import javafx.application.Platform;
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
import net.lingala.zip4j.core.ZipFile;
import sun.nio.ch.IOUtil;

public class Upspeeder extends Application {

	public static DoubleProperty progress;
	public static DoubleProperty bitrate;
	public static int framerate = 60;
	public static float speed = 1;
	public static JFXButton outputFileButton;
	
	public static Process p;
	public static StringProperty icodec;
	public static StringProperty ilength;
	public static StringProperty iquality;
	public static ProgressBar progressbar;
	
	public static StringProperty lengtho;
	public static StringProperty estfilesizeo;
	
	public static File outputfile;
	public static File inputfile;
	public static FrameGrab grab;
	
	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				if (p != null && p.isAlive()) p.destroyForcibly();
			}
		}));
		Application.launch(args);
		System.exit(1);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					File ffmpeg = new File(System.getenv("AppData"), "upspeeder/ffmpeg/ffmpeg-N-100872-gd8b2fae3c7-win64-gpl/bin/");
					if (ffmpeg.exists()) {
						return;
					}
					File upspeeder = new File(System.getenv("AppData"), "upspeeder/");
					upspeeder.mkdir();
					System.out.println("Downloading FFmpeg.");
					URL url = new URL("https://github.com/BtbN/FFmpeg-Builds/releases/download/autobuild-2021-01-29-12-37/ffmpeg-N-100872-gd8b2fae3c7-win64-gpl.zip");
					ReadableByteChannel rbc = Channels.newChannel(url.openStream());
					FileOutputStream fos = new FileOutputStream(new File(System.getenv("AppData"), "upspeeder/ffmpeg.zip"));
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					fos.close();
					rbc.close();
					System.out.println("Unpacking FFmpeg");
					ZipFile zip = new ZipFile(new File(System.getenv("AppData"), "upspeeder/ffmpeg.zip"));
					zip.extractAll(new File(upspeeder, "ffmpeg").getAbsolutePath());
					System.out.println("FFmpeg installed");
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							progress.set(0);
							outputFileButton.setDisable(false);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
		}).start();
		
		VBox root = new VBox(getSplitPane(getInputPane(primaryStage), getSettingsPane(), getOutputPane(primaryStage)), getHBoxPane());
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
		
		Label resolution = new Label("Quality: <Unknown>");
		resolution.setLayoutX(30);
		resolution.setLayoutY(299);
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
				 int time = (int) grab.getVideoTrack().getMeta().getTotalDuration();
				 //estfilesizeo.setValue("Est: " + String.format("%.2f", (time * bitrate.doubleValue()) / 10000000f) + "Gb");
				 recalculateOutput();
			}
		});
		
		inputPane.getChildren().addAll(inputLabel, loadFileButton, codec, length, resolution);
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
				recalculateOutput();
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
		
		Label ctb = new Label("Speeding up by PTS 1");
		ctb.setPrefSize(220, 18);
		ctb.setLayoutX(196);
		ctb.setTextAlignment(TextAlignment.RIGHT);
		ctb.setLayoutY(478);
		
		tickrate.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				try {
					int tickrate = Integer.parseInt(newValue);
					speed = tickrate / 20F;
					ctb.setText("Speeding up by PTS " + speed);
					recalculateOutput();
				} catch (Exception e) {
					
				}
			}
		});
		
		stP.getChildren().addAll(title, bitrateSlider, bitrLbl, cbitrateL, tickrateLbl, tickrate, ctb);
		return settingsPane;
	}
	
	private Pane getOutputPane(Stage primaryStage) {
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
		
		outputFileButton = new JFXButton("Start");
		outputFileButton.setPrefSize(180, 47);
		outputFileButton.setLayoutX(20);
		File ffmpeg = new File(System.getenv("AppData"), "upspeeder/ffmpeg/ffmpeg-N-100872-gd8b2fae3c7-win64-gpl/bin/");
		if (!ffmpeg.exists()) outputFileButton.setDisable(true);
		outputFileButton.setStyle("-fx-background-color: #5b7784; -fx-text-fill: white");
		outputFileButton.setLayoutY(155);
		outputFileButton.setFont(Font.loadFont(Upspeeder.class.getResourceAsStream("helv.ttf"), 14));
		outputFileButton.setMnemonicParsing(false);
		outputFileButton.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				 FileChooser chooser = new FileChooser();
				 chooser.setTitle("Select Output File");
				 chooser.getExtensionFilters().add(new ExtensionFilter("Video", "*.mp4"));
				 outputfile = chooser.showSaveDialog(primaryStage);
				 if (outputfile == null) return;
				 copy();
			}
		});
		
		Label length = new Label("Length: <Unknown>");
		length.setLayoutX(30);
		length.setLayoutY(272);
		length.setPrefSize(173, 27);
		lengtho = length.textProperty();
		
		Label est = new Label("Est: <Unknown>");
		est.setLayoutX(30);
		est.setLayoutY(245);
		est.setPrefSize(173, 27);
		estfilesizeo = est.textProperty();
		
		outputPane.getChildren().addAll(outputLabel, outputFileButton, length, est);
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
		progressbar = new ProgressBar();
		File ffmpeg = new File(System.getenv("AppData"), "upspeeder/ffmpeg/ffmpeg-N-100872-gd8b2fae3c7-win64-gpl/bin/");
		if (ffmpeg.exists()) progressbar.setProgress(0);
		progressbar.setPrefHeight(20);
		progressbar.setPrefWidth(900);
		Upspeeder.progress = progressbar.progressProperty();
		HBox progressPane = new HBox(new Pane(progressbar));
		progressPane.setAlignment(Pos.CENTER_LEFT);
		progressPane.setSpacing(5.0);
		return progressPane;
	}

	// Actual Stuff
	
	public void recalculateOutput() {
		if (inputfile != null) {
			int time = (int) (grab.getVideoTrack().getMeta().getTotalDuration() * speed);
			lengtho.set("Length: " + Duration.ofSeconds(time).toMinutes() + "m");
			double ingb = (time * bitrate.doubleValue()) / 10000000f;
			if (ingb >= 1) estfilesizeo.setValue("Est: " + String.format("%.2f", ingb) + "Gb");
			else estfilesizeo.setValue("Est: " + (int) ((time * bitrate.doubleValue()) / 10000f) + "Mb");
		}
	}
	
	public void copy() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					File ffmpeg = new File(System.getenv("AppData"), "upspeeder/ffmpeg/ffmpeg-N-100872-gd8b2fae3c7-win64-gpl/bin/ffmpeg.exe");
					String command = ffmpeg.getAbsolutePath() + " -hwaccel cuda -an -sn -r 60 -hwaccel_output_format cuda -i \"" + inputfile.getAbsolutePath() + "\" -b:v " + (bitrate.intValue() * 1000) + " -y -c:v h264_nvenc -filter:v \"setpts=" + speed + "*PTS\" \"" + outputfile.getAbsolutePath() + "\"";
					System.out.println(command);
					ProcessBuilder builder = new ProcessBuilder(command.split(" "));
					long time = System.currentTimeMillis();
					p = builder.start();
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					String line = reader.readLine();
					final float frames = grab.getVideoTrack().getMeta().getTotalFrames() * speed;
					while (p.isAlive()) {
						System.out.println(line);
						try {
							float frame = Integer.parseInt(line.split("rame=")[1].trim().split("fps=")[0].trim());
							System.out.println("Current frame: " + frame);
							System.out.println("Max Frames: " + frames);
							progress.set(frame / frames);
						} catch (Exception e) {
							
						}
						line = reader.readLine();
					}
					System.out.println("Done: " + (System.currentTimeMillis() - time) + "ms");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void loadFileData() {
		try {
			grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(inputfile));
			ilength.setValue("Length: " + Duration.ofSeconds((int) grab.getVideoTrack().getMeta().getTotalDuration()).toMinutes() + "m");
			icodec.setValue("Codec: " + grab.getVideoTrack().getMeta().getCodec().name());
			iquality.setValue("Quality: " + grab.getMediaInfo().getDim().getWidth() + "x" + grab.getMediaInfo().getDim().getHeight());
		} catch (IOException | JCodecException e) {
			
		}
	}
	
}
