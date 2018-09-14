package net.shrimpworks.unreal.packages;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Texture;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PackageTest {

	private Path tmpMap;

	@Before
	public void setup() throws IOException {

		// unpack our test map to a temporary location
		// TODO we could actually try to get a SeekableByteChannel out of something, rather
		tmpMap = Files.createTempFile("test-map-", ".unr");
		try (InputStream is = getClass().getResourceAsStream("SCR-CityStreet.unr.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {

			Files.copy(gis, tmpMap, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@After
	public void teardown() throws IOException {
		Files.deleteIfExists(tmpMap);
	}

	@Test
	public void readLevelInfo() throws IOException {
		try (Package pkg = new Package(tmpMap)) {
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

			// TODO add test read UT2003/4 screenshots - maps are just big though
//		if (object.className().equals("MaterialSequence")) {
//			System.out.println(object.property("FallbackMaterial"));
//			ExportedObject fallback = pkg.objectByRef(((ObjectProperty)object.property("FallbackMaterial")).value);
//			Object fallbackObj = fallback.object();
//			if (fallbackObj instanceof Texture) {
//				Texture.MipMap[] mipMaps = ((Texture)fallbackObj).mipMaps();
//				BufferedImage bufferedImage = mipMaps[0].get();
//				ImageIO.write(bufferedImage, "png", new File("/tmp/screenshot.png"));
//			}
//		}
		}
	}
}
