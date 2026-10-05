/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  com.llamalad7.mixinextras.injector.v2.WrapWithCondition
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  org.joml.Quaternionfc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArgs
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.invoke.arg.Args
 */
package me.alpha432.chimeraclient.mio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.alpha432.chimeraclient.mio.render.ViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value={HeldItemRenderer.class}, priority=9999)
public abstract class MioViewModelHeldItemRendererMixin {
    @Inject(method={"method_3228(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_759;method_3233(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V")})
    private void mio$transformItem(AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float f2, Hand hand, float f3, ItemStack itemStack, float f4, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int n, CallbackInfo callbackInfo) {
        float f5;
        boolean bl;
        boolean bl2;
        ViewModel viewModel = ViewModel.INSTANCE;
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        if (viewModel == null || !viewModel.isEnabled() || clientPlayerEntity == null) {
            return;
        }
        boolean bl3 = itemStack.contains(DataComponentTypes.FOOD) || itemStack.getItem() instanceof PotionItem;
        boolean bl4 = bl2 = clientPlayerEntity.getActiveItem().getItem() == itemStack.getItem();
        if (bl3 && clientPlayerEntity.getActiveItem() == itemStack && !viewModel.eating.getValue().booleanValue()) {
            return;
        }
        boolean bl5 = bl = clientPlayerEntity.getMainArm() == Arm.LEFT;
        if (hand == Hand.MAIN_HAND) {
            float f6;
            float f7 = bl3 && bl2 ? 0.0f : -viewModel.mainX.getValue().floatValue();
            float f8 = f6 = bl3 && bl2 ? 0.0f : viewModel.mainZ.getValue().floatValue();
            if (bl) {
                f7 = -f7;
            }
            matrixStack.translate(f7, viewModel.mainY.getValue().floatValue(), f6);
            matrixStack.scale(viewModel.mainScaleX.getValue().floatValue(), viewModel.mainScaleY.getValue().floatValue(), viewModel.mainScaleZ.getValue().floatValue());
            matrixStack.multiply((Quaternionfc)RotationAxis.NEGATIVE_X.rotationDegrees(viewModel.mainRotateX.getValue().floatValue()));
            matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(viewModel.mainRotateY.getValue().floatValue()));
            matrixStack.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees(viewModel.mainRotateZ.getValue().floatValue()));
            return;
        }
        float f9 = bl3 && bl2 ? 0.0f : viewModel.offX.getValue().floatValue();
        float f10 = f5 = bl3 && bl2 ? 0.0f : viewModel.offZ.getValue().floatValue();
        if (bl) {
            f9 = -f9;
        }
        matrixStack.translate(f9, viewModel.offY.getValue().floatValue(), f5);
        matrixStack.scale(viewModel.offScaleX.getValue().floatValue(), viewModel.offScaleY.getValue().floatValue(), viewModel.offScaleZ.getValue().floatValue());
        matrixStack.multiply((Quaternionfc)RotationAxis.NEGATIVE_X.rotationDegrees(viewModel.offRotateX.getValue().floatValue()));
        matrixStack.multiply((Quaternionfc)RotationAxis.NEGATIVE_Y.rotationDegrees(viewModel.offRotateY.getValue().floatValue()));
        matrixStack.multiply((Quaternionfc)RotationAxis.NEGATIVE_Z.rotationDegrees(viewModel.offRotateZ.getValue().floatValue()));
    }

    @Inject(method={"method_3219(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;IFFLnet/minecraft/class_1306;)V"}, at={@At(value="HEAD")})
    private void mio$transformArm(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int n, float f, float f2, Arm arm, CallbackInfo callbackInfo) {
        ViewModel viewModel = ViewModel.INSTANCE;
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        if (viewModel == null || !viewModel.isEnabled() || !viewModel.arm.getValue().booleanValue() || clientPlayerEntity == null) {
            return;
        }
        float f3 = -viewModel.mainX.getValue().floatValue();
        if (clientPlayerEntity.getMainArm() == Arm.LEFT) {
            f3 = -f3;
        }
        matrixStack.translate(f3, viewModel.mainY.getValue().floatValue(), viewModel.mainZ.getValue().floatValue());
        matrixStack.scale(viewModel.mainScaleX.getValue().floatValue(), viewModel.mainScaleY.getValue().floatValue(), viewModel.mainScaleZ.getValue().floatValue());
    }

    @WrapOperation(method={"method_22976(FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;Lnet/minecraft/class_746;I)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_759;method_3228(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V")})
    private void mio$swingProgress(HeldItemRenderer heldItemRenderer, AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float f2, Hand hand, float f3, ItemStack itemStack, float f4, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int n, Operation<Void> operation) {
        ViewModel viewModel = ViewModel.INSTANCE;
        float f5 = viewModel != null ? viewModel.swingProgress(hand, f3) : f3;
        operation.call(new Object[]{heldItemRenderer, abstractClientPlayerEntity, Float.valueOf(f), Float.valueOf(f2), hand, Float.valueOf(f5), itemStack, Float.valueOf(f4), matrixStack, orderedRenderCommandQueue, n});
    }

    @WrapWithCondition(method={"method_22976(FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;Lnet/minecraft/class_746;I)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_4587;method_22907(Lorg/joml/Quaternionfc;)V")})
    private boolean mio$noSway(MatrixStack matrixStack, Quaternionfc quaternionfc) {
        ViewModel viewModel = ViewModel.INSTANCE;
        return viewModel == null || !viewModel.isEnabled() || viewModel.noSway.getValue() == false;
    }

    @ModifyExpressionValue(method={"method_3228(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_742;method_6123()Z")})
    private boolean mio$noTridentAnim(boolean bl) {
        ViewModel viewModel = ViewModel.INSTANCE;
        return viewModel != null && viewModel.isEnabled() && viewModel.noTridentAnim.getValue() != false ? false : bl;
    }

    @ModifyArgs(method={"method_3224(Lnet/minecraft/class_4587;Lnet/minecraft/class_1306;F)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/class_4587;method_46416(FFF)V"))
    private void mio$instantSwap(Args args) {
        ViewModel viewModel = ViewModel.INSTANCE;
        if (viewModel != null && viewModel.isEnabled() && viewModel.instantSwap.getValue().booleanValue()) {
            args.set(1, (Object)Float.valueOf(-0.52f));
        }
    }

    @ModifyArgs(method={"method_3218(Lnet/minecraft/class_4587;FLnet/minecraft/class_1306;Lnet/minecraft/class_1799;Lnet/minecraft/class_1657;)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/class_4587;method_46416(FFF)V", ordinal=0))
    private void mio$eatMultiplier(Args args) {
        ViewModel viewModel = ViewModel.INSTANCE;
        if (viewModel != null && viewModel.isEnabled()) {
            args.set(1, (Object)Float.valueOf(((Float)args.get(1)).floatValue() * viewModel.eatMultiplier.getValue().floatValue()));
        }
    }
}

