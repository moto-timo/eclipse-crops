/*******************************************************************************
 * Copyright (c) 2016 Intel Corporation and others.
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Intel Corporation - CROPS Poky version
 *******************************************************************************/
package org.yocto.crops.poky.core.internal.board;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.yocto.crops.poky.core.internal.Activator;
import org.yocto.crops.poky.core.internal.PokyPreferences;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class PokyManager {

	// Build tool ids
	public static final String BOARD_OPTION_ID = "org.yocto.crops.poky.option.board"; //$NON-NLS-1$
	public static final String PLATFORM_OPTION_ID = "org.yocto.crops.poky.option.platform"; //$NON-NLS-1$
	public static final String PACKAGE_OPTION_ID = "org.yocto.crops.poky.option.package"; //$NON-NLS-1$
	public static final String AVR_TOOLCHAIN_ID = "org.yocto.crops.poky.toolChain.avr"; //$NON-NLS-1$

	public static final String LIBRARIES_URL = "http://crops.github.io/libraries/library_index.json"; //$NON-NLS-1$
	public static final String LIBRARIES_FILE = "library_index.json"; //$NON-NLS-1$

	private static final String LIBRARIES = "libraries"; //$NON-NLS-1$

	// arduinocdt install properties
	private static final String VERSION_KEY = "version"; //$NON-NLS-1$
	private static final String ACCEPTED_KEY = "accepted"; //$NON-NLS-1$
	private static final String VERSION = "2"; //$NON-NLS-1$

	private Properties props;

	private Map<String, PokyPackage> packages;
	private Map<String, PokyLibrary> installedLibraries;

	private Path getVersionFile() {
		return PokyPreferences.getPokyHome().resolve(".version"); //$NON-NLS-1$
	}

	private void init() throws CoreException {
		if (props == null) {
			if (!Files.exists(PokyPreferences.getPokyHome())) {
				try {
					Files.createDirectories(PokyPreferences.getPokyHome());
				} catch (IOException e) {
					throw Activator.coreException(e);
				}
			}

			props = new Properties();
			Path propsFile = getVersionFile();
			if (Files.exists(propsFile)) {
				try (FileReader reader = new FileReader(propsFile.toFile())) {
					props.load(reader);
				} catch (IOException e) {
					throw Activator.coreException(e);
				}
			}

			// See if we need a conversion
			int version = Integer.parseInt(props.getProperty(VERSION_KEY, "1")); //$NON-NLS-1$
			if (version < Integer.parseInt(VERSION)) {
				// Need to move the directories around
				convertPackageDirs();

				props.setProperty(VERSION_KEY, VERSION);
				try (FileWriter writer = new FileWriter(getVersionFile().toFile())) {
					props.store(writer, ""); //$NON-NLS-1$
				} catch (IOException e) {
					throw Activator.coreException(e);
				}
			}
		}
	}

	private void convertPackageDirs() throws CoreException {
		Path packagesDir = PokyPreferences.getPokyHome().resolve("packages"); //$NON-NLS-1$
		if (!Files.isDirectory(packagesDir)) {
			return;
		}

		try {
			Files.list(packagesDir).forEach(path -> {
				try {
					Path hardwarePath = path.resolve("hardware"); //$NON-NLS-1$
					Path badPath = hardwarePath.resolve(path.getFileName());
					Path tmpDir = Files.createTempDirectory(packagesDir, "tbd"); //$NON-NLS-1$
					Path badPath2 = tmpDir.resolve(badPath.getFileName());
					Files.move(badPath, badPath2);
					Files.list(badPath2).forEach(archPath -> {
						try {
							Optional<Path> latest = Files.list(archPath)
									.reduce((path1, path2) -> compareVersions(path1.getFileName().toString(),
											path2.getFileName().toString()) > 0 ? path1 : path2);
							if (latest.isPresent()) {
								Files.move(latest.get(), hardwarePath.resolve(archPath.getFileName()));
							}
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
					recursiveDelete(tmpDir);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (RuntimeException | IOException e) {
			throw Activator.coreException(e);
		}
	}

	public void convertLibrariesDir() throws CoreException {
		Path librariesDir = PokyPreferences.getPokyHome().resolve("libraries"); //$NON-NLS-1$
		if (!Files.isDirectory(librariesDir)) {
			return;
		}

		try {
			Path tmpDir = Files.createTempDirectory("alib"); //$NON-NLS-1$
			Path tmpLibDir = tmpDir.resolve("libraries"); //$NON-NLS-1$
			Files.move(librariesDir, tmpLibDir);
			Files.list(tmpLibDir).forEach(path -> {
				try {
					Optional<Path> latest = Files.list(path)
							.reduce((path1, path2) -> compareVersions(path1.getFileName().toString(),
									path2.getFileName().toString()) > 0 ? path1 : path2);
					if (latest.isPresent()) {
						Files.move(latest.get(), librariesDir.resolve(path.getFileName()));
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			recursiveDelete(tmpDir);
		} catch (RuntimeException | IOException e) {
			throw Activator.coreException(e);
		}

	}

	public boolean licenseAccepted() throws CoreException {
		init();
		return Boolean.getBoolean(props.getProperty(ACCEPTED_KEY, Boolean.FALSE.toString()));
	}

	public void acceptLicense() throws CoreException {
		init();
		props.setProperty(ACCEPTED_KEY, Boolean.TRUE.toString());
		try (FileWriter writer = new FileWriter(getVersionFile().toFile())) {
			props.store(writer, ""); //$NON-NLS-1$
		} catch (IOException e) {
			throw Activator.coreException(e);
		}
	}

	public Collection<PokyPlatform> getInstalledPlatforms() throws CoreException {
		List<PokyPlatform> platforms = new ArrayList<>();
		for (PokyPackage pkg : getPackages()) {
			platforms.addAll(pkg.getInstalledPlatforms());
		}
		return platforms;
	}

	public PokyPlatform getInstalledPlatform(String packageName, String architecture) throws CoreException {
		PokyPackage pkg = getPackage(packageName);
		return pkg != null ? pkg.getInstalledPlatform(architecture) : null;
	}

	public Collection<PokyPlatform> getAvailablePlatforms(IProgressMonitor monitor) throws CoreException {
		List<PokyPlatform> platforms = new ArrayList<>();
		Collection<URL> urls = PokyPreferences.getBoardUrlList();
		SubMonitor sub = SubMonitor.convert(monitor, urls.size() + 1);

		sub.beginTask("Downloading package descriptions", urls.size()); //$NON-NLS-1$
		for (URL url : urls) {
			Path packagePath = PokyPreferences.getPokyHome()
					.resolve(Paths.get(url.getPath()).getFileName());
			try {
				Files.createDirectories(PokyPreferences.getPokyHome());
				try (InputStream in = url.openStream()) {
					Files.copy(in, packagePath, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				throw Activator.coreException(String.format("Error loading %s", url.toString()), e); //$NON-NLS-1$
			}
			sub.worked(1);
		}

		sub.beginTask("Loading available packages", 1); //$NON-NLS-1$
		resetPackages();
		for (PokyPackage pkg : getPackages()) {
			platforms.addAll(pkg.getAvailablePlatforms());
		}
		sub.done();

		return platforms;
	}

	public void installPlatforms(Collection<PokyPlatform> platforms, IProgressMonitor monitor) throws CoreException {
		SubMonitor sub = SubMonitor.convert(monitor, platforms.size());
		for (PokyPlatform platform : platforms) {
			sub.setTaskName(String.format("Installing %s", platform.getName())); //$NON-NLS-1$
			platform.install(sub);
			sub.worked(1);
		}
		sub.done();
	}

	public void uninstallPlatforms(Collection<PokyPlatform> platforms, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, platforms.size());
		for (PokyPlatform platform : platforms) {
			sub.setTaskName(String.format("Uninstalling %s", platform.getName())); //$NON-NLS-1$
			platform.uninstall(sub);
			sub.worked(1);
		}
		sub.done();
	}

	public static List<PokyPlatform> getSortedPlatforms(Collection<PokyPlatform> platforms) {
		List<PokyPlatform> result = new ArrayList<>(platforms);
		Collections.sort(result, (plat1, plat2) -> {
			int c1 = plat1.getPackage().getName().compareToIgnoreCase(plat2.getPackage().getName());
			if (c1 > 0) {
				return 1;
			} else if (c1 < 0) {
				return -1;
			} else {
				return plat1.getArchitecture().compareToIgnoreCase(plat2.getArchitecture());
			}
		});
		return result;
	}

	public static List<PokyLibrary> getSortedLibraries(Collection<PokyLibrary> libraries) {
		List<PokyLibrary> result = new ArrayList<>(libraries);
		Collections.sort(result, (lib1, lib2) -> {
			return lib1.getName().compareToIgnoreCase(lib2.getName());
		});
		return result;
	}

	private void initPackages() throws CoreException {
		if (packages == null) {
			init();
			packages = new HashMap<>();

			try {
				Files.list(PokyPreferences.getPokyHome())
						.filter(path -> path.getFileName().toString().startsWith("package_")) //$NON-NLS-1$
						.forEach(path -> {
							try (Reader reader = new FileReader(path.toFile())) {
								PackageIndex index = new Gson().fromJson(reader, PackageIndex.class);
								for (PokyPackage pkg : index.getPackages()) {
									pkg.init();
									packages.put(pkg.getName(), pkg);
								}
							} catch (IOException e) {
								Activator.log(e);
							}
						});
			} catch (IOException e) {
				throw Activator.coreException(e);
			}
		}
	}

	private Collection<PokyPackage> getPackages() throws CoreException {
		initPackages();
		return packages.values();
	}

	public void resetPackages() {
		packages = null;
	}

	private PokyPackage getPackage(String packageName) throws CoreException {
		if (packageName == null) {
			return null;
		} else {
			initPackages();
			return packages.get(packageName);
		}
	}

	public Collection<PokyBoard> getInstalledBoards() throws CoreException {
		List<PokyBoard> boards = new ArrayList<>();
		for (PokyPlatform platform : getInstalledPlatforms()) {
			boards.addAll(platform.getBoards());
		}
		return boards;
	}

	public PokyBoard getBoard(String packageName, String architecture, String boardId) throws CoreException {
		for (PokyPlatform platform : getInstalledPlatforms()) {
			if (platform.getPackage().getName().equals(packageName)
					&& platform.getArchitecture().equals(architecture)) {
				return platform.getBoard(boardId);
			}
		}

		// For backwards compat, check platform name
		for (PokyPlatform platform : getInstalledPlatforms()) {
			if (platform.getPackage().getName().equals(packageName)
					&& platform.getName().equals(architecture)) {
				return platform.getBoardByName(boardId);
			}
		}

		return null;
	}

	public PokyTool getTool(String packageName, String toolName, String version) {
		PokyPackage pkg = packages.get(packageName);
		return pkg != null ? pkg.getTool(toolName, version) : null;
	}

	public void initInstalledLibraries() throws CoreException {
		init();
		if (installedLibraries == null) {
			installedLibraries = new HashMap<>();

			Path librariesDir = PokyPreferences.getPokyHome().resolve("libraries"); //$NON-NLS-1$
			if (Files.isDirectory(librariesDir)) {
				try {
					Files.find(librariesDir, 2,
							(path, attrs) -> path.getFileName().toString().equals("library.properties")) //$NON-NLS-1$
							.forEach(path -> {
								try {
									PokyLibrary library = new PokyLibrary(path);
									installedLibraries.put(library.getName(), library);
								} catch (CoreException e) {
									throw new RuntimeException(e);
								}
							});
				} catch (IOException e) {
					throw Activator.coreException(e);
				}
			}
		}
	}

	public Collection<PokyLibrary> getInstalledLibraries() throws CoreException {
		initInstalledLibraries();
		return installedLibraries.values();
	}

	public PokyLibrary getInstalledLibrary(String name) throws CoreException {
		initInstalledLibraries();
		return installedLibraries.get(name);
	}

	public Collection<PokyLibrary> getAvailableLibraries(IProgressMonitor monitor) throws CoreException {
		try {
			initInstalledLibraries();
			Map<String, PokyLibrary> libs = new HashMap<>();

			SubMonitor sub = SubMonitor.convert(monitor, "Downloading library index", 2);
			Path librariesPath = PokyPreferences.getPokyHome().resolve(LIBRARIES_FILE);
			URL librariesUrl = new URL(LIBRARIES_URL);
			Files.createDirectories(PokyPreferences.getPokyHome());
			Files.copy(librariesUrl.openStream(), librariesPath, StandardCopyOption.REPLACE_EXISTING);
			sub.worked(1);

			try (Reader reader = new FileReader(librariesPath.toFile())) {
				sub.setTaskName("Calculating available libraries");
				LibraryIndex libraryIndex = new Gson().fromJson(reader, LibraryIndex.class);
				for (PokyLibrary library : libraryIndex.getLibraries()) {
					String libraryName = library.getName();
					if (!installedLibraries.containsKey(libraryName)) {
						PokyLibrary current = libs.get(libraryName);
						if (current == null || compareVersions(library.getVersion(), current.getVersion()) > 0) {
							libs.put(libraryName, library);
						}
					}
				}
			}
			sub.done();
			return libs.values();
		} catch (IOException e) {
			throw Activator.coreException(e);
		}
	}

	public void installLibraries(Collection<PokyLibrary> libraries, IProgressMonitor monitor) throws CoreException {
		SubMonitor sub = SubMonitor.convert(monitor, libraries.size());
		for (PokyLibrary library : libraries) {
			sub.setTaskName(String.format("Installing %s", library.getName())); //$NON-NLS-1$
			library.install(sub);
			try {
				PokyLibrary newLibrary = new PokyLibrary(library.getInstallPath().resolve("library.properties")); //$NON-NLS-1$
				installedLibraries.put(newLibrary.getName(), newLibrary);
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
			sub.worked(1);
		}
		sub.done();
	}

	public void uninstallLibraries(Collection<PokyLibrary> libraries, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor sub = SubMonitor.convert(monitor, libraries.size());
		for (PokyLibrary library : libraries) {
			sub.setTaskName(String.format("Installing %s", library.getName())); //$NON-NLS-1$
			library.uninstall(sub);
			installedLibraries.remove(library.getName());
			sub.worked(1);
		}
		sub.done();
	}

	public Collection<PokyLibrary> getLibraries(IProject project)
			throws CoreException {
		initInstalledLibraries();
		IEclipsePreferences settings = getSettings(project);
		String librarySetting = settings.get(LIBRARIES, "[]"); //$NON-NLS-1$
		JsonArray libArray = new JsonParser().parse(librarySetting).getAsJsonArray();

		List<PokyLibrary> libraries = new ArrayList<>(libArray.size());
		for (JsonElement libElement : libArray) {
			if (libElement.isJsonPrimitive()) {
				String libName = libElement.getAsString();
				PokyLibrary lib = installedLibraries.get(libName);
				if (lib != null) {
					libraries.add(lib);
				}
			} else {
				JsonObject libObj = libElement.getAsJsonObject();
				String packageName = libObj.get("package").getAsString(); //$NON-NLS-1$
				String platformName = libObj.get("platform").getAsString(); //$NON-NLS-1$
				String libName = libObj.get("library").getAsString(); //$NON-NLS-1$
				PokyPackage pkg = getPackage(packageName);
				if (pkg != null) {
					PokyPlatform platform = pkg.getInstalledPlatform(platformName);
					if (platform != null) {
						PokyLibrary lib = platform.getLibrary(libName);
						if (lib != null) {
							libraries.add(lib);
						}
					}
				}
			}
		}
		return libraries;
	}

	public void setLibraries(final IProject project, final Collection<PokyLibrary> libraries) throws CoreException {
		JsonArray elements = new JsonArray();
		for (PokyLibrary library : libraries) {
			PokyPlatform platform = library.getPlatform();
			if (platform != null) {
				JsonObject libObj = new JsonObject();
				libObj.addProperty("package", platform.getPackage().getName()); //$NON-NLS-1$
				libObj.addProperty("platform", platform.getArchitecture()); //$NON-NLS-1$
				libObj.addProperty("library", library.getName()); //$NON-NLS-1$
				elements.add(libObj);
			} else {
				elements.add(new JsonPrimitive(library.getName()));
			}
		}
		IEclipsePreferences settings = getSettings(project);
		settings.put(LIBRARIES, new Gson().toJson(elements));
		try {
			settings.flush();
		} catch (BackingStoreException e) {
			throw Activator.coreException(e);
		}
	}

	private IEclipsePreferences getSettings(IProject project) {
		return new ProjectScope(project).getNode(Activator.getId());
	}

	public static void downloadAndInstall(String url, String archiveFileName, Path installPath,
			IProgressMonitor monitor) throws IOException {
		Exception error = null;
		for (int retries = 3; retries > 0 && !monitor.isCanceled(); --retries) {
			try {
				URL dl = new URL(url);
				Path dlDir = PokyPreferences.getPokyHome().resolve("downloads"); //$NON-NLS-1$
				Files.createDirectories(dlDir);
				Path archivePath = dlDir.resolve(archiveFileName);
				URLConnection conn = dl.openConnection();
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(10000);
				Files.copy(conn.getInputStream(), archivePath, StandardCopyOption.REPLACE_EXISTING);

				boolean isWin = Platform.getOS().equals(Platform.OS_WIN32);

				// extract
				ArchiveInputStream archiveIn = null;
				try {
					String compressor = null;
					String archiver = null;
					if (archiveFileName.endsWith("tar.bz2")) { //$NON-NLS-1$
						compressor = CompressorStreamFactory.BZIP2;
						archiver = ArchiveStreamFactory.TAR;
					} else if (archiveFileName.endsWith(".tar.gz") || archiveFileName.endsWith(".tgz")) { //$NON-NLS-1$ //$NON-NLS-2$
						compressor = CompressorStreamFactory.GZIP;
						archiver = ArchiveStreamFactory.TAR;
					} else if (archiveFileName.endsWith(".tar.xz")) { //$NON-NLS-1$
						compressor = CompressorStreamFactory.XZ;
						archiver = ArchiveStreamFactory.TAR;
					} else if (archiveFileName.endsWith(".zip")) { //$NON-NLS-1$
						archiver = ArchiveStreamFactory.ZIP;
					}

					InputStream in = new BufferedInputStream(new FileInputStream(archivePath.toFile()));
					if (compressor != null) {
						in = new CompressorStreamFactory().createCompressorInputStream(compressor, in);
					}
					archiveIn = new ArchiveStreamFactory().createArchiveInputStream(archiver, in);

					for (ArchiveEntry entry = archiveIn.getNextEntry(); entry != null; entry = archiveIn
							.getNextEntry()) {
						if (entry.isDirectory()) {
							continue;
						}

						// Magic file for git tarballs
						Path path = Paths.get(entry.getName());
						if (path.endsWith("pax_global_header")) { //$NON-NLS-1$
							continue;
						}

						// Strip the first directory of the path
						Path entryPath = installPath.resolve(path.subpath(1, path.getNameCount()));

						Files.createDirectories(entryPath.getParent());

						if (entry instanceof TarArchiveEntry) {
							TarArchiveEntry tarEntry = (TarArchiveEntry) entry;
							if (tarEntry.isLink()) {
								Path linkPath = Paths.get(tarEntry.getLinkName());
								linkPath = installPath.resolve(linkPath.subpath(1, linkPath.getNameCount()));
								Files.deleteIfExists(entryPath);
								Files.createSymbolicLink(entryPath, entryPath.getParent().relativize(linkPath));
							} else if (tarEntry.isSymbolicLink()) {
								Path linkPath = Paths.get(tarEntry.getLinkName());
								Files.deleteIfExists(entryPath);
								Files.createSymbolicLink(entryPath, linkPath);
							} else {
								Files.copy(archiveIn, entryPath, StandardCopyOption.REPLACE_EXISTING);
							}
							if (!isWin && !tarEntry.isSymbolicLink()) {
								int mode = tarEntry.getMode();
								Files.setPosixFilePermissions(entryPath, toPerms(mode));
							}
						} else {
							Files.copy(archiveIn, entryPath, StandardCopyOption.REPLACE_EXISTING);
						}
					}
				} finally {
					if (archiveIn != null) {
						archiveIn.close();
					}
				}
				return;
			} catch (IOException | CompressorException | ArchiveException e) {
				error = e;
				// retry
			}
		}

		// out of retries
		if (error instanceof IOException) {
			throw (IOException) error;
		} else {
			throw new IOException(error);
		}
	}

	public static int compareVersions(String version1, String version2) {
		if (version1 == null) {
			return version2 == null ? 0 : -1;
		}

		if (version2 == null) {
			return 1;
		}

		String[] v1 = version1.split("\\."); //$NON-NLS-1$
		String[] v2 = version2.split("\\."); //$NON-NLS-1$
		for (int i = 0; i < Math.max(v1.length, v2.length); ++i) {
			if (v1.length <= i) {
				return v2.length < i ? 0 : -1;
			}

			if (v2.length <= i) {
				return 1;
			}

			try {
				int vi1 = Integer.parseInt(v1[i]);
				int vi2 = Integer.parseInt(v2[i]);
				if (vi1 < vi2) {
					return -1;
				}

				if (vi1 > vi2) {
					return 1;
				}
			} catch (NumberFormatException e) {
				// not numbers, do string compares
				int c = v1[i].compareTo(v2[i]);
				if (c < 0) {
					return -1;
				}
				if (c > 0) {
					return 1;
				}
			}
		}

		return 0;
	}

	private static Set<PosixFilePermission> toPerms(int mode) {
		Set<PosixFilePermission> perms = new HashSet<>();
		if ((mode & 0400) != 0) {
			perms.add(PosixFilePermission.OWNER_READ);
		}
		if ((mode & 0200) != 0) {
			perms.add(PosixFilePermission.OWNER_WRITE);
		}
		if ((mode & 0100) != 0) {
			perms.add(PosixFilePermission.OWNER_EXECUTE);
		}
		if ((mode & 0040) != 0) {
			perms.add(PosixFilePermission.GROUP_READ);
		}
		if ((mode & 0020) != 0) {
			perms.add(PosixFilePermission.GROUP_WRITE);
		}
		if ((mode & 0010) != 0) {
			perms.add(PosixFilePermission.GROUP_EXECUTE);
		}
		if ((mode & 0004) != 0) {
			perms.add(PosixFilePermission.OTHERS_READ);
		}
		if ((mode & 0002) != 0) {
			perms.add(PosixFilePermission.OTHERS_WRITE);
		}
		if ((mode & 0001) != 0) {
			perms.add(PosixFilePermission.OTHERS_EXECUTE);
		}
		return perms;
	}

	public static void recursiveDelete(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}
}