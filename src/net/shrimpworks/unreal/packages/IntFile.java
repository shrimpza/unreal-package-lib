package net.shrimpworks.unreal.packages;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
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

	private static final Pattern SECTION = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");
	private static final Pattern KEY_VALUE = Pattern.compile("\\s*([^=]*)=(.*)");

	private static final Pattern MAP_VALUE = Pattern.compile("\\s*\\(([^)]*)\\)");

	private final List<Section> sections;

	public IntFile(Path intFile) throws IOException {
		this(FileChannel.open(intFile, StandardOpenOption.READ));
	}

	public IntFile(SeekableByteChannel channel) throws IOException {
		this.sections = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(Channels.newReader(channel, "UTF-8"))) {
			String r;
			Section section = null;
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
							for (String s : m.group(1).trim().split(",")) {
								m = KEY_VALUE.matcher(s);
								if (m.matches()) {
									vals.put(m.group(1).trim(), m.group(2).trim());
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
			return null;
		}

		@Override
		public String toString() {
			return String.format("Section [name=%s, values=%s]", name, values);
		}
	}

	public interface Value {
	}

	public static class SimpleValue implements Value {

		public final String value;

		public SimpleValue(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	public static class ListValue implements Value {

		public final List<Value> values;

		public ListValue(List<Value> values) {
			this.values = values;
		}

		@Override
		public String toString() {
			return values.toString();
		}
	}

	public static class MapValue implements Value {

		public final Map<String, String> value;

		public MapValue(Map<String, String> value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value.toString();
		}
	}
}
