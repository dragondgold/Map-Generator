package com.andres.map.generator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class MapGenerator {

	public static void main(String[] args) { 
		
		int height, width, seed, frec, dist;
		boolean svg = false, binary = false, log;
		String name;
		
		Options commandLineOptions = new Options();
		CommandLineParser mCommandLineParser = new PosixParser(); 
		CommandLine mCommandLine;
		
		commandLineOptions.addOption("w", "width", true, "Ancho del mapa");
		commandLineOptions.addOption("h", "height", true, "Alto del mapa");
		commandLineOptions.addOption("d", "distance", true, "Minima distancia entre los puntos para" +
				"generar el diagrama de Voronoi, por defecto 3");
		commandLineOptions.addOption("s", "seed", true, "Seed usado para la generacion aleatoria, si no" +
				"se define es aleatoria");
		commandLineOptions.addOption("f", "frequency", true, "Frecuencia del Perlin Noise, por defecto 5");
		commandLineOptions.addOption("svg", "svg", true, "Guarda el mapa en formato svg con el nombre dado");
		commandLineOptions.addOption("b", "binary", true, "Guarda el mapa en formato binario .map con el nombre dado");
		commandLineOptions.addOption("l", "log", false, "Define si se crea un archivo log.txt con el proceso de creacion del archivo binario");
		commandLineOptions.addOption("help", false, "Imprime esto");
		
		// Analizo los comandos pasados
		try { mCommandLine = mCommandLineParser.parse(commandLineOptions, args); }
		catch (ParseException e2) { e2.printStackTrace(); return; }
		
		// Help
		if(mCommandLine.hasOption("help")){
			HelpFormatter mHelpFormatter = new HelpFormatter();
			mHelpFormatter.printHelp("eclipseExporter", commandLineOptions);
			return;
		}
		// Width
		if(mCommandLine.getOptionValues("width") != null) width = Integer.decode(mCommandLine.getOptionValue("width"));
		else{
			System.out.println("ERROR - Debe especificar el ancho del mapa"); return;
		}
		// Height
		if(mCommandLine.getOptionValues("height") != null) height = Integer.decode(mCommandLine.getOptionValue("height"));
		else{
			System.out.println("ERROR - Debe especificar el alto del mapa"); return;
		}
		// Distance
		if(mCommandLine.getOptionValues("distance") != null) dist = Integer.decode(mCommandLine.getOptionValue("distance"));
		else dist = 3;
		// Seed
		if(mCommandLine.getOptionValues("seed") != null) seed = Integer.decode(mCommandLine.getOptionValue("seed"));
		else seed = (int)System.currentTimeMillis();
		// Frequency
		if(mCommandLine.getOptionValues("frequency") != null) frec = Integer.decode(mCommandLine.getOptionValue("frequency"));
		else frec = 5;
		
		// Save mode
		if(mCommandLine.getOptionValue("svg") == null && mCommandLine.getOptionValue("binary") == null){
			System.out.println("ERROR - Debe especificar el formato en que guardar el archivo"); return;
		}
		// SVG y Binario
		if(mCommandLine.getOptionValue("svg") != null){
			svg = true; name = mCommandLine.getOptionValue("svg");
		}else if(mCommandLine.getOptionValue("binary") != null){
			binary = true; name = mCommandLine.getOptionValue("binary");
		}else{
			System.out.println("ERROR - Debe especificar el nombre del archivo a guardar"); return;
		}
		
		// Log
		log = mCommandLine.hasOption("log");
		
		// Creo el mapa en base a los parametros
		RandomMapGenerator mapGenerator = new RandomMapGenerator();
		mapGenerator.generateMap(width, height, dist, seed, frec);
		if(svg) mapGenerator.saveSVGMap(name);
		else if(binary) mapGenerator.saveBinaryFile(name, log);
		
	}
	
}
