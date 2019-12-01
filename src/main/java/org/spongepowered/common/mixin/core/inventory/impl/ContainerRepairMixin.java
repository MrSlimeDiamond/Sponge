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
package org.spongepowered.common.mixin.core.inventory.impl;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.RepairContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IntReferenceHolder;
import org.spongepowered.api.event.item.inventory.UpdateAnvilEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.bridge.inventory.LensProviderBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.inventory.adapter.impl.slots.InputSlotAdapter;
import org.spongepowered.common.inventory.adapter.impl.slots.OutputSlotAdapter;
import org.spongepowered.common.inventory.fabric.Fabric;
import org.spongepowered.common.inventory.lens.Lens;
import org.spongepowered.common.inventory.lens.impl.DefaultIndexedLens;
import org.spongepowered.common.inventory.lens.impl.comp.PrimaryPlayerInventoryLens;
import org.spongepowered.common.inventory.lens.impl.minecraft.container.ContainerLens;
import org.spongepowered.common.inventory.lens.impl.slot.InputSlotLens;
import org.spongepowered.common.inventory.lens.impl.slot.OutputSlotLens;
import org.spongepowered.common.inventory.lens.impl.slot.SlotLensCollection;
import org.spongepowered.common.inventory.lens.impl.slot.SlotLensProvider;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.List;

@Mixin(RepairContainer.class)
public abstract class ContainerRepairMixin extends ContainerMixin implements LensProviderBridge {

    @Shadow private String repairedItemName;
    @Shadow @Final private IInventory outputSlot;
    @Shadow public IntReferenceHolder maximumCost;
    @Shadow private int materialCost;
    @Shadow @Final private IInventory inputSlots;

    @Override
    public Lens bridge$rootLens(final Fabric fabric, final InventoryAdapter adapter) {
        final List<Lens> lenses = new ArrayList<>();
        lenses.add(new DefaultIndexedLens(0, 3, bridge$getSlotProvider()));
        lenses.add(new PrimaryPlayerInventoryLens(3, bridge$getSlotProvider(), true));
        return new ContainerLens(adapter.bridge$getFabric().fabric$getSize(), (Class<? extends Inventory>) adapter.getClass(), bridge$getSlotProvider(), lenses);
    }

    @Override
    public SlotLensProvider bridge$slotProvider(final Fabric fabric, final InventoryAdapter adapter) {
        final SlotLensCollection.Builder builder = new SlotLensCollection.Builder()
                .add(2, InputSlotAdapter.class, i -> new InputSlotLens(i, s -> true, t -> true))
                .add(1, OutputSlotAdapter.class, i -> new OutputSlotLens(i, s -> false, t -> false))
                .add(36);
        return builder.build();
    }

    @Inject(method = "updateRepairOutput", at = @At(value = "RETURN"))
    private void impl$throwUpdateAnvilEvent(final CallbackInfo ci) {
        if (!ShouldFire.UPDATE_ANVIL_EVENT || !SpongeImplHooks.isMainThread()) {
            return;
        }

        final ItemStack itemstack = this.inputSlots.getStackInSlot(0);
        final ItemStack itemstack2 = this.inputSlots.getStackInSlot(1);
        final ItemStack result = this.outputSlot.getStackInSlot(0);
        final UpdateAnvilEvent event = SpongeCommonEventFactory.callUpdateAnvilEvent(
                (RepairContainer) (Object) this, itemstack, itemstack2, result,
                this.repairedItemName, this.maximumCost.get(), this.materialCost);

        final ItemStackSnapshot finalItem = event.getResult().getFinal();
        if (event.isCancelled() || finalItem.isEmpty()) {
            this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
            this.maximumCost.set(0);
            this.materialCost = 0;
            this.detectAndSendChanges();
            return;
        }

        this.outputSlot.setInventorySlotContents(0, ItemStackUtil.fromSnapshotToNative(event.getResult().getFinal()));
        this.maximumCost.set(event.getCosts().getFinal().getLevelCost());
        this.materialCost = event.getCosts().getFinal().getMaterialCost();
        this.listeners.forEach(l -> l.sendWindowProperty(((RepairContainer)(Object) this), 0, this.maximumCost.get()));
        this.detectAndSendChanges();
    }

}
