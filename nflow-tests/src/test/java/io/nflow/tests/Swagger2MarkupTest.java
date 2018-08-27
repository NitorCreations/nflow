package io.nflow.tests;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.ClassRule;
import org.junit.Test;

import io.github.swagger2markup.Swagger2MarkupConverter;
import io.nflow.tests.runner.NflowServerRule;

public class Swagger2MarkupTest extends AbstractNflowTest {

  private static final Path SWAGGER2_MARKUP_ASCIIDOC_DIR = Paths.get("src/main/asciidoc/swagger2markup");

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().build();

  public Swagger2MarkupTest() {
    super(server);
  }

  @Test
  public void convertRemoteSwaggerToAsciiDoc() throws MalformedURLException {
    Swagger2MarkupConverter.from(new URL(server.getHttpAddress() + "/nflow/api/swagger.json")).build()
        .toFolder(SWAGGER2_MARKUP_ASCIIDOC_DIR);

    // Then validate that the right number of AsciiDoc files have been created
    String[] files = SWAGGER2_MARKUP_ASCIIDOC_DIR.toFile().list();
    assertEquals(5, files.length);
  }
}
