package cc.freecore.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public final class FreeCoreClient implements ClientModInitializer {
    public static final String MOD_ID = "freecoreclient";

    @Override
    public void onInitializeClient() {
        ConfigManager.initialize(MinecraftClient.getInstance());
    }
}
