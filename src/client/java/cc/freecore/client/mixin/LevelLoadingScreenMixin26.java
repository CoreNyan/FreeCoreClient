package cc.freecore.client.mixin;

import cc.freecore.client.FreeCoreClientRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the FreeCore background for the dedicated "Downloading terrain" screen. */
@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin26 {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void freecore$levelBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                          float delta, CallbackInfo ci) {
        FreeCoreClientRuntime.renderBackground(graphics, graphics.guiWidth(), graphics.guiHeight());
        ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void freecore$levelAnimation(GuiGraphicsExtractor graphics, int width, int height,
                                         float delta, CallbackInfo ci) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        FreeCoreClientRuntime.renderBackground(graphics, w, h);
        double t = System.nanoTime() / 1_000_000_000.0;
        int cx = w / 2;
        int cy = h / 2 - 26;
        int radius = 18;
        int head = (int) ((t * 1.6) % 8);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0 * i / 8.0;
            int x = cx + (int) Math.round(Math.cos(a) * radius);
            int y = cy + (int) Math.round(Math.sin(a) * radius);
            int distance = Math.floorMod(i - head, 8);
            int alpha = 24 + (7 - distance) * 22;
            graphics.fill(x - 2, y - 2, x + 3, y + 3, (alpha << 24) | 0xffffff);
        }
        graphics.centeredText(cc.freecore.client.FreeCoreText.font(), cc.freecore.client.FreeCoreText.component("正在加载世界"), cx, cy + 36, 0xfff0f0f0);
        int barW = Math.min(240, Math.max(140, w / 4));
        int phase = (int) ((Math.sin(t * 1.5) + 1.0) * 0.5 * (barW - 42));
        graphics.fill(cx - barW / 2, cy + 56, cx + barW / 2, cy + 58, 0x44ffffff);
        graphics.fill(cx - barW / 2 + phase, cy + 56, cx - barW / 2 + phase + 42, cy + 58, 0xddffffff);
        ci.cancel();
    }
}
