package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IntFileTest {

	private Path tmpInt;
	private Path tmpUcl;

	@Before
	public void setup() throws IOException {
		tmpInt = Files.createTempFile("test-int-", ".int");
		try (InputStream is = getClass().getResourceAsStream("IntFile.int")) {
			Files.copy(is, tmpInt, StandardCopyOption.REPLACE_EXISTING);
		}
		tmpUcl = Files.createTempFile("test-ucl-", ".ucl");
		try (InputStream is = getClass().getResourceAsStream("UclFile.ucl")) {
			Files.copy(is, tmpUcl, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@After
	public void teardown() throws IOException {
		Files.deleteIfExists(tmpInt);
		Files.deleteIfExists(tmpUcl);
	}

	@Test
	public void parseInt() throws IOException {
		IntFile intFile = new IntFile(tmpInt);

		assertTrue(intFile.sections().contains("Language"));

		IntFile.Section lang = intFile.section("Language");
		assertTrue(lang.value("Language") instanceof IntFile.SimpleValue);
		assertTrue(lang.value("LangId") instanceof IntFile.SimpleValue);
		assertEquals("English (International)", ((IntFile.SimpleValue)lang.value("Language")).value);
		assertEquals("9", ((IntFile.SimpleValue)lang.value("LangId")).value);

		IntFile.Section pub = intFile.section("Public");
		assertTrue(pub.value("Preferences") instanceof IntFile.ListValue);
		assertTrue(pub.asList("Preferences").values.get(0) instanceof IntFile.MapValue);
		assertEquals("Advanced", ((IntFile.MapValue)pub.asList("Preferences").values.get(0)).get("Caption"));

		assertTrue(pub.keys().contains("Object"));
	}

	@Test
	public void parseUcl() throws IOException {
		IntFile uclFile = new IntFile(tmpUcl, true);

		assertFalse(uclFile.sections().isEmpty());
		assertFalse(uclFile.section("root").keys().isEmpty());

		assertTrue(uclFile.section("root").value("Mutator") instanceof IntFile.MapValue);
		assertEquals("My Mutator", ((IntFile.MapValue)uclFile.section("root").value("Mutator")).get("FallbackName"));
	}
}
