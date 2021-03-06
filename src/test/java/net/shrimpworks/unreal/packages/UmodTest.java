package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UmodTest {

	private Path tmpMod;

	@BeforeEach
	public void setup() throws IOException {
		// unpack a test mod to a temporary location
		tmpMod = Files.createTempFile("test-mod-", ".umod");
		try (InputStream is = getClass().getResourceAsStream("MonsterHunt.umod.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {

			Files.copy(gis, tmpMod, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@AfterEach
	public void teardown() throws IOException {
		Files.deleteIfExists(tmpMod);
	}

	@Test
	public void readUmodFile() throws IOException {
		try (Umod umod = new Umod(tmpMod)) {

			assertNotNull(umod.manifestIni);
			assertNotNull(umod.manifestInt);
			assertNotNull(umod.manifestInt.section("Setup"));
			assertEquals("Monster Hunt v5.0", umod.manifestInt.section("Setup").value("LocalProduct").toString());
			assertEquals("Monster Hunt v5.0", umod.manifestIni.section("Setup").value("Product").toString());

			// lets find a random map in the package
			boolean found = false;
			for (Umod.UmodFile file : umod.files) {
				assertEquals(40, file.sha1().length());

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