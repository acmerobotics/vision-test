package com.acmerobotics.library.vision;

import java.io.File;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.tools.ant.DirectoryScanner;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Main {
	
	public static final int IMAGE_WIDTH = 480;

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
		for (File input : inputImages) {
			if (input.isFile()) {
				Mat original = Imgcodecs.imread(input.getPath());
				int width = original.width(), height = original.height();
				Mat resized = new Mat();
				Imgproc.resize(original, resized, new Size(IMAGE_WIDTH, (height * IMAGE_WIDTH) / width));
				Mat[] outputs = Beacon.processBeacon(resized);
				for (int i = 0; i < outputs.length; i++) {
					Mat output = outputs[i];
					String[] parts = input.getName().split("\\.");
					File outputFile = new File(outputDir.getPath() + "\\" + parts[0] + (i > 0 ? "_" + i : "") + "." + parts[1].toLowerCase());
					Imgcodecs.imwrite(outputFile.getPath(), output);
					System.out.print(input.getPath() + "@" + width + "x" + height);
					System.out.print(" => ");
					System.out.println(outputFile.getPath() + "@" + output.width() + "x" + output.height());
				}
			}
		}
	}
	
}
