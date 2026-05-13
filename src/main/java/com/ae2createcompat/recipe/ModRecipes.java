package com.ae2createcompat.recipe;

import com.ae2createcompat.AE2CreateCompat;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 配方类型注册 - AE2 与 Create 联动的特殊配方系统
 *
 * ME Pattern Recipe 定义一种 AE2 合成模式到 Create 加工设备的映射：
 * - 输入: AE2 合成所需的材料列表
 * - 输出: Create 加工后的产物
 * - 映射: 使用的 Create 加工设备类型
 *
 * 支持的加工类型：
 * - PRESS: 滚筒压制
 * - MIXING: 混合器混合
 * - CUTTING: 切割机切割
 * - WASHING: 密封风扇洗涤
 * - DRYING: 密封风扇烘干
 * - BLASTING: 密封风扇喷射
 * - HAUNTING: 密封风扇抽气
 * - FILLING: 翻滚漏斗注入
 * - DEPLOYING: 发射器操作
 * - CRAFTING: 装配器合成
 */
public class ModRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, AE2CreateCompat.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AE2CreateCompat.MODID);

    /**
     * ME 合成模式配方类型
     */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MEPatternRecipe>> ME_PATTERN_RECIPE_TYPE =
            RECIPE_TYPES.register("me_pattern", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "ae2createcompat:me_pattern";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MEPatternRecipe>> ME_PATTERN_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("me_pattern", () -> new RecipeSerializer<>() {
                private static final MapCodec<MEPatternRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                        ItemStack.SIMPLE_ITEM_CODEC.fieldOf("output").forGetter(MEPatternRecipe::getOutput),
                        ItemStack.SIMPLE_ITEM_CODEC.listOf().fieldOf("inputs").forGetter(MEPatternRecipe::getInputs),
                        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.optionalFieldOf("processing_type", "CRAFTING")
                                .forGetter(MEPatternRecipe::getProcessingType)
                ).apply(inst, MEPatternRecipe::new));

                private static final StreamCodec<RegistryFriendlyByteBuf, MEPatternRecipe> STREAM_CODEC =
                        StreamCodec.of(
                                ModRecipes::encodeToNetwork,
                                ModRecipes::decodeFromNetwork
                        );

                @Override
                public MapCodec<MEPatternRecipe> codec() {
                    return CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, MEPatternRecipe> streamCodec() {
                    return STREAM_CODEC;
                }
            });

    private static void encodeToNetwork(RegistryFriendlyByteBuf buf, MEPatternRecipe recipe) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.getOutput());
        buf.writeVarInt(recipe.getInputs().size());
        for (ItemStack input : recipe.getInputs()) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, input);
        }
        buf.writeUtf(recipe.getProcessingType());
    }

    private static MEPatternRecipe decodeFromNetwork(RegistryFriendlyByteBuf buf) {
        ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int size = buf.readVarInt();
        java.util.List<ItemStack> inputs = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            inputs.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        String processingType = buf.readUtf();
        return new MEPatternRecipe(output, inputs, processingType);
    }

    /**
     * Create 加工类型常量
     */
    public static final class ProcessingTypes {
        public static final String PRESS = "PRESS";
        public static final String MIXING = "MIXING";
        public static final String CUTTING = "CUTTING";
        public static final String WASHING = "WASHING";
        public static final String DRYING = "DRYING";
        public static final String BLASTING = "BLASTING";
        public static final String HAUNTING = "HAUNTING";
        public static final String FILLING = "FILLING";
        public static final String DEPLOYING = "DEPLOYING";
        public static final String CRAFTING = "CRAFTING";
    }

    /**
     * ME 合成模式配方 - 定义 AE2 合成模式到 Create 加工设备的映射
     */
    public static class MEPatternRecipe implements Recipe<RecipeInput> {
        private final ItemStack output;
        private final java.util.List<ItemStack> inputs;
        private final String processingType;

        public MEPatternRecipe(ItemStack output, java.util.List<ItemStack> inputs) {
            this(output, inputs, ProcessingTypes.CRAFTING);
        }

        public MEPatternRecipe(ItemStack output, java.util.List<ItemStack> inputs, String processingType) {
            this.output = output;
            this.inputs = inputs;
            this.processingType = processingType;
        }

        public ItemStack getOutput() {
            return output;
        }

        public java.util.List<ItemStack> getInputs() {
            return inputs;
        }

        public String getProcessingType() {
            return processingType;
        }

        @Override
        public boolean matches(RecipeInput input, Level level) {
            return true;
        }

        @Override
        public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
            return output.copy();
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return true;
        }

        @Override
        public ItemStack getResultItem(HolderLookup.Provider registries) {
            return output;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return ME_PATTERN_RECIPE_SERIALIZER.get();
        }

        @Override
        public RecipeType<?> getType() {
            return ME_PATTERN_RECIPE_TYPE.get();
        }
    }
}
