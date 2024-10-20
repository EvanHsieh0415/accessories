package io.wispforest.accessories.menu;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.menu.variants.AccessoriesExperimentalMenu;
import io.wispforest.accessories.menu.variants.AccessoriesMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.apache.commons.lang3.function.TriFunction;

public class AccessoriesMenuTypes {

    public static MenuType<AccessoriesMenu> ORIGINAL_MENU;
    public static MenuType<AccessoriesExperimentalMenu> EXPERIMENTAL_MENU;

    public static void registerMenuType() {
        ORIGINAL_MENU = registerMenuType("original_menu", AccessoriesMenu::of);
        EXPERIMENTAL_MENU = registerMenuType("experimental_menu", AccessoriesExperimentalMenu::of);
    }

    private static <T extends AbstractContainerMenu> MenuType<T> registerMenuType(String path, TriFunction<Integer, Inventory, AccessoriesMenuData, T> func) {
        return AccessoriesInternals.registerMenuType(Accessories.of(path), AccessoriesMenuData.ENDEC, func);
    }

    @Environment(EnvType.CLIENT)
    public static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerClientMenuConstructors(MenuRegisterCallback callback) {
        callback.register(AccessoriesMenuTypes.ORIGINAL_MENU, AccessoriesScreen::new);
        callback.register(AccessoriesMenuTypes.EXPERIMENTAL_MENU, AccessoriesExperimentalScreen::new);
    }

    public interface MenuRegisterCallback {
        <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> factory);
    }
}
