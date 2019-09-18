package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.Import;

public class DependencyCheck {

	public static void main(String[] args) throws IOException {
		Path map = Files.createTempFile("test-map-", ".unr");
		try (InputStream is = PackageTest.class.getResourceAsStream("SCR-CityStreet.unr.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, map, StandardCopyOption.REPLACE_EXISTING);
		}

//		Path map = Paths.get("/home/shrimp/tmp/UT/System/Engine.u");
		try (Package pkg = new Package(map)) {
			System.out.println(prettyPrintImports(pkg.packageImports(), ""));
			System.out.println(prettyPrintExports(pkg.rootExports(), ""));
		}
	}

	private static String prettyPrintExports(Collection<Export> exports, String padded) {
		StringBuilder sb = new StringBuilder();
		exports.stream().sorted(Comparator.comparing(Export::name)).forEach(e -> {
			String childPad = String.format("  %s", padded);
			sb.append(String.format("%s%s: %s%n", padded, e.name().name, e.classIndex.get().name().name));
			e.children().stream().sorted(Comparator.comparing(Export::name)).forEach(child -> {
				sb.append(String.format("%s%s: %s%n", childPad, child.name.name, child.classIndex.get().name().name));
				Set<Export> subChildren = child.children();
				if (!subChildren.isEmpty()) {
					sb.append(prettyPrintExports(subChildren, String.format("  %s", childPad)));
				}
			});
		});
		return sb.toString();
	}

	private static String prettyPrintImports(Collection<Import> imports, String padded) {
		StringBuilder sb = new StringBuilder();
		imports.stream().sorted(Comparator.comparing(Import::name)).forEach(i -> {
			String childPad = String.format("  %s", padded);
			sb.append(String.format("%s%s: %s%n", padded, i.name().name, i.className.name));
			i.children().stream().sorted(Comparator.comparing(Import::name)).forEach(child -> {
				sb.append(String.format("%s%s: %s%n", childPad, child.name.name, child.className.name));
				Set<Import> subChildren = child.children();
				if (!subChildren.isEmpty()) {
					sb.append(prettyPrintImports(subChildren, String.format("  %s", childPad)));
				}
			});
		});
		return sb.toString();
	}
}
