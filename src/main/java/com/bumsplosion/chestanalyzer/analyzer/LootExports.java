package com.bumsplosion.chestanalyzer.analyzer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.Registries;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

public class LootExports {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // =========================
    // EXPORT + LAST RESULTS
    // =========================
    public static Map<String, LootCore.TableExport> LAST_RESULTS = new HashMap<>();

    public static Path exportLatest(String filename) {

        if (LAST_RESULTS == null || LAST_RESULTS.isEmpty()) {
            throw new IllegalStateException("No previous analyzeloot results found.");
        }

        try {
            String safe = filename.replaceAll("[^a-zA-Z0-9-_]", "_");

            Path folder = Path.of("loot_exports");
            Files.createDirectories(folder);

            Map<String, Object> wrapper = new TreeMap<>();
            wrapper.put("tables", LAST_RESULTS);

            Path file = folder.resolve(safe + ".json");

            Files.writeString(file, GSON.toJson(wrapper));

            return file;

        } catch (Exception e) {
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    // =========================
    // STRUCTURE EXPORT
    // =========================
    public static Path exportStructures(MinecraftServer server) {

        Map<String, Map<String, String>> out = new TreeMap<>();

        server.reloadableRegistries()
                .getKeys(Registries.LOOT_TABLE)
                .forEach(id -> {

                    String path = id.getPath();
                    String namespace = id.getNamespace();

                    if (!path.startsWith("chests/")) return;

                    String structure = path.substring("chests/".length());

                    out.computeIfAbsent(namespace, k -> new TreeMap<>())
                            .put(structure, id.toString());
                });

        try {
            Path folder = Path.of("loot_exports");
            Files.createDirectories(folder);

            Path file = folder.resolve("loot_structures.json");
            Files.writeString(file, GSON.toJson(out));

            return file;

        } catch (Exception e) {
            throw new RuntimeException("Structure export failed: " + e.getMessage(), e);
        }
    }
}