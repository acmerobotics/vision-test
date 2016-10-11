package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Beacon {
	
	public static final int LOWER_SAT = 30;
	public static final int UPPER_SAT = 255;
	
	public static final int LOWER_VALUE = 160;
	public static final int UPPER_VALUE = 255;
	
	public static final double BEACON_WIDTH = 8.5;
	public static final double BEACON_HEIGHT = 5.7;
	public static final double BEACON_WH_RATIO = BEACON_WIDTH / BEACON_HEIGHT;
	
	public static void processBeacon(Mat image) {
		ScalarRange red = new ScalarRange();
		red.add(new Scalar(150, LOWER_SAT, LOWER_VALUE), new Scalar(180, UPPER_SAT, UPPER_VALUE));
		red.add(new Scalar(0, LOWER_SAT, LOWER_VALUE), new Scalar(20, UPPER_SAT, UPPER_VALUE));
		
		ScalarRange blue = new ScalarRange();
		blue.add(new Scalar(90, LOWER_SAT, LOWER_VALUE), new Scalar(125, UPPER_SAT, UPPER_VALUE));
		
		ColorDetector redDetector = new ColorDetector(red);
		ColorDetector blueDetector = new ColorDetector(blue);
		
		redDetector.analyzeImage(image);
		blueDetector.analyzeImage(image);
		
		List<ColorRegion> redRegions = redDetector.getContours();
		List<ColorRegion> blueRegions = blueDetector.getContours();
		
		Mat gray = new Mat(), gray2 = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.cvtColor(image, gray2, Imgproc.COLOR_BGR2GRAY);
		
		redDetector.clipRegion(gray, gray);
		blueDetector.clipRegion(gray2, gray2);
		
		Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
		Imgproc.threshold(gray2, gray2, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
		
		Mat bg = redDetector.getMask();
		Core.bitwise_not(bg, bg);
		Core.bitwise_or(gray, bg, gray);
		
		Mat bg2 = blueDetector.getMask();
		Core.bitwise_not(bg2, bg2);
		Core.bitwise_or(gray2, bg2, gray2);
		
		List<Circle> buttons = findButtons(gray);
		List<Circle> buttons2 = findButtons(gray2);
		
		addButtonsToRegions(redRegions, buttons);
		addButtonsToRegions(blueRegions, buttons2);
	
		double error = 1;
		for (ColorRegion region : redRegions) {
			int numButtons = region.getButtons().size();
			if (numButtons == 2) {
				Rect bounds = region.getBounds();
				Imgproc.rectangle(image, new Point(bounds.x, bounds.y), new Point(bounds.x + bounds.width, bounds.y + bounds.height), new Scalar(255, 255, 0), 2);
				double widthHeightRatio = ((double) bounds.width) / bounds.height;
				error = Math.pow(widthHeightRatio - BEACON_WH_RATIO, 2);
			} else if (numButtons == 1) {
				error = 1;
				Rect bestRect = null;
				for (ColorRegion region2 : blueRegions) {
					if (region2.getButtons().size() != 1) continue;
					Rect combined = Util.combineRects(region.getBounds(), region2.getBounds());
					double widthHeightRatio = ((double) combined.width) / combined.height;
					double error2 = Math.pow(widthHeightRatio - BEACON_WH_RATIO, 2);	
					if (error2 < error) {
						error = error2;
						bestRect = combined;
					}
				}
				if (bestRect != null) Imgproc.rectangle(image, new Point(bestRect.x, bestRect.y), new Point(bestRect.x + bestRect.width, bestRect.y + bestRect.height), new Scalar(255, 255, 0), 2);
			}
		}
		Imgproc.rectangle(image, new Point(0, 0), new Point(100, 30), new Scalar(255, 255, 255), -1);
		Imgproc.putText(image, ((int) Math.round(100 * error)) + "%", new Point(10, 25), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 255, 0), 2);
	}
	
	protected static void addButtonsToRegions(List<ColorRegion> regions, List<Circle> buttons) {
		for (ColorRegion region : regions) {
			for (Circle button : buttons) {
				if (region.inBounds(button.pt)) region.addButton(button);
			}
		}
	}
	
	public static List<Circle> findButtons(Mat gray) {
		Mat temp = new Mat();
		Imgproc.GaussianBlur(gray, temp, new Size(9, 9), 2);
		
		Mat circles = new Mat();
		Imgproc.HoughCircles(temp, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 15, 200, 20, 0, 30);

		List<Circle> circleList = new ArrayList<Circle>();
		int numCircles = circles.cols();        
        for (int i = 0; i < numCircles; i++) {
            Circle button = Circle.fromDoubleArray(circles.get(0, i));
            circleList.add(button);
        }
        
        return circleList;
	}
	
	public static void drawButtons(Mat image, List<Circle> circles, Scalar color, int thickness) {
		for (Circle circle : circles) {
			Imgproc.circle(image, circle.pt, circle.radius, color, thickness);
		}
	}

	public static void drawRegions(Mat image, List<ColorRegion> regions, Scalar color, int thickness) {
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		for (ColorRegion region : regions) {
			contours.add(region.getContour());
		}
		Imgproc.drawContours(image, contours, -1, color, thickness);
	}

}
