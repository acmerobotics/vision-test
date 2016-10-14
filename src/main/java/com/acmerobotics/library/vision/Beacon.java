package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Beacon {
	
	public enum BeaconColor {
		RED,
		BLUE, 
		UNKNOWN
	}
	
	public static final double BEACON_HEIGHT = 5.7;
	public static final double BEACON_WIDTH = 8.5;
	public static final double BEACON_WH_RATIO = BEACON_WIDTH / BEACON_HEIGHT;
	
	private List<BeaconRegion> beaconRegions;
	private RotatedRect bounds;
	
	public Beacon(BeaconRegion center) {
		beaconRegions = new ArrayList<BeaconRegion>();
		beaconRegions.add(center);
		bounds = center.getBounds();
	}
	
	public Beacon(BeaconRegion region1, BeaconRegion region2) {
		beaconRegions = new ArrayList<BeaconRegion>();
		if (region1.getBounds().center.x < region2.getBounds().center.x) {
			beaconRegions.add(region1);
			beaconRegions.add(region2);
			calculateBoundsTwoRegions(region1, region2);
		} else {
			beaconRegions.add(region2);
			beaconRegions.add(region1);
			calculateBoundsTwoRegions(region2, region1);
		}
	}
	
	private void calculateBoundsTwoRegions(BeaconRegion leftRegion, BeaconRegion rightRegion) {
		List<Point> allPoints = new ArrayList<Point>();
		List<Point> leftPoints = leftRegion.getContour().toList();
		List<Point> rightPoints = rightRegion.getContour().toList();
		allPoints.addAll(leftPoints);
		allPoints.addAll(rightPoints);
		MatOfPoint2f points = new MatOfPoint2f();
		points.fromList(allPoints);
		bounds = Imgproc.minAreaRect(points);
	}
	
	public double getAspectRatioError() {
		double widthHeightRatio = bounds.size.width / bounds.size.height;
		double heightWidthRatio = bounds.size.height / bounds.size.width;
		double whError = Math.pow(widthHeightRatio - BEACON_WH_RATIO, 2);
		double hwError = Math.pow(heightWidthRatio - BEACON_WH_RATIO, 2);
		return Math.min(whError, hwError);
	}
	
	public BeaconRegion getLeftRegion() {
		return beaconRegions.get(0);
	}
	
	public BeaconRegion getRightRegion() {
		return beaconRegions.get(beaconRegions.size() - 1);
	}
	
	public RotatedRect getBounds() {
		return this.bounds;
	}
	
	public void draw(Mat image) {
		BeaconRegion.drawRegions(image, beaconRegions);
		
		Util.drawRotatedRect(image, bounds, new Scalar(255, 255, 0), 2);
	}
	
}
