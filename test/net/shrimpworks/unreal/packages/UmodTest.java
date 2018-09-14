package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;

import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UmodTest {

	private Path tmpMod;

	@Before
	public void setup() throws IOException {

		// unpack a test mod to a temporary location
		// TODO we could actually try to get a SeekableByteChannel out of something, rather
		tmpMod = Files.createTempFile("test-mod-", ".umod");
		try (InputStream is = getClass().getResourceAsStream("MonsterHunt.umod.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {

			Files.copy(gis, tmpMod, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@After
	public void teardown() throws IOException {
		Files.deleteIfExists(tmpMod);
	}

	@Test
	public void umod() throws IOException {
		try (Umod umod = new Umod(tmpMod)) {

			// lets find a random map in the package
			boolean found = false;
			for (Umod.UmodFile file : umod.files) {
				if (file.name.endsWith(".unr")) {
					Package pkg = new Package(new PackageReader(file.read()));

					assertNotNull(pkg.sha1Hash());

					ExportedObject levelInfo = pkg.objectsByClassName("LevelInfo").iterator().next();
					Object level = levelInfo.object();

					assertEquals("Kenneth \"Shrimp\" Watson", ((StringProperty)level.property("Author")).value);

					found = true;
					break;
				}
			}

			assertTrue(found);
		}
	}
}
