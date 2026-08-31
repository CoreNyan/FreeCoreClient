package cc.freecore.client.mixin;

import cc.freecore.client.ConfigManager;
import cc.freecore.client.FreeCoreConfig;
import cc.freecore.client.GuiActions;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.PauseScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void freecore$injectButtons(CallbackInfo ci) {
        int index = 0;
        for (FreeCoreConfig.ButtonConfig button : ConfigManager.get().buttons) {
            if (button == null || button.label == null) continue;
            addDrawableChild(ButtonWidget.builder(Text.literal(button.label), ignored -> GuiActions.perform(this, button))
                    .dimensions(width / 2 - 100, height / 4 + 120 + index++ * 24, 200, 20).build());
        }
    }
}
