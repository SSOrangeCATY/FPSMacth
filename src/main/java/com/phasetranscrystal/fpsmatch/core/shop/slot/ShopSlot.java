package com.phasetranscrystal.fpsmatch.core.shop.slot;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.shop.event.CheckCostEvent;
import com.phasetranscrystal.fpsmatch.core.shop.event.ShopSlotChangeEvent;
import com.phasetranscrystal.fpsmatch.core.shop.functional.ChangeShopItemModule;
import com.phasetranscrystal.fpsmatch.core.shop.functional.ListenerModule;
import com.phasetranscrystal.fpsmatch.common.entity.drop.DropType;
import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ShopSlot{
    public static final Codec<ShopSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("ItemStack").forGetter(ShopSlot::process),
            Codec.INT.fieldOf("defaultCost").forGetter(ShopSlot::getDefaultCost),
            Codec.INT.fieldOf("maxBuyCount").forGetter(ShopSlot::getMaxBuyCount),
            Codec.INT.fieldOf("groupId").forGetter(ShopSlot::getGroupId),
            Codec.list(Codec.STRING).fieldOf("listenerModule").forGetter(ShopSlot::getListenerNames)
    ).apply(instance, (itemstack,dC,mBC,gId,fL) -> {
        ShopSlot shopSlot = new ShopSlot(itemstack,dC,mBC,gId);
        fL.forEach(name->{
            ListenerModule lm = FPSMCore.getInstance().getListenerModuleManager().getListenerModule(name);
            if(lm != null){
                shopSlot.addListener(lm);
            }else{
                System.out.println("error : couldn't find listener module by -> " + name);
            }
        });
        return shopSlot;
    }));

    // 物品供应器，用于提供物品栈
    public Supplier<ItemStack> itemSupplier;
    // 返回检查器，用于检查物品栈是否可以返回
    public final Predicate<ItemStack> returningChecker;
    // 默认价格
    public int defaultCost;
    // 当前价格
    private int cost;
    // 组ID
    private int groupId = -1;
    // 已购买数量
    private int boughtCount = 0;
    // 最大购买数量
    private int maxBuyCount = 1;
    // 是否锁定
    private boolean locked = false;
    // 索引
    private int index = -1;
    private final ArrayList<ListenerModule> listener = new ArrayList<>();

    /**
     * 获取当前价格
     * @return 当前价格
     */
    public int getCost() {
        return cost;
    }

    public void setCost(int count){
        this.cost = count;
    }

    public void setDefaultCost(int count){
        this.defaultCost = count;
    }

    public int getDefaultCost() {
        return defaultCost;
    }

    /**
     * 获取组ID
     * @return 组ID
     */
    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId){
        this.groupId = groupId;
    }

    /**
     * 获取已购买数量
     * @return 已购买数量
     */
    public int getBoughtCount() {
        return boughtCount;
    }

    /**
     * 获取最大购买数量
     * @return 最大购买数量
     */
    public int getMaxBuyCount() {
        return maxBuyCount;
    }

    /**
     * 判断是否锁定
     * @return 是否锁定
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * 处理物品，返回一个新的物品栈
     * @return 新的物品栈
     */
    public ItemStack process() {
        return itemSupplier.get();
    }

    /**
     * 重置当前价格为默认价格
     */
    public void resetCost() {
        cost = defaultCost;
    }

    /**
     * 设置为锁定状态
     */
    public void lock() {
        locked = true;
    }

    public void lock(int boughtCount) {
        locked = true;
        this.boughtCount = Math.min(this.getMaxBuyCount(),boughtCount);
    }


    /**
     * 设置为非锁定状态
     */
    public void unlock() {
        locked = false;
    }

    public void unlock(int count) {
        this.boughtCount -= Math.min(this.boughtCount,Math.max(0,count));
        if(boughtCount < maxBuyCount){
            this.unlock();
        }
    }

    /**
     * 设置最大购买数量
     * @param maxBuyCount 最大购买数量
     */
    public void setMaxBuyCount(int maxBuyCount) {
        this.maxBuyCount = maxBuyCount;
    }

    /**
     * 判断是否有组
     * @return 是否有组
     */
    public boolean haveGroup(){
        return groupId >= 0;
    }

    /**
     * 设置索引
     * @param index 索引
     */
    public void setIndex(int index){
        if(this.index < 0){
            this.index = index;
        }
    }

    /**
     * 获取索引
     * @return 索引
     */
    public int getIndex(){
        return index;
    }

    /**
     * 构造函数，用于创建一个新的物品槽位
     * @param itemStack 物品栈
     * @param defaultCost 默认价格
     */
    public ShopSlot(ItemStack itemStack, int defaultCost) {
        if(itemStack.getItem() instanceof IGun iGun){
            FPSMUtil.fixGunItem(itemStack, iGun);
        }
        this.itemSupplier = itemStack::copy;
        this.defaultCost = defaultCost;
        this.cost = defaultCost;
        this.returningChecker = getDefaultChecker();
    }

    /**
     * 构造函数，用于创建一个新的物品槽位，并设置最大购买数量
     * @param itemStack 物品栈
     * @param defaultCost 默认价格
     * @param maxBuyCount 最大购买数量
     */
    public ShopSlot(ItemStack itemStack, int defaultCost, int maxBuyCount) {
        this(itemStack, defaultCost);
        this.maxBuyCount = maxBuyCount;
    }

    /**
     * 构造函数，用于创建一个新的物品槽位，并设置组ID和返回检查器
     * @param itemStack 物品
     * @param defaultCost 默认价格
     * @param maxBuyCount 最大购买数量
     * @param groupId 组ID
     */
    public ShopSlot(ItemStack itemStack, int defaultCost, int maxBuyCount, int groupId) {
        this(itemStack,defaultCost,maxBuyCount);
        this.groupId = groupId;
    }

    /**
     * 构造函数，用于创建一个新的物品槽位，并设置组ID和返回检查器
     * @param supplier 物品供应器
     * @param defaultCost 默认价格
     * @param maxBuyCount 最大购买数量
     * @param groupId 组ID
     * @param checker 退款检查器
     */
    public ShopSlot(Supplier<ItemStack> supplier, int defaultCost, int maxBuyCount, int groupId, Predicate<ItemStack> checker) {
        this.itemSupplier = supplier;
        this.defaultCost = defaultCost;
        this.cost = defaultCost;
        this.maxBuyCount = maxBuyCount;
        this.groupId = groupId;
        this.returningChecker = checker;
    }

    /**
     * 判断是否可以购买
     * @param money 当前金钱
     * @return 是否可以购买
     */
    public boolean canBuy(int money) {
        return money >= cost && boughtCount < maxBuyCount;
    }

    /**
     * 判断是否可以返回
     * @return 是否可以返回
     */
    public boolean canReturn(Player player) {
        if(player.getInventory().clearOrCountMatchingItems(this.returningChecker,0,player.inventoryMenu.getCraftSlots()) > 0){
            return boughtCount > 0 && !locked;
        }else{
            return false;
        }
    }

    public Predicate<ItemStack> getDefaultChecker(){
        return (itemStack)->{
            ItemStack shopItem = this.process();
            if(itemStack.getItem() instanceof IGun iGun){
                ResourceLocation gunId = iGun.getGunId(itemStack);
                return shopItem.getItem() instanceof IGun shopGun && gunId.equals(shopGun.getGunId(shopItem));
            }else {
                return itemStack.getDisplayName().getString().equals(shopItem.getDisplayName().getString()) && shopItem.getItem() == itemStack.getItem();
            }
        };
    }

    /**
     * 重置物品槽位
     */
    public void reset() {
        cost = defaultCost;
        boughtCount = 0;
        locked = false;
        this.listener.forEach((listenerModule -> {
            if(listenerModule instanceof ChangeShopItemModule changeShopItemModule){
                this.itemSupplier = changeShopItemModule.defaultItem()::copy;
            }
        }));
    }

    /**
     * 当组内物品槽位发生变化时调用
     */
    public final void onGroupSlotChanged(ShopSlotChangeEvent event) {
        if(!event.isCancelLogic() && this.isLocked() && this.getBoughtCount() >= 1){
            int i = event.player.getInventory().clearOrCountMatchingItems(this.returningChecker,1,event.player.inventoryMenu.getCraftSlots());
            if(i > 0){
                this.boughtCount--;
                this.unlock();
                FPSMCore.playerDropMatchItem(event.player,this.process().copy());
                event.setCancelLogic(true);
            }
        }
        if(!this.listener.isEmpty()){
            listener.forEach(listenerModule -> {
                listenerModule.handle(event);
            });
        }
    }

    public void handleCheckCostEvent(CheckCostEvent event){
        if(this.canReturn(event.player()) && getListenerNames().contains("returnGoods")){
            event.addCost(this.getCost());
        }
    }

    public void addListener(ListenerModule listener) {
        this.listener.add(listener);
        this.listener.sort(Comparator.comparingInt(ListenerModule::getPriority).reversed());
    }
    public List<String> getListenerNames(){
        List<String> names = new ArrayList<>();
        this.listener.forEach(listenerModule -> {
            names.add(listenerModule.getName());
        });
        return names;
    }

    /**
     * 购买物品 不要直接使用！从ShopData层判定与调用
     * @param player 玩家
     * @param money 当前金钱
     * @return 购买后剩余金钱
     */
    public int buy(Player player, int money) {
        boughtCount++;
        ItemStack itemStack = process();
        DropType type = DropType.getItemDropType(itemStack);
        if(!type.playerPredicate.test(player)){
            if(type != DropType.MISC){
                Predicate<ItemStack> test = DropType.getPredicateByDropType(type);
                List<NonNullList<ItemStack>> items = ImmutableList.of(player.getInventory().items,player.getInventory().armor,player.getInventory().offhand);
                for(List<ItemStack> itemStackList : items ){
                    for(ItemStack itemStack1 : itemStackList){
                        if(test.test(itemStack1)){
                            FPSMCore.playerDropMatchItem((ServerPlayer) player,itemStack1.copy());
                            itemStack1.shrink(1);
                            break;
                        }
                    }
                }
            }
        }
        if(itemStack.getItem() instanceof ArmorItem armorItem){
            player.setItemSlot(armorItem.getEquipmentSlot(),itemStack);
        }else{
            player.getInventory().add(itemStack);
            FPSMUtil.sortPlayerInventory(player);
        }
        return money - cost;
    }

    public void setItemSupplier(Supplier<ItemStack> itemSupplier) {
        this.itemSupplier = itemSupplier;
    }

    //同上
    /**
     * 返回物品
     * @param player 玩家
     */
    public void returnItem(Player player) {
        returnItem(player, 1);
    }

    /**
     * 返回指定数量的物品
     *
     * @param player 玩家
     * @param count  返回数量
     */
    public void returnItem(Player player, int count) {
        count = Math.min(boughtCount, count);
        player.getInventory().clearOrCountMatchingItems(returningChecker, count, player.inventoryMenu.getCraftSlots());
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        boughtCount -= count;
    }


    public void removeListenerModule(String module) {
        this.listener.removeIf(listenerModule -> listenerModule.getName().equals(module));
    }

    public ShopSlot copy() {
        ItemStack itemStack = this.itemSupplier.get();
        if(itemStack.getItem() instanceof IGun iGun){
            FPSMUtil.fixGunItem(itemStack, iGun);
        }
        ShopSlot slot = new ShopSlot(itemStack::copy,this.defaultCost,this.maxBuyCount,this.groupId,this.returningChecker);
        slot.setIndex(this.index);
        slot.listener.addAll(this.listener);
        return slot;
    }

    public int getAmmoCount() {
        ItemStack itemStack = this.itemSupplier.get();
        if(itemStack.getItem() instanceof IGun iGun){
            return iGun.getMaxDummyAmmoAmount(itemStack);
        }
        return 0;
    }

    public int getDummyAmmoCount() {
        ItemStack itemStack = this.itemSupplier.get();
        if(itemStack.getItem() instanceof IGun iGun){
            return iGun.getMaxDummyAmmoAmount(itemStack);
        }
        return 0;
    }
    public String toString(){
        return "ShopSlot{" +
                "itemStack=" + this.process() +
                ", defaultCost=" + defaultCost +
                ", cost=" + cost +
                ", groupId=" + groupId +
                ", boughtCount=" + boughtCount +
                ", maxBuyCount=" + maxBuyCount +
                ", locked=" + locked +
                ", index=" + index +
                ", listener=" + listener +
                '}';
    }
}
