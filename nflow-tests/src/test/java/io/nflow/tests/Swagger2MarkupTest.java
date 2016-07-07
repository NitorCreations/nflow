package io.nflow.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Test;

import io.github.robwin.swagger2markup.Swagger2MarkupConverter;
import io.nflow.tests.runner.NflowServerRule;

public class Swagger2MarkupTest extends AbstractNflowTest {

  private static final String SWAGGER2_MARKUP_ASCIIDOC_DIR = "src/main/asciidoc/swagger2markup";

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().build();

  public Swagger2MarkupTest() {
    super(server);
  }

  @Test
  public void convertRemoteSwaggerToAsciiDoc() throws IOException {
    Swagger2MarkupConverter.from(server.getHttpAddress() + "/api/swagger.json").build().intoFolder(SWAGGER2_MARKUP_ASCIIDOC_DIR);

    // Then validate that three AsciiDoc files have been created
    String[] files = new File(SWAGGER2_MARKUP_ASCIIDOC_DIR).list();
    assertEquals(4, files.length);
  }
}
