package com.zhuhongming.bettercommandblock.client;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ItemStackCommandBlock extends BaseCommandBlock {

    private final Minecraft minecraft;

    public ItemStackCommandBlock(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    @Nullable
    public ServerLevel getLevel() {
        if (this.minecraft.getSingleplayerServer() == null) {
            return null;
        }
        return this.minecraft.getSingleplayerServer().overworld();
    }

    @Override
    public void onUpdated() {}

    @Override
    public Vec3 getPosition() {
        return this.minecraft.player != null ? this.minecraft.player.position() : Vec3.ZERO;
    }

    @Override
    public CommandSourceStack createCommandSourceStack() {
        return new CommandSourceStack(
                this,
                this.getPosition(),
                net.minecraft.world.phys.Vec2.ZERO,
                this.getLevel(),
                2,
                this.getName().getString(),
                this.getName(),
                this.minecraft.getSingleplayerServer(),
                null);
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
