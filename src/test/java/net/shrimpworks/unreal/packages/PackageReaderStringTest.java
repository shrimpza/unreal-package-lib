package net.shrimpworks.unreal.packages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * String reading is version and length-prefix sensitive; these cover the encodings which are
 * easy to get wrong, using hand-built buffers rather than whole packages.
 */
public class PackageReaderStringTest {

	/** Encode a value the way packages store compact indexes; see {@link PackageReader#readIndex()}. */
	private static byte[] compactIndex(int value) {
		int num = Math.abs(value);
		int first = (value < 0 ? 0x80 : 0) | (num & 0x3F);
		num >>>= 6;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(num > 0 ? first | 0x40 : first);
		while (num > 0) {
			int b = num & 0x7F;
			num >>>= 7;
			out.write(num > 0 ? b | 0x80 : b);
		}
		return out.toByteArray();
	}

	private static byte[] bytes(byte[]... parts) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (byte[] p : parts) out.write(p, 0, p.length);
		return out.toByteArray();
	}

	/** Reads a string from a throwaway package-less buffer, and asserts nothing beyond it was consumed. */
	private static String read(int version, int length, byte[] content, int expectedRead) throws IOException {
		// trailing sentinel bytes would show up in the result, or in the read position, on an over-read
		Path tmp = Files.createTempFile("test-string-", ".bin");
		try {
			Files.write(tmp, bytes(content, "SENTINEL".getBytes(StandardCharsets.US_ASCII)));
			try (PackageReader reader = new PackageReader(tmp)) {
				reader.version = version;
				reader.moveTo(0);
				String result = reader.readString(length);
				assertEquals(expectedRead, reader.position(), "unexpected number of bytes consumed");
				return result;
			}
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	public void ue1WideString() throws IOException {
		// UE1 string properties use a compact index length; negative means UCS-2
		String value = "Jarosław (Smoke) Brzuzy";
		byte[] chars = (value + "\0").getBytes(StandardCharsets.UTF_16LE);
		byte[] content = bytes(compactIndex(-(value.length() + 1)), chars);

		assertEquals(value, read(69, content.length, content, content.length));
	}

	@Test
	public void ue1MultiByteLength() throws IOException {
		// lengths over 63 need two index bytes; reading only the first orphans the second into the string
		String value = "Dm-Congo][   by adam sims aka Oblivion_Soldier (send feedback to nobody@example.com)";
		byte[] chars = (value + "\0").getBytes(StandardCharsets.ISO_8859_1);
		byte[] length = compactIndex(value.length() + 1);
		assertEquals(2, length.length);

		byte[] content = bytes(length, chars);
		assertEquals(value, read(69, content.length, content, content.length));
	}

	@Test
	public void ue1NameLength() throws IOException {
		// name table entries are prefixed with a plain byte length instead
		byte[] content = bytes(new byte[] { 12 }, "PrunedPaths\0".getBytes(StandardCharsets.ISO_8859_1));

		assertEquals("PrunedPaths", read(69, -1, content, content.length));
	}

	@Test
	public void ue2ColourMarkupRetained() throws IOException {
		// UT2003/2004 embed colour markup as ESC followed by an RGB triplet; only whitespace may be trimmed
		String value = "\u001b\u00ff\u0001\u0001snare";
		byte[] chars = (" " + value + " \0").getBytes(StandardCharsets.ISO_8859_1);
		byte[] content = bytes(compactIndex(chars.length), chars);

		assertEquals(value, read(128, content.length, content, content.length));
	}

	@Test
	public void preV64FixedLength() throws IOException {
		// pre-v64 strings run to a NUL, but must not run past the property they belong to
		byte[] content = "Well, no terminator here".getBytes(StandardCharsets.ISO_8859_1);

		assertEquals("Well, no terminator here", read(63, content.length, content, content.length));
	}
}
