package com.walkietalkie.client;

import com.walkietalkie.item.Frequency;
import com.walkietalkie.net.ModNetwork;
import com.walkietalkie.net.SetFrequencyPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import java.math.BigDecimal;

public class WalkieTalkieScreen extends Screen {
    private static final int[] STEPS = {-1000, -100, -1, 1, 100, 1000};
    private static final String[] LABELS = {"-10", "-1", "-0.01", "+0.01", "+1", "+10"};

    private final InteractionHand hand;
    private int frequency;
    private EditBox box;

    public WalkieTalkieScreen(InteractionHand hand, int frequency) {
        super(Component.translatable("gui.walkietalkie.title"));
        this.hand = hand;
        this.frequency = frequency;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        box = new EditBox(this.font, cx - 45, cy - 30, 90, 20, Component.empty());
        box.setMaxLength(6);
        box.setFilter(s -> s.matches("[0-9]{0,3}(\\.[0-9]{0,2})?"));
        box.setValue(Frequency.format(frequency));
        addRenderableWidget(box);
        setInitialFocus(box);

        for (int i = 0; i < STEPS.length; i++) {
            final int step = STEPS[i];
            addRenderableWidget(Button.builder(Component.literal(LABELS[i]), b -> adjust(step))
                    .bounds(cx - 126 + i * 42, cy - 4, 40, 20).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.walkietalkie.set"), b -> apply())
                .bounds(cx - 60, cy + 24, 120, 20).build());
    }

    private int parseBox() {
        try {
            String text = box.getValue();
            if (text.isEmpty() || text.equals(".")) return frequency;
            return Frequency.clamp(new BigDecimal(text).movePointRight(2).intValue());
        } catch (NumberFormatException e) {
            return frequency;
        }
    }

    private void adjust(int step) {
        frequency = Frequency.clamp(parseBox() + step);
        box.setValue(Frequency.format(frequency));
    }

    private void apply() {
        frequency = parseBox();
        ModNetwork.CHANNEL.sendToServer(new SetFrequencyPacket(frequency, hand == InteractionHand.MAIN_HAND));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter / numpad Enter
            apply();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int cx = this.width / 2;
        int cy = this.height / 2;
        g.drawCenteredString(this.font, this.title, cx, cy - 64, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("gui.walkietalkie.range"), cx, cy - 48, 0xA0A0A0);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
