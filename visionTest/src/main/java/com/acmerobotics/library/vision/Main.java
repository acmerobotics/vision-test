package com.acmerobotics.library.vision;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.tools.ant.DirectoryScanner;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.acmerobotics.library.vision.Beacon.BeaconColor;
import com.acmerobotics.library.vision.Beacon.Score;
import com.acmerobotics.library.vision.BeaconAnalyzer.ButtonDetectionMethod;
import com.acmerobotics.library.vision.ImageOverlay.ImageRegion;

public class Main {
	
	public static final int IMAGE_WIDTH = 480;
	
	private static String currentFile;
	private static int currentFileIndex;

	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		CommandLineParser cmdParser = new DefaultParser();
		try {
			CommandLine cmdLine = cmdParser.parse(buildOptions(), args);
			File outputDir = new File(cmdLine.getOptionValue("output"));
			String[] inputFiles = cmdLine.getOptionValue("input").split(";");
			
			DirectoryScanner scanner = new DirectoryScanner();
			scanner.setIncludes(inputFiles);
			scanner.setBasedir(System.getProperty("user.dir"));
			scanner.setCaseSensitive(false);
			scanner.scan();
			String[] files = scanner.getIncludedFiles();		
			
			File[] inputs = new File[files.length];
			for (int i = 0; i < files.length; i++) {
				inputs[i] = new File(files[i]);
			}
			
			if (!outputDir.exists()) {
				outputDir.mkdirs();
			}
			
			processImages(inputs, outputDir);
		} catch (ParseException t) {
			System.out.println("tool failed: " + t.getClass() + ": " + t.getMessage());
			System.exit(-1);
		}
	}
	
	public static Options buildOptions() {
		Options options = new Options();
		
		Option output = Option.builder("o")
				.required()
				.longOpt("output")
				.hasArg()
				.build();
		
		Option input = Option.builder("i")
				.required()
				.longOpt("input")
				.hasArg()
				.build();
		
		options.addOption(output);
		options.addOption(input);
		
		return options;
	}
	
	public static void processImages(File[] inputImages, File outputDir) {
		double msTotal = 0;
		for (File input : inputImages) {
			if (input.isFile()) {
				File outputFile = new File(outputDir.getPath() + "\\" + input.getName());
				setCurrentFile(outputFile.getPath());
				
				Mat original = Imgcodecs.imread(input.getPath());
				int width = original.width(), height = original.height();
				Mat resized = new Mat();
				Imgproc.resize(original, resized, new Size(IMAGE_WIDTH, (height * IMAGE_WIDTH) / width));
				
				long start = System.currentTimeMillis();
//				analyzeColor(resized);
				analyzeImage(resized);
				long stop = System.currentTimeMillis();
				msTotal += (stop - start);
				
				Imgcodecs.imwrite(outputFile.getPath(), resized);
				
				System.out.print(input.getPath() + "@" + width + "x" + height);
				System.out.print(" => ");
				System.out.print(outputFile.getPath() + "@" + resized.width() + "x" + resized.height());
				System.out.println(" (" + (stop - start) + "ms)");
			}
		}
		System.out.println("mean process time: " + (int) (msTotal / inputImages.length) + "ms");
	}
	
	public static void analyzeColor(Mat image) {
		Mat hsv = new Mat(), sat = new Mat(), val = new Mat(), hue = new Mat();
		
		Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
		
		Core.extractChannel(hsv, hue, 0);
		Core.extractChannel(hsv, sat, 1);
		Core.extractChannel(hsv, val, 2);
		
		ScalarRange hueRange = new ScalarRange();
		hueRange.add(new Scalar(90, 0, 0), new Scalar(125, 255, 255));
		
		ScalarRange satRange = new ScalarRange();
		satRange.add(new Scalar(0, 80, 0), new Scalar(180, 255, 255));
		
		ScalarRange valRange = new ScalarRange();
		valRange.add(new Scalar(0, 0, 180), new Scalar(180, 255, 255));
		
		List<ScalarRange> ranges = new ArrayList<ScalarRange>();
		ranges.add(hueRange);
		ranges.add(satRange);
		ranges.add(valRange);

		Mat temp = new Mat();
		for (int i = 0; i < 3; i++) {
			ScalarRange range = ranges.get(i);
			ColorDetector detector = new ColorDetector(range);
			detector.analyzeImage(image);
//			List<ColorRegion> regions = detector.getRegions();
//			image.copyTo(temp);
//			ColorRegion.drawRegions(temp, regions, new Scalar(0, 0, 255), 2);
			writeImage(detector.getMask());
			Core.extractChannel(hsv, temp, i);
			Imgproc.cvtColor(temp, temp, Imgproc.COLOR_GRAY2BGR);
//			ColorRegion.drawRegions(temp, regions, new Scalar(0, 0, 255), 2);
			writeImage(temp);
		}
	}
	
	public static void analyzeImage(Mat image) {
		List<Beacon> beacons = BeaconAnalyzer.analyzeImage(image);
		
		Collections.sort(beacons, new Comparator<Beacon>() {

			@Override
			public int compare(Beacon o1, Beacon o2) {
				Size s1 = o1.getBounds().size;
				Size s2 = o2.getBounds().size;
				double area1 = s1.width * s1.height;
				double area2 = s2.width * s2.height;
				return (area1 > area2) ? -1 : 1;
			}
			
		});
		
		ImageOverlay overlay = new ImageOverlay(image);
		overlay.setBackgroundColor(new Scalar(0, 0, 0));
		
		for (Beacon result : beacons) {
			Score score = result.getScore();
			
			result.draw(image);
			
			String description = "";
			description += score.getNumericScore() + " " + score.toString() + "  ";
			description += (result.getLeftRegion().getColor() == Beacon.BeaconColor.RED ? "R" : "B") + ",";
			description += result.getRightRegion().getColor() == Beacon.BeaconColor.RED ? "R" : "B";
			
			overlay.drawText(description, ImageRegion.TOP_LEFT, Core.FONT_HERSHEY_SIMPLEX, 0.15, new Scalar(255, 255, 255), 3);
		}
	}
	
	public static void setCurrentFile(String filename) {
		currentFile = filename;
		currentFileIndex = 0;
	}
	
	public static int writeImage(Mat image) {
		String[] parts = currentFile.split("\\.");
		Imgcodecs.imwrite(parts[0] + "_" + (++currentFileIndex) + "." + parts[1], image);
		return currentFileIndex;
	}
	
}
