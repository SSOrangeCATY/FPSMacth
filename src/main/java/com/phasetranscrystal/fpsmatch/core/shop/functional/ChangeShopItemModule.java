package com.phasetranscrystal.fpsmatch.core.shop.functional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.core.data.save.ISavedData;
import com.phasetranscrystal.fpsmatch.core.shop.event.ShopSlotChangeEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * 改变商店物品的监听模块。
 * <p>
 * 该模块用于在商店槽位变更事件中动态修改槽位的物品和价格。
 * 支持在购买时替换物品和价格，并在退回时恢复默认设置。
 */
public record ChangeShopItemModule(ItemStack defaultItem, int defaultCost, ItemStack changedItem, int changedCost) implements ListenerModule, ISavedData<ChangeShopItemModule> {
    /**
     * 该模块的编解码器，用于序列化和反序列化。
     */
    public static final Codec<ChangeShopItemModule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("defaultItem").forGetter(ChangeShopItemModule::defaultItem),
            Codec.INT.fieldOf("defaultCost").forGetter(ChangeShopItemModule::defaultCost),
            ItemStack.CODEC.fieldOf("changedItem").forGetter(ChangeShopItemModule::changedItem),
            Codec.INT.fieldOf("changedCost").forGetter(ChangeShopItemModule::changedCost)
    ).apply(instance, ChangeShopItemModule::new));

    /**
     * 获取该模块的编解码器。
     *
     * @return 编解码器
     */
    @Override
    public Codec<ChangeShopItemModule> codec() {
        return CODEC;
    }

    /**
     * 注册该模块到监听模块管理器。
     */
    public void read() {
        FPSMatch.listenerModuleManager.addListenerType(this);
    }

    /**
     * 处理商店槽位变更事件。
     * <p>
     * 如果槽位已购买且不符合退回条件，则不执行任何操作。
     * 如果事件标志为正，则将槽位的物品和价格替换为新设置。
     * 如果事件标志为负，则将槽位的物品和价格恢复为默认设置。
     *
     * @param event 商店槽位变更事件
     */
    @Override
    public void handle(ShopSlotChangeEvent event) {
        if (event.shopSlot.getBoughtCount() > 0 && !event.shopSlot.returningChecker.test(changedItem)) return;
        if (event.flag > 0) {
            event.shopSlot.itemSupplier = changedItem::copy;
            event.shopSlot.setCost(changedCost);
        } else if (event.flag < 0) {
            event.shopSlot.returnItem(event.player);
            event.addMoney(event.shopSlot.getCost());
            event.shopSlot.itemSupplier = defaultItem::copy;
            event.shopSlot.setCost(defaultCost);
        }
    }

    /**
     * 获取该模块的名称。
     * <p>
     * 如果修改后的物品是枪械，则使用枪械的 ID 作为名称。
     * 否则，使用默认物品的注册名称作为名称。
     *
     * @return 模块名称
     */
    @Override
    public String getName() {
        String name;
        if(this.changedItem.getItem() instanceof IGun iGun){
            name = iGun.getGunId(this.changedItem).toString().replace(":","_");
        }else{
            name = BuiltInRegistries.ITEM.getKey(this.defaultItem.getItem()).toString().replace(":","_");
        }
        return "changeItem_" + name;
    }

    /**
     * 获取该模块的优先级。
     * @return 模块优先级
     */
    @Override
    public int getPriority() {
        return 1;
    }
}
