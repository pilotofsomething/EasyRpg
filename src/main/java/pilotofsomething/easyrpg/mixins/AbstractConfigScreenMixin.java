package pilotofsomething.easyrpg.mixins;

import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pilotofsomething.easyrpg.item.LootItemKt;

@Mixin(AbstractConfigScreen.class)
public class AbstractConfigScreenMixin {
	@Inject(method = "save()V", at = @At("RETURN"))
	private void save(CallbackInfo ci) {
		LootItemKt.setupQualities();
	}
}
