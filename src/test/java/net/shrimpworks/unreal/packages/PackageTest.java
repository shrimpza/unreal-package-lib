package net.shrimpworks.unreal.packages;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.ExportedGroup;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.ImportedPackage;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.objects.Model;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Polys;
import net.shrimpworks.unreal.packages.entities.objects.Texture;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PackageTest {

	private static Path unrMap;
	private static Path ut2Map;

	@BeforeAll
	public static void setup() throws IOException {
		// unpack our test map to a temporary location
		unrMap = Files.createTempFile("test-map-", ".unr");
		try (InputStream is = PackageTest.class.getResourceAsStream("SCR-CityStreet.unr.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, unrMap, StandardCopyOption.REPLACE_EXISTING);
		}

		ut2Map = Files.createTempFile("test-map-", ".ut2");
		try (InputStream is = PackageTest.class.getResourceAsStream("UWRM-Torpedo.ut2.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, ut2Map, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@AfterAll
	public static void teardown() throws IOException {
		Files.deleteIfExists(unrMap);
		Files.deleteIfExists(ut2Map);
		unrMap = null;
		ut2Map = null;
	}

	@Test
	public void readGeometryTypes() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			pkg.objectsByClassName("Model").forEach(o -> {
				assertTrue(o.object() instanceof Model);
			});

			pkg.objectsByClassName("Polys").forEach(o -> {
				assertTrue(o.object() instanceof Polys);
			});
		}
	}

	@Test
	public void readLevelInfo() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			assertNotNull(pkg.sha1Hash());

			// read level info (also in LevelSummary, but missing Screenshot)
			ExportedObject levelInfo = pkg.objectsByClassName("LevelInfo").iterator().next();
			Object level = levelInfo.object();

			// read some basic level info
			assertEquals("Kenneth \"Shrimp\" Watson", ((StringProperty)level.property("Author")).value);
			assertEquals("City Street", ((StringProperty)level.property("Title")).value);

			// get the screenshot, and then save it to file
			Property shotProp = level.property("Screenshot");
			assertTrue(shotProp instanceof ObjectProperty);

			ExportedObject screenshot = pkg.objectByRef(((ObjectProperty)shotProp).value);
			Object object = screenshot.object();

			assertTrue(object instanceof Texture);

			Path tmpScreenshot = Files.createTempFile("test-ss-", ".png");

			try {
				Texture.MipMap[] mipMaps = ((Texture)object).mipMaps();
				BufferedImage bufferedImage = mipMaps[0].get();
				ImageIO.write(bufferedImage, "png", tmpScreenshot.toFile());

				// attempt to load the saved screenshot, should work if it was written correctly
				assertNotNull(ImageIO.read(tmpScreenshot.toFile()));
			} finally {
				Files.deleteIfExists(tmpScreenshot);
			}

		}
	}

	@Test
	public void readUE2Level() throws IOException {
		try (Package pkg = new Package(ut2Map)) {
			assertNotNull(pkg.sha1Hash());
			ExportedObject screenshot = pkg.objectByName(new Name("Shot00052"));

			Object object = screenshot.object();

			assertTrue(object instanceof Texture);

			Path tmpScreenshot = Files.createTempFile("test-ss-", ".png");

			try {
				Texture.MipMap[] mipMaps = ((Texture)object).mipMaps();
				BufferedImage bufferedImage = mipMaps[0].get();
				ImageIO.write(bufferedImage, "png", tmpScreenshot.toFile());

				// attempt to load the saved screenshot, should work if it was written correctly
				assertNotNull(ImageIO.read(tmpScreenshot.toFile()));
			} finally {
				Files.deleteIfExists(tmpScreenshot);
			}
		}
	}

	@Test
	public void readImports() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			Collection<ImportedPackage> imports = pkg.imports();
			ImportedPackage.ImportedObject levelSummary = imports.stream()
																 .filter(p -> p.name().name.equals("Engine"))
																 .flatMap(p -> p.objects().stream())
																 .filter(o -> o.name.name.equals("LevelSummary") &&
																			  o.type.name.equals("Class"))
																 .findFirst().get();
			assertNotNull(levelSummary);
		}
	}

	@Test
	public void readExports() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			Collection<ExportedGroup> exports = pkg.exports();
			Export levelInfo = exports.stream()
									  .filter(g -> g.name().equals(Name.NONE))
									  .flatMap(g -> g.objects().stream())
									  .filter(e -> e.name.name.startsWith("LevelInfo")).findFirst()
									  .get();
			assertNotNull(levelInfo);
		}
	}
}
