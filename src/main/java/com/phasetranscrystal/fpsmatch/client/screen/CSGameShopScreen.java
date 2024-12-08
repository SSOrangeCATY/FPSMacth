package com.phasetranscrystal.fpsmatch.client.screen;

import com.mojang.blaze3d.platform.Window;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.client.data.ClientData;
import com.phasetranscrystal.fpsmatch.core.data.ShopData;
import com.phasetranscrystal.fpsmatch.core.shop.ItemType;
import com.phasetranscrystal.fpsmatch.net.ShopActionC2SPacket;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import icyllis.modernui.ModernUI;
import icyllis.modernui.R;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.audio.AudioManager;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.forge.ModernUIForge;
import icyllis.modernui.mc.forge.MuiForgeApi;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.ImageView;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.RelativeLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class CSGameShopScreen extends Fragment implements ScreenCallback{
    private static final String[] TOP_NAME_KEYS = new String[]{"fpsm.shop.title.equipment","fpsm.shop.title.pistol","fpsm.shop.title.mid_rank","fpsm.shop.title.rifle","fpsm.shop.title.throwable"};
    private static final String[] TOP_NAME_KEYS_TEST = new String[]{"装备","手枪","中级","步枪","投掷物"};
    public static final String TACZ_MODID = "tacz";
    public static final String TACZ_AWP_ICON = "gun/hud/ai_awp.png";
    public static final Map<ItemType, List<GunButtonLayout>> shopButtons = new HashMap<>();
    public static final String BACKGROUND = "ui/cs/background.png";
    public static final int CT_COLOR = RenderUtil.color(150,200,250);
    public static final int T_COLOR = RenderUtil.color(234, 192, 85);
    public static final int DISABLE_TEXTURE_COLOR = RenderUtil.color(65,65,65);
    public static final int DISABLE_TEXT_COLOR = RenderUtil.color(100,100,100);
    public static boolean refreshFlag = false;
    public static boolean debug;
    private static CSGameShopScreen INSTANCE;
    private RelativeLayout window = null;

    public CSGameShopScreen(boolean debug){
        CSGameShopScreen.debug = debug;
    }

    public static CSGameShopScreen getInstance(boolean debug) {
        if(INSTANCE == null) {
            INSTANCE = new CSGameShopScreen(debug);
        }
        return INSTANCE;
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Configurator.setRootLevel(Level.DEBUG);
        try (ModernUI app = new ModernUI()) {
            app.run(getInstance(true));
        }
        AudioManager.getInstance().close();
        System.gc();
    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState) {
        if (window == null) {
            window = new RelativeLayout(getContext());
            var content = new LinearLayout(getContext());
            content.setOrientation(LinearLayout.HORIZONTAL);
            ImageView background = new ImageView(getContext());
            ImageDrawable imageDrawable = new ImageDrawable(Image.create(FPSMatch.MODID, "ui/cs/background.png"));
            imageDrawable.setAlpha(60);

            background.setImageDrawable(imageDrawable);
            background.setScaleType(ImageView.ScaleType.FIT_CENTER);
            var shopWindow = new LinearLayout(this.getContext());
            for (int i = 0; i < 5; i++) {
                var shopTitleBackground = new ShapeDrawable();
                shopTitleBackground.setShape(ShapeDrawable.RECTANGLE);
                shopTitleBackground.setStroke(0, 0xFFFF0000);
                var typeBar = new LinearLayout(getContext());
                typeBar.setOrientation(LinearLayout.VERTICAL);
                var titleBar = new LinearLayout(getContext());

                int gunButtonWeight = switch (i) {
                    case 2 -> 180;
                    case 3 -> 200;
                    default -> 140;
                };
                int textColor = RenderUtil.color(203, 203, 203);
                TextView numTab = new TextView(getContext());
                numTab.setTextColor(textColor);
                numTab.setText(String.valueOf(i + 1));
                numTab.setTextSize(15);
                numTab.setPadding(15, 10, 0, 0);
                numTab.setGravity(Gravity.LEFT);

                TextView title = new TextView(getContext());
                title.setTextColor(textColor);
                title.setText(I18n.get(TOP_NAME_KEYS_TEST[i]));
                title.setTextSize(21);
                title.setGravity(Gravity.CENTER);

                titleBar.addView(numTab, new LinearLayout.LayoutParams(numTab.dp(25), -1));
                titleBar.addView(title, new LinearLayout.LayoutParams(title.dp(gunButtonWeight - 25), -1));
                typeBar.addView(titleBar, new LinearLayout.LayoutParams(-1, titleBar.dp(44)));
                List<GunButtonLayout> buttons = new ArrayList<>();
                for (int j = 0; j < 5; j++) {
                    var shopHolderBackground = new ShapeDrawable();
                    shopHolderBackground.setShape(ShapeDrawable.RECTANGLE);
                    shopHolderBackground.setCornerRadius(3);
                    shopHolderBackground.setColor(RenderUtil.color(42, 42, 42));
                    var shop = new LinearLayout(getContext());
                    var gun = new LinearLayout(getContext());
                    if (shopButtons.getOrDefault(ItemType.values()[i], new ArrayList<>()).isEmpty()) {
                        buttons.add(new GunButtonLayout(getContext(), ItemType.values()[i], j));
                    } else buttons.add(new GunButtonLayout(getContext(), ItemType.values()[i], j));
                    gun.addView(buttons.get(j), new LinearLayout.LayoutParams(-1, -1));
                    shop.setGravity(Gravity.CENTER);
                    shop.addView(gun, new LinearLayout.LayoutParams(gun.dp(gunButtonWeight), gun.dp(90)));
                    typeBar.addView(shop, new LinearLayout.LayoutParams(-1, shop.dp(98)));
                }
                shopButtons.put(ItemType.values()[i], buttons);
                shopWindow.addView(typeBar, new LinearLayout.LayoutParams(typeBar.dp(gunButtonWeight + 30), -1));
            }
            content.addView(shopWindow, new LinearLayout.LayoutParams(shopWindow.dp(950), shopWindow.dp(550)));
            RelativeLayout.LayoutParams shopWindowParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            shopWindowParams.setMargins(content.dp(240), content.dp(170 + 40), 0, 0);

            RelativeLayout.LayoutParams shopWindowBackGroundParams = new RelativeLayout.LayoutParams(
                    background.dp(950),
                    background.dp(550));
            shopWindowBackGroundParams.setMargins(background.dp(240), background.dp(208), 0, 0);

            HeadBarLayout headBarLayout = new HeadBarLayout(getContext());
            RelativeLayout.LayoutParams titleBarParams = new RelativeLayout.LayoutParams(headBarLayout.dp(950), headBarLayout.dp(38));
            titleBarParams.setMargins(headBarLayout.dp(240), headBarLayout.dp(170), 0, 0);

            window.addView(headBarLayout, titleBarParams);
            window.addView(background, shopWindowBackGroundParams);
            window.addView(content, shopWindowParams);
        }
        return window;
    }

    public static class HeadBarLayout extends RelativeLayout {
        public final TextView moneyText;
        public final TextView cooldownText;
        public final TextView nextRoundMinMoneyText;
        public HeadBarLayout(Context context) {
            super(context);
            ImageView background = new ImageView(getContext());
            ImageDrawable imageDrawable =new ImageDrawable(Image.create(FPSMatch.MODID, "ui/cs/background.png"));
            imageDrawable.setAlpha(60);
            background.setImageDrawable(imageDrawable);
            background.setScaleType(ImageView.ScaleType.FIT_XY);
            addView(background);
            moneyText = new TextView(getContext());
            RelativeLayout.LayoutParams moneyParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            moneyParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            moneyParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            moneyParams.setMargins(25,0,0,0);
            moneyText.setLayoutParams(moneyParams);
            moneyText.setTextColor(T_COLOR);
            moneyText.setTextSize(18);

            cooldownText = new TextView(getContext());
            RelativeLayout.LayoutParams cooldownParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            cooldownParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            cooldownText.setText(I18n.get("fpsm.shop.title.cooldown",0));
            cooldownText.setLayoutParams(cooldownParams);
            cooldownText.setTextSize(18);


            nextRoundMinMoneyText = new TextView(getContext());
            RelativeLayout.LayoutParams minmoneyText = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            minmoneyText.addRule(RelativeLayout.CENTER_IN_PARENT);
            minmoneyText.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            nextRoundMinMoneyText.setText(I18n.get("fpsm.shop.title.min.money", ClientData.clientShopData.getNextRoundMinMoney()));
            minmoneyText.setMargins(0,0,20,0);
            nextRoundMinMoneyText.setLayoutParams(minmoneyText);
            nextRoundMinMoneyText.setTextSize(15);
            addView(moneyText);
            addView(cooldownText);
            addView(nextRoundMinMoneyText);
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            updateText();
        }

        public void updateText(){
            moneyText.setText("$"+ClientData.getMoney());
        }
    }


    public static class GunButtonLayout extends RelativeLayout {
        public static final ColorStateList TINT_LIST = new ColorStateList(
                new int[][]{
                        new int[]{-R.attr.state_enabled},
                        StateSet.get(StateSet.VIEW_STATE_ENABLED)},
                new int[]{
                        DISABLE_TEXTURE_COLOR,
                        T_COLOR
                });
        public final ItemType type;
        public final int index;
        public final ImageView imageView;
        public final ShapeDrawable backgroud;
        public final RelativeLayout returnGoodsLayout;
        public final ValueAnimator backgroundAnimeFadeIn;
        public final ValueAnimator backgroundAnimeFadeOut;
        public final TextView numText;
        public final TextView itemNameText;
        public final TextView costText;
        public final TextView returnGoodsText;
        public Image icon = Image.create(FPSMatch.MODID,TACZ_AWP_ICON);

        public GunButtonLayout(Context context, ItemType type, int index) {
            super(context);
            this.type = type;
            this.index = index;

            setGravity(Gravity.CENTER);
            setLayoutParams(new LayoutParams(-1, -1));

            this.backgroud = new ShapeDrawable();
            backgroud.setShape(ShapeDrawable.RECTANGLE);
            backgroud.setColor(RenderUtil.color(42, 42, 42));
            backgroud.setCornerRadius(3);
            backgroud.setAlpha(200);
            setBackground(backgroud);

            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ImageDrawable imageDrawable = new ImageDrawable(this.icon);
            var imageParam = new RelativeLayout.LayoutParams(39, 13);
            imageParam.addRule(RelativeLayout.CENTER_IN_PARENT);
            imageView.setLayoutParams(imageParam);
            imageView.setScaleX(3);
            imageView.setScaleY(3);
            imageView.setImageDrawable(imageDrawable);
            imageView.setImageTintList(TINT_LIST);
            imageView.setForegroundGravity(Gravity.CENTER);
            addView(imageView);

            numText = new TextView(getContext());
            numText.setTextSize(13);
            numText.setText(String.valueOf(this.index + 1));
            RelativeLayout.LayoutParams numParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            numParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            numParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            numParams.setMargins(5,5,0,0);
            numText.setLayoutParams(numParams);

            itemNameText = new TextView(getContext());
            itemNameText.setTextSize(13);
            itemNameText.setText("AWP");
            RelativeLayout.LayoutParams itemNameParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            itemNameParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            itemNameParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            itemNameParams.setMargins(0,5,5,0);
            itemNameText.setLayoutParams(itemNameParams);

            returnGoodsText = new TextView(getContext());
            returnGoodsText.setTextSize(15);
            returnGoodsText.setText("↩");
            RelativeLayout.LayoutParams returnGoodsParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            returnGoodsParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            returnGoodsParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            returnGoodsParams.setMargins(5,12,0,0);
            returnGoodsText.setLayoutParams(returnGoodsParams);
            returnGoodsLayout = new RelativeLayout(getContext()){
                @Override
                public void setEnabled(boolean enabled) {
                    returnGoodsText.setAlpha(enabled ? 255:0);
                    super.setEnabled(enabled);
                }
            };
            returnGoodsLayout.addView(returnGoodsText);
            returnGoodsLayout.setOnClickListener((l)->{
                FPSMatch.INSTANCE.sendToServer(new ShopActionC2SPacket(ClientData.currentMap,this.type,this.index, ShopActionC2SPacket.ACTION_RETURN));
            });

            returnGoodsLayout.setEnabled(false);
            addView(returnGoodsLayout);

            costText = new TextView(getContext());
            costText.setText("$"+ClientData.clientShopData.getSlotData(this.type, this.index).cost());
            costText.setTextSize(12);
            RelativeLayout.LayoutParams costParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            costParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            costParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            costParams.setMargins(0,0,5,5);
            costText.setLayoutParams(costParams);

            addView(numText);
            addView(itemNameText);
            addView(costText);

            backgroundAnimeFadeIn = ValueAnimator.ofInt(42, 72);
            backgroundAnimeFadeIn.setDuration(200);
            backgroundAnimeFadeIn.setInterpolator(TimeInterpolator.SINE);
            backgroundAnimeFadeIn.addUpdateListener(animation -> {
                int color = (int) animation.getAnimatedValue();
                this.backgroud.setColor(RenderUtil.color(color,color,color));
            });

            backgroundAnimeFadeOut = ValueAnimator.ofInt(72, 42);
            backgroundAnimeFadeOut.setDuration(200);
            backgroundAnimeFadeOut.setInterpolator(TimeInterpolator.SINE);
            backgroundAnimeFadeOut.addUpdateListener(animation -> {
                int color = (int) animation.getAnimatedValue();
                this.backgroud.setColor(RenderUtil.color(color,color,color));
            });

            setOnClickListener((v) -> {
                ShopData.ShopSlot currentSlot = ClientData.clientShopData.getSlotData(this.type, this.index);
                boolean actionFlag = ClientData.getMoney() >= currentSlot.cost();
                if(checkSlots(actionFlag)) {
                    FPSMatch.INSTANCE.sendToServer(new ShopActionC2SPacket(ClientData.currentMap, this.type, this.index, ShopActionC2SPacket.ACTION_BUY));
                }
            });
        }

        public void setStats(boolean enable){
            backgroud.setStroke(enable ? 1:0,RenderUtil.color(255,255,255));
            this.returnGoodsLayout.setEnabled(enable);
        }

        public void setElementsColor(boolean enable){
            imageView.setEnabled(enable);
            if(enable){
                numText.setTextColor(CSGameShopScreen.T_COLOR);
                itemNameText.setTextColor(CSGameShopScreen.T_COLOR);
                costText.setTextColor(CSGameShopScreen.T_COLOR);
            }else{
                numText.setTextColor(CSGameShopScreen.DISABLE_TEXT_COLOR);
                itemNameText.setTextColor(CSGameShopScreen.DISABLE_TEXT_COLOR);
                costText.setTextColor(CSGameShopScreen.DISABLE_TEXT_COLOR);
            }
        }

        public boolean checkSlots(boolean enable){
            if (!enable) return false;
            if(this.type == ItemType.THROWABLE){
                if (ClientData.clientShopData.getThrowableTypeBoughtCount() >= 4){
                    return false;
                }
                if(this.index == 0){
                    return ClientData.clientShopData.getSlotData(this.type, this.index).boughtCount() < 2;
                }
            }
            if(this.type == ItemType.EQUIPMENT){
                if(this.index == 0){
                    if(ClientData.clientShopData.getSlotData(this.type, this.index).canReturn()){
                        CSGameShopScreen.shopButtons.get(ItemType.EQUIPMENT).get(1).costText.setText("$"+ClientData.clientShopData.getSlotData(this.type, 1).cost());
                        CSGameShopScreen.shopButtons.get(ItemType.EQUIPMENT).get(1).invalidate();
                        return false;
                    }
                }

                if(this.index == 0){
                    if(ClientData.clientShopData.getSlotData(this.type, 1).canReturn() && !ClientData.clientShopData.getSlotData(this.type, this.index).canReturn()){
                        return false;
                    }
                }
            }

            if(!ClientData.clientShopData.getSlotData(this.type,this.index).canReturn()){
                backgroud.setStroke(0,RenderUtil.color(255,255,255));
                returnGoodsLayout.setEnabled(false);
                if(this.type == ItemType.EQUIPMENT){
                    if(this.index == 0){
                        CSGameShopScreen.shopButtons.get(ItemType.EQUIPMENT).get(1).costText.setText("$"+ClientData.clientShopData.getSlotData(this.type, 1).cost());
                        CSGameShopScreen.shopButtons.get(ItemType.EQUIPMENT).get(1).invalidate();
                    }
                }
            }

            return !ClientData.clientShopData.getSlotData(this.type, this.index).canReturn();
        }

        public void updateButtonState() {
           boolean enable = ClientData.getMoney() >= ClientData.clientShopData.getSlotData(this.type,this.index).cost();
           setElementsColor(checkSlots(enable));
           if(!this.isHovered()) {
               backgroundAnimeFadeIn.start();
           }else{
               backgroundAnimeFadeOut.start();
           }

            if(refreshFlag){
                ShopData.ShopSlot data = ClientData.clientShopData.getSlotData(this.type,this.index);
                setStats(data.canReturn());
                String fixedName = data.name().replace("[","").replace("]","").replaceAll("§.", "");
                this.itemNameText.setText(fixedName);
                this.costText.setText("$"+ data.cost());
                ResourceLocation texture = data.getTexture();
                this.icon = RenderUtil.getGunTextureByRL(texture);
                if (this.icon == null){
                    this.icon = Image.create(texture.getNamespace(), texture.getPath()+".png");
                }
                this.imageView.setImageDrawable(new ImageDrawable(this.icon));

                this.invalidate();
                if(this.type == ItemType.THROWABLE && this.index == 4){
                    refreshFlag = false;
                }
            }
        }

        @Override
        public void draw(@NotNull Canvas canvas) {
            super.draw(canvas);
            updateButtonState();
        }
    }
}
