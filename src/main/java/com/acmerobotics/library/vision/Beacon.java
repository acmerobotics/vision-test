package com.acmerobotics.library.vision;

import java.io.File;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Beacon {
	
	public static final int IMAGE_WIDTH = 480;
	
	public static void processImages(File[] inputImages, File outputDir) {
		for (File input : inputImages) {
			if (input.isFile()) {
				Mat original = Imgcodecs.imread(input.getPath());
				int width = original.width(), height = original.height();
				System.out.print(input.getPath() + "@" + width + "x" + height);
				Mat resized = new Mat();
				Imgproc.resize(original, resized, new Size(IMAGE_WIDTH, (height * IMAGE_WIDTH) / width));
				Mat output = process(resized);
				File outputFile = new File(outputDir.getPath() + "\\" + input.getName());
				Imgcodecs.imwrite(outputFile.getPath(), output);
				System.out.print(" => ");
				System.out.println(outputFile.getPath() + "@" + output.width() + "x" + output.height());
			}
		}
	}
	
	public static Mat process(Mat image) {
		Mat gray = new Mat(), blurred = new Mat(), edges = new Mat(), output = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		
		Imgproc.medianBlur(blurred, blurred, 5);
		
		// 19, 13
		Imgproc.adaptiveThreshold(gray, blurred, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 25, 15);
		
		Imgproc.GaussianBlur(blurred, blurred, new Size(9, 9), 2, 2);
		
		Mat circles = new Mat();
        Imgproc.HoughCircles(blurred, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 15, 200, 25, 0, 30);
        
		Imgproc.Canny(blurred, edges, 200, 100);
		
		output = blurred;
		
        if (output.channels() == 1) {
            Imgproc.cvtColor(output, output, Imgproc.COLOR_GRAY2BGR);
        }
        
        int numCircles = circles.cols();        
        for (int i = 0; i < numCircles; i++) {
            Circle button = Circle.fromDoubleArray(circles.get(0, i));
            Imgproc.circle(output, button.pt, button.radius, new Scalar(0, 255, 0), 2);
        }
        
        return output;
	}

}
