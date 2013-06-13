package com.andres.map.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.Color;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import de.erichseifert.vectorgraphics2d.SVGGraphics2D;

import libnoiseforjava.exception.ExceptionInvalidParam;
import libnoiseforjava.module.Perlin;
import libnoiseforjava.util.ColorCafe;
import libnoiseforjava.util.ImageCafe;
import libnoiseforjava.util.NoiseMap;
import libnoiseforjava.util.NoiseMapBuilderPlane;
import libnoiseforjava.util.RendererImage;

import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

public class RandomMapGenerator {
	
	/** Lista de polígonos, la clase GPolygon es la clase Java Polygon de Java pero con la posibilidad
	 * de añadirle un color
	 */
	private ArrayList<GPolygon> polygonsList = new ArrayList<GPolygon>();
	private SVGGraphics2D mSvgGraphics2D;
	int width, height;
	int seed;

	/**
	 * Genera un mapa aleatorio con la semilla dada utilizando Perlin Noise + Voronoi
	 * http://sourceforge.net/projects/simplevoronoi/
	 * @param width ancho del mapa
	 * @param height alto del mapa
	 * @param minDist distancia minima entre puntos para generar Voronoi (define el tamaño de los
	 * polígonos)
	 * @param seed semilla para el generador aleatorio
	 * @param freq frecuencia del perlin noise
	 * @return SVGGraphics2D, gráfico vectorial del mapa
	 */
	public SVGGraphics2D generateMap(final int width, final int height, final float minDist,
			final int seed, final int freq){
		
		Voronoi mVoronoi = new Voronoi(0);
		PoissonDiskSampling mDiskSampling = new PoissonDiskSampling(seed);
		Point[] pointsList;
		List<GraphEdge> edgesList;
		ImageCafe perlinMap;
		
		this.width = width;
		this.height = height;
		this.seed = seed;
		
		long start, end;
		
		// Genero el Poisson Disk Sampling
        pointsList = mDiskSampling.generatePoisson(width, height, minDist, 30);
        
        // Paso a array las coordenadas (x,y) para Voronoi
        double xValues[] = new double[pointsList.length];
        double yValues[] = new double[pointsList.length];
        for(int n = 0; n < pointsList.length; ++n){
        	xValues[n] = pointsList[n].x;
        	yValues[n] = pointsList[n].y;
        }
        
        start = System.currentTimeMillis();
        // Genero Voronoi en base a los puntos generados con el Poisson Disk Sampling
        edgesList = mVoronoi.generateVoronoi(xValues, yValues, 0, width, 0, height);
        end = System.currentTimeMillis();
        System.out.println("Voronoi Time: " + (end-start) + " mS");
        System.out.println("Edges quantity: " + edgesList.size());
        
		// Height map generado con Perlin Noise
        perlinMap = generatePerlinMap(-0.15, freq);
        
        start = System.currentTimeMillis();
        
        /* Uso un HashMap para optimizar la velocidad de busqueda.
         * Utilizando un bucle for() el mismo demora en una prueba 30.000mS contra 200mS de este metodo.
         * Cada una de las key del HashMap es uno de los sites (puntos) de Voronoi y de analiza cada Edge. 
         */
        Map<Integer, List<GraphEdge>> edgesByPolygon = new HashMap<>();
        for (GraphEdge edge : edgesList) {
            List<GraphEdge> list = edgesByPolygon.get(edge.site1);
            if (list == null) {
                list = new ArrayList<>();
                edgesByPolygon.put(edge.site1, list);
            }
            list.add(edge);

            list = edgesByPolygon.get(edge.site2);
            if (list == null) {
                list = new ArrayList<>();
                edgesByPolygon.put(edge.site2, list);
            }
            list.add(edge);
        }
        // Paso los datos a los Polígonos, reservo antes el espacio en el List
        polygonsList.ensureCapacity(pointsList.length);
        for (Integer key : edgesByPolygon.keySet()) {
        	List<GraphEdge> list = edgesByPolygon.get(key);
        	GPolygon mGPolygon = new GPolygon();
        	polygonsList.add(mGPolygon);
        	for(GraphEdge mGraphEdge : list){
	        	mGPolygon.addPoint((int)mGraphEdge.x1, (int)mGraphEdge.y1);
				mGPolygon.addPoint((int)mGraphEdge.x2, (int)mGraphEdge.y2);
        	}
        	mGPolygon.site = key;
        }
        end = System.currentTimeMillis();
        System.out.println("Polygon Search Time: " + (end-start) + " mS");
        
        // Creo la imagen con el mapa (http://trac.erichseifert.de/vectorgraphics2d/wiki/Usage)
        start = System.currentTimeMillis();
		mSvgGraphics2D = new SVGGraphics2D(0, 0, width, height);
		mSvgGraphics2D.setBackground(new Color(255,0,0));
	    
		// Ordeno los puntos y creo el Grafico SVG con los colores correspondientes
		for(int n = 0; n < polygonsList.size(); ++n){
			GPolygon mPolygon = polygonsList.get(n);
			
			ColorCafe rgbCafe = perlinMap.getValue((int)xValues[mPolygon.site], (int)yValues[mPolygon.site]);
			Color mColor = new Color(rgbCafe.getRed(), rgbCafe.getGreen(), rgbCafe.getBlue());
        	mSvgGraphics2D.setColor(mColor);
        	
        	polygonsList.set(n, orderPoints(polygonsList.get(n)));
        	polygonsList.get(n).setColor(mColor);
	    	mSvgGraphics2D.fillPolygon(polygonsList.get(n));
		}
		
	    end = System.currentTimeMillis();
        System.out.println("Polygon Fill and order Time: " + (end-start) + " mS");
        System.out.println("Points quantity: " + pointsList.length);
        System.out.println("Map Generation Finished");
                
		return mSvgGraphics2D;
	}
	
