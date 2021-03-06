/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.hosted.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.graalvm.compiler.options.Option;
import org.graalvm.compiler.options.OptionType;

import com.oracle.svm.core.option.HostedOptionKey;
import com.oracle.svm.core.option.OptionUtils;
import com.oracle.svm.hosted.ImageClassLoader;

/**
 * Gathers configuration files from specified directories without having to provide each
 * configuration file individually.
 */
public final class ConfigurationDirectories {
    public static final class FileNames {
        private static final String SUFFIX = "-config.json";

        public static final String DYNAMIC_PROXY_NAME = "proxy" + SUFFIX;
        public static final String RESOURCES_NAME = "resource" + SUFFIX;
        public static final String JNI_NAME = "jni" + SUFFIX;
        public static final String REFLECTION_NAME = "reflect" + SUFFIX;
    }

    public static final class Options {
        @Option(help = "Directories directly containing configuration files for dynamic features at runtime.", type = OptionType.User)//
        static final HostedOptionKey<String[]> ConfigurationFileDirectories = new HostedOptionKey<>(null);

        @Option(help = "Resource path above configuration resources for dynamic features at runtime.", type = OptionType.User)//
        public static final HostedOptionKey<String[]> ConfigurationResourceRoots = new HostedOptionKey<>(null);
    }

    static List<String> findConfigurationFiles(String fileName) {
        List<String> files = new ArrayList<>();
        for (String directory : OptionUtils.flatten(",", Options.ConfigurationFileDirectories.getValue())) {
            Path path = Paths.get(directory, fileName);
            if (Files.exists(path)) {
                files.add(path.toString());
            }
        }
        return files;
    }

    static List<String> findConfigurationResources(String fileName, ImageClassLoader classLoader) {
        List<String> resources = new ArrayList<>();
        for (String root : OptionUtils.flatten(",", Options.ConfigurationResourceRoots.getValue())) {
            /*
             * Resource path handling is cumbersome: we want users to be able to pass "/" or "." for
             * the classpath root, but only relative paths without "." are permitted, so we strip
             * these first. Redundant separators also do not work, so we change "root//fileName" to
             * "root/fileName".
             */
            final String separator = "/"; // always for resources (not platform-dependent)
            String relativeRoot = Stream.of(root.split(separator)).filter(part -> !part.isEmpty() && !part.equals(".")).collect(Collectors.joining(separator));
            String relativePath = relativeRoot.isEmpty() ? fileName : (relativeRoot + '/' + fileName);
            if (classLoader.findResourceByName(relativePath) != null) {
                resources.add(relativePath);
            }
        }
        return resources;
    }
}
