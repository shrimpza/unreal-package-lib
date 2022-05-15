package net.shrimpworks.unreal.packages;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Texture2D;
import net.shrimpworks.unreal.packages.entities.properties.Property;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PackageUE3Test extends PackageTestUtils {

	private static final String UT3_MAP_URL = "https://code.shrimpworks.za.net/artefacts/net/shrimpworks/unreal-package-lib/resources/DM-MCC-Morbias.ut3.gz";

	private static Path ut3Map;

	@BeforeAll
	public static void setup() throws IOException, InterruptedException {
		ut3Map = fetchAndCache(UT3_MAP_URL, Paths.get(System.getProperty("java.io.tmpdir", "/tmp")).resolve("DM-MMC-Morbias.ut3"));
	}

	@Test
	public void exportTexture() throws IOException {
		try (PackageReader reader = new PackageReader(ut3Map, false);
			 Package pkg = new Package(reader)) {

			try {
				ExportedObject o = pkg.objectByName(new Name("MorbiasScreenshot"));
				System.out.printf("%s :: %s%n", o.classIndex.get().name().name, o.name.name);
				Object obj = o.object();
				for (Property property : obj.properties) {
					System.out.println(property.getClass().getSimpleName() + ": " + property);
				}
				assertTrue(obj instanceof Texture2D);

				Path tmpScreenshot = Files.createTempFile("test-ss-", ".png");

				Texture2D.MipMap[] mipMaps = ((Texture2D)obj).mipMaps();
				BufferedImage bufferedImage = mipMaps[0].get();
				ImageIO.write(bufferedImage, "png", tmpScreenshot.toFile());

				// attempt to load the saved screenshot, should work if it was written correctly
				assertNotNull(ImageIO.read(tmpScreenshot.toFile()));

			} finally {
				System.out.println(reader.stats);
			}
		}
	}

	@Test
	public void iterateExportsProperties() throws IOException {
		try (PackageReader reader = new PackageReader(ut3Map, false);
			 Package pkg = new Package(reader)) {
			try {
				Arrays.stream(pkg.objects)
					  .filter(Objects::nonNull)
					  .sorted(Comparator.comparing(e -> e.name.name))
					  .forEach(e -> {
						  try {
							  System.out.printf("%s :: %s%n", e.classIndex.get().name().name, e.name.name);
							  for (Property property : e.object().properties) {
								  System.out.printf("  - %s :: %s%n", property.getClass().getSimpleName(), property.name.name);
							  }
						  } catch (Throwable ex) {
							  System.out.println("   xxx Failed to read properties: " + ex.getMessage());
						  }
						  System.out.println("----");
					  });
			} finally {
				System.out.println(reader.stats);
			}
		}
	}

	@Test
	public void readUT3Package() throws IOException {
		try (PackageReader reader = new PackageReader(ut3Map, false);
			 Package pkg = new Package(reader)) {
			assertNotNull(pkg.sha1Hash());

			Collection<Export> ex = pkg.rootExports();
			assertFalse(ex.isEmpty());

			Collection<Import> im = pkg.packageImports();
			assertFalse(im.isEmpty());
		}
	}
}
