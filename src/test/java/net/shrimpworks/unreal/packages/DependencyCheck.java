package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.ImportedPackage;

public class DependencyCheck {

	public static void main(String[] args) throws IOException {
//		Path map = Paths.get("/home/shrimp/tmp/DOM-300kCompass.unr");
//		Path map = Paths.get("/home/shrimp/tmp/DM-Dust.unr");
//		Path map = Paths.get("/home/shrimp/tmp/DM-Mortuary.unr");
		Path map = Paths.get("/home/shrimp/tmp/TO-Flight423.unr");
		try (Package pkg = new Package(map)) {
			for (Import anImport : pkg.imports) {
//				if (!anImport.className.name.equals("Package")) continue;
				System.out.printf("%s.%s: %s.%s%n",
								  anImport.classPackage.name, anImport.className.name,
								  anImport.packageName.get().name().name, anImport.name.name);
			}

			pkg.imports();
//			System.out.println(prettyPrintImports(pkg.imports(), ""));
		}
	}

	private static String prettyPrintImports(Collection<ImportedPackage> packages, String padded) {
		StringBuilder sb = new StringBuilder();
		packages.forEach(i -> {
			String nextPad = String.format("  %s", padded);
			sb.append(String.format("%s%s%n", padded, i.name.name));
			sb.append(prettyPrintImports(i.packages, nextPad));
			i.objects.forEach(io -> sb.append(String.format("%s%s: %s%n", nextPad, io.type, io.name)));
		});
		return sb.toString();
	}
}
