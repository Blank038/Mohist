/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.loading;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.minecraftforge.fml.loading.LogMarkers.CORE;

public class LibraryFinder {
    private static final Logger LOGGER = LogManager.getLogger();
    private static Path libsPath;
    static Path findLibsPath() {
        if (libsPath == null) {
            final Path asm = findJarPathFor("org/objectweb/asm/Opcodes.class", "asm");
            // go up SIX parents to find the libs dir
            final Path libs = asm.getParent().getParent().getParent().getParent().getParent().getParent();
            LOGGER.debug(CORE, "Found probable library path {}", libs);
            libsPath = libs;
        }
        return libsPath;
    }

    public static Path findJarPathFor(final String className, final String jarName) {
        final URL resource = LibraryFinder.class.getClassLoader().getResource(className);
        return findJarPathFor(className, jarName, resource);
    }

    public static Path findJarPathFor(final String resourceName, final String jarName, final URL resource) {
        try {
            Path path;
            final URI uri = resource.toURI();
            if (uri.getScheme().equals("jar") && uri.getRawSchemeSpecificPart().contains("!/")) {
                int lastExcl = uri.getRawSchemeSpecificPart().lastIndexOf("!/");
                path = Paths.get(new URI(uri.getRawSchemeSpecificPart().substring(0, lastExcl)));
            } else {
                path = Paths.get(new URI("file://"+uri.getRawSchemeSpecificPart().substring(0, uri.getRawSchemeSpecificPart().length()-resourceName.length())));
            }
            LOGGER.debug(CORE, "Found JAR {} at path {}", jarName, path.toString());
            return path;
        } catch (NullPointerException | URISyntaxException e) {
            LOGGER.fatal(CORE, "Failed to find JAR for class {} - {}", resourceName, jarName);
            throw new RuntimeException("Unable to locate "+resourceName+" - "+jarName, e);
        }
    }

    static Path getForgeLibraryPath(final String mcVersion, final String forgeVersion, final String forgeGroup) {
        Path forgePath = findLibsPath().resolve(MavenCoordinateResolver.get(forgeGroup, "mohist", "", "universal", mcVersion+"-"+forgeVersion));
        LOGGER.debug(CORE, "Found forge path {} is {}", forgePath, pathStatus(forgePath));
        return forgePath;
    }

    static String pathStatus(final Path path) {
        return Files.exists(path) ? "present" : "missing";
    }

    static Path[] getMCPaths(final String mcVersion, final String mcpVersion, final String forgeVersion, final String forgeGroup, final String type) {
        Path srgMcPath = findLibsPath().resolve(MavenCoordinateResolver.get("net.minecraft", type, "", "srg", mcVersion+"-"+mcpVersion));
        Path mcExtrasPath = findLibsPath().resolve(MavenCoordinateResolver.get("net.minecraft", type, "", "extra", mcVersion+"-"+mcpVersion));
        Path patchedBinariesPath = findLibsPath().resolve(MavenCoordinateResolver.get(forgeGroup, "mohist", "", type, mcVersion+"-"+forgeVersion));
        LOGGER.debug(CORE,"SRG MC at {} is {}", srgMcPath.toString(), pathStatus(srgMcPath));
        LOGGER.debug(CORE,"MC Extras at {} is {}", mcExtrasPath.toString(), pathStatus(mcExtrasPath));
        LOGGER.debug(CORE,"Forge patches at {} is {}", patchedBinariesPath.toString(), pathStatus(patchedBinariesPath));
        return new Path[] { patchedBinariesPath, mcExtrasPath, srgMcPath };
    }
}
