# Unreal Package Lib

A small Java library for reading Unreal Engine packages.

Unreal Packages, are used by Unreal Engine games for packaging content such as
maps, textures, sounds, and the gameplay code itself.

Although the files all have different file extensions for organisation
purposes only (for example, `.unr` or `.ut2` for maps, `.utx` for textures, 
`.u` for code), they all have the same structure and are capable of holding
the same content.

This implementation supports at least Unreal Engine 1 and 2, and has been
tested using content and assets from Unreal, Unreal Tournament, and Unreal
Tournament 2004. Your mileage may vary with other games using these engines.

Also provided via the `Umod` class is the ability to read and extract the 
contents of `.umod` installers, commonly used to distribute larger Unreal and 
Unreal Tournament modifications.

Finally, reading of Unreal Engine's `.int` files is provided via the `IntFile`
class, which simplifies processing some of the non-INI file like properties
contained within these files.  

## Current Implementation

- Reading all a packages' exported objects and their properties.
- Read and export textures from most supported formats.
- Light-weight memory efficient implementation.
- Use Umod package contents in combination with the package reader, to allow
  inspecting and exporting objects without needing to extract the Umod 
  contents. 
- Extendable with more object readers if needed (meshes, sounds, etc).
- Reading array properties on objects is not implemented.
- There is currently no support for reading or extraction of data such as 
  UnrealScript classes.


## Usage

Simple usage example, to read the title of a UT99 map, and save its screenshot. 

There's a lot of boilerplate here, which could be hidden behind some utilities
within your own implementation.

```java
// load the package from file
Package pkg = new Package(Path.get("DM-MyLevel.unr"));

// get the LevelInfo object, which contains the properties we're looking for 
ExportedObject levelInfo = pkg.objectsByClassName("LevelInfo").iterator().next();
Object level = levelInfo.object();

// read the map title
String title = ((StringProperty)level.property("Title")).value;

// get a Texture object from the Screenshot property
Property shotProp = level.property("Screenshot");
ExportedObject shotObject = pkg.objectByRef(((ObjectProperty)shotProp).value);
Texture shot = (Texture)shotObject.object();

// get and save the first mipmap (the full size texture) to file 
Texture.MipMap[] mipMaps = shot.mipMaps();
ImageIO.write(mipMaps[0].get(), "png", Path.get("scheenshot.png").toFile());
```

In this example, we unpack/extract the contents of a UMod file to dist:

```java
Path dest = Paths.get("/path/to/unpack/to"); 
Umod umod = new Umod(Paths.get("path/to/file.umod"));

// create a small buffer for file transfer
ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);

for (Umod.UmodFile f : umod.files) {
  // skip the umod's manifest (.ini, .ini) files
  if (f.name.startsWith("System\\Manifest")) continue;

  System.out.printf("Unpacking %s ", f.name);
  Path out = dest.resolve(filePath(f.name));

  // re-create the directory structure within the umod
  if (!Files.exists(out)) Files.createDirectories(out);

  out = out.resolve(fileName(f.name));
  System.out.printf("to %s%n", out);

  try (FileChannel fileChannel = FileChannel.open(out, 
       StandardOpenOption.WRITE, StandardOpenOption.CREATE, 
       StandardOpenOption.TRUNCATE_EXISTING);
    SeekableByteChannel fileData = f.read()) {

    // stream the file form the umod to disk
    while (fileData.read(buffer) > 0) {
      fileData.read(buffer);
      buffer.flip();
      fileChannel.write(buffer);
      buffer.clear();
    }
  }
}

// --- file name and path utility functions used during unpacking

private String fileName(String path) {
  String tmp = path.replaceAll("\\\\", "/");
  return tmp.substring(Math.max(0, tmp.lastIndexOf("/") + 1));
}

private String filePath(String path) {
  String tmp = path.replaceAll("\\\\", "/");
  return tmp.substring(0, Math.max(0, tmp.lastIndexOf("/")));
}

```

For further usage examples, including reading of a umod packages contents in 
combination with the Package reader, refer to the unit tests.


## Building

Building the project requires Gradle. From the project root directory, execute:

```bash
./gradlew build     # linux/macos
gradlew.bat build   # windows
```

After build completes, the `.build/libs/` directory will include a `.jar` file
suitable for inclusion in your project. 

## Binaries / Maven Repository

A Maven repository containing the latest builds is also available:

Repository URL: https://code.shrimpworks.za.net/artefacts 

- GroupID: `net.shrimpworks`
- Artefact: `unreal-package-lib`
- Version: `1.+`
