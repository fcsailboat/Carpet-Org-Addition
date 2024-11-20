package org.carpetorgaddition.network;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.CarpetOrgAddition;

public class PacketFactory {
    public static <T extends CustomPayload> CustomPayload.Id<T> createId(String path) {
        Identifier identifier = Identifier.of(CarpetOrgAddition.MOD_ID, path);
        return new CustomPayload.Id<>(identifier);
    }
}
