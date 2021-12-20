package io.nflow.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.nflow.tests.extension.NflowServerConfig;

import io.github.swagger2markup.Swagger2MarkupConverter;
import org.junit.jupiter.api.Test;

public class Swagger2MarkupTest extends AbstractNflowTest {

  private static final Path SWAGGER2_MARKUP_ASCIIDOC_DIR = Paths.get("src/main/asciidoc/swagger2markup");

  public static NflowServerConfig server = new NflowServerConfig.Builder().build();

  public Swagger2MarkupTest() {
    super(server);
  }

  @Test
  public void convertRemoteSwaggerToAsciiDoc() throws MalformedURLException {
    Swagger2MarkupConverter.from(new URL(server.getHttpAddress() + "/nflow/api/swagger.json")).build()
        .toFolder(SWAGGER2_MARKUP_ASCIIDOC_DIR);

    // Then validate that the right number of AsciiDoc files have been created
    String[] files = SWAGGER2_MARKUP_ASCIIDOC_DIR.toFile().list();
    assertThat(files, arrayWithSize(5));
  }
}
