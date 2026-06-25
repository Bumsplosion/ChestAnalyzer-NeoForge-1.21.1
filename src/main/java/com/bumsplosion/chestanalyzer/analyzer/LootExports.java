package com.bumsplosion.chestanalyzer.analyzer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.Registries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class LootExports {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Object LAST_RESULT;
    public static String LAST_TYPE;

    // =========================
    // DEEP EXPORT MODEL
    // =========================
    public record DeepExport(
            String table,
            int iterations,
            LootCore.TableExport data
    ) {}

    // =========================
    // DEEP SCAN ENTRY
    // =========================
    public static LootCore.TableExport deepScan(ServerLevel level, ResourceLocation id, int max) {
        return LootCore.runDeep(level, id, max);
    }

    // =========================
    // EXPORT DEEP
    // =========================
    public static void exportDeep(
            LootCore.TableExport result,
            String tableId,
            String fileName
    ) {

        try {
            String safe = (fileName == null || fileName.isBlank())
                    ? "deep_" + tableId.replace(":", "_") + "_" + System.currentTimeMillis()
                    : fileName.replaceAll("[^a-zA-Z0-9-_]", "_");

            DeepExport export = new DeepExport(
                    tableId,
                    result.iterations(),
                    result
            );

            Path folder = Path.of("loot_exports");
            Files.createDirectories(folder);

            Files.writeString(folder.resolve(safe + ".json"), GSON.toJson(export));

        } catch (Exception e) {
            throw new RuntimeException("Deep export failed: " + e.getMessage(), e);
        }
    }

    // =========================
    // EXPORT LATEST
    // =========================
    public static void exportLatest(String filename) {

        if (LAST_RESULT == null) {
            throw new IllegalStateException("No previous result");
        }

        try {
            String safe = filename.replaceAll("[^a-zA-Z0-9-_]", "_");

            Path folder = Path.of("loot_exports");
            Files.createDirectories(folder);

            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("type", LAST_TYPE);
            wrapper.put("data", LAST_RESULT);

            Files.writeString(folder.resolve(safe + ".json"), GSON.toJson(wrapper));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================
    // STRUCTURE EXPORT
    // =========================
    public static void exportStructures(MinecraftServer server) {

        Map<String, Map<String, String>> out = new TreeMap<>();

        server.registryAccess()
                .registryOrThrow(Registries.LOOT_TABLE)
                .keySet()
                .forEach(id -> {

                    String path = id.getPath();

                    if (!path.startsWith("chests/")) return;

                    String structure = path.substring("chests/".length());
                    String mod = id.getNamespace();

                    out.computeIfAbsent(mod, k -> new TreeMap<>())
                            .put(structure, id.toString());
                });

        try {
            Path folder = Path.of("loot_exports");
            Files.createDirectories(folder);

            Path file = folder.resolve("loot_structures.json");
            Files.writeString(file, GSON.toJson(out));

        } catch (Exception e) {
            throw new RuntimeException("Structure export failed: " + e.getMessage(), e);
        }
    }
}