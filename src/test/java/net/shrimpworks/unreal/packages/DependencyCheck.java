package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import net.shrimpworks.unreal.packages.entities.ImportedPackage;

public class DependencyCheck {

	public static void main(String[] args) throws IOException {
		Path map = Files.createTempFile("test-map-", ".unr");
		try (InputStream is = PackageTest.class.getResourceAsStream("SCR-CityStreet.unr.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, map, StandardCopyOption.REPLACE_EXISTING);
		}

		try (Package pkg = new Package(map)) {
			System.out.println(prettyPrintImports(pkg.imports(), ""));
		}
	}

	private static String prettyPrintImports(Collection<ImportedPackage> packages, String padded) {
		StringBuilder sb = new StringBuilder();
		packages.forEach(i -> {
			String nextPad = String.format("  %s", padded);
			sb.append(String.format("%s%s%n", padded, i.name().name));
			sb.append(prettyPrintImports(i.packages(), nextPad));
			i.objects().forEach(io -> sb.append(String.format("%s%s: %s%n", nextPad, io.name.name, io.type.name)));
		});
		return sb.toString();
	}
}
