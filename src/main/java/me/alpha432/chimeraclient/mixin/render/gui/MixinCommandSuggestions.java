/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.ParseResults
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.suggestion.Suggestions
 *  org.jspecify.annotations.Nullable
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package me.alpha432.chimeraclient.mixin.render.gui;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import me.alpha432.chimeraclient.ChimeraClient;
import me.alpha432.chimeraclient.manager.CommandManager;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientCommandSource;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ChatInputSuggestor.class})
public abstract class MixinCommandSuggestions {
    @Shadow
    private ChatInputSuggestor.SuggestionWindow field_21612;
    @Shadow
    private @Nullable CompletableFuture<Suggestions> field_21611;
    @Shadow
    private @Nullable ParseResults<ClientCommandSource> field_21610;
    @Shadow
    boolean field_21614;
    @Shadow
    @Final
    TextFieldWidget field_21599;

    @Shadow
    protected abstract void method_23937();

    @Inject(method={"method_23934"}, at={@At(value="RETURN")})
    public void updateCommandInfo(CallbackInfo ci, @Local StringReader stringReader) {
        ParseResults parse;
        if (!stringReader.canRead() || !stringReader.getString().startsWith(ChimeraClient.commandManager.getCommandPrefix())) {
            return;
        }
        stringReader.skip();
        CommandDispatcher<CommandManager> dispatcher = ChimeraClient.commandManager.getDispatcher();
        this.field_21610 = parse = dispatcher.parse(stringReader, ChimeraClient.commandManager);
        if (this.field_21612 == null || !this.field_21614) {
            this.field_21611 = dispatcher.getCompletionSuggestions(parse, this.field_21599.getCursor());
            this.field_21611.thenRun(() -> {
                if (this.field_21611.isDone()) {
                    this.method_23937();
                }
            });
        }
    }
}
