package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.acmerobotics.library.vision.Beacon.BeaconColor;

public class BeaconAnalyzer {
	
	public static final int LOWER_SAT = 0;
	public static final int UPPER_SAT = 255;
	
	public static final int LOWER_VALUE = 160;
	public static final int UPPER_VALUE = 255;
	
	public enum ButtonAnalysisMethod {
		BUTTON_HOUGH,
		BUTTON_ELLIPSE
	}
	
	public static List<Beacon> processBeacon(Mat image) {
		ScalarRange red = new ScalarRange();
		red.add(new Scalar(150, LOWER_SAT, LOWER_VALUE), new Scalar(180, UPPER_SAT, UPPER_VALUE));
		red.add(new Scalar(0, LOWER_SAT, LOWER_VALUE), new Scalar(10, UPPER_SAT, UPPER_VALUE));
		
		ScalarRange blue = new ScalarRange();
		blue.add(new Scalar(90, LOWER_SAT, LOWER_VALUE), new Scalar(125, UPPER_SAT, UPPER_VALUE));
		
		Mat hsv = new Mat();
		Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
		
		Mat gray = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		
		Mat hue = new Mat(), sat = new Mat(), val = new Mat(), mask = new Mat();
		Core.extractChannel(hsv, hue, 0);
		Core.extractChannel(hsv, sat, 1);
		Core.extractChannel(hsv, val, 2);
		
		ScalarRange hueRange = new ScalarRange();
		hueRange.add(new Scalar(150), new Scalar(180));
		hueRange.add(new Scalar(0), new Scalar(10));
//		hueRange.add(new Scalar(90), new Scalar(125));
		mask = hueRange.inRange(hue);
		Core.bitwise_and(hue, mask, hue);
		
		Core.inRange(sat, new Scalar(LOWER_SAT), new Scalar(UPPER_SAT), mask);
		mask.copyTo(sat);
//		Core.bitwise_and(mask, sat, sat);
		
		Core.inRange(val, new Scalar(LOWER_VALUE), new Scalar(UPPER_VALUE), mask);
		mask.copyTo(val);
//		Core.bitwise_and(mask, val, val);
		
//		Main.writeImage(hue);
//		Main.writeImage(sat);
//		Main.writeImage(val);
		
		ColorDetector redDetector = new ColorDetector(red);
		ColorDetector blueDetector = new ColorDetector(blue);
		
		List<BeaconRegion> redRegions = findBeaconRegions(image, redDetector, BeaconColor.RED, ButtonAnalysisMethod.BUTTON_ELLIPSE);
		List<BeaconRegion> blueRegions = findBeaconRegions(image, blueDetector, BeaconColor.BLUE, ButtonAnalysisMethod.BUTTON_ELLIPSE);
		
		BeaconRegion.drawRegions(image, redRegions);
		BeaconRegion.drawRegions(image, blueRegions);
		
		List<Beacon> results = new ArrayList<Beacon>();
		
		int i = 0;
		while (i < redRegions.size()) {
			BeaconRegion redRegion = redRegions.get(i);
			int numButtons = redRegion.getButtons().size();
			System.out.println("found region with " + numButtons + " button(s)");
			if (numButtons == 2) {
				results.add(new Beacon(redRegion));
				redRegions.remove(i);
			} else if (numButtons == 1 || numButtons == 0) {
				i++;
			} else {
				redRegions.remove(i);
			}
		}
		
		while (i < blueRegions.size()) {
			BeaconRegion blueRegion = blueRegions.get(i);
			int numButtons = blueRegion.getButtons().size();
			if (numButtons == 2) {
				results.add(new Beacon(blueRegion));
				blueRegions.remove(i);
			} else if (numButtons == 1) {
				i++;
			} else {
				blueRegions.remove(i);
			}
		}
		
		for (BeaconRegion region : redRegions) {
			Beacon bestMatch = null;
			double bestError = 1.0;
			for (BeaconRegion region2 : blueRegions) {
				Beacon combined = new Beacon(region, region2);
				double error = combined.getAspectRatioError();
				if (error < bestError) {
					bestMatch = combined;
					bestError = error;
				}
			}
			results.add(bestMatch);
		}
		
		Imgproc.rectangle(image, new Point(0, 0), new Point(75, 30 * results.size()), new Scalar(255, 255, 255), -1);
		int y = 25;
		
		results.sort(new Comparator<Beacon>() {

			@Override
			public int compare(Beacon o1, Beacon o2) {
				Size s1 = o1.getBounds().size;
				Size s2 = o2.getBounds().size;
				double area1 = s1.width * s1.height;
				double area2 = s2.width * s2.height;
				return (area1 > area2) ? 1 : -1;
			}
			
		});
		
		for (Beacon result : results) {
			result.draw(image);
			
			double error = result.getAspectRatioError();
			Imgproc.putText(image, ((int) Math.round(100 * error)) + "%", new Point(10, y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0), 2);
			y += 30;
		}
		
		return results;
	}
	
	public static List<BeaconRegion> findBeaconRegions(Mat image, ColorDetector detector, BeaconColor color, ButtonAnalysisMethod method) {
		detector.analyzeImage(image);
		List<ColorRegion> regions = detector.getRegions();
		
		Mat gray = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		detector.clipRegion(gray, gray);
		
		Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
		Mat bg = detector.getMask();
		Core.bitwise_not(bg, bg);
		Core.bitwise_or(gray, bg, gray);
		
		List<Circle> buttons = findButtons(gray, method);
		
		List<BeaconRegion> beaconRegions = new ArrayList<BeaconRegion>();
		for (ColorRegion region : regions) {
			BeaconRegion beaconRegion = new BeaconRegion(region, color);
			for (Circle button : buttons) {
				if (beaconRegion.getBounds().boundingRect().contains(button.pt)) beaconRegion.addButton(button);
			}
			beaconRegions.add(beaconRegion);
		}
		return beaconRegions;
	}
	
	public static List<Circle> findButtons(Mat gray, ButtonAnalysisMethod method) {
		if (method == ButtonAnalysisMethod.BUTTON_HOUGH) {
			return findButtonsHough(gray);
		} else if (method == ButtonAnalysisMethod.BUTTON_ELLIPSE) {
			return findButtonsEllipse(gray);
		} else {
			return null;
		}
	}
	
	public static List<Circle> findButtonsHough(Mat gray) {
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
	
	public static List<Circle> findButtonsEllipse(Mat gray) {
		List<Circle> circles = new ArrayList<Circle>();
		
		Mat edges = new Mat();
		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
		Imgproc.morphologyEx(gray, edges, Imgproc.MORPH_CLOSE, kernel);
		Imgproc.Canny(edges, edges, 200, 100);
		
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		for (MatOfPoint contour : contours) {
			Rect boundingRect = Imgproc.boundingRect(contour);
			double eccentricity = ((double) boundingRect.width) / boundingRect.height;
			if (Math.abs(eccentricity - 1) <= 0.25) {
				MatOfPoint2f ellipseContour = new MatOfPoint2f();
				ellipseContour.fromArray(contour.toArray());
				System.out.println("attempting to find ellipse with " + contour.toArray().length + " point(s)");
				RotatedRect ellipse = Imgproc.fitEllipse(ellipseContour);
				circles.add(new Circle(ellipse.center, (int) Math.round((ellipse.size.width + ellipse.size.height) / 4)));
			}
		}
		
		return circles;
	}

}
