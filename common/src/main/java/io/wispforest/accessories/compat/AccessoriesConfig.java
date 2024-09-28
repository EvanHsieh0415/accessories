package io.wispforest.accessories.compat;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.TargetType;
import io.wispforest.accessories.impl.PlayerEquipControl;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = Accessories.MODID)
public class AccessoriesConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public ClientData clientData = new ClientData();

    public static class ClientData {

        public PlayerEquipControl equipControl = PlayerEquipControl.MUST_NOT_CROUCH;

        public boolean forceNullRenderReplacement = false;

        public boolean disableEmptySlotScreenError = false;

        public boolean showUnusedSlots = false;

        public boolean allowSlotScrolling = true;

        public ScreenType selectedScreenType = ScreenType.NONE;

        @ConfigEntry.Gui.CollapsibleObject()
        public ExperimentalScreenSettings experimentalScreenSettings = new ExperimentalScreenSettings();

        public static class ExperimentalScreenSettings {
            public boolean isDarkMode = false;
            public boolean showEquippedStackSlotType = true;
        }

        @ConfigEntry.Gui.CollapsibleObject()
        public LegacyScreenSettings legacyScreenSettings = new LegacyScreenSettings();

        public static class LegacyScreenSettings {
            public boolean showGroupTabs = true;
        }

        @ConfigEntry.Gui.CollapsibleObject()
        public ScreenButtonPositions screenButtonPositions = new ScreenButtonPositions();

        public static class ScreenButtonPositions {
            public int inventoryButtonXOffset = 62;
            public int inventoryButtonYOffset = 8;

            public int creativeInventoryButtonXOffset = 96;
            public int creativeInventoryButtonYOffset = 6;
        }

        @ConfigEntry.Gui.CollapsibleObject()
        public HoverOptions hoverOptions = new HoverOptions();

        public static class HoverOptions {

            @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
            public HoveredOptions hoveredOptions = new HoveredOptions();

            public static class HoveredOptions {
                public boolean brightenHovered = true;
                public boolean cycleBrightness = true;

                public boolean line = false;
                public boolean clickbait = false;
            }

            @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
            public UnHoveredOptions unHoveredOptions = new UnHoveredOptions();

            public static class UnHoveredOptions {
                public boolean renderUnHovered = true;

                public boolean darkenUnHovered = true;
                public float darkenedBrightness = 0.5f;
                public float darkenedOpacity = 1f;
            }
        }

        public List<RenderSlotTarget> disabledDefaultRenders = new ArrayList<>();
    }

    public List<SlotAmountModifier> modifiers = new ArrayList<>();

    public static class SlotAmountModifier {
        public String slotType;
        public int amount = 0;
    }

    public static class RenderSlotTarget {
        public String slotType = "";
        public TargetType targetType = TargetType.ALL;
    }

    public enum ScreenType {
        NONE(-1),
        ORIGINAL(1),
        EXPERIMENTAL_V1(2);

        private final int screenIndex;

        ScreenType(int screenIndex) {
            this.screenIndex = screenIndex;
        }

        public boolean isValid() {
            return this.screenIndex >= 1;
        }
    }
}
