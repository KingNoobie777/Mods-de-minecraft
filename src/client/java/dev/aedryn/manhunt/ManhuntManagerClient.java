package dev.aedryn.manhunt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ManhuntManagerClient implements ClientModInitializer {
    private static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("manhunt_manager", "main")
        );

        // SDL scancode for the right Alt key (AltGr on most Spanish keyboards).
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.manhunt_manager.open_menu",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_RALT,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.level == null || client.player == null) {
                    continue;
                }
                if (client.gui.screen() instanceof ManhuntManagerScreen) {
                    client.gui.setScreen(null);
                } else if (client.gui.screen() == null) {
                    client.gui.setScreen(new ManhuntManagerScreen());
                }
            }
        });
    }

    private static final class ManhuntManagerScreen extends Screen {
        private String runner;
        private final Set<String> hunters = new LinkedHashSet<>();
        private String notice = "Selecciona un Runner y al menos un Hunter.";

        private ManhuntManagerScreen() {
            super(Component.translatable("screen.manhunt_manager.title"));
        }

        @Override
        protected void init() {
            clearWidgets();
            int center = width / 2;
            int listWidth = Math.min(310, width - 36);
            int left = center - listWidth / 2;

            addRenderableWidget(Button.builder(
                Component.literal("PREPARAR PARTIDA"),
                button -> prepareMatch()
            ).bounds(left, height - 76, listWidth, 20).build());

            int half = (listWidth - 8) / 2;
            addRenderableWidget(Button.builder(Component.literal("INICIAR"), button -> send("start"))
                .bounds(left, height - 51, half, 20).build());
            addRenderableWidget(Button.builder(Component.literal("DETENER"), button -> send("stop"))
                .bounds(left + half + 8, height - 51, half, 20).build());

            addRenderableWidget(Button.builder(Component.literal("REINICIAR"), button -> {
                send("reset");
                runner = null;
                hunters.clear();
                notice = "Configuración reiniciada.";
                init();
            }).bounds(left, height - 26, half, 20).build());

            addRenderableWidget(Button.builder(Component.literal("CERRAR"), button -> onClose())
                .bounds(left + half + 8, height - 26, half, 20).build());

            rebuildPlayerButtons();
        }

        private void rebuildPlayerButtons() {
            // Rebuild only the dynamic player list; keep the four action buttons.
            // Screen's widget list is intentionally rebuilt through init on player changes.
            // The list itself is reconstructed below using the current connected players.
            List<String> names = new ArrayList<>();
            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
                client.level.players().forEach(player -> names.add(player.getGameProfile().getName()));
            }
            names.sort(Comparator.naturalOrder());

            int center = width / 2;
            int listWidth = Math.min(310, width - 36);
            int left = center - listWidth / 2;
            int maxRows = Math.max(0, Math.min(names.size(), (height - 150) / 23));

            for (int i = 0; i < maxRows; i++) {
                String name = names.get(i);
                String role = name.equals(runner) ? "RUNNER" : hunters.contains(name) ? "HUNTER" : "SIN ROL";
                String label = name + "   [" + role + "]";
                Button playerButton = Button.builder(Component.literal(label), button -> {
                    if (name.equals(runner)) {
                        runner = null;
                    } else if (hunters.contains(name)) {
                        hunters.remove(name);
                    } else if (runner == null) {
                        runner = name;
                        hunters.remove(name);
                    } else {
                        hunters.add(name);
                    }
                    notice = "Runner: " + (runner == null ? "sin elegir" : runner)
                        + " | Hunters: " + (hunters.isEmpty() ? "ninguno" : String.join(", ", hunters));
                    init();
                }).bounds(left, 68 + i * 23, listWidth, 20).build();
                addRenderableWidget(playerButton);
            }
            if (names.size() > maxRows) {
                notice = "Mostrando " + maxRows + " de " + names.size()
                    + " jugadores. Aumenta el tamaño de la ventana para ver más.";
            }
        }

        private void prepareMatch() {
            if (runner == null) {
                notice = "Selecciona primero un Runner.";
                return;
            }
            if (hunters.isEmpty()) {
                notice = "Selecciona al menos un Hunter.";
                return;
            }
            String hunterList = String.join(",", hunters);
            send("setup " + runner + " " + hunterList);
            notice = "Preparando: " + runner + " contra " + hunterList;
        }

        private void send(String command) {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) {
                notice = "No hay una conexión activa con un mundo.";
                return;
            }
            client.player.connection.sendCommand("manhunt " + command);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractBackground(graphics, mouseX, mouseY, delta);
            int center = width / 2;
            graphics.fill(0, 0, width, height, 0xB8101218);
            graphics.fill(center - 174, 12, center + 174, height - 12, 0xEE202630);
            graphics.fill(center - 174, 12, center + 174, 14, 0xFFFFB84D);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);

            int center = width / 2;
            graphics.centeredText(font, Component.literal("MANHUNT MANAGER"), center, 24, 0xFFFFD18A);
            graphics.centeredText(font, Component.literal("Minecraft Java 26.3  |  Fabric"), center, 40, 0xFFB9C2CF);

            graphics.text(font, "JUGADORES CONECTADOS", center - Math.min(155, width / 2 - 18), 55, 0xFFFFFFFF);
            String roleLine = "Runner: " + (runner == null ? "—" : runner)
                + "     Hunters: " + (hunters.isEmpty() ? "—" : String.join(", ", hunters));
            graphics.centeredText(font, Component.literal(roleLine), center, height - 101, 0xFFFFD18A);
            graphics.centeredText(font, Component.literal(notice), center, height - 88, 0xFFD0D7E2);
            graphics.centeredText(font, Component.literal("Clic en un jugador: primero Runner; después, Hunters. AltGr: abrir/cerrar."),
                center, height - 8, 0xFF9AA6B5);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
