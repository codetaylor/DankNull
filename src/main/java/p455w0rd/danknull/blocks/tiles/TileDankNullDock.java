package p455w0rd.danknull.blocks.tiles;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import p455w0rd.danknull.api.IDankNullHandler;
import p455w0rd.danknull.init.ModDataFixing.DankNullFixer;
import p455w0rd.danknull.init.ModGlobals.NBT;
import p455w0rd.danknull.inventory.DankNullHandler;
import p455w0rd.danknull.inventory.cap.CapabilityDankNull;
import p455w0rd.danknull.items.ItemDankNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static p455w0rd.danknull.inventory.cap.DankNullCapabilityProvider.DANK_NULL_CAP_TAG;

/**
 * @author p455w0rd
 */
public class TileDankNullDock extends TileEntity {

    private final DankNullFixer fixer = new DankNullFixer(FixTypes.BLOCK_ENTITY);
    private ItemStack dankNull = ItemStack.EMPTY;
    private IDankNullHandler dankNullHandler = null;

    @Override
    public boolean hasCapability(final Capability<?> capability, final EnumFacing facing) {
        if (!getDankNull().isEmpty()) {
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityDankNull.DANK_NULL_CAPABILITY) {
                return true;
            }
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(final Capability<T> capability, final EnumFacing facing) {
        if (!getDankNull().isEmpty()) {
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(dankNullHandler);
            }
            if (capability == CapabilityDankNull.DANK_NULL_CAPABILITY) {
                return CapabilityDankNull.DANK_NULL_CAPABILITY.cast(dankNullHandler);
            }
        }
        return super.getCapability(capability, facing);
    }

    public void removeDankNull() {
        if (!getDankNull().isEmpty()) {
            dankNull = ItemStack.EMPTY;
            dankNullHandler = null;
            markDirty();
        }
    }

    public ItemStack getDankNull() {
        return dankNull;
    }

    public void setDankNull(final ItemStack dankNull) {
        this.dankNull = dankNull.copy();
        if (!this.dankNull.isEmpty()) {
            dankNullHandler = new DankNullHandler(ItemDankNull.getTier(this.dankNull)) {

                @Override
                public ItemStack getStackInSlot(final int slot) {
                    validateSlot(slot);
                    return getExtractableStackInSlot(slot);
                }

                protected void onContentsChanged(final int slot) {
                    //sort();
//        updateSelectedSlot();
                    onDataChanged();
                    TileDankNullDock.this.markDirty();
                }

                @Override
                protected void onDataChanged() {
                    super.onDataChanged();
                    TileDankNullDock.this.markDirty();
                }
            };
            CapabilityDankNull.DANK_NULL_CAPABILITY.readNBT(dankNullHandler, null, this.dankNull.getTagCompound().getCompoundTag(DANK_NULL_CAP_TAG));
        }
        markDirty();
    }

    @Override
    public boolean shouldRefresh(final World world, final BlockPos pos, final IBlockState oldState, final IBlockState newSate) {
        return super.shouldRefresh(world, pos, oldState, newSate);
    }

    @Override
    @Nonnull
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), -1, getUpdateTag());
    }

    @Override
    public void onDataPacket(final NetworkManager net, final SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    @Override
    public void markDirty() {
        super.markDirty();
        //VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        if (world != null) { // Shouldn't be null
            world.markBlockRangeForRenderUpdate(pos, pos);
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            world.scheduleBlockUpdate(pos, getBlockType(), 0, 0);
        }
    }

    @Override
    public void readFromNBT(final NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey(NBT.DOCKEDSTACK, Constants.NBT.TAG_COMPOUND)) {
            final NBTTagCompound dockedTag = nbt.getCompoundTag(NBT.DOCKEDSTACK);
            final ItemStack dankNull = new ItemStack(dockedTag);
            setDankNull(dankNull);
            if (!dankNull.isEmpty() && dankNull.hasTagCompound() && dankNull.getTagCompound().hasKey(DANK_NULL_CAP_TAG)) {
                CapabilityDankNull.DANK_NULL_CAPABILITY.readNBT(dankNullHandler, null, dankNull.getTagCompound().getCompoundTag(DANK_NULL_CAP_TAG));
            }
        }
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        final ItemStack dankNull = getDankNull();
        if (dankNullHandler != null) {
            dankNull.getTagCompound().setTag(DANK_NULL_CAP_TAG, CapabilityDankNull.DANK_NULL_CAPABILITY.writeNBT(dankNullHandler, null));
        }
        compound.setTag(NBT.DOCKEDSTACK, dankNull.serializeNBT());
        return compound;
    }

}
