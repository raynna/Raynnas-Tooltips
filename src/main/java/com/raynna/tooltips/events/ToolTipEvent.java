package com.raynna.tooltips.events;

import com.raynna.tooltips.Tooltip;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@EventBusSubscriber(modid = Tooltip.MOD_ID, value = Dist.CLIENT)
public class ToolTipEvent {

    public static final String GRAY = "§7";        // Gray
    public static final String LIGHT_PURPLE = "§d"; // Light Purple/Pink
    public static final String YELLOW = "§e";      // Yellow
    public static final String WHITE = "§f";       // White

    public static final String HEART_ICON = "❤";
    public static final String SATURATION_ICON = "🍗";

    private static AtomicInteger myToolTipIndex;

    private static boolean shouldProcessTooltip(ItemTooltipEvent event) {
        return event.getEntity() == null || event.getEntity().isAlive();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemHover(ItemTooltipEvent event) {
        if (!shouldProcessTooltip(event)) return;
        myToolTipIndex = new AtomicInteger(1);
        TooltipContext context = new TooltipContext(event);
        handleEnchantTooltips(context);
        handleFoodEffectsTooltips(context);
        handleDebugTooltips(context);
    } 

    private static class TooltipContext {
        public final ItemTooltipEvent event;
        public final ItemStack stack;
        public final boolean isCreative;
        public final boolean isShiftDown;
        public final boolean isAltDown;
        public final boolean isControlDown;

        public TooltipContext(ItemTooltipEvent event) {
            this.event = event;
            this.stack = event.getItemStack();
            this.isCreative = Minecraft.getInstance().player.isCreative();
            this.isShiftDown = GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(),
                    GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
            this.isAltDown = GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(),
                    GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
            this.isControlDown = GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(),
                    GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
        }
    }

    private static void removeTooltipLines(TooltipContext context, String... matches) {
        Iterator<Component> it = context.event.getToolTip().iterator();
        while (it.hasNext()) {
            Component component = it.next();
            String line = component.getString().toLowerCase();
            for (String match : matches) {
                if (line.contains(match.toLowerCase())) {
                    it.remove();
                    break;
                }
            }
        }
    }

    private static void handleFoodEffectsTooltips(TooltipContext context) {
        ItemStack stack = context.stack;
        List<Component> tooltip = context.event.getToolTip();
        if (stack.isEmpty()) {
            return;
        }

        FoodProperties foodProps = stack.getItem().getFoodProperties(stack, null);
        if (foodProps == null) {
            return;
        }
        int index = Math.min(myToolTipIndex.getAndIncrement(), tooltip.size());


        String saturdation = String.format("%.1f", foodProps.saturation());
        String nutrition = String.format(String.valueOf(foodProps.nutrition()));
        tooltip.add(index, Component.literal(HEART_ICON + nutrition + " " + SATURATION_ICON + saturdation));

        List<FoodProperties.PossibleEffect> effects = foodProps.effects();
        if (!effects.isEmpty()) {
            index = Math.min(myToolTipIndex.getAndIncrement(), tooltip.size());
            tooltip.add(index, Component.literal(LIGHT_PURPLE + " Effects:"));
            for (FoodProperties.PossibleEffect effectPair : effects) {
                MobEffectInstance effect = effectPair.effect();
                float probability = effectPair.probability();
                String effectName = effect.getEffect().value().getDisplayName().getString();
                String duration = MobEffectUtil.formatDuration(effect, 1.0f, 20.0f).getString();
                String amplifier = getAmplifierString(effect.getAmplifier());

                index = Math.min(myToolTipIndex.getAndIncrement(), tooltip.size());
                tooltip.add(index, Component.literal(WHITE + " " + effectName + " " + amplifier));

                index = Math.min(myToolTipIndex.getAndIncrement(), tooltip.size());
                tooltip.add(index,
                        Component.literal(GRAY + "    Duration: " + duration +
                                (probability < 1.0F ? " (" + (int) (probability * 100) + "% chance)" : "")));

            }
        }
    }

    private static String getAmplifierString(int amplifier) {
        if (amplifier <= 0) return "";
        return switch (amplifier) {
            case 1 -> "II";
            case 2 -> "III";
            case 3 -> "IV";
            case 4 -> "V";
            default -> "[" + (amplifier + 1) + "]";
        };
    }

    private static void handleEnchantTooltips(TooltipContext context) {
        ItemStack stack = context.stack;
        List<Component> tooltip = context.event.getToolTip();

        if (!stack.isEnchanted()) return;

        ItemEnchantments enchantments = stack.getTagEnchantments();
        if (enchantments == null || enchantments.isEmpty()) return;

        Set<String> enchantNames = new HashSet<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Enchantment enchant = entry.getKey().value();
            enchantNames.add(enchant.description().getString());
        }

        Iterator<Component> it = tooltip.iterator();
        while (it.hasNext()) {
            String line = it.next().getString();
            for (String enchantName : enchantNames) {
                if (line.contains(enchantName)) {
                    it.remove();
                    break;
                }
            }
        }
        AtomicInteger index = new AtomicInteger(Math.min(myToolTipIndex.getAndIncrement(), context.event.getToolTip().size()));
        if (context.isAltDown) {
            tooltip.add(index.get(), Component.literal(LIGHT_PURPLE + "Enchantments:"));

            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                Enchantment enchant = entry.getKey().value();
                int level = entry.getIntValue();
                String name = enchant.description().getString();
                index.set(Math.min(myToolTipIndex.getAndIncrement(), context.event.getToolTip().size()));
                tooltip.add(index.get(), Component.literal(WHITE + " " + name + " " + level));
                if (!name.matches("^[a-zA-Z0-9].*")) {
                    name = name.replaceAll("^[^ ]+", "").trim();
                }
                index.set(Math.min(myToolTipIndex.getAndIncrement(), context.event.getToolTip().size()));
                tooltip.add(index.get(), Component.literal(GRAY + "    " + getEnchantmentDescription(name, level)));
            }
        } else {
            StringBuilder compactLine = new StringBuilder(LIGHT_PURPLE + "Enchantments: ");

            boolean first = true;
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                Enchantment enchant = entry.getKey().value();
                int level = entry.getIntValue();
                String name = enchant.description().getString();

                if (!first) {
                    compactLine.append(", ");
                }
                compactLine.append(WHITE).append(name).append(" ").append(level);
                first = false;
            }

