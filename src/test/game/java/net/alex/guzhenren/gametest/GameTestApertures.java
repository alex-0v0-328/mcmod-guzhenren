package net.alex.guzhenren.gametest;

import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * 空窍[Aperture]、存储、本命、克隆与 Epic Fight 套件共用的空窍夹具：同一档案已连接的生存模拟玩家；其以资质[Aptitude] 80
 * 开窍、余皆不设；仿服务端打开其主存储界面。
 */

final class GameTestApertures {

    private GameTestApertures() {}

    static ServerPlayer mortal(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-gametest-mock").build();
    }

    static ServerPlayer awakenedMortal(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        ApertureService.awaken(player, 80);
        return player;
    }

    static ApertureStorageMenu openStorage(ServerPlayer player) {
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);
        player.containerMenu = menu;
        return menu;
    }
}
