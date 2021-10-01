package pilotofsomething.easyrpg.gui;

import io.github.cottonmc.cotton.gui.widget.WWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.data.Texture;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

/** Draws a bar that dynamically obtains it fill percentage from a function
 *  This was basically just copy-pasted from LibGui's WBar
 * */
public class WDynamicBar extends WWidget {
	/**
	 * The background texture. If not null, it will be
	 * drawn behind the bar contents.
	 */
	@Nullable
	protected final Texture bg;

	/**
	 * The bar texture. If not null, it will be
	 * drawn to represent the current field.
	 */
	@Nullable
	protected final Texture bar;

	protected final Callable<Float> percentage;

	/**
	 * The direction of this bar, representing where the bar will grow
	 * when the field increases.
	 */
	protected final Direction direction;

	/**
	 * The translation key of the tooltip.
	 *
	 * @see #withTooltip(String) formatting instructions
	 */
	protected String tooltipLabel;

	/**
	 * A tooltip text component. This can be used instead of {@link #tooltipLabel},
	 * or together with it. In that case, this component will be drawn after the other label.
	 */
	protected Text tooltipTextComponent;

	public int barColor = 0xFFFFFF, barBackground = 0x000000;
	public float opacity = 0.5f, bgOpacity = 0.25f;

	public void setBarColor(int color) {
		barColor = color;
	}

	public void setBarBackgroundColor(int color) {
		barBackground = color;
	}

	public void setBarOpacity(float amt) {
		opacity = amt;
	}

	public void setBarBackgroundOpacity(float amt) {
		bgOpacity = amt;
	}

	public WDynamicBar(@Nullable Texture bg, @Nullable Texture bar, Callable<Float> percentage) {
		this(bg, bar, percentage, Direction.UP);
	}

	public WDynamicBar(@Nullable Texture bg, @Nullable Texture bar, Callable<Float> percentage, Direction dir) {
		this.bg = bg;
		this.bar = bar;
		this.percentage = percentage;
		this.direction = dir;
	}

	public WDynamicBar(Identifier bg, Identifier bar, Callable<Float> percentage) {
		this(bg, bar, percentage, Direction.UP);
	}

	public WDynamicBar(Identifier bg, Identifier bar, Callable<Float> percentage, Direction dir) {
		this(new Texture(bg), new Texture(bar), percentage, dir);
	}

	/**
	 * Adds a tooltip to the WBar.
	 *
	 * <p>Formatting Guide: The tooltip label is passed into {@code String.format} and can receive two integers
	 * (%d) - the first is the current value of the bar's focused field, and the second is the
	 * bar's focused maximum.
	 *
	 * @param label the translation key of the string to render on the tooltip
	 * @return this bar with tooltip enabled and set
	 */
	public WDynamicBar withTooltip(String label) {
		this.tooltipLabel = label;
		return this;
	}

	/**
	 * Adds a tooltip {@link Text} to the WBar.
	 *
	 * @param label the added tooltip label
	 * @return this bar
	 */
	public WDynamicBar withTooltip(Text label) {
		this.tooltipTextComponent = label;
		return this;
	}

	@Override
	public boolean canResize() {
		return true;
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
		if (bg != null) {
			ScreenDrawing.texturedRect(matrices, x, y, getWidth(), getHeight(), bg, 0xFFFFFFFF);
		} else {
			ScreenDrawing.coloredRect(matrices, x, y, getWidth(), getHeight(), ScreenDrawing.colorAtOpacity(barBackground, bgOpacity));
		}

		float percent;
		try {
			percent = percentage.call();
		} catch (Exception ignored) {
			percent = 0.0f;
		}
		if (percent < 0) percent = 0f;
		if (percent > 1) percent = 1f;

		int barMax = getWidth();
		if (direction == Direction.DOWN || direction == Direction.UP) barMax = getHeight();
		percent = ((int) (percent * barMax)) / (float) barMax; //Quantize to bar size

		int barSize = (int) (barMax * percent);
		if (barSize <= 0) return;

		switch (direction) { //anonymous blocks in this switch statement are to sandbox variables
			case UP -> {
				int left = x;
				int top = y + getHeight();
				top -= barSize;
				if (bar != null) {
					ScreenDrawing.texturedRect(matrices, left, top, getWidth(), barSize, bar.image(), bar.u1(), MathHelper.lerp(percent, bar.v2(), bar.v1()), bar.u2(), bar.v2(), 0xFFFFFFFF);
				} else {
					ScreenDrawing.coloredRect(matrices, left, top, getWidth(), barSize, ScreenDrawing.colorAtOpacity(barColor, opacity));
				}
			}

			case RIGHT -> {
				if (bar != null) {
					ScreenDrawing.texturedRect(matrices, x, y, barSize, getHeight(), bar.image(), bar.u1(), bar.v1(), MathHelper.lerp(percent, bar.u1(), bar.u2()), bar.v2(), 0xFFFFFFFF);
				} else {
					ScreenDrawing.coloredRect(matrices, x, y, barSize, getHeight(), ScreenDrawing.colorAtOpacity(barColor, opacity));
				}
			}

			case DOWN -> {
				if (bar != null) {
					ScreenDrawing.texturedRect(matrices, x, y, getWidth(), barSize, bar.image(), bar.u1(), bar.v1(), bar.u2(), MathHelper.lerp(percent, bar.v1(), bar.v2()), 0xFFFFFFFF);
				} else {
					ScreenDrawing.coloredRect(matrices, x, y, getWidth(), barSize, ScreenDrawing.colorAtOpacity(barColor, opacity));
				}
			}

			case LEFT -> {
				int left = x + getWidth();
				int top = y;
				left -= barSize;
				if (bar != null) {
					ScreenDrawing.texturedRect(matrices, left, top, barSize, getHeight(), bar.image(), MathHelper.lerp(percent, bar.u2(), bar.u1()), bar.v1(), bar.u2(), bar.v2(), 0xFFFFFFFF);
				} else {
					ScreenDrawing.coloredRect(matrices, left, top, barSize, getHeight(), ScreenDrawing.colorAtOpacity(barColor, opacity));
				}
			}
		}
	}

	/**
	 * The direction of a {@link WDynamicBar}, representing where the bar will
	 * grown when its field increases.
	 */
	public enum Direction {
		UP,
		RIGHT,
		DOWN,
		LEFT;
	}
}
