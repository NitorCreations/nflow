package io.nflow.tests;

import static java.lang.System.getProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
    // currently parboiled 1.3.2 AsmUtils does not work on new jdks - fails with trying to call ClassLoader.findLoadedClass
    assumeTrue("1.8".equals(getProperty("java.specification.version")));
    Swagger2MarkupConverter.from(new URL(server.getHttpAddress() + "/nflow/api/swagger.json")).build()
        .toFolder(SWAGGER2_MARKUP_ASCIIDOC_DIR);

    // Then validate that the right number of AsciiDoc files have been created
    String[] files = SWAGGER2_MARKUP_ASCIIDOC_DIR.toFile().list();
    assertThat(files, arrayWithSize(5));
  }
}
