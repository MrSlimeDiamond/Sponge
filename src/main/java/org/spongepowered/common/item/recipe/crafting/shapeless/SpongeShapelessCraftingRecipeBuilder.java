/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.item.recipe.crafting.shapeless;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.crafting.CraftingGridInventory;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.ShapelessCraftingRecipe;
import org.spongepowered.common.inventory.util.InventoryUtil;
import org.spongepowered.common.item.recipe.SpongeRecipeRegistration;
import org.spongepowered.common.item.recipe.ingredient.IngredientUtil;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.SpongeCatalogBuilder;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

public class SpongeShapelessCraftingRecipeBuilder extends SpongeCatalogBuilder<RecipeRegistration, ShapelessCraftingRecipe.Builder>
        implements ShapelessCraftingRecipe.Builder.EndStep, ShapelessCraftingRecipe.Builder.ResultStep {

    private org.spongepowered.api.item.inventory.ItemStack result;
    private Function<net.minecraft.inventory.CraftingInventory, net.minecraft.item.ItemStack> resultFunction;
    private Function<net.minecraft.inventory.CraftingInventory, NonNullList<net.minecraft.item.ItemStack>> remainingItemsFunction;
    private NonNullList<Ingredient> ingredients = NonNullList.create();
    private String group;

    @Override
    public ResultStep addIngredients(ItemType... ingredients) {
        for (ItemType ingredient : ingredients) {
            this.ingredients.add(Ingredient.of(() -> ((Item) ingredient)));
        }
        return this;
    }

    @Override
    public ResultStep addIngredients(Supplier<? extends ItemType>... ingredients) {
        for (Supplier<? extends ItemType> ingredient : ingredients) {
            this.ingredients.add(Ingredient.of(() -> ((Item) ingredient.get())));
        }
        return this;
    }

    @Override
    public ResultStep addIngredients(org.spongepowered.api.item.recipe.crafting.Ingredient... ingredients) {
        for (org.spongepowered.api.item.recipe.crafting.Ingredient ingredient : ingredients) {
            this.ingredients.add(IngredientUtil.toNative(ingredient));
        }
        return this;
    }

    @Override
    public ResultStep remainingItems(Function<CraftingGridInventory, List<org.spongepowered.api.item.inventory.ItemStack>> remainingItemsFunction) {
        this.remainingItemsFunction = grid -> {
            final NonNullList<ItemStack> mcList = NonNullList.create();
            remainingItemsFunction.apply(InventoryUtil.toSpongeInventory(grid)).forEach(stack -> mcList.add(ItemStackUtil.toNative(stack)));
            return mcList;
        };
        return this;
    }

    @Override
    public EndStep result(final ItemStackSnapshot result) {
        checkNotNull(result, "result");
        this.result = result.createStack();
        this.resultFunction = null;
        return this;
    }

    @Override
    public EndStep result(org.spongepowered.api.item.inventory.ItemStack result) {
        checkNotNull(result, "result");
        this.result = result;
        this.resultFunction = null;
        return this;
    }

    @Override
    public EndStep result(Function<CraftingGridInventory, org.spongepowered.api.item.inventory.ItemStack> resultFunction, org.spongepowered.api.item.inventory.ItemStack exemplaryResult) {
        this.resultFunction = (inv) -> ItemStackUtil.toNative(resultFunction.apply(InventoryUtil.toSpongeInventory(inv)));
        this.result = exemplaryResult.copy();
        return this;
    }

    @Override
    public EndStep group(@Nullable final String name) {
        this.group = name;
        return this;
    }

    @Override
    public ShapelessCraftingRecipe.Builder.EndStep key(ResourceKey key) {
        super.key(key);
        return this;
    }

    @Override
    protected RecipeRegistration build(ResourceKey key) {
        checkState(!this.ingredients.isEmpty(), "The ingredients are not set.");

        final ItemStack resultStack = ItemStackUtil.toNative(this.result);
        final IRecipeSerializer<?> serializer = SpongeRecipeRegistration.determineSerializer(resultStack, this.resultFunction, this.remainingItemsFunction,
                this.ingredients, IRecipeSerializer.SHAPELESS_RECIPE, SpongeShapelessCraftingRecipeSerializer.SPONGE_CRAFTING_SHAPELESS);
        return new SpongeShapelessCraftingRecipeRegistration((ResourceLocation) (Object) key, serializer, this.group, this.ingredients, resultStack, this.resultFunction, this.remainingItemsFunction);
    }

    @Override
    public ShapelessCraftingRecipe.Builder reset() {
        super.reset();
        this.result = null;
        this.resultFunction = null;
        this.ingredients = NonNullList.create();
        this.group = null;
        this.remainingItemsFunction = null;
        return this;
    }
}
