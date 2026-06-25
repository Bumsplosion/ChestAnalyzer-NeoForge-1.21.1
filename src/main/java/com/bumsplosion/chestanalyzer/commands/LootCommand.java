package com.bumsplosion.chestanalyzer.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import com.bumsplosion.chestanalyzer.analyzer.LootCore;
import com.bumsplosion.chestanalyzer.analyzer.LootExports;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Path;

public class LootCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("analyzeloot")
                        .requires(s -> s.hasPermission(2))

                        // =====================
                        // RUN_ALL
                        // =====================
                        .then(Commands.literal("run_all")
                                .then(Commands.argument("iterations", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {

                                            var source = ctx.getSource();
                                            var level = source.getLevel();
                                            int iterations = IntegerArgumentType.getInteger(ctx, "iterations");

                                            Map<String, LootCore.TableExport> results = new HashMap<>();

                                            var server = source.getServer();

                                            server.reloadableRegistries()
                                                    .getKeys(Registries.LOOT_TABLE)
                                                    .forEach(id -> {

                                                        String full = id.toString();

                                                        if (!full.startsWith("minecraft:chests/")) return;

                                                        var result = LootCore.simulate(level, id, iterations);
                                                        var table = LootCore.buildTable(result);

                                                        results.put(full, table);
                                                    });

                                            LootExports.LAST_RESULTS = results;

                                            source.sendSuccess(() ->
                                                    Component.literal(
                                                            "Completed run_all on " + results.size() + " loot tables"
                                                    ), false);

                                            return 1;
                                        })
                                )
                        )

                        // =====================
                        // RUN
                        // =====================
                        .then(Commands.literal("run")
                                .then(Commands.argument("table", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> {

                                            var server = ctx.getSource().getServer();

                                            server.reloadableRegistries()
                                                    .getKeys(Registries.LOOT_TABLE)
                                                    .forEach(id -> {
                                                        if (id.toString().startsWith("minecraft:chests/")) {
                                                            builder.suggest(id.toString());
                                                        }
                                                    });

                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("iterations", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {

                                                    var source = ctx.getSource();
                                                    var level = source.getLevel();

                                                    var tableId = ResourceLocationArgument.getId(ctx, "table");
                                                    int iterations = IntegerArgumentType.getInteger(ctx, "iterations");

                                                    var result = LootCore.simulate(level, tableId, iterations);
                                                    var table = LootCore.buildTable(result);

                                                    // STORE LAST RESULT
                                                    LootExports.LAST_RESULTS = Map.of(tableId.toString(), table);

                                                    source.sendSuccess(() ->
                                                            Component.literal("Loot Scan Complete → " + tableId), false);

                                                    table.items().forEach((id, stats) ->
                                                            source.sendSuccess(() ->
                                                                    Component.literal(
                                                                            id + " → " + String.format("%.2f", stats.chance()) + "%"
                                                                    ), false)
                                                    );

                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // =====================
                        // EXPORT
                        // =====================
                        .then(Commands.literal("export")
                                .then(Commands.argument("filename", StringArgumentType.word())
                                        .executes(ctx -> {

                                            var source = ctx.getSource();
                                            String filename = StringArgumentType.getString(ctx, "filename");

                                            Path file = LootExports.exportLatest(filename);

                                            source.sendSuccess(() ->
                                                    Component.literal("Exported → " + file.toAbsolutePath()), false);

                                            return 1;
                                        })
                                )
                        )

                        // =====================
                        // STRUCTURES
                        // =====================
                        .then(Commands.literal("structures")
                                .executes(ctx -> {

                                    var source = ctx.getSource();
                                    var server = source.getServer();

                                    Path file = LootExports.exportStructures(server);

                                    source.sendSuccess(() ->
                                            Component.literal("Exported → " + file.toAbsolutePath()),false);

                                    return 1;
                                })
                        )
        );
    }
}