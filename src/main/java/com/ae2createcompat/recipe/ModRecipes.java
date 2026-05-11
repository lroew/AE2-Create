package com.ae2createcompat.recipe;

import com.ae2createcompat.AE2CreateCompat;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 配方类型注册 - 预留 AE2 与 Create 联动的特殊配方系统
 */
public class ModRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, AE2CreateCompat.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AE2CreateCompat.MODID);

    /**
     * ME 合成模式配方类型 - 用于定义 AE2 合成模式如何在 Create 设备中处理
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
                        ItemStack.SIMPLE_ITEM_CODEC.listOf().fieldOf("inputs").forGetter(MEPatternRecipe::getInputs)
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
    }

    private static MEPatternRecipe decodeFromNetwork(RegistryFriendlyByteBuf buf) {
        ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int size = buf.readVarInt();
        java.util.List<ItemStack> inputs = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            inputs.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return new MEPatternRecipe(output, inputs);
    }

    /**
     * ME 合成模式配方 - 定义一种 AE2 合成模式到 Create 加工设备的映射
     *
     * 例如：AE2 合成"铁锭" -> Create 滚筒压制（矿石处理）
     */
    public static class MEPatternRecipe implements Recipe<RecipeInput> {
        private final ItemStack output;
        private final java.util.List<ItemStack> inputs;

        public MEPatternRecipe(ItemStack output, java.util.List<ItemStack> inputs) {
            this.output = output;
            this.inputs = inputs;
        }

        public ItemStack getOutput() {
            return output;
        }

        public java.util.List<ItemStack> getInputs() {
            return inputs;
        }

        @Override
        public boolean matches(RecipeInput input, Level level) {
            return true; // 自定义匹配逻辑
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
