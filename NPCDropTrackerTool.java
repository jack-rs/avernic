package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import osv.util.Misc;

/**
 * 
 * sara-kc = 0 arma-kc = 0 bandos-kc = 0 zammy-kc = 0 kbd-kc = 0 chaos-kc = 0 barrel-kc = 0 mole-kc = 0 dagannoth-kc = 0 kraken-kc = 0 callis-kc = 0 venena-kc = 0
 * 
 * KRAKEN, GIANT_MOLE, CHAOS_ELEMENTAL, KALPHITE_QUEEN, CALLISTO, VENENATIS, VETION, KING_BLACK_DRAGON, GENERAL_GRAARDOR, COMMANDER_ZILYANA, KREE_ARRA, KRIL_TSUTSAROTH,
 * DAGANNOTH_REX, DAGANNOTH_SUPREME, DAGANNOTH_PRIME, BARRELCHEST;
 * 
 * @author Jason MacKeigan
 * @date Feb 6, 2015, 2:24:32 PM
 */
public class NPCDropTrackerTool {

	public static void main(String... args) throws IOException {
		Path path = Paths.get("./Data", "characters");
		File[] files = path.toFile().listFiles();
		System.out.println("Starting modification app.");
		System.out.println(Misc.insertCommas(Integer.toString(files.length)) + " files have been found.");
		System.out.println("Starting to modify character files, please wait...");
		for (File file : files) {
			List<String> fileLines = new ArrayList<>();
			Map<String, Integer> data = new HashMap<>();
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.equals("[EOF]")) {
						continue;
					}
					if (line.split("=").length < 2) {
						fileLines.add(line);
						continue;
					}
					String key = line.split("=")[0].trim();
					int value;
					try {
						value = Integer.parseInt(line.split("=")[1].trim());
					} catch (NumberFormatException nfe) {
						fileLines.add(line);
						continue;
					}
                    switch (key) {
                        case "sara-kc":
                            data.put("Commander Zilyana", value);
                            break;
                        case "arma-kc":
                            data.put("Kree Arra", value);
                            break;
                        case "bandos-kc":
                            data.put("General Graardor", value);
                            break;
                        case "zammy-kc":
                            data.put("Kril Tsutsaroth", value);
                            break;
                        case "kbd-kc":
                            data.put("King Black Dragon", value);
                            break;
                        case "chaos-kc":
                            data.put("Chaos Elemental", value);
                            break;
                        case "barrel-kc":
                            data.put("Barrelchest", value);
                            break;
                        case "mole-kc":
                            data.put("Giant Mole", value);
                            break;
                        case "kraken-kc":
                            data.put("Kraken", value);
                            break;
                        case "callis-kc":
                            data.put("Callisto", value);
                            break;
                        case "venena-kc":
                            data.put("Venenatis", value);
                            break;
                        default:
                            fileLines.add(line);
                            break;
                    }
				}
            }
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				for (String line : fileLines) {
					writer.write(line);
					writer.newLine();
				}
				writer.newLine();
				writer.write("[NPC-TRACKER]");
				writer.newLine();
				for (Entry<String, Integer> entry : data.entrySet()) {
					writer.write(entry.getKey() + " = " + entry.getValue());
					writer.newLine();
				}
				writer.newLine();
				writer.write("[EOF]");
            }

		}
		System.out.println("Finished!");
	}
}
