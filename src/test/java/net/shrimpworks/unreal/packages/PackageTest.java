package net.shrimpworks.unreal.packages;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.objects.Model;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Polys;
import net.shrimpworks.unreal.packages.entities.objects.Texture;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Polygon;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Vector;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;
import net.shrimpworks.unreal.packages.entities.properties.StructProperty;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PackageTest {

	private static Path unrMap;
	private static Path ut2Map;

	@BeforeAll
	public static void setup() throws IOException {
		// unpack our test map to a temporary location
		unrMap = Files.createTempFile("test-map-", ".unr");
		try (InputStream is = PackageTest.class.getResourceAsStream("SCR-CityStreet.unr.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, unrMap, StandardCopyOption.REPLACE_EXISTING);
		}

		ut2Map = Files.createTempFile("test-map-", ".ut2");
		try (InputStream is = PackageTest.class.getResourceAsStream("UWRM-Torpedo.ut2.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, ut2Map, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@AfterAll
	public static void teardown() throws IOException {
		Files.deleteIfExists(unrMap);
		Files.deleteIfExists(ut2Map);
		unrMap = null;
		ut2Map = null;
	}

	@Test
	public void readGeometryTypes() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			pkg.objectsByClassName("Model").forEach(o -> {
				assertTrue(o.object() instanceof Model);
			});

			pkg.objectsByClassName("Polys").forEach(o -> {
				assertTrue(o.object() instanceof Polys);
			});
		}
	}

	@Test
	public void readLevelInfo() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			assertNotNull(pkg.sha1Hash());

			// read level info (also in LevelSummary, but missing Screenshot)
			ExportedObject levelInfo = pkg.objectsByClassName("LevelInfo").iterator().next();
			Object level = levelInfo.object();

			// read some basic level info
			assertEquals("Kenneth \"Shrimp\" Watson", ((StringProperty)level.property("Author")).value);
			assertEquals("City Street", ((StringProperty)level.property("Title")).value);

			// get the screenshot, and then save it to file
			Property shotProp = level.property("Screenshot");
			assertTrue(shotProp instanceof ObjectProperty);

			ExportedObject screenshot = pkg.objectByRef(((ObjectProperty)shotProp).value);
			Object object = screenshot.object();

			assertTrue(object instanceof Texture);

			Path tmpScreenshot = Files.createTempFile("test-ss-", ".png");

			try {
				Texture.MipMap[] mipMaps = ((Texture)object).mipMaps();
				BufferedImage bufferedImage = mipMaps[0].get();
				ImageIO.write(bufferedImage, "png", tmpScreenshot.toFile());

				// attempt to load the saved screenshot, should work if it was written correctly
				assertNotNull(ImageIO.read(tmpScreenshot.toFile()));
			} finally {
				Files.deleteIfExists(tmpScreenshot);
			}

		}
	}

	@Test
	public void readUE2Level() throws IOException {
		try (Package pkg = new Package(ut2Map)) {
			assertNotNull(pkg.sha1Hash());
			ExportedObject screenshot = pkg.objectByName(new Name("Shot00052"));

			Object object = screenshot.object();

			assertTrue(object instanceof Texture);

			Path tmpScreenshot = Files.createTempFile("test-ss-", ".png");

			try {
				Texture.MipMap[] mipMaps = ((Texture)object).mipMaps();
				BufferedImage bufferedImage = mipMaps[0].get();
				ImageIO.write(bufferedImage, "png", tmpScreenshot.toFile());

				// attempt to load the saved screenshot, should work if it was written correctly
				assertNotNull(ImageIO.read(tmpScreenshot.toFile()));
			} finally {
				Files.deleteIfExists(tmpScreenshot);
			}
		}
	}

	@Test
	public void readImports() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			Collection<Import> imports = pkg.packageImports();
			assertFalse(imports.isEmpty());
			Import engine = imports.stream()
								   .filter(i -> i.name.name.equals("Engine"))
								   .findFirst().get();
			assertNotNull(engine);
			Import levelSummary = engine.children().stream()
										.filter(o -> o.name.name.equals("LevelSummary") && o.className.name.equals("Class"))
										.findFirst().get();
			assertNotNull(levelSummary);
		}
	}

	@Test
	public void readExports() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			Collection<Export> local = pkg.rootExports();
			assertFalse(local.isEmpty());
			Export levelInfo = local.stream()
									.filter(e -> e.name.name.startsWith("LevelInfo"))
									.findFirst().get();
			assertNotNull(levelInfo);
			assertNotNull(pkg.objectByExport(levelInfo));
		}
	}


	@Test
	public void readPolys() throws IOException {
		class BrushThing {

			final Object brush;
			final Model model;
			final Polys polys;
			final StructProperty.VectorProperty location;

			public BrushThing(Object brush, Model model, Polys polys, StructProperty.VectorProperty location) {
				this.brush = brush;
				this.model = model;
				this.polys = polys;
				this.location = location;
			}
		}

		try (Package pkg = new Package(Paths.get("/home/shrimp/tmp/DM-Morbias][.unr"))) {
			int minX = 0, maxX = 0;
			int minY = 0, maxY = 0;

			List<BrushThing> things = new ArrayList<>();
			for (ExportedObject brush : pkg.objectsByClassName("Brush")) {
				Object brushObj = brush.object();
				Property modelProp = brushObj.property("Brush");
				Property location = brushObj.property("Location");
				if (modelProp instanceof ObjectProperty && location instanceof StructProperty.VectorProperty) {
					ExportedObject modelExp = pkg.objectByRef(((ObjectProperty)modelProp).value);
					if (modelExp != null && modelExp.object() instanceof Model) {
						Model model = (Model)modelExp.object();
						ExportedObject polysExp = pkg.objectByRef(model.polys);
						if (polysExp != null && polysExp.object() instanceof Polys) {
							things.add(new BrushThing(brushObj, model, (Polys)polysExp.object(), (StructProperty.VectorProperty)location));
						}
					}
				}
			}

			for (BrushThing thing : things) {
				minX = Math.min(minX, (int)thing.location.x);
				maxX = Math.max(maxX, (int)thing.location.x);
				minY = Math.min(minY, (int)thing.location.y);
				maxY = Math.max(maxY, (int)thing.location.y);
			}

			BufferedImage bigImage = new BufferedImage(maxX - minX, maxY - minY, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = bigImage.createGraphics();
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, bigImage.getWidth(), bigImage.getHeight());

			graphics.setStroke(new BasicStroke(5));
			graphics.setColor(Color.BLACK);

			// TODO rotate brushes
			// https://www.gamefromscratch.com/post/2012/11/24/GameDev-math-recipes-Rotating-one-point-around-another-point.aspx
			// https://stackoverflow.com/questions/2259476/rotating-a-point-about-another-point-2d

			for (BrushThing thing : things) {
				for (Polygon p : thing.polys.polys) {
					int[] pointsX = new int[p.vertices.size() + 1];
					int[] pointsY = new int[p.vertices.size() + 1];
					for (int i = 0; i < p.vertices.size(); i++) {
						Vector vertex = p.vertices.get(i);
						pointsX[i] = (int)(vertex.x - minX + thing.location.x);
						pointsY[i] = (int)(vertex.y - minY + thing.location.y);
					}
					pointsX[pointsX.length - 1] = (int)(thing.location.x + p.vertices.get(0).x - minX);
					pointsY[pointsY.length - 1] = (int)(thing.location.y + p.vertices.get(0).y - minY);
					graphics.drawPolygon(pointsX, pointsY, p.vertices.size());
				}
				graphics.drawString(thing.brush.export.name.name, (int)thing.location.x - minX, (int)thing.location.y - minY);
				graphics.drawString(thing.model.export.name.name, (int)thing.location.x - minX, (int)thing.location.y - minY + 15);
			}

			ImageIO.write(bigImage, "png", new File("map.png"));
		}
	}}