	/**
	 * Obtiene la lista de los polígonos de Voronoi
	 * @return
	 */
	public List<GPolygon> getPolygonList (){
		return polygonsList;
	}
	
	/**
	 * Guarda el mapa generado en una imagen vectorial .svg
	 * @param name nombre del archivo (sin el .svg)
	 */
	public void saveSVGMap (final String name){
		// Guardo la imagen como SVN
	    FileOutputStream file;
		try { file = new FileOutputStream(name + ".svg"); }
		catch (FileNotFoundException e) { e.printStackTrace(); return;}
		
        try { file.write(mSvgGraphics2D.getBytes()); }
        catch (IOException e) { e.printStackTrace(); }

        try { file.close(); }
        catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Guarda el mapa generado en un archivo binario .map el cual contiene todas las coordenadas de los
	 * polígonos en orden, su color, y dimensiones del mapa. El archivo de guarda en formato binario
	 * del siguiente modo, siendo siempre el MSB primero y TODOS los numeros positivos:
	 * 1) 4 bytes ancho del mapa
	 * 2) 4 bytes alto del mapa
	 * 3) 4 bytes número de polígonos
	 * 4) Por cada polígono se almacena:
	 * 		a) 2 bytes que indican el número de puntos del polígono
	 * 		b) Se escriben 8 bytes para las coordenadas de cada punto del polígono:
	 * 			.) 4 bytes indicando coordenada x
	 * 			.) 4 bytes indicando coordenada y
	 * 		c) 2 bytes del color rojo
	 * 		d) 2 bytes del color verde
	 * 		e) 2 bytes del color azul
	 * @param name nombre del archivo a guardar (sin el .map)
	 * @param log define si se crea un archivo log.txt donde se almacena un log del progreso
	 */
	public void saveBinaryFile (final String name, boolean log){
		try {
			PrintStream logFile = null;
			if(log) logFile = new PrintStream(new FileOutputStream("log.txt"));
			DataOutputStream out = new DataOutputStream(new FileOutputStream(name + ".map"));
			
			System.out.println("Polygon quantity: " + polygonsList.size());
			if(log) logFile.println("Polygon quantity: " + polygonsList.size());
			
			out.writeInt(width);
			out.writeInt(height);
			out.writeInt(polygonsList.size());
			for(GPolygon mGPolygon : polygonsList){
				savePolygon(out, mGPolygon, logFile);
			}
			
		} catch (FileNotFoundException e) { e.printStackTrace(); }
		  catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Guarda un polígono en el DataOutputStream. El polígono es guardado de la siguiente manera,
	 * siendo en todos los casos primero guardandose el MSB y luego el LSB. TODOS los numeros son
	 * positivos:
	 * 1) 2 bytes que indican el número de puntos del polígono
	 * 2) Se escriben 8 bytes para las coordenadas de cada punto del polígono:
	 * 		a) 4 bytes indicando coordenada x
	 * 		b) 4 bytes indicando coordenada y
	 * 3) 2 bytes del color rojo
	 * 4) 2 bytes del color verde
	 * 5) 2 bytes del color azul
	 * @param out donde guardar los datos
	 * @param mPolygon polígono a almacenar
	 * @param log PrintStream donde almacenar el log.txt si es null no se utiliza
	 */
	private void savePolygon (DataOutputStream out, GPolygon mPolygon, PrintStream log){
		try {
			out.writeShort(mPolygon.npoints);
			if(log != null) log.println("pointCount: " + mPolygon.npoints);
			for(int n = 0; n < mPolygon.npoints; ++n){
				out.writeInt(mPolygon.xpoints[n]);
				out.writeInt(mPolygon.ypoints[n]);
				if(log != null) log.println("(" + mPolygon.xpoints[n] + "," + mPolygon.ypoints[n] + ")");
			}
			out.writeShort(mPolygon.mColor.getRed());
			out.writeShort(mPolygon.mColor.getGreen());
			out.writeShort(mPolygon.mColor.getBlue());
			if(log != null) log.println("R: " + mPolygon.mColor.getRed() + " G: " + mPolygon.mColor.getGreen() +
					" B: " + mPolygon.mColor.getBlue());
			if(log != null) log.println();
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Crea un HeightMap usando Perlin Noise con la libreria SimpleNoise para Java
	 * Original: http://simplynoise.com/
	 * @param n
	 * @return
	 */
	private ImageCafe generatePerlinMap (final double n, final int freq){
		try {
			Perlin myModule = new Perlin();
	    	myModule.setFrequency(freq);
			myModule.setOctaveCount(6);
	    	myModule.setPersistence(0.5);
	    	myModule.setSeed(seed);
	
	    	NoiseMap heightMap = new NoiseMap(width, height);
	    	NoiseMapBuilderPlane heightMapBuilder = new NoiseMapBuilderPlane();
	    	heightMapBuilder.setDestSize(heightMap.getWidth(), heightMap.getHeight());
	    	heightMapBuilder.setSourceModule (myModule);
	    	heightMapBuilder.setDestNoiseMap (heightMap);
	    	heightMapBuilder.setBounds (6.0, 10.0, 1.0, 5.0);
	    	heightMapBuilder.build();
	
	    	RendererImage renderer = new RendererImage();
	    	renderer.clearGradient ();
	    	renderer.addGradientPoint (-1.0000, new ColorCafe (0, 0, 0, 255));
	    	renderer.addGradientPoint ( n, 		new ColorCafe (0, 0, 0, 255));
	    	renderer.addGradientPoint ( n+0.01, new ColorCafe (255, 255, 255, 255));
	    	renderer.addGradientPoint ( 1.0000, new ColorCafe (255, 255, 255, 255));
	
	    	ImageCafe image = new ImageCafe(heightMap.getWidth(), heightMap.getHeight());
	    	renderer.setSourceNoiseMap (heightMap);
	    	renderer.setDestImage (image);
	    	renderer.enableLight(true);
	    	renderer.setLightContrast(3.0); 	// Triple the contrast
	    	renderer.setLightBrightness(2.0); 	// Double the brightness
	    	renderer.render();
	    	
	    	return image;
		} catch (ExceptionInvalidParam e) { e.printStackTrace(); return null;}
	}

	/**
	 * Determina el valor mínimo de la lista
	 * @param list lista de valores
	 * @param lenght cantidad de valores a muestrear desde el 0
	 * @return array siendo index [0] el número mínimo y index [1] la posición en al array
	 * pasado donde se encuentra el número mínimo
	 */
	private int[] minValue (int[] list, int lenght){
		int min = list[0], minIndex = 0;
		for(int n = 0; n < lenght; ++n){
			if(list[n] < min){
				min = list[n];
				minIndex = n;
			}
		}
		return new int[] {min, minIndex};
	}

	/**
	 * Determina si un punto esta sobre la línea (0), del lado derecho (>0), del lado izquierdo (<0)
	 * @param x coordenada x del punto
	 * @param y coordenada y del punto
	 * @param x1 x1 de la linea
	 * @param y1 y1 de la linea
	 * @param x2 x2 de la linea
	 * @param y2 y2 de la linea
	 * @return
	 */
	private int pointPosition (int x, int y, int x1, int y1, int x2, int y2){
		return ( (x2-x1)*(y-y1) - (y2-y1)*(x-x1) );
	}
	
	/**
	 * Ordena los puntos de un polígono de forma que pueda ser dibujado utilizando el
	 * Gift Wrapping Algorithm.
	 * http://stackoverflow.com/questions/10020949/gift-wrapping-algorithm
	 * http://en.wikipedia.org/wiki/Gift_wrapping_algorithm
	 * @param mPolygon polígono a ordenar
	 * @return nuevo polígono ordenado para dibujar
	 */
	private GPolygon orderPoints (GPolygon mPolygon){
		
		int[] data = minValue(mPolygon.xpoints, mPolygon.npoints);
		Point2D pointOnHull = new Point2D(data[0], mPolygon.ypoints[data[1]]);
		Point2D endPoint = new Point2D();
		GPolygon hull = new GPolygon();
		
		do{
			hull.addPoint(pointOnHull.x, pointOnHull.y);
			endPoint.x = mPolygon.xpoints[0];
			endPoint.y = mPolygon.ypoints[0];
			
			for(int k = 0; k < mPolygon.npoints; ++k){
				if(endPoint.isEqual(pointOnHull) || 
					pointPosition(mPolygon.xpoints[k], mPolygon.ypoints[k],
							pointOnHull.x, pointOnHull.y,
							endPoint.x, endPoint.y) < 0 ){
					endPoint.x = mPolygon.xpoints[k];
					endPoint.y = mPolygon.ypoints[k];
				}
			}
			pointOnHull.x = endPoint.x;
			pointOnHull.y = endPoint.y;
		}while(!endPoint.isEqual(hull.xpoints[0], hull.ypoints[0]));
		
		return hull;
	}
}
