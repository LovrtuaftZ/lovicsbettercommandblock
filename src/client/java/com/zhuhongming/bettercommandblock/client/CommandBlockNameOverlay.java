package com.zhuhongming.bettercommandblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zhuhongming.bettercommandblock.api.CommandBlockEntityNameAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class CommandBlockNameOverlay {

    private CommandBlockNameOverlay() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.consumers() == null) {
                return;
            }
            render(context.matrixStack(), context.camera(), context.consumers(), context.tickDelta());
        });
    }

    private static void render(
            PoseStack poseStack, Camera camera, MultiBufferSource bufferSource, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
        if (!(blockEntity instanceof CommandBlockEntity commandBlockEntity)) {
            return;
        }

        Component name =
                ((CommandBlockEntityNameAccessor) commandBlockEntity).bettercommandblock$getStoredCustomName();
        if (name == null) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        Vec3 labelPos = Vec3.atCenterOf(pos).add(0.0D, 0.75D, 0.0D);
        if (cameraPos.distanceToSqr(labelPos) > 64.0D * 64.0D) {
            return;
        }

        int light = minecraft.level.getMaxLocalRawBrightness(pos);

        poseStack.pushPose();
        poseStack.translate(labelPos.x - cameraPos.x, labelPos.y - cameraPos.y, labelPos.z - cameraPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        float scale = 0.025F;
        poseStack.scale(-scale, -scale, scale);

        Font font = minecraft.font;
        float opacity = minecraft.options.getBackgroundOpacity(0.25F);
        int backgroundColor = (int) (opacity * 255.0F) << 24;
        float textX = -font.width(name) / 2.0F;
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(
                name,
                textX,
                0.0F,
                -1,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                backgroundColor,
                light);
        poseStack.popPose();
    }
}
