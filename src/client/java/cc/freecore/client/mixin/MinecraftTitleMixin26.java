package cc.freecore.client.mixin;

import cc.freecore.client.FreeCoreClientRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftTitleMixin26 {
    @Inject(method = "updateTitle", at = @At("HEAD"), cancellable = true)
    private void freecore$keepTitle(CallbackInfo ci) {
        String title = FreeCoreClientRuntime.getConfig().windowTitle;
        if (title != null && !title.isBlank()) {
            ((Minecraft) (Object) this).getWindow().setTitle(title);
            ci.cancel();
        }
    }
}
