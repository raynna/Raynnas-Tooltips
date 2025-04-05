package com.raynna.tooltips;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.raynna.tooltips.Tooltip.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config {

    public static final int SERVER_VERSION = 1;
    public static final int CLIENT_VERSION = 1;

    public static final class Server {

        public static ModConfigSpec SPEC;
        public static final ModConfigSpec.IntValue SERVER_CONFIG_VERSION;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            SERVER_CONFIG_VERSION = builder.translation("Server Config Version: ").comment("DO NOT CHANGE. Used for tracking config updates.").defineInRange("config_version", SERVER_VERSION, 1, Integer.MAX_VALUE);
            SPEC = builder.build();
        }

    }

    public static final class Client {

        static ModConfigSpec SPEC;

        public static final ModConfigSpec.IntValue CLIENT_CONFIG_VERSION;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            CLIENT_CONFIG_VERSION = builder.translation("Client Config Version: ").comment("DO NOT CHANGE. Used for tracking config updates.").defineInRange("config_version", CLIENT_VERSION, 1, Integer.MAX_VALUE);
            SPEC = builder.build();
        }
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Loading event) {
        ModConfig config = event.getConfig();
        if (config.getSpec() == Server.SPEC) {
            int storedVersion = Server.SERVER_CONFIG_VERSION.get();
            if (storedVersion < SERVER_VERSION) {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
                Path configFilePath = Paths.get("config", MOD_ID + "-common.toml");

                if (Files.exists(configFilePath)) {
                    try {
                        Files.delete(configFilePath);
                        System.out.println("Old config file deleted due to version change.");
                    } catch (IOException e) {
                        System.err.println("Failed to delete old config file: " + e.getMessage());
                    }
                }
                // Register all config sections again

                Server.SERVER_CONFIG_VERSION.set(SERVER_VERSION);
                Server.SPEC = builder.build();
                config.getSpec().validateSpec(config);
                assert config.getLoadedConfig() != null;
                config.getLoadedConfig().save();
                System.out.println("Config rebuilt due to version change.");
            }
            System.out.println("Reloaded on server");
        }
        if (config.getSpec() == Client.SPEC) {
            int storedVersion = Client.CLIENT_CONFIG_VERSION.get();
            if (storedVersion < CLIENT_VERSION) {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
                Client.CLIENT_CONFIG_VERSION.set(CLIENT_VERSION);
                Client.SPEC = builder.build();
            }
            System.out.println("Reloaded on client");
        }
    }
}