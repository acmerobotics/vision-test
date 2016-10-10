package com.acmerobotics.library.vision;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Util {
	
	public static Mat expandChannels(Mat src, int numChannels) {
		Mat dest = new Mat(src.size(), CvType.CV_8UC(numChannels));
		for (int i = 0; i < numChannels; i++) {
			Core.insertChannel(src, dest, i);
		}
		return dest;
	}

}
