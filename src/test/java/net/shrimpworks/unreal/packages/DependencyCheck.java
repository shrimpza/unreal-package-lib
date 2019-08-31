package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import net.shrimpworks.unreal.packages.entities.ExportedGroup;
import net.shrimpworks.unreal.packages.entities.ImportedPackage;

public class DependencyCheck {

	public static void main(String[] args) throws IOException {
//		Path map = Files.createTempFile("test-map-", ".unr");
//		try (InputStream is = PackageTest.class.getResourceAsStream("SCR-CityStreet.unr.gz");
//			 GZIPInputStream gis = new GZIPInputStream(is)) {
//			Files.copy(gis, map, StandardCopyOption.REPLACE_EXISTING);
//		}

		Path map = Paths.get("/home/shrimp/tmp/UPB-E3L6D.unr");
		try (Package pkg = new Package(map)) {
//			System.out.println(prettyPrintImports(pkg.imports(), ""));
			System.out.println(prettyPrintExports(pkg.exports(), ""));
		}
	}

	private static String prettyPrintImports(Collection<ImportedPackage> imports, String padded) {
		StringBuilder sb = new StringBuilder();
		imports.forEach(i -> {
			String nextPad = String.format("  %s", padded);
			sb.append(String.format("%s%s%n", padded, i.name().name));
			sb.append(prettyPrintImports(i.packages(), nextPad));
			i.objects().forEach(io -> sb.append(String.format("%s%s: %s%n", nextPad, io.name.name, io.type.name)));
		});
		return sb.toString();
	}

	private static String prettyPrintExports(Collection<ExportedGroup> exports, String padded) {
		StringBuilder sb = new StringBuilder();
		exports.forEach(e -> {
			String nextPad = String.format("  %s", padded);
			sb.append(String.format("%s%s%n", padded, e.name().name));
			sb.append(prettyPrintExports(e.packages(), nextPad));
			e.objects().forEach(io -> sb.append(String.format("%s%s: %s%n", nextPad, io.name.name, io.classIndex.get().name().name)));
		});
		return sb.toString();
	}
}
