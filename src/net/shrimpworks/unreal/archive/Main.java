package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws IOException {
		System.out.println("Hello world");

		Package pgk = new Package(Paths.get("/home/shrimp/tmp/SCR-CityStreet.unr"));
		System.out.printf("Package version: %d%n", pgk.version());

		System.out.println(pgk.exports()[0]);
		System.out.println(pgk.object(pgk.exports()[0]));

		System.out.println(pgk.exports()[0]);

		System.out.println(pgk.exports()[0].flags());

		System.out.println(pgk.exports()[0].objClass().get());

		if (pgk.exports()[0].objClass().get() instanceof Package.Import) {
			System.out.println(((Package.Import)pgk.exports()[0].objClass().get()).packageName().get());
		}
	}
}
