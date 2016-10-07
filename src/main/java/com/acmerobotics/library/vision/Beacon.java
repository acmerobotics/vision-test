package com.acmerobotics.library.vision;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
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
				Mat resized = new Mat();
				Imgproc.resize(original, resized, new Size(IMAGE_WIDTH, (height * IMAGE_WIDTH) / width));
				Mat output = processColor(resized);
				File outputFile = new File(outputDir.getPath() + "\\" + input.getName());
				Imgcodecs.imwrite(outputFile.getPath(), output);
				System.out.print(input.getPath() + "@" + width + "x" + height);
				System.out.print(" => ");
				System.out.println(outputFile.getPath() + "@" + output.width() + "x" + output.height());
			}
		}
	}
	
	public static Mat processColor(Mat image) {
		Mat hsv = new Mat(), mask = new Mat(), mask2 = new Mat(), gray = new Mat(), temp = new Mat();
		
//		Imgproc.medianBlur(image, image, );
//		Imgproc.GaussianBlur(image, image, new Size(9, 9), 2);
		
		Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
		Core.inRange(hsv, new Scalar(160, 50, 100), new Scalar(180, 255, 255), mask);
		Core.inRange(hsv, new Scalar(0, 50, 100), new Scalar(5, 255, 255), mask2);
		Core.bitwise_or(mask, mask2, mask);

//		Core.extractChannel(hsv, hue, 0);
//		
//		int maxHue = Integer.MIN_VALUE;
//		byte[] pixels = new byte[hue.width() * hue.height()];
//		hue.get(0, 0, pixels);
//		for (int i = 0; i < pixels.length; i++) {
//			int currentHue = (pixels[i] & 0xFF) << 1;
//			if (currentHue > maxHue) {
//				maxHue = currentHue;
//			}
//		}
//		System.out.println("max hue: " + maxHue);
//		Core.bitwise_and(image, mask, image);
		
		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
		Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
		Imgproc.dilate(mask, mask, kernel);

		// second dilate seems unnecessary
//		Imgproc.dilate(mask, mask, kernel);
		
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(mask, contours, temp, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
//		List<Mat> maskComponents = new ArrayList<Mat>();
//		for (int i = 0; i < 3; i++) {
//			maskComponents.add(mask);
//		}
//		Core.merge(maskComponents, mask2);
//		
//		Core.bitwise_and(image, mask2, image);
		
		// approximate a polygon to each contours
		// turns out it is probably better to use the contour bounding box instead
//		MatOfPoint poly = new MatOfPoint();
//		List<MatOfPoint> polys = new ArrayList<MatOfPoint>();
//		MatOfPoint2f contour = new MatOfPoint2f(), approx = new MatOfPoint2f();
//		for (int i = 0; i < contours.size(); i++) {
//			contours.get(i).convertTo(contour, CvType.CV_32FC2);
//			double peri = Imgproc.arcLength(contour, true);
//			Imgproc.approxPolyDP(contour, approx, 0.06 * peri, true);
//			approx.convertTo(poly, CvType.CV_32S);
//			polys.add(poly);
//		}
//		Imgproc.drawContours(image, polys, -1, new Scalar(0, 0, 255), 2);
		
		Imgproc.drawContours(image, contours, -1, new Scalar(0, 0, 0), 3);
		
		return image;
	}
	
	public static Mat processButtons(Mat image) {
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
