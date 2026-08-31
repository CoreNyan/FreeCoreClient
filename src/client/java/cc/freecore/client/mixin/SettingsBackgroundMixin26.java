package cc.freecore.client.mixin;

import cc.freecore.client.BackgroundManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces vanilla dirt/blur backgrounds across every Screen menu. TitleScreen
 * and PauseScreen have dedicated render paths and are intentionally excluded. */
@Mixin(Screen.class)
public abstract class SettingsBackgroundMixin26 {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void freecore$settingsBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                              float delta, CallbackInfo ci) {
        String name = ((Object) this).getClass().getName();
        boolean customMenu = !name.contains("TitleScreen")
                && !name.contains("PauseScreen");
        if (customMenu) {
            Screen screen = (Screen) (Object) this;
            BackgroundManager.render(graphics, screen.width, screen.height);
            ci.cancel();
        }
    }
}
