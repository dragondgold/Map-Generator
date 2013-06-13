package com.andres.map.generator;

public class Point2D {

	public int x;
	public int y;
	public double angle;

	public Point2D() {
	}
	
	Point2D(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public boolean isEqual (int x, int y){
		return (x == this.x && y == this.y);
	}
	
	public boolean isEqual (Point2D mPoint2d){
		return (mPoint2d.x == x && mPoint2d.y == y);
	}

}
