package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ColorRegion {
	
	private MatOfPoint contour;
	private Rect rect;
	private List<Circle> buttons;
	
	public ColorRegion(MatOfPoint contour) {
		this.contour = contour;
		this.rect = Imgproc.boundingRect(contour);
		this.buttons = new ArrayList<Circle>();
	}
	
	public boolean inBounds(Point pt) {
		return rect.contains(pt);
	}
	
	public MatOfPoint getContour() {
		return contour;
	}
	
	public Rect getBounds() {
		return rect;
	}
	
	public void addButton(Circle button) {
		buttons.add(button);
	}
	
	public List<Circle> getButtons() {
		return buttons;
	}
	
	public void drawRegion(Mat image, Scalar color, int thickness) {
		List<ColorRegion> regionsToDraw = new ArrayList<ColorRegion>();
		regionsToDraw.add(this);
		Beacon.drawRegions(image, regionsToDraw, color, thickness);
	}
	
	public void drawBounds(Mat image, Scalar color, int thickness) {
		Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), color, thickness);
	}

}
