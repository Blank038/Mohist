/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.items.wrapper;

import com.mohistmc.api.event.InvWrapperMoveItemEvent;
import com.mohistmc.inventory.InventoryOwner;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;

public class InvWrapper implements IItemHandlerModifiable
{
    private final IInventory inv;

    public InvWrapper(IInventory inv)
    {
        this.inv = inv;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        InvWrapper that = (InvWrapper) o;

        return getInv().equals(that.getInv());

    }

    @Override
    public int hashCode()
    {
        return getInv().hashCode();
    }

    @Override
    public int getSlots()
    {
        return getInv().getContainerSize();
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot)
    {
        return getInv().getItem(slot);
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty())
            return ItemStack.EMPTY;

        ItemStack stackInSlot = getInv().getItem(slot);
        CraftItemStack oitemstack = CraftItemStack.asCraftMirror(stackInSlot);
        InvWrapperMoveItemEvent.Insert event = new InvWrapperMoveItemEvent.Insert(InventoryOwner.getInventory(getInv()), oitemstack);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return stack;
        }

        int m;
        if (!stackInSlot.isEmpty())
        {
            if (stackInSlot.getCount() >= Math.min(stackInSlot.getMaxStackSize(), getSlotLimit(slot)))
                return stack;

            if (!ItemHandlerHelper.canItemStacksStack(stack, stackInSlot))
                return stack;

            if (!getInv().canPlaceItem(slot, stack))
                return stack;

            m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - stackInSlot.getCount();

            if (stack.getCount() <= m)
            {
                if (!simulate)
                {
                    ItemStack copy = stack.copy();
                    copy.grow(stackInSlot.getCount());
                    getInv().setItem(slot, copy);
                    getInv().setChanged();
                }

                return ItemStack.EMPTY;
            }
            else
            {
                // copy the stack to not modify the original one
                stack = stack.copy();
                if (!simulate)
                {
                    ItemStack copy = stack.split(m);
                    copy.grow(stackInSlot.getCount());
                    getInv().setItem(slot, copy);
                    getInv().setChanged();
                    return stack;
                }
                else
                {
                    stack.shrink(m);
                    return stack;
                }
            }
        }
        else
        {
            if (!getInv().canPlaceItem(slot, stack))
                return stack;

            m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
            if (m < stack.getCount())
            {
                // copy the stack to not modify the original one
                stack = stack.copy();
                if (!simulate)
                {
                    getInv().setItem(slot, stack.split(m));
                    getInv().setChanged();
                    return stack;
                }
                else
                {
                    stack.shrink(m);
                    return stack;
                }
            }
            else
            {
                if (!simulate)
                {
                    getInv().setItem(slot, stack);
                    getInv().setChanged();
                }
                return ItemStack.EMPTY;
            }
        }

    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (amount == 0)
            return ItemStack.EMPTY;

        ItemStack stackInSlot = getInv().getItem(slot);

        if (stackInSlot.isEmpty())
            return ItemStack.EMPTY;

        CraftItemStack oitemstack = CraftItemStack.asCraftMirror(stackInSlot);
        InvWrapperMoveItemEvent.Extract event = new InvWrapperMoveItemEvent.Extract(InventoryOwner.getInventory(getInv()), oitemstack);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return ItemStack.EMPTY;
        }
        if (simulate)
        {
            if (stackInSlot.getCount() < amount)
            {
                return stackInSlot.copy();
            }
            else
            {
                ItemStack copy = stackInSlot.copy();
                copy.setCount(amount);
                return copy;
            }
        }
        else
        {
            int m = Math.min(stackInSlot.getCount(), amount);

            ItemStack decrStackSize = getInv().removeItem(slot, m);
            getInv().setChanged();
            return decrStackSize;
        }
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack)
    {
        getInv().setItem(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return getInv().getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack)
    {
        return getInv().canPlaceItem(slot, stack);
    }

    public IInventory getInv()
    {
        return inv;
    }
}
