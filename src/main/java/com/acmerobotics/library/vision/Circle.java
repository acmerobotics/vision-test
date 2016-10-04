package com.acmerobotics.library.vision;

import org.opencv.core.Point;

public class Circle {

    public Point pt;
    public int radius;

    public Circle(Point pt, int radius) {
        this.pt = pt;
        this.radius = radius;
    }

    public static Circle fromDoubleArray(double[] arr) {
        Point pt = new Point();
        pt.x = Math.round(arr[0]);
        pt.y = Math.round(arr[1]);
        int radius = (int) Math.round(arr[2]);
        return new Circle(pt, radius);
    }

}
