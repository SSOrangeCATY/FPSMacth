package com.phasetranscrystal.fpsmatch.client.shop;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;


public class ClientShopSlot{
    private ItemStack itemStack;
    private int cost;
    @Nullable
    private ResourceLocation texture = null;
    private int boughtCount = 0;
    private boolean locked = false;
    
    public ClientShopSlot(ItemStack itemStack, int defaultCost) {
        this.itemStack = itemStack;
        this.cost = defaultCost;
    }

    public int cost(){
        return cost;
    }

    public ResourceLocation texture(){
        return texture;
    }

    public ItemStack itemStack(){
        return itemStack;
    }

    public int boughtCount(){
        return boughtCount;
    }

    public boolean isLocked(){
        return locked;
    }

    public void setLock(boolean lock){
        this.locked = lock;
    }

    public void setBoughtCount(int count){
        this.boughtCount = count;
    }
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setTexture(@Nullable ResourceLocation hudTexture) {
        this.texture = hudTexture;
    }

    public static ClientShopSlot getDefault(){
        return new ClientShopSlot(ItemStack.EMPTY,0);
    }

    public String name() {
        return this.itemStack.getDisplayName().getString().replace("[","").replace("]","").replaceAll("§.", "");
    }

    public boolean canReturn() {
        return this.boughtCount > 0 && !locked;
    }
}