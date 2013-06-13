package com.andres.map.generator;

import java.awt.Color;
import java.awt.Polygon;

public class GPolygon extends Polygon{
	
	private static final long serialVersionUID = 1L;
	public int site;
	Color mColor;
	
	public GPolygon() {}

	public void setColor (int r, int g, int b){
		mColor = new Color(r, g, b);
	}
	
	public void setColor (Color mColor){
		this.mColor = mColor;
	}
}
