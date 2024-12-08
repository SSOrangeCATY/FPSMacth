package com.phasetranscrystal.fpsmatch.client.data;

import com.phasetranscrystal.fpsmatch.core.data.ShopData;
import com.phasetranscrystal.fpsmatch.core.data.TabData;
import com.phasetranscrystal.fpsmatch.core.shop.ItemType;

import java.util.*;

public class ClientData {
    public static String currentMap = "fpsm_none";
    public static String currentGameType = "error";
    public static boolean currentMapSupportShop = true;
    public static final ShopData clientShopData = new ShopData(ShopData.getDefaultShopItemData(false));
    public static final Map<UUID, TabData> tabData = new HashMap<>();
    public static final Map<UUID,Integer> playerMoney = new HashMap<>();
    public static int cTWinnerRounds = 0;
    public static int tWinnerRounds = 0;
    public static int pauseTime = 0;
    public static int roundTime = 0;
    public static boolean isDebug = false;
    public static boolean isStart = false;
    public static boolean isError = false;
    public static boolean isPause = false;
    public static boolean isWaiting = false;
    public static boolean isWarmTime = false;
    public static boolean isWaitingWinner = false;
    public static int nextRoundMoney = 0;
    public static int purchaseTime = 1;
    public static boolean canOpenShop = false;
    public static int dismantleBombStates = 0; // 0 = 没拆呢 | 1 = 正在拆 | 2 = 错误可能是不在队伍或者地图导致的
    public static UUID bombUUID = null;
    public static float dismantleBombProgress = 0;
    public static boolean customTab = true;

    public static ShopData.ShopSlot getSlotData(ItemType type, int index) {
        return clientShopData.getSlotData(type,index);
    }

    public static int getMoney(){
        return clientShopData.getMoney();
    }

    public static void reset() {
        currentMap = "fpsm_none";
        currentGameType = "error";
        currentMapSupportShop = true;
        clientShopData.reset();
        tabData.clear();
        playerMoney.clear();
        cTWinnerRounds = 0;
        tWinnerRounds = 0;
        pauseTime = 0;
        roundTime = 0;
        isDebug = false;
        isStart = false;
        isError = false;
        isPause = false;
        isWaiting = false;
        isWarmTime = false;
        isWaitingWinner = false;
        nextRoundMoney = 0;
        purchaseTime = 1;
        canOpenShop = false;
        dismantleBombStates = 0;
        bombUUID = null;
        dismantleBombProgress = 0;
    }
}
