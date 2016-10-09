package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Scalar;

public class ScalarRange {
	
	private List<Scalar> ranges;
	
	public ScalarRange() {
		ranges = new ArrayList<Scalar>();
	}
	
	public ScalarRange(Scalar lower, Scalar upper) {
		this();
		add(lower, upper);
	}
	
	public List<Scalar> getRanges() {
		return ranges;
	}
	
	public ScalarRange add(ScalarRange other) {
		List<Scalar> otherRanges = other.getRanges();
		for (int i = 0; i < otherRanges.size(); i += 2) {
			add(otherRanges.get(i), otherRanges.get(i + 1));
		}
		return this;
	}
	
	public ScalarRange add(Scalar lower, Scalar upper) {
		ranges.add(lower);
		ranges.add(upper);
		return this;
	}
	
	public boolean contains(Scalar scalar) {
		double[] value = scalar.val;
		for (int i = 0; i < ranges.size(); i++) {
			double[] lower = ranges.get(i).val;
			double[] upper = ranges.get(i + 1).val;
			boolean inside = true;
			for (int j = 0; j < lower.length; j++) {
				inside = inside || (value[i] >= lower[i] && value[i] <= upper[i]);
			}
			if (inside) return true;
		}
		return false;
	}
	
}
