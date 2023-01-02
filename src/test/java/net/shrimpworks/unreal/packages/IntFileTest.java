package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntFileTest {

	private Path tmpInt;
	private Path tmpUcl;
	private Path tmpIni;

	@BeforeEach
	public void setup() throws IOException {
		tmpInt = Files.createTempFile("test-int-", ".int");
		try (InputStream is = getClass().getResourceAsStream("IntFile.int")) {
			Files.copy(is, tmpInt, StandardCopyOption.REPLACE_EXISTING);
		}
		tmpUcl = Files.createTempFile("test-ucl-", ".ucl");
		try (InputStream is = getClass().getResourceAsStream("UclFile.ucl")) {
			Files.copy(is, tmpUcl, StandardCopyOption.REPLACE_EXISTING);
		}
		tmpIni = Files.createTempFile("test-ini-", ".ini");
		try (InputStream is = getClass().getResourceAsStream("UT3IniFile.ini")) {
			Files.copy(is, tmpIni, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@AfterEach
	public void teardown() throws IOException {
		Files.deleteIfExists(tmpInt);
		Files.deleteIfExists(tmpUcl);
		Files.deleteIfExists(tmpIni);
	}

	@Test
	public void parseIntFile() throws IOException {
		IntFile intFile = new IntFile(tmpInt);

		assertTrue(intFile.sections().contains("Language"));

		IntFile.Section lang = intFile.section("Language");
		assertTrue(lang.value("Language") instanceof IntFile.SimpleValue);
		assertTrue(lang.value("LangId") instanceof IntFile.SimpleValue);
		assertEquals("English (International)", ((IntFile.SimpleValue)lang.value("Language")).value());
		assertEquals("9", ((IntFile.SimpleValue)lang.value("LangId")).value());

		IntFile.Section pub = intFile.section("Public");
		assertTrue(pub.value("Preferences") instanceof IntFile.ListValue);
		assertTrue(pub.asList("Preferences").get(0) instanceof IntFile.MapValue);
		assertEquals("Advanced", ((IntFile.MapValue)pub.asList("Preferences").get(0)).get("Caption"));

		assertTrue(pub.keys().contains("Object"));

		assertTrue(pub.value("Quoted") instanceof IntFile.MapValue);
		IntFile.MapValue quoted = (IntFile.MapValue)(pub.value("Quoted"));
		assertEquals("Mutator,My Cool Mutator!", quoted.get("Description"));
	}

	@Test
	public void parseUclFile() throws IOException {
		IntFile uclFile = new IntFile(tmpUcl, true);

		assertFalse(uclFile.sections().isEmpty());
		assertFalse(uclFile.section("root").keys().isEmpty());

		assertTrue(uclFile.section("root").value("Mutator") instanceof IntFile.MapValue);
		assertEquals("My Mutator", ((IntFile.MapValue)uclFile.section("root").value("Mutator")).get("FallbackName"));
	}

	@Test
	public void parseUt3IniFile() throws IOException {
		IntFile iniFile = new IntFile(tmpIni);

		IntFile.Section charData = iniFile.section("UTGame.UTCustomChar_Data");
		assertTrue(charData.value("+Parts") instanceof IntFile.ListValue);
		assertTrue(charData.asList("+Parts").get(0) instanceof IntFile.MapValue);
		assertEquals("PART_Head", ((IntFile.MapValue)charData.asList("+Parts").get(0)).get("Part"));

		assertTrue(charData.value("+Characters") instanceof IntFile.ListValue);
		assertTrue(charData.asList("+Characters").get(0) instanceof IntFile.MapValue);
		assertEquals("Homer CELLSHADE", ((IntFile.MapValue)charData.asList("+Characters").get(0)).get("CharName"));
	}
}
