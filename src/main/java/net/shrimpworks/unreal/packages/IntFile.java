package net.shrimpworks.unreal.packages;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Provides a simple <code>.int</code> file reader implementations, supporting
 * some basic concepts like repeat keys, values which contain compound values
 * (objects), and combinations of all.
 */
public class IntFile {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final Pattern SECTION = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");
	private static final Pattern KEY_VALUE = Pattern.compile("\\s*([^=]*)=(.*)");

	private static final Pattern MAP_VALUE = Pattern.compile("\\s*\\((.*)\\)");
	// TODO MAP_SUB attempts to support objects inside objects
	private static final Pattern MAP_SUB = Pattern.compile(".*?([\\s]*?,?([^=]*)=\\(([^)]*)\\).*?).*?");
	private static final String MAP_VALUE_SPLIT = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

	private final List<Section> sections;

	public IntFile(Path intFile) throws IOException {
		this(FileChannel.open(intFile, StandardOpenOption.READ), false, DEFAULT_CHARSET);
	}

	/**
	 * Create a new IntFile from a file path.
	 *
	 * @param intFile       path to int file to read
	 * @param syntheticRoot if true, will create a synthetic section named "root"
	 *                      to hold items which don't have a section header
	 * @throws IOException reading file failed
	 */
	public IntFile(Path intFile, boolean syntheticRoot) throws IOException {
		this(FileChannel.open(intFile, StandardOpenOption.READ), syntheticRoot, DEFAULT_CHARSET);
	}

	public IntFile(SeekableByteChannel channel) throws IOException {
		this(channel, false, DEFAULT_CHARSET);
	}

	/**
	 * Create a new IntFile from a byte channel.
	 *
	 * @param channel       channel to read
	 * @param syntheticRoot if true, will create a synthetic section named "root"
	 *                      to hold items which don't have a section header
	 * @throws IOException reading channel failed
	 */
	public IntFile(SeekableByteChannel channel, boolean syntheticRoot, Charset encoding) throws IOException {
		this.sections = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(Channels.newReader(channel, encoding))) {
			String r;
			Section section = null;
			if (syntheticRoot) {
				section = new Section("root", new HashMap<>());
				sections.add(section);
			}
			while ((r = reader.readLine()) != null) {
				Matcher m = SECTION.matcher(r);
				if (m.matches()) {
					section = new Section(m.group(1).trim(), new HashMap<>());
					sections.add(section);
				} else if (section != null) {
					m = KEY_VALUE.matcher(r);
					if (m.matches()) {
						String k = m.group(1).trim();
						String v = m.group(2).trim();

						Value value;

						m = MAP_VALUE.matcher(v);
						if (m.matches()) {
							Map<String, String> vals = new HashMap<>();
							for (String s : m.group(1).trim().split(MAP_VALUE_SPLIT, -1)) {
								m = KEY_VALUE.matcher(s);
								if (m.matches()) {
									vals.put(m.group(1).trim(), m.group(2).trim().replaceAll("\"", ""));
								}
							}
							value = new MapValue(vals);
						} else {
							value = new SimpleValue(v);
						}

						Value current = section.values.get(k);
						if (current instanceof ListValue) {
							((ListValue)current).values.add(value);
						} else if (current != null) {
							section.values.put(k, new ListValue(new ArrayList<>(Arrays.asList(current, value))));
						} else {
							section.values.put(k, value);
						}
					}
				}
			}
		}
	}

	/**
	 * Get a section.
	 *
	 * @param section the section name
	 * @return the section, or null if not found
	 */
	public Section section(String section) {
		return sections.stream().filter(s -> s.name.equalsIgnoreCase(section)).findFirst().orElse(null);
	}

	/**
	 * Get a list of all sections within this .int file.
	 *
	 * @return section names
	 */
	public Collection<String> sections() {
		return sections.stream().map(s -> s.name).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return String.format("IntFile [sections=%s]", sections);
	}

	/**
	 * An <code>.int</code> file section.
	 * <p>
	 * In the file structure, these are identified by their [Heading].
	 */
	public static class Section {

		public final String name;
		private final Map<String, Value> values;

		public Section(String name, Map<String, Value> values) {
			this.name = name;
			this.values = values;
		}

		/**
		 * Retrieve a value by its key.
		 *
		 * @param key key
		 * @return value, or null if the key does not exist
		 */
		public Value value(String key) {
			return values.get(key);
		}

		/**
		 * Get a list of all keys within this section.
		 *
		 * @return key names
		 */
		public Collection<String> keys() {
			return values.keySet();
		}

		/**
		 * Convenience to always retrieve a value as a list.
		 * <p>
		 * This is useful if, based on reading the file a value appears to
		 * be a singleton, but code reading it perhaps expects a list to be
		 * present.
		 *
		 * @param key the key
		 * @return a list of values, or null if the key does not exist
		 */
		public ListValue asList(String key) {
			Value val = value(key);
			if (val instanceof ListValue) return (ListValue)val;
			if (val != null) return new ListValue(Collections.singletonList(val));
			return new ListValue(Collections.emptyList());
		}

		@Override
		public String toString() {
			return String.format("Section [name=%s, values=%s]", name, values);
		}
	}

	public interface Value {
	}

	public record SimpleValue(String value) implements Value {

		@Override
		public String toString() {
			return value;
		}
	}

	public record ListValue(List<Value> values) implements Value {

		public Value get(int index) {
			return values.get(index);
		}

		@Override
		public String toString() {
			return values.toString();
		}
	}

	public record MapValue(Map<String, String> value) implements Value {

		public String get(String key) {
			return value.get(key);
		}

		public String getOrDefault(String key, String defaultValue) {
			return value.getOrDefault(key, defaultValue);
		}

		public boolean containsKey(String key) {
			return value.containsKey(key);
		}

		@Override
		public String toString() {
			return value.toString();
		}
	}
}
