package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.acmerobotics.library.vision.BeaconResult.BeaconColor;

public class Beacon {
	
	public static final int LOWER_SAT = 30;
	public static final int UPPER_SAT = 255;
	
	public static final int LOWER_VALUE = 160;
	public static final int UPPER_VALUE = 255;
	
	public static final double BEACON_WIDTH = 8.5;
	public static final double BEACON_HEIGHT = 5.7;
	public static final double BEACON_WH_RATIO = BEACON_WIDTH / BEACON_HEIGHT;
	
	public static List<BeaconResult> processBeacon(Mat image) {
		ScalarRange red = new ScalarRange();
		red.add(new Scalar(150, LOWER_SAT, LOWER_VALUE), new Scalar(180, UPPER_SAT, UPPER_VALUE));
		red.add(new Scalar(0, LOWER_SAT, LOWER_VALUE), new Scalar(20, UPPER_SAT, UPPER_VALUE));
		
		ScalarRange blue = new ScalarRange();
		blue.add(new Scalar(90, LOWER_SAT, LOWER_VALUE), new Scalar(125, UPPER_SAT, UPPER_VALUE));
		
		ColorDetector redDetector = new ColorDetector(red);
		ColorDetector blueDetector = new ColorDetector(blue);
		
		List<ColorRegion> redRegions = findColorRegions(image, redDetector);
		List<ColorRegion> blueRegions = findColorRegions(image, blueDetector);
		
		List<BeaconResult> results = new ArrayList<BeaconResult>();
		
		int i = 0;
		while (i < redRegions.size()) {
			ColorRegion redRegion = redRegions.get(i);
			int numButtons = redRegion.getButtons().size();
			if (numButtons == 2) {
				results.add(new BeaconResult(BeaconColor.RED, BeaconColor.RED, redRegion, redRegion.getButtons(), redRegion.getBounds()));
				redRegions.remove(i);
			} else if (numButtons == 1) {
				i++;
			} else {
				redRegions.remove(i);
			}
		}
		
		while (i < blueRegions.size()) {
			ColorRegion blueRegion = blueRegions.get(i);
			int numButtons = blueRegion.getButtons().size();
			if (numButtons == 2) {
				results.add(new BeaconResult(BeaconColor.BLUE, BeaconColor.BLUE, blueRegion, blueRegion.getButtons(), blueRegion.getBounds()));
				blueRegions.remove(i);
			} else if (numButtons == 1) {
				i++;
			} else {
				blueRegions.remove(i);
			}
		}
		
		for (ColorRegion region : redRegions) {
			ColorRegion bestMatch = null;
			double bestError = 1.0;
			Rect bestBounds = null;
			for (ColorRegion region2 : blueRegions) {
				Rect combined = Util.combineRects(region.getBounds(), region2.getBounds());
				double error = calcAspectRatioError(combined);
				if (error < bestError) {
					bestMatch = region2;
					bestBounds = combined;
					bestError = error;
				}
			}
			if (bestMatch != null) {
				region.getButtons().addAll(bestMatch.getButtons());
				if (region.getBounds().x < bestBounds.x) {
					results.add(new BeaconResult(BeaconColor.RED, BeaconColor.BLUE, region, bestMatch, region.getButtons(), bestBounds));
				} else {					
					results.add(new BeaconResult(BeaconColor.BLUE, BeaconColor.RED, bestMatch, region, region.getButtons(), bestBounds));
				}
			} else {
				break;
			}
		}
		
		for (BeaconResult result : results) {
			result.drawBeacon(image);
		}
		
		return results;
	}
	
	public static double calcAspectRatioError(Rect beacon) {
		double widthHeightRatio = ((double) beacon.width) / beacon.height;
		return Math.pow(widthHeightRatio - BEACON_WH_RATIO, 2);
	}
	
	public static List<ColorRegion> findColorRegions(Mat image, ColorDetector detector) {
		detector.analyzeImage(image);
		List<ColorRegion> regions = detector.getRegions();
		
		Mat gray = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		detector.clipRegion(gray, gray);
		
		Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
		Mat bg = detector.getMask();
		Core.bitwise_not(bg, bg);
		Core.bitwise_or(gray, bg, gray);
		
		addButtonsToRegions(regions, findButtons(gray));
		
		return regions;
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
