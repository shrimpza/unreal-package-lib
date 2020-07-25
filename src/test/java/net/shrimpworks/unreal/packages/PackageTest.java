package net.shrimpworks.unreal.packages;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.objects.Model;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Polys;
import net.shrimpworks.unreal.packages.entities.objects.Texture;
import net.shrimpworks.unreal.packages.entities.properties.ArrayProperty;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PackageTest {

	private static final String UT3_MAP_URL = "https://code.shrimpworks.za.net/artefacts/net/shrimpworks/unreal-package-lib/resources/DM-MCC-Morbias.ut3.gz";

	private static Path unrMap;
	private static Path ut2Map;
	private static Path ut3Map;

	@BeforeAll
	public static void setup() throws IOException, InterruptedException {
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

		ut3Map = fetchAndCache(UT3_MAP_URL, Paths.get(System.getProperty("java.io.tmpdir", "/tmp")).resolve("DM-MMC-Morbias.ut3"));
	}

	private static Path fetchAndCache(String url, Path output) throws IOException, InterruptedException {
		if (Files.exists(output)) return output;

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
		System.out.printf("Downloading %s to %s%n", url, output.toAbsolutePath());
		HttpResponse<InputStream> send = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
		try (InputStream is = send.body();
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, output, StandardCopyOption.REPLACE_EXISTING);
			return output;
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
	public void arrayProperties() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			ExportedObject pathNode = pkg.objectsByClassName("DynamicAmbientSound").stream().findFirst().get();
			Property sounds = pkg.object(pathNode).property("Sounds");
			assertTrue(sounds instanceof ArrayProperty);
			assertFalse(((ArrayProperty)sounds).values.isEmpty());
		}
	}

	@Test
	public void meh() throws IOException {
//		try (PackageReader reader = new PackageReader(Paths.get("/home/shrimp/tmp/DM-MCC-Morbias-UPK.ut3"), false);
		try (PackageReader reader = new PackageReader(ut3Map, false);
			 Package pkg = new Package(reader)) {
			assertNotNull(pkg.sha1Hash());

			Arrays.stream(pkg.objects)
				  .filter(Objects::nonNull)
				  .sorted(Comparator.comparing(e -> e.name.name))
				  .forEach(e -> {
					  try {
						  System.out.printf("%s :: %s%n", e.classIndex.get().name().name, e.name.name);
						  for (Property property : e.object().properties) {
							  System.out.printf("  - %s :: %s%n", property.getClass().getSimpleName(), property.name.name);
						  }
						  System.out.println("----");
					  } catch (Throwable ex) {
						  ex.printStackTrace();
						  // bomb out
						  fail(ex);
					  }
				  });

//			ExportedObject o = pkg.objectByName(new Name("Texture2D"));
//			pkg.objectsByClassName("Texture2D").forEach(o -> {
//				System.out.printf("%s :: %s%n", o.classIndex.get().name().name, o.name.name);
//				for (Property property : o.object().properties) {
//					System.out.println(property.getClass().getSimpleName() + ": " + property.toString());
//				}
//			});

//			ExportedObject o = pkg.objectByName(new Name("MorbiasScreenshot"));
//			System.out.printf("%s :: %s%n", o.classIndex.get().name().name, o.name.name);
//			Object obj = o.object();
//			for (Property property : obj.properties) {
//				System.out.println(property.getClass().getSimpleName() + ": " + property.toString());
//			}
//			assertTrue(obj instanceof Texture);
//
//			Path tmpScreenshot = Files.createTempFile("test-ss-", ".png");
//
//			try {
//				Texture.MipMap[] mipMaps = ((Texture)obj).mipMaps();
//				BufferedImage bufferedImage = mipMaps[0].get();
//				ImageIO.write(bufferedImage, "png", tmpScreenshot.toFile());
//
//				// attempt to load the saved screenshot, should work if it was written correctly
//				assertNotNull(ImageIO.read(tmpScreenshot.toFile()));
//			} finally {
//				Files.deleteIfExists(tmpScreenshot);
//			}

			System.out.println(reader.stats);
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
			Collection<Import> imports = pkg.packageImports();
			assertFalse(imports.isEmpty());
			Import engine = imports.stream()
								   .filter(i -> i.name.name.equals("Engine"))
								   .findFirst().get();
			assertNotNull(engine);
			Import levelSummary = engine.children().stream()
										.filter(o -> o.name.name.equals("LevelSummary") && o.className.name.equals("Class"))
										.findFirst().get();
			assertNotNull(levelSummary);
		}
	}

	@Test
	public void readExports() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			Collection<Export> local = pkg.rootExports();
			assertFalse(local.isEmpty());
			Export levelInfo = local.stream()
									.filter(e -> e.name.name.startsWith("LevelInfo"))
									.findFirst().get();
			assertNotNull(levelInfo);
			assertNotNull(pkg.objectByExport(levelInfo));
		}
	}
}
