package com.phasetranscrystal.fpsmatch.client;

import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.core.data.ShopItemData;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import icyllis.arc3d.core.RawPtr;
import icyllis.modernui.ModernUI;
import icyllis.modernui.R;
import icyllis.modernui.audio.AudioManager;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.ColorFilter;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.ImageView;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;


public class CSGameShopScreen extends Fragment {
    private static final String[] TOP_NAME_KEYS = new String[]{"fpsm.shop.title.equipment","fpsm.shop.title.pistol","fpsm.shop.title.mid_rank","fpsm.shop.title.rifle","fpsm.shop.title.throwable"};
    private static final String[] TOP_NAME_KEYS_TEST = new String[]{"装备","手枪","中级","步枪","投掷物"};
    public static final String TACZ_MODID = "tacz";
    public static final String TACZ_AWP_ICON = "gun/hud/ai_awp.png";
    private final ShopItemData data = new ShopItemData();
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Configurator.setRootLevel(Level.DEBUG);
        try (ModernUI app = new ModernUI()) {
            app.run(new CSGameShopScreen()); // 在这里传入您的 Fragment 实例
        }
        AudioManager.getInstance().close();
        System.gc();
    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState) {
        var content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.HORIZONTAL);
        var background = new ImageDrawable(Image.create(FPSMatch.MODID,"ui/cs/background.png"));
        var gunImage = new ImageDrawable(Image.create(FPSMatch.MODID,TACZ_AWP_ICON));
        var shopWindow = new LinearLayout(this.getContext());
        for(int i = 0; i<5; i++){
            var shopTitleBackground = new ShapeDrawable();
            shopTitleBackground.setShape(ShapeDrawable.RECTANGLE);
            shopTitleBackground.setStroke(0,0xFFFF0000);
            var typeBar = new LinearLayout(getContext());
            typeBar.setOrientation(LinearLayout.VERTICAL);

            var titleBar = new LinearLayout(getContext());

            int gunButtonWeight = switch (i) {
                case 2 -> 180;
                case 3 -> 200;
                default -> 140;
            };
            int textColor = RenderUtil.color(203,203,203);
            TextView numTab = new TextView(getContext());
            numTab.setTextColor(textColor);
            numTab.setText(String.valueOf(i + 1));
            numTab.setTextSize(15);
            numTab.setPadding(15,10,0,0);
            numTab.setGravity(Gravity.LEFT);

            TextView title = new TextView(getContext());
            title.setTextColor(textColor);
            title.setText(I18n.get(TOP_NAME_KEYS_TEST[i]));
            title.setTextSize(21);
            title.setGravity(Gravity.CENTER);

            titleBar.addView(numTab,new LinearLayout.LayoutParams(25, -1));
            titleBar.addView(title,new LinearLayout.LayoutParams(gunButtonWeight - 25, -1));
            typeBar.addView(titleBar,new LinearLayout.LayoutParams(-1, 44));

            for(int j = 0; j<5; j++){
                var shopHolderBackground = new ShapeDrawable();
                shopHolderBackground.setShape(ShapeDrawable.RECTANGLE);
                shopHolderBackground.setCornerRadius(3);
                shopHolderBackground.setColor(RenderUtil.color(42,42,42));

                var shop = new LinearLayout(getContext());
                var gun = new LinearLayout(getContext());
                var gunButton = new GunButtonLinearLayout(getContext(),this.data.getSlotData(ShopItemData.ItemType.values()[i],j));
                gun.addView(gunButton,new LinearLayout.LayoutParams(-1,-1));
                shop.setGravity(Gravity.CENTER);
                shop.addView(gun,new LinearLayout.LayoutParams(gunButtonWeight,90));
                typeBar.addView(shop,new LinearLayout.LayoutParams(-1,98));
            }
            shopWindow.addView(typeBar,new LinearLayout.LayoutParams(gunButtonWeight + 30,-1));
        }
        background.setAlpha(100);
        shopWindow.setBackground(background);
        content.addView(shopWindow,new LinearLayout.LayoutParams(950,550));
        return content;
    }



    public static class GunButtonLinearLayout extends LinearLayout {
        public final ShopItemData.ShopSlot shopSlot;
        public boolean isBuy = false;
        public int[] buttonCanBuy = new int[]{ 1 };
        public int[] buttonCantBuy = new int[]{ 0 };

        public GunButtonLinearLayout(Context context, ShopItemData.ShopSlot shopSlot) {
            super(context);
            this.shopSlot = shopSlot;

            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
            ShapeDrawable buttonBackground = new ShapeDrawable();
            buttonBackground.setShape(ShapeDrawable.RECTANGLE);
            buttonBackground.setColor(RenderUtil.color(42, 42, 42));
            buttonBackground.setCornerRadius(3);
            setBackground(buttonBackground);
            ImageView gunImageView = new ImageView(context);
            ImageDrawable imageDrawable = new ImageDrawable(Image.create(FPSMatch.MODID, TACZ_AWP_ICON));
            gunImageView.setLayoutParams(new LinearLayout.LayoutParams(39, 13));
            gunImageView.setScaleX(3);
            gunImageView.setScaleY(3);
            gunImageView.setImageDrawable(imageDrawable);
            ColorStateList tintList = new ColorStateList(
                    new int[][] {
                            new int[]{-R.attr.state_enabled},
                            StateSet.get(StateSet.VIEW_STATE_ENABLED)},
                    new int[] {
                            RenderUtil.color(65,65,65),
                            RenderUtil.color(234,192,85)
                    });

            gunImageView.setImageTintList(tintList);
            addView(gunImageView);

            setOnClickListener((v) -> {
                if (!isBuy) {
                    isBuy = true;
                    ItemStack itemStack = shopSlot.item().itemStack();
                    buttonBackground.setStroke(1,RenderUtil.color(255,255,255));
                    System.out.println("bought : " + (itemStack == null ? "debugItem" : itemStack.getDisplayName().getString()) + " cost->" + shopSlot.item().cost());
                    gunImageView.setEnabled(false);
                } else {
                    isBuy = false;
                    ItemStack itemStack = shopSlot.item().itemStack();
                    gunImageView.setEnabled(true);
                    buttonBackground.setStroke(0,RenderUtil.color(255,255,255));
                    System.out.println("return goods : " + (itemStack == null ? "debugItem" : itemStack.getDisplayName().getString()) + " return cost->" + shopSlot.item().cost());
                }
            });
        }
    }

}
