package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Beacon {
	
	public static Mat[] processBeacon(Mat image) {
		ScalarRange red = new ScalarRange();
		red.add(new Scalar(160, 40, 160), new Scalar(180, 255, 255));
		red.add(new Scalar(0, 40, 160), new Scalar(5, 255, 255));
		
		ScalarRange blue = new ScalarRange();
		blue.add(new Scalar(95, 40, 160), new Scalar(125, 255, 255));
		
		ColorDetector redDetector = new ColorDetector(red);
		ColorDetector blueDetector = new ColorDetector(blue);
		
		redDetector.analyzeImage(image);
		blueDetector.analyzeImage(image);
		
		List<MatOfPoint> redContours = redDetector.getContours();
		List<MatOfPoint> blueContours = blueDetector.getContours();
		
		Imgproc.drawContours(image, redContours, -1, new Scalar(0, 0, 255), 3);
		Imgproc.drawContours(image, blueContours, -1, new Scalar(255, 0, 0), 3);
		
		Mat gray = new Mat(), gray2 = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.cvtColor(image, gray2, Imgproc.COLOR_BGR2GRAY);
		
		redDetector.clipRegion(gray, gray);
		blueDetector.clipRegion(gray2, gray2);
		
		Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
		Imgproc.threshold(gray2, gray2, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
		
		List<Circle> buttons = findButtons(gray);
		List<Circle> buttons2 = findButtons(gray2);
		
		drawButtons(image, buttons, new Scalar(0, 255, 0), 3);
		drawButtons(image, buttons2, new Scalar(0, 255, 0), 3);
		
		return new Mat[] { image };
	}
	
	public static List<Circle> findButtons(Mat gray) {
		Mat temp = new Mat();
		Imgproc.GaussianBlur(gray, temp, new Size(5, 5), 2);
		
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

}
