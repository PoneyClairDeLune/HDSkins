package com.minelittlepony.hdskins.client;

import java.util.*;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.ducks.ClientPlayerInfo;
import com.minelittlepony.hdskins.mixin.client.MixinClientPlayer;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.util.Identifier;

public class PlayerSkins {
    @Nullable
    public static PlayerSkins of(AbstractClientPlayerEntity player) {
        ClientPlayerInfo info = ((ClientPlayerInfo)((MixinClientPlayer)player).getBackingClientData());
        if (info == null) {
            return null;
        }
        return info.getSkins();
    }

    private final ClientPlayerInfo playerInfo;

    private final Set<Identifier> providedSkinTypes = new HashSet<>();

    private final Map<SkinType, Identifier> customTextures = new HashMap<>();

    private final Map<SkinType, MinecraftProfileTexture> customProfiles = new HashMap<>();

    private final Map<SkinType, MinecraftProfileTexture> vanillaProfiles = new HashMap<>();

    public PlayerSkins(ClientPlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }

    public Set<Identifier> getProvidedSkinTypes() {
        return providedSkinTypes;
    }

    @Nullable
    public Identifier getSkin(SkinType type) {
        return HDSkins.getInstance().getResourceManager()
                .getCustomPlayerTexture(playerInfo.getGameProfile(), type)
                .orElseGet(() -> Optional.ofNullable(customTextures.get(type))
                .orElseGet(() -> type.getEnum().map(playerInfo.getVanillaTextures()::get)
                .orElse(null)));
    }

    @Nullable
    public String getModel() {
        return HDSkins.getInstance().getResourceManager()
                .getCustomPlayerModel(playerInfo.getGameProfile())
                .orElseGet(() -> getModelFrom(customProfiles)
                .orElseGet(() -> getModelFrom(vanillaProfiles)
                .orElse(null)));
    }

    public void load(PlayerSkinProvider provider, GameProfile profile, boolean requireSecure) {
        HDSkins.getInstance().getProfileRepository().fetchSkins(profile, this::onCustomTextureLoaded);

        provider.loadSkin(profile, this::onVanillaTextureLoaded, requireSecure);
    }

    private void onCustomTextureLoaded(SkinType type, Identifier location, MinecraftProfileTexture profileTexture) {
        customTextures.put(type, location);
        customProfiles.put(type, profileTexture);
        providedSkinTypes.add(type.getId());
    }

    private void onVanillaTextureLoaded(Type type, Identifier location, MinecraftProfileTexture profileTexture) {
        playerInfo.getVanillaTextures().put(type, location);
        vanillaProfiles.put(SkinType.forVanilla(type), profileTexture);
        providedSkinTypes.add(SkinType.forVanilla(type).getId());
    }

    private Optional<String> getModelFrom(Map<SkinType, MinecraftProfileTexture> texture) {
        return Optional.ofNullable(texture.get(SkinType.SKIN))
                .map(t -> VanillaModels.of(t.getMetadata("model")));
    }
}
