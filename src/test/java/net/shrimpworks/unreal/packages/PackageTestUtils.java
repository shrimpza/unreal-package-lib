package net.shrimpworks.unreal.packages;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

public class PackageTestUtils {

	protected static Path fetchAndCache(String url, Path output) throws IOException, InterruptedException {
		if (Files.exists(output)) return output;

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
		System.out.printf("Downloading %s to %s%n", url, output.toAbsolutePath());
		HttpResponse<InputStream> send = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
		try (InputStream is = send.body();
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, output, StandardCopyOption.REPLACE_EXISTING);
			return output;
		}
	}

}
