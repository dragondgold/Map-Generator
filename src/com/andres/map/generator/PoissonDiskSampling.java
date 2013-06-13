package com.andres.map.generator;

import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

public class PoissonDiskSampling {

	long seed;
	float cellSize;
	int gridWidth;
	int gridHeight;
	int width;
	int height;
	Random crazy = new Random();
	
	PoissonDiskSampling() {
		seed = System.currentTimeMillis();
	}
	
	PoissonDiskSampling(long seed) {
		seed = this.seed;
	}

	/**
	 * Genera puntos en un plano de tamaño 'width' y 'height' usando el algoritmo Poisson Disk Sampling.
	 * @param minDist distancia mínima entre los puntos 
	 * @param pointsPerIteration es la cantidad de puntos que se analizan/agregan por iteración,
	 * el valor que mejor suele ajustarse es 30.
	 */
	public Point[] generatePoisson(int width, int height, float minDist, int pointsPerIteration) {
		
		// Tamaño de la celda (asi lo define el algoritmo)
		cellSize = minDist / (float)Math.sqrt(2.0);
	
		// Ancho y alto de la grilla basado en el tamaño de la celda
		gridWidth = (int)((float)width / cellSize) + 1;
		gridHeight = (int)((float)height / cellSize) + 1;
	
		this.height = height;
		this.width = width;
		
		System.out.println("gridWidth: " + gridWidth);
		System.out.println("gridHeight: " + gridHeight);
		System.out.println("cellSize: " + cellSize);
		
		Point pointsGrid[][] = new Point[gridWidth][gridHeight]; // Grilla en donde cada (x,y) hay un punto
		Stack<Point> proccessList = new Stack<Point>();
		Stack<Point> samplePoints = new Stack<Point>();
	
		// Inicializo el grid
		for(int x = 0; x < gridWidth; ++x){
			for(int y = 0; y < gridHeight; ++y){
				pointsGrid[x][y] = new Point(-1,-1);
			}
		}
		
		// Semilla para la generacion aleatoria
		crazy.setSeed(seed);
	
		// Genero el primer punto aleatorio
		Point firstPoint = new Point(crazy.nextInt(width), crazy.nextInt(height));
		proccessList.push(firstPoint);
		samplePoints.push(firstPoint);
		pointsGrid[realToGrid(firstPoint).x][realToGrid(firstPoint).y] = firstPoint;
	
		// Continuo hasta que no tenga más puntos para procesar (grilla llena)
		while(!proccessList.empty()){
	
			// Punto desde el cual genero el resto de los puntos alrededor
			//System.out.println("Get List");
			Point mPoint = proccessList.pop();
			//System.out.println("proccessList size: " + proccessList.size());
	
			for(int n = 0; n < pointsPerIteration; ++n){
				// Genero un punto aleatorio
				Point newPoint = getRandomPointAround(mPoint, minDist);
				//System.out.println("Random Point (" + newPoint.x + "," + newPoint.y + ")");
	
				// Si el punto se encuentra dentro del area del area de la grilla y no esta cerca de otros puntos lo creo
				if(isInsideDefinedArea(newPoint) &&
						!isInsideNeighbourhood(newPoint, minDist, pointsGrid)){
					//System.out.println("Adding Point (" + newPoint.x + "," + newPoint.y + ")");
					proccessList.push(newPoint);
					samplePoints.push(newPoint);
					pointsGrid[realToGrid(newPoint).x][realToGrid(newPoint).y] = newPoint;
				}
			}
		}
	
		System.out.println("Voronoi finished");
		return Arrays.copyOf(samplePoints.toArray(), samplePoints.toArray().length, Point[].class);
	}
	
	/**
	 * Determina si hay algún punto demasiado cerca (menor a minDist) del punto que se pasa como 
	 * argumento considerando un radio de analisis de 2 grillas (cuadritos)
	 */
	private boolean isInsideNeighbourhood (Point mPoint, float minDist, Point grid[][]){
	
		java.awt.Point gridPos = realToGrid(mPoint);
	 
		int x = (gridPos.x < 2) ? 0 : gridPos.x - 2;
		int limitX = (gridPos.x + 2 > gridWidth-1) ? gridWidth-1 : gridPos.x + 2;
	
		int y, limitY;
	
		for(;x <= limitX; ++x){
			y = (gridPos.y < 2) ? 0 : gridPos.y - 2;
			limitY = (gridPos.y + 2 > gridHeight)  ? gridHeight : gridPos.y + 2;
			for(; y < limitY; ++y){
				//std::cout << "X: " << x << " - Y: " << y << std::endl;
				if(getDistance(mPoint, grid[x][y]) < minDist){
					//System.out.println("Inside Neighbourhood");
					return true;
				}
			}
		}
		//System.out.println("Outside Neighbourhood");
		return false;
	}
	
	/**
	 * Computa la distancia entre dos puntos
	 */
	private double getDistance(Point point1, Point point2){
		double dx = point1.x - point2.x;
		double dy = point1.y - point2.y;
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	/**
	 * Obtiene el número de grilla (x,y) donde se encuentra el punto
	 */
	private java.awt.Point realToGrid (Point mPoint){
		
		java.awt.Point gridCord = new java.awt.Point();
		
		gridCord.x = (int)(mPoint.x / cellSize);
		gridCord.y = (int)(mPoint.y / cellSize);
	
		return gridCord;
	}
	
	/**
	 * Verifica que el punto se encuentre dentro del area de dibujo
	 */
	private boolean isInsideDefinedArea (Point mPoint){
		if(mPoint.x < 0 || mPoint.x > width || mPoint.y < 0 || mPoint.y > height) return false;
		else return true;
	}
	
	private Point getRandomPointAround (Point mPoint, float minDist){
	
		Point newPoint = new Point();
		
		// http://stackoverflow.com/questions/3680637/how-to-generate-a-random-double-in-a-given-range
		// Número aleatorio entre 0 y 1
		double r1 = 0 + (1 - 0) * crazy.nextDouble();
		double r2 = 0 + (1 - 0) * crazy.nextDouble();
	
		// Radio aleatorio entre minDist y 2minDist
		double radio = minDist * (r1 + 1);
		// Angulo aleatorio
		double angle = 2 * Math.PI * r2;
	
		newPoint.x = mPoint.x + radio * Math.cos(angle);
		newPoint.y = mPoint.y + radio * Math.sin(angle);
		
		return newPoint;
	}

}
