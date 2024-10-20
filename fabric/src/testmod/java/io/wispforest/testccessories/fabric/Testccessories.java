package io.wispforest.testccessories.fabric;

import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.testccessories.fabric.accessories.*;
import io.wispforest.testccessories.fabric.client.TestScreenPacket;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class Testccessories implements ModInitializer {

    public static final String MODID = "testccessories";

    public static MenuType<TestMenu> TEST_MENU_TYPE;

    @Override
    public void onInitialize() {
        TEST_MENU_TYPE = Registry.register(BuiltInRegistries.MENU, of("test_menu"), new MenuType<>(TestMenu::new, FeatureFlags.DEFAULT_FLAGS));

        AppleAccessory.init();
        PotatoAccessory.init();
        PointedDripstoneAccessory.init();
        TntAccessory.init();
        RingIncreaserAccessory.init();

        UniqueSlotHandling.EVENT.register(UniqueSlotTest.INSTANCE);

        TestItems.init();

        AccessoriesNetworking.CHANNEL.registerClientboundDeferred(TestScreenPacket.class, TestScreenPacket.ENDEC);
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}