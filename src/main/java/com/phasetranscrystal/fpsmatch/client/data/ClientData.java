package com.phasetranscrystal.fpsmatch.client.data;

import com.phasetranscrystal.fpsmatch.core.data.ShopData;
import com.phasetranscrystal.fpsmatch.core.data.TabData;

import java.util.*;

public class ClientData {
    public static String currentMap = "fpsm_none";
    public static String currentGameType = "error";
    public static boolean currentMapSupportShop = true;
    public static final ShopData clientShopData = new ShopData(ShopData.getDefaultShopItemData(false));
    public static final Map<UUID, TabData> tabData = new HashMap<>();
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
    public static boolean leavePurchaseArea = false;

    public static ShopData.ShopSlot getSlotData(ShopData.ItemType type, int index) {
        return clientShopData.getSlotData(type,index);
    }

    public static int getMoney(){
        return clientShopData.getMoney();
    }

}