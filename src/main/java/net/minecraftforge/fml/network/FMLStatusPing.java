/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.network;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static net.minecraftforge.fml.network.FMLNetworkConstants.NETWORK;

/**
 * {
 *      "fmlNetworkVersion" : FMLNETVERSION,
 *      "channels": [
 *          {
 *              "res": "fml:handshake",
 *              "version": "1.2.3.4",
 *              "required": true
 *          }
 *     ],
 *     "mods": [
 *          {
 *              "modid": "modid",
 *              "modmarker": "<somestring>"
 *          }
 *     ]
 * }
 *
 */
public class FMLStatusPing {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CHANNEL_TRUNCATE_LIMIT = 150;
    private static final int MOD_TRUNCATE_LIMIT = 150;
    private static volatile boolean warnedAboutTruncation = false;

    private transient Map<ResourceLocation, Pair<String, Boolean>> channels;
    private transient Map<String, String> mods;
    private transient int fmlNetworkVer;
    private transient boolean truncated;

    public FMLStatusPing() {
        this.channels = NetworkRegistry.buildChannelVersionsForListPing();
        this.mods = new HashMap<>();
        ModList.get().forEachModContainer((modid, mc) ->
                    mods.put(modid, mc.getCustomExtension(ExtensionPoint.DISPLAYTEST).
                            map(Pair::getLeft).map(Supplier::get).orElse(FMLNetworkConstants.IGNORESERVERONLY)));
        this.fmlNetworkVer = FMLNetworkConstants.FMLNETVERSION;
        this.truncated = false;
    }

    private FMLStatusPing(Map<ResourceLocation, Pair<String, Boolean>> deserialized, Map<String,String> modMarkers, int fmlNetVer, boolean truncated) {
        this.channels = ImmutableMap.copyOf(deserialized);
        this.mods = modMarkers;
        this.fmlNetworkVer = fmlNetVer;
        this.truncated = truncated;
    }

    public static class Serializer {
        public static FMLStatusPing deserialize(JsonObject forgeData, JsonDeserializationContext ctx) {
            try {
                final Map<ResourceLocation, Pair<String, Boolean>> channels = StreamSupport.stream(JSONUtils.getAsJsonArray(forgeData, "channels").spliterator(), false).
                        map(JsonElement::getAsJsonObject).
                        collect(Collectors.toMap(jo -> new ResourceLocation(JSONUtils.getAsString(jo, "res")),
                                jo -> Pair.of(JSONUtils.getAsString(jo, "version"), JSONUtils.getAsBoolean(jo, "required")))
                        );

                final Map<String, String> mods = StreamSupport.stream(JSONUtils.getAsJsonArray(forgeData, "mods").spliterator(), false).
                        map(JsonElement::getAsJsonObject).
                        collect(Collectors.toMap(jo -> JSONUtils.getAsString(jo, "modId"), jo->JSONUtils.getAsString(jo, "modmarker")));

                final int remoteFMLVersion = JSONUtils.getAsInt(forgeData, "fmlNetworkVersion");
                final boolean truncated = JSONUtils.getAsBoolean(forgeData, "truncated", false);
                return new FMLStatusPing(channels, mods, remoteFMLVersion, truncated);
            } catch (JsonSyntaxException e) {
                LOGGER.debug(NETWORK, "Encountered an error parsing status ping data", e);
                return null;
            }
        }

        public static JsonObject serialize(FMLStatusPing forgeData, JsonSerializationContext ctx) {
            JsonObject obj = new JsonObject();
            JsonArray channels = new JsonArray();
            boolean truncated = forgeData.channels.size() > CHANNEL_TRUNCATE_LIMIT || forgeData.mods.size() > MOD_TRUNCATE_LIMIT;
            if (truncated && !warnedAboutTruncation)
            {
                warnedAboutTruncation = true;
                LOGGER.warn("Heuristically truncating mod and/or network channel list in server status ping packet. Compatibility report " +
                        "in the multiplayer screen may be inaccurate.");
            }

            forgeData.channels.entrySet().stream().limit(CHANNEL_TRUNCATE_LIMIT).forEach(entry -> {
                ResourceLocation namespace = entry.getKey();
                Pair<String, Boolean> version = entry.getValue();
                JsonObject mi = new JsonObject();
                mi.addProperty("res", namespace.toString());
                mi.addProperty("version", version.getLeft());
                mi.addProperty("required", version.getRight());
                channels.add(mi);
            });

            obj.add("channels", channels);

            JsonArray modTestValues = new JsonArray();
            forgeData.mods.entrySet().stream().limit(MOD_TRUNCATE_LIMIT).forEach(entry -> {
                String modId = entry.getKey();
                String value = entry.getValue();
                JsonObject mi = new JsonObject();
                mi.addProperty("modId", modId);
                mi.addProperty("modmarker", value);
                modTestValues.add(mi);
            });
            obj.add("mods", modTestValues);
            obj.addProperty("fmlNetworkVersion", forgeData.fmlNetworkVer);
            obj.addProperty("truncated", truncated);
            return obj;
        }
    }

    public Map<ResourceLocation, Pair<String, Boolean>> getRemoteChannels() {
        return this.channels;
    }

    public Map<String,String> getRemoteModData() {
        return mods;
    }

    public int getFMLNetworkVersion() {
        return fmlNetworkVer;
    }

    public boolean isTruncated()
    {
        return truncated;
    }

}
