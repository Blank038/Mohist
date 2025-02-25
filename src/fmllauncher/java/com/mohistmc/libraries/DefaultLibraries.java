/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2022.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.libraries;

import com.mohistmc.config.MohistConfigUtil;
import com.mohistmc.network.download.DownloadSource;
import com.mohistmc.network.download.UpdateUtils;
import com.mohistmc.util.JarLoader;
import com.mohistmc.util.JarTool;
import com.mohistmc.util.MD5Util;
import com.mohistmc.util.i18n.i18n;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultLibraries {
	public static HashMap<String, String> fail = new HashMap<>();
	public static final AtomicLong allSize = new AtomicLong(); // global

	public static void run() {
		System.out.println(i18n.get("libraries.checking.start"));
		System.out.println(i18n.get("libraries.downloadsource", DownloadSource.get().name()));
		String url = DownloadSource.get().getUrl();
		LinkedHashMap<File, String> libs = getDefaultLibs();
		AtomicLong currentSize = new AtomicLong();
		Set<File> defaultLibs = new LinkedHashSet<>();
		for (File lib : getDefaultLibs().keySet()) {
			if (lib.exists() && MohistConfigUtil.getString(MohistConfigUtil.mohistyml, "libraries_black_list:", "xxxxx").contains(lib.getName())) {
				continue;
			}
			if (lib.exists() && MD5Util.getMd5(lib).equals(libs.get(lib))) {
				currentSize.addAndGet(lib.length());
				continue;
			}
			defaultLibs.add(lib);
		}
		for (File lib : defaultLibs) {
			lib.getParentFile().mkdirs();

			String u = url + "libraries/" + lib.getAbsolutePath().replaceAll("\\\\", "/").split("/libraries/")[1];
			System.out.println(i18n.get("libraries.global.percentage", Math.min(Math.round((float) (currentSize.get() * 100) / allSize.get()), 100) + "%")); //Global percentage
			try {
				UpdateUtils.downloadFile(u, lib, libs.get(lib));
				if (lib.getName().endsWith(".jar") && !lib.getName().contains("asm-tree-6.1.1.jar"))
					new JarLoader().loadJar(lib);
				currentSize.addAndGet(lib.length());
				fail.remove(u.replace(url, ""));
			} catch (Exception e) {
				if (e.getMessage() != null && !e.getMessage().equals("md5")) {
					System.out.println(i18n.get("file.download.nook", u));
					lib.delete();
				}
				fail.put(u.replace(url, ""), lib.getAbsolutePath());
			}
		}
		/*FINISHED | RECHECK IF A FILE FAILED*/
		if (!fail.isEmpty()) {
			run();
		} else System.out.println(i18n.get("libraries.check.end"));
	}

	public static LinkedHashMap<File, String> getDefaultLibs() {
		LinkedHashMap<File, String> temp = new LinkedHashMap<>();
		try {
			BufferedReader b = new BufferedReader(new InputStreamReader(DefaultLibraries.class.getClassLoader().getResourceAsStream("libraries.txt")));
			String str;
			while (true) {
				if (!((str = b.readLine()) != null)) break;
				String[] s = str.split("\\|");
				temp.put(new File(JarTool.getJarDir() + "/" + s[0]), s[1]);
				allSize.addAndGet(Long.parseLong(s[2]));
			}
			b.close();
			return temp;
		} catch (IOException e) {
			return temp;
		}
	}
}
