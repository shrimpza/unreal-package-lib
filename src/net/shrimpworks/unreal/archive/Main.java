package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws IOException {
		System.out.println("Hello world");

		new PackageReader(Paths.get("/home/shrimp/tmp/SCR-CityStreet.unr"));
	}
}
