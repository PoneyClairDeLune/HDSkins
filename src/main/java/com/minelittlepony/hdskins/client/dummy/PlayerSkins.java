package com.minelittlepony.hdskins.client.dummy;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.dummy.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.Closeable;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

public abstract class PlayerSkins<T extends PlayerSkins.PlayerSkin> implements Closeable {
    public static final int POSE_STANDING = 0;
    public static final int POSE_SLEEPING = 1;
    public static final int POSE_RIDING = 2;
    public static final int POSE_SWIMMING = 3;
    public static final int POSE_RIPTIDE = 4;
    public static final PlayerSkins<PlayerSkin> EMPTY = new PlayerSkins<>(Posture.NULL) {
        @Override
        public PlayerSkin createTexture(SkinType type, Supplier<Identifier> blank) {
            return new PlayerSkins.PlayerSkin() {
                @Override
                public Identifier getId() {
                    return blank.get();
                }

                @Override
                public void close() { }

                @Override
                public boolean isReady() {
                    return false;
                }
            };
        }

        @Override
        protected boolean isProvided(SkinType type) {
            return false;
        }
    };

    protected final Map<SkinType, T> textures = new HashMap<>();

    @Nullable
    private Set<Identifier> providedSkinTypes;
    private long setAt;

    protected final Posture posture;

    protected PlayerSkins(Posture posture) {
        this.posture = posture;
    }

    protected abstract T createTexture(SkinType type, Supplier<Identifier> blank);

    public Posture getPosture() {
        return posture;
    }

    public boolean usesThinSkin() {
        return false;
    }

    public boolean isSetupComplete() {
        return textures.size() > 0
            && get(getPosture().getActiveSkinType()).isReady();
    }

    public T get(SkinType type) {
        return textures.computeIfAbsent(type, t -> createTexture(t, () -> posture.getDefaultSkin(t, usesThinSkin())));
    }

    public Set<Identifier> getProvidedSkinTypes() {
        long now = System.currentTimeMillis();
        if (providedSkinTypes == null || setAt < now) {
            setAt = now + 500;
            providedSkinTypes = SkinType.REGISTRY.stream().filter(this::isProvided).map(SkinType::getId).collect(Collectors.toSet());
        }
        return providedSkinTypes;
    }

    protected abstract boolean isProvided(SkinType type);

    @Override
    public void close() {
        textures.values().forEach(PlayerSkin::close);
        textures.clear();
        providedSkinTypes = null;
    }

    public interface PlayerSkin extends Closeable {
        Identifier getId();

        @Override
        void close();

        boolean isReady();
    }

    public interface SkinFactory<T extends PlayerSkins.PlayerSkin, K extends PlayerSkins<? extends T>> {
        T create(SkinType type, Supplier<Identifier> blank, K parent);
    }

    public interface Posture {
        Posture NULL = new Posture() {
            @Override
            public GameProfile getProfile() {
                return MinecraftClient.getInstance().getSession().getProfile();
            }

            @Override
            public int getPose() {
                return POSE_STANDING;
            }

            @Override
            public SkinType getActiveSkinType() {
                return SkinType.SKIN;
            }

            @Override
            public Identifier getDefaultSkin(SkinType type, boolean slim) {
                return PlayerPreview.getDefaultTexture(type, slim);
            }

            @Override
            public EquipmentSet getEquipment() {
                return HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();
            }
        };

        GameProfile getProfile();

        EquipmentSet getEquipment();

        int getPose();

        SkinType getActiveSkinType();

        Identifier getDefaultSkin(SkinType type, boolean slim);
    }
}
