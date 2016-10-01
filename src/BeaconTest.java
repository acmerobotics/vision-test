import java.io.File;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class BeaconTest {
	
	public enum OutputImage {
		ORIGINAL,
		BLUR,
		EDGE
	}
	
	public static final OutputImage OUTPUT_IMAGE = OutputImage.EDGE;

	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		File outputDir = new File("output");
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		
		String imageSuffix = "";
		switch(OUTPUT_IMAGE) {
		case ORIGINAL:
			imageSuffix = "";
			break;
		case BLUR:
			imageSuffix = "_blur";
			break;
		case EDGE:
			imageSuffix = "_edge";
			break;
		}
		
		File imagesDir = new File("images");
		File[] images = imagesDir.listFiles();
		Mat next, output;
		for (File image : images) {
			next = Imgcodecs.imread(image.getAbsolutePath());
			Imgproc.resize(next, next, new Size(next.width() / 8, next.height() / 8));
			output = process(next, OUTPUT_IMAGE);
			String[] parts = image.getName().split("\\.");
			Imgcodecs.imwrite("output\\" + parts[0] + imageSuffix + "." + parts[1], output);
			System.out.print("Processed " + image.getName() + " ");
			System.out.println(next.width() + "x" + next.height());
		}
		System.out.println("Processed " + images.length + " images");
	}
	
	public static Mat process(Mat image, OutputImage outputImage) {
		Mat gray = new Mat(), blurred = new Mat(), edges = new Mat(), output = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.adaptiveThreshold(gray, blurred, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 35, 7);
		
//		Size morphSize = new Size(3, 3);
//		Mat morphMat = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, morphSize);
//		Imgproc.dilate(blurred, blurred, morphMat);
//		Imgproc.erode(blurred, blurred, morphMat);
		
		Imgproc.GaussianBlur(blurred, blurred, new Size(9, 9), 2, 2);
		
		Mat circles = new Mat();
        Imgproc.HoughCircles(blurred, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 15, 200, 25, 0, 25);
        
		Imgproc.Canny(blurred, edges, 200, 100);
        
		switch (outputImage) {
		case ORIGINAL:
			output = image;
			break;
		case BLUR:
			output = blurred;
			break;
		case EDGE:
			output = edges;
			break;
		}
		
        if (output.channels() == 1) {
            Imgproc.cvtColor(output, output, Imgproc.COLOR_GRAY2BGR);
        }
        
        int x, y;
        int numCircles = circles.cols();        
        for (int i = 0; i < numCircles; i++) {
            Circle button = Circle.fromDoubleArray(circles.get(0, i));
            x = (int) Math.round(button.pt.x);
            y = (int) Math.round(button.pt.y);
            Imgproc.circle(output, button.pt, button.radius, new Scalar(0, 255, 0), 2);
//            Imgproc.line(output, new Point(x - 3, y - 3), new Point(x + 3, y + 3), new Scalar(0, 255, 255));
//            Imgproc.line(output, new Point(x - 3, y + 3), new Point(x + 3, y - 3), new Scalar(0, 255, 255));
        }
        
        return output;
	}
	
}
