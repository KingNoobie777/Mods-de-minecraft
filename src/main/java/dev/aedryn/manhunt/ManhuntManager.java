package dev.aedryn.manhunt;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Objective;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class ManhuntManager implements ModInitializer {
    private static boolean configured;
    private static boolean running;
    private static boolean finished;
    private static int ticks;
    private static final String PREFIX = "§6[Manhunt] §r";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(literal("manhunt")
                .then(literal("setup")
                    .then(argument("runner", StringArgumentType.word())
                        .then(argument("hunters", StringArgumentType.greedyString())
                            .executes(context -> setup(
                                context.getSource(),
                                StringArgumentType.getString(context, "runner"),
                                StringArgumentType.getString(context, "hunters")
                            )))))
                .then(literal("start").executes(context -> start(context.getSource())))
                .then(literal("stop").executes(context -> stop(context.getSource())))
                .then(literal("reset").executes(context -> reset(context.getSource())))
                .then(literal("status").executes(context -> status(context.getSource())))
            )
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!running || !configured || finished) return;
            ticks++;

            if (ticks % 5 == 0) {
                // Vanilla compass targets the world spawn, so move it to the runner.
                run(server, "execute at @a[tag=runner,limit=1] run setworldspawn ~ ~ ~");

                // A hunter gets a compass after a death; reset immediately to prevent duplicates.
                run(server, "give @a[tag=hunter,scores={deaths=1..}] minecraft:compass");
                run(server, "scoreboard players reset @a[tag=hunter,scores={deaths=1..}] deaths");
            }

            if (ticks % 20 == 0) {
                run(server, "execute as @e[type=minecraft:item] if items entity @s contents minecraft:compass run kill @s");
                checkVictory(server);

                if (running && !finished) {
                    int seconds = ticks / 20;
                    String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
                    run(server, "title @a actionbar {\"text\":\"MANHUNT  " + time + "\",\"color\":\"gold\"}");
                }
            }
        });
    }

    private static int setup(CommandSourceStack source, String runner, String hunterList) {
        MinecraftServer server = source.getServer();
        if (!safeName(runner)) return error(source, "Nombre de Runner no válido.");

        String[] hunters = hunterList.split(",");
        if (hunters.length == 0) return error(source, "Selecciona al menos un Hunter.");

        for (int i = 0; i < hunters.length; i++) {
            hunters[i] = hunters[i].trim();
            if (!safeName(hunters[i]) || hunters[i].equalsIgnoreCase(runner)) {
                return error(source, "Lista de Hunters no válida.");
            }
            if (server.getPlayerList().getPlayerByName(hunters[i]) == null) {
                return error(source, "El Hunter " + hunters[i] + " no está conectado.");
            }
        }
        if (server.getPlayerList().getPlayerByName(runner) == null) {
            return error(source, "El Runner no está conectado.");
        }

        configured = true;
        running = false;
        finished = false;
        ticks = 0;

        run(server, "gamerule commandBlockOutput false");
        run(server, "scoreboard objectives add deaths deathCount");
        run(server, "scoreboard objectives add dragonKill minecraft.killed:minecraft.ender_dragon");
        run(server, "scoreboard objectives add gameOver dummy");
        run(server, "tag @a remove runner");
        run(server, "tag @a remove hunter");
        run(server, "tag " + runner + " add runner");
        run(server, "scoreboard players reset @a deaths");
        run(server, "scoreboard players reset @a dragonKill");
        run(server, "scoreboard players set win gameOver 0");

        for (String hunter : hunters) {
            run(server, "tag " + hunter + " add hunter");
            // Save each hunter's current position as their respawn point.
            run(server, "spawnpoint " + hunter);
        }
        run(server, "execute at " + runner + " run setworldspawn ~ ~ ~");

        source.sendSuccess(() -> Component.literal(PREFIX + "Partida preparada. Runner: " + runner
            + " | Hunters: " + String.join(", ", hunters)), false);
        source.sendSuccess(() -> Component.literal(PREFIX + "Pulsa INICIAR en el menú o usa /manhunt start."), false);
        return 1;
    }

    private static int start(CommandSourceStack source) {
        if (!configured) return error(source, "Primero prepara la partida.");
        MinecraftServer server = source.getServer();
        ticks = 0;
        finished = false;
        running = true;
        run(server, "scoreboard players set win gameOver 0");
        run(server, "scoreboard players reset @a deaths");
        run(server, "scoreboard players reset @a dragonKill");
        run(server, "give @a[tag=hunter] minecraft:compass");
        run(server, "title @a title {\"text\":\"MANHUNT START!\",\"color\":\"gold\",\"bold\":true}");
        run(server, "title @a subtitle {\"text\":\"Good luck!\",\"color\":\"yellow\"}");
        source.sendSuccess(() -> Component.literal(PREFIX + "¡Manhunt iniciado!"), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        running = false;
        source.sendSuccess(() -> Component.literal(PREFIX + "Partida detenida."), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        running = false;
        configured = false;
        finished = false;
        ticks = 0;
        run(server, "tag @a remove runner");
        run(server, "tag @a remove hunter");
        run(server, "scoreboard players reset @a deaths");
        run(server, "scoreboard players reset @a dragonKill");
        run(server, "scoreboard players set win gameOver 0");
        run(server, "title @a clear");
        source.sendSuccess(() -> Component.literal(PREFIX + "Roles y estado de partida reiniciados."), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        String state = running ? "EN CURSO" : configured ? "PREPARADA" : "SIN CONFIGURAR";
        int elapsed = ticks / 20;
        source.sendSuccess(() -> Component.literal(PREFIX + "Estado: " + state
            + (running ? " | Tiempo: " + String.format("%02d:%02d", elapsed / 60, elapsed % 60) : "")), false);
        return 1;
    }

    private static void checkVictory(MinecraftServer server) {
        // Runner death takes priority if both win conditions are met in the same second.
        run(server, "execute if entity @a[tag=runner,scores={deaths=1..}] unless score win gameOver matches 1 run title @a title {\"text\":\"HUNTERS WIN!\",\"color\":\"red\",\"bold\":true}");
        run(server, "execute if entity @a[tag=runner,scores={deaths=1..}] unless score win gameOver matches 1 run title @a subtitle {\"text\":\"The Runner has died\",\"color\":\"yellow\"}");
        run(server, "execute if entity @a[tag=runner,scores={deaths=1..}] unless score win gameOver matches 1 run scoreboard players set win gameOver 1");

        run(server, "execute if entity @a[tag=runner,scores={dragonKill=1..}] unless score win gameOver matches 1 run title @a title {\"text\":\"RUNNER WINS!\",\"color\":\"green\",\"bold\":true}");
        run(server, "execute if entity @a[tag=runner,scores={dragonKill=1..}] unless score win gameOver matches 1 run title @a subtitle {\"text\":\"Ender Dragon defeated\",\"color\":\"yellow\"}");
        run(server, "execute if entity @a[tag=runner,scores={dragonKill=1..}] unless score win gameOver matches 1 run scoreboard players set win gameOver 1");

        if (hasWinner(server)) {
            finished = true;
            running = false;
            run(server, "title @a actionbar {\"text\":\"GAME OVER\",\"color\":\"red\",\"bold\":true}");
        }
    }

    private static boolean hasWinner(MinecraftServer server) {
        Objective objective = server.getScoreboard().getObjective("gameOver");
        if (objective == null) return false;
        return server.getScoreboard()
            .getOrCreatePlayerScore(ScoreHolder.forNameOnly("win"), objective)
            .get() == 1;
    }

    private static boolean safeName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }

    private static int error(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(PREFIX + message));
        return 0;
    }

    private static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }
}