            tooltip.add(index.get(), Component.literal(compactLine.toString()));

            index.set(Math.min(myToolTipIndex.getAndIncrement(), context.event.getToolTip().size()));
            tooltip.add(index.get(), Component.literal(YELLOW + "  Enchant Details [Left Alt]"));
        }
    }

    private static String getEnchantmentDescription(String enchantName, int level) {
        switch (enchantName.toLowerCase()) {
            case "aqua affinity":
                return "Increases the rate of underwater mining speed.";
            case "flame":
                return "Arrows set targets on fire.";
            case "capturing":
                return String.format("Gives a chance of %s for any mob killed to drop their spawn egg (0.5 per level)", level * 0.5);
            case "breach":
                return String.format("Negate the effectiveness of enemy armour by %d%% (15%% per level)", level * 15);
            case "density":
                return String.format("Increases the damage by %s for each block fallen (0.5 per level)", level * 0.5);
            case "protection":
                return String.format("Reduces all damage by %d%% (4%% per level)", level * 4);
            case "fire protection":
                return String.format("Reduces fire damage by %d%% (8%% per level)", level * 8);
            case "feather falling":
                return String.format("Reduces fall damage by %d%% (12%% per level)", level * 12);
            case "blast protection":
                return String.format("Reduces explosion damage by %d%% (8%% per level)", level * 8);
            case "projectile protection":
                return String.format("Reduces projectile damage by %d%% (8%% per level)", level * 8);
            case "frost walker":
                return "Water walked on turns into frosted ice and prevents player from taking damage from magma blocks.";
            case "thorns":
                return String.format("%d%% chance to damage attackers (15%% per level)", level * 15);
            case "depth strider":
                return String.format("Increases underwater speed by %d%% (33%% per level)", level * 33);
            case "soul speed":
                return String.format("Increases speed on soul sand by %d%% (30%% per level)", level * 30);
            case "swift sneak":
                return String.format("Increases sneak speed by %d%% (15%% per level)", level * 15);
            case "sharpness":
                return String.format("+%.1f damage (1.25 per level)", level * 1.25);
            case "smite":
                return String.format("+%.1f damage vs undead (2.5 per level)", level * 2.5);
            case "bane of arthropods":
                return String.format("+%.1f damage vs arthropods (2.5 per level)", level * 2.5);
            case "knockback":
                return String.format("Adds %d blocks knockback (3 per level)", level * 3);
            case "fire aspect":
                return String.format("Burns target for %d seconds (4 per level)", level * 4);
            case "looting":
                return String.format("+%d%% loot drops (33%% per level)", level * 33);
            case "sweeping edge":
                return String.format("Adds %d%% sweeping damage (25%% per level)", 25 + (level - 1) * 25);
            case "impaling":
                return String.format("+%.1f damage vs aquatic mobs (2.5 per level)", level * 2.5);
            case "efficiency":
                return String.format("+%d%% mining speed (30%% per level)", level * 30);
            case "fortune":
                int fortuneChance = level * 20;
                return String.format("%d%% chance for extra drops (+20%% per level)", Math.min(fortuneChance, 100));
            case "unbreaking":
                double reduction = 100 - (100 / (level + 1.0));
                return String.format("Reduces durability loss by %.1f%%", reduction);
            case "power":
                return String.format("+%d%% arrow damage (25%% per level)", level * 25);
            case "punch":
                return String.format("Adds %d blocks knockback (3 per level)", level * 3);
            case "quick charge":
                return String.format("%d%% faster reload (25%% per level)", level * 25);
            case "piercing":
                return String.format("Pierces through %d entities (1 per level)", level);
            case "mending":
                return "Repairs 2 durability per XP orb";
            case "infinity":
                return "100% chance to not consume arrows";
            case "channeling":
                return level == 1 ? "Channels a bolt of lightning on hit enemy during storms" : "Max level is I";
            case "riptide":
                return String.format("Launches player %d blocks (scales with level)", level * 3);
            case "curse of binding":
                return "Item cannot be removed from armour slots, unless the cause is death or breaking.";
            case "curse of vanishing":
                return "Item will be destroy upon death";
            case "silk touch":
                return "Mined blocks will drop as blocks instead of breaking into other items/blocks";
            default:
                return "Active (level " + level + ")";
        }
    }

    private static void handleDebugTooltips(TooltipContext context) {
        if (!context.isCreative &&
                !(context.event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(2))) {
            return;
        }
        AtomicInteger index = new AtomicInteger(context.event.getToolTip().size());
        if (!context.isShiftDown) {
            context.event.getToolTip().add(context.event.getToolTip().size(), Component.literal(YELLOW + "Debug" + GRAY + " [Left Shift]"));
            return;
        } else {
            context.event.getToolTip().add(context.event.getToolTip().size(), Component.literal(YELLOW + "Debug"));
        }
        context.event.getToolTip().add(index.getAndIncrement(), Component.literal(WHITE + "Description: " + GRAY + context.stack.getItem().getDescriptionId()));
        context.event.getToolTip().add(index.getAndIncrement(), Component.literal(WHITE + "Tags: "));
        context.stack.getTags().forEach(tag ->
                context.event.getToolTip().add(index.getAndIncrement(), Component.literal(GRAY + "- " + tag.location()))
        );
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(ToolTipEvent.class);
    }
}
