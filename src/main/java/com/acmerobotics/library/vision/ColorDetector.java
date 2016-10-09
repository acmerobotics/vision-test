package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ColorDetector {
	
	public ScalarRange range;
	
	private List<MatOfPoint> contours;
	private Mat mask;
	
	public ColorDetector(ScalarRange range) {
		this.range = range;
		this.contours = null;
		this.mask = new Mat();
	}
	
	public ScalarRange getColorRange() {
		return range;
	}
	
	public void setColorRange(ScalarRange range) {
		this.range = range;
	}
	
	public void analyzeImage(Mat image) {
		Mat hsv = new Mat(), mask2 = new Mat(), gray = new Mat(), edges = new Mat();
		
		Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		
		this.mask = Mat.zeros(image.size(), CvType.CV_8U);
		List<Scalar> hsvRanges = range.getRanges();
		for (int i = 0; i < hsvRanges.size(); i += 2) {
			Core.inRange(hsv, hsvRanges.get(i), hsvRanges.get(i + 1), mask2);
			Core.bitwise_or(mask2, mask, mask);
		}
		
		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
		Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
		Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25, 25));
		Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel2);
		
		Core.bitwise_and(gray, mask, gray);
		Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
		Imgproc.GaussianBlur(gray, gray, new Size(7, 7), 2);
		Imgproc.Canny(gray, edges, 200, 100);
		
		this.contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
	}
	
	public List<MatOfPoint> getContours() {
		return contours;
	}
	
	public void clipRegion(Mat src, Mat dest) {
		Mat tempMask = new Mat(dest.size(), dest.type());
		for (int i = 0; i < dest.channels(); i++) {
			Core.insertChannel(mask, tempMask, i);
		}
		Core.bitwise_and(src, tempMask, dest);
	}

}
