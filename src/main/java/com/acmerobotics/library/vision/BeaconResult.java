package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class BeaconResult {
	
	public enum BeaconColor {
		RED,
		BLUE, 
		UNKNOWN
	}
	
	private BeaconColor leftColor, rightColor;
	private List<ColorRegion> regions;
	private List<Circle> buttons;
	private Rect bounds;
	
	public BeaconResult(BeaconColor left, BeaconColor right, List<ColorRegion> regions, List<Circle> buttons, Rect bounds) {
		this.leftColor = left;
		this.rightColor = right;
		this.regions = regions;
		this.buttons = buttons;
		this.bounds = bounds;
	}
	
	public BeaconResult(BeaconColor left, BeaconColor right, ColorRegion region, List<Circle> buttons, Rect bounds) {
		this(left, right, new ArrayList<ColorRegion>(), buttons, bounds);
		this.regions.add(region);
	}
	
	public BeaconResult(BeaconColor left, BeaconColor right, ColorRegion leftRegion, ColorRegion rightRegion, List<Circle> buttons, Rect bounds) {
		this(left, right, new ArrayList<ColorRegion>(), buttons, bounds);
		this.regions.add(leftRegion);
		this.regions.add(rightRegion);
	}
	
	public BeaconColor getLeftColor() {
		return this.leftColor;
	}
	
	public BeaconColor getRightColor() {
		return this.rightColor;
	}
	
	public void drawBeacon(Mat image) {
		Imgproc.rectangle(image, new Point(0, 0), new Point(75, 30), new Scalar(255, 255, 255), -1);
		if (regions.size() > 0) {
			Beacon.drawRegions(image, regions, new Scalar(0, 255, 0), 2);
			Beacon.drawButtons(image, buttons, new Scalar(0, 255, 255), 2);
			double error = Beacon.calcAspectRatioError(bounds);
			Util.drawRect(image, bounds, new Scalar(255, 255, 255), 2);
			Imgproc.putText(image, ((int) Math.round(100 * error)) + "%", new Point(10, 25), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0), 2);
		} else {
			Imgproc.putText(image, "???", new Point(10, 25), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0), 2);
		}
	}
	
}
