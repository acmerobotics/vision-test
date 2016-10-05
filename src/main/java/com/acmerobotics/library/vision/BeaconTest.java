package com.acmerobotics.library.vision;

import java.io.File;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class BeaconTest {
	
	public enum OutputImage {
		ORIGINAL,
		BLUR,
		EDGE
	}
	
	public static final OutputImage OUTPUT_IMAGE = OutputImage.ORIGINAL;
	public static final int IMAGE_WIDTH = 480;

	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		File imagesDir = new File("images");
		File[] images = imagesDir.listFiles();
		if (images == null) {
			System.out.println("No files found");
			System.exit(-1);
		}
		
		File outputDir = new File("output");
		if (outputDir.exists()) {
			for (File output : outputDir.listFiles()) {
				deleteRecursive(output);				
			}
		} else {
			outputDir.mkdirs();
		}
		
		for (File input : imagesDir.listFiles()) {
			processFile(input, outputDir);
		}
	}
	
	public static boolean deleteRecursive(File file) {
		if (file.isDirectory()) {
			for (File next: file.listFiles()) {
				deleteRecursive(next);
			}
		}
		return file.delete();
	}
	
	public static void processFile(File inputFile, File outputDir) {
		File outputFile = new File(outputDir.getPath() + "\\" + inputFile.getName());
		if (inputFile.isDirectory()) {
			for (File input : inputFile.listFiles()) {
				processFile(input, outputFile);
			}
		} else {
			Mat image = Imgcodecs.imread(inputFile.getAbsolutePath());
			Imgproc.resize(image, image, new Size(IMAGE_WIDTH, (IMAGE_WIDTH * image.height()) / image.width()));
			Mat output = process(image, OUTPUT_IMAGE);
			if (!outputDir.exists()) {
				outputDir.mkdirs();
			}
			Imgcodecs.imwrite(outputFile.getPath(), output);
			System.out.print("processed " + inputFile.getPath() + "@");
			System.out.println(output.width() + "x" + output.height());
		}
	}
	
	public static Mat process(Mat image, OutputImage outputImage) {
		Mat gray = new Mat(), blurred = new Mat(), edges = new Mat(), output = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		
		Imgproc.medianBlur(blurred, blurred, 5);
		
		// 19, 13
		Imgproc.adaptiveThreshold(gray, blurred, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 25, 15);
		
		Imgproc.GaussianBlur(blurred, blurred, new Size(9, 9), 2, 2);
		
		Mat circles = new Mat();
        Imgproc.HoughCircles(blurred, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 15, 200, 25, 0, 30);
        
		Imgproc.Canny(blurred, edges, 200, 100);
        
		switch (outputImage) {
		case ORIGINAL:
			output = image;
			break;
		case BLUR:
			output = blurred;
			break;
		case EDGE:
			output = edges;
			break;
		}
		
        if (output.channels() == 1) {
            Imgproc.cvtColor(output, output, Imgproc.COLOR_GRAY2BGR);
        }
        
        int x, y;
        int numCircles = circles.cols();        
        for (int i = 0; i < numCircles; i++) {
            Circle button = Circle.fromDoubleArray(circles.get(0, i));
            x = (int) Math.round(button.pt.x);
            y = (int) Math.round(button.pt.y);
            Imgproc.circle(output, button.pt, button.radius, new Scalar(0, 255, 0), 2);
//            Imgproc.line(output, new Point(x - 3, y - 3), new Point(x + 3, y + 3), new Scalar(0, 255, 255));
//            Imgproc.line(output, new Point(x - 3, y + 3), new Point(x + 3, y - 3), new Scalar(0, 255, 255));
        }
        
        return output;
	}
	
}
