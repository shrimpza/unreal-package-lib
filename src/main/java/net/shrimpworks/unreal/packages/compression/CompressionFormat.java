package net.shrimpworks.unreal.packages.compression;

public enum CompressionFormat {
	NONE(0),
	LZIB(1),
	LZO(2),
	LZX(4);

	private final int flag;

	CompressionFormat(int flag) {
		this.flag = flag;
	}

	public static CompressionFormat fromFlag(int flag) {
		for (CompressionFormat value : CompressionFormat.values()) {
			if (value.flag == flag) return value;
		}
		return NONE;
	}
}
