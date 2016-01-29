package com.nitorcreations.nflow.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Test;

import com.nitorcreations.nflow.tests.runner.NflowServerRule;

import io.github.robwin.swagger2markup.Swagger2MarkupConverter;

public class Swagger2MarkupTest extends AbstractNflowTest {

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().build();

  public Swagger2MarkupTest() {
    super(server);
  }

  @Test
  public void convertRemoteSwaggerToAsciiDoc() throws IOException {
    // TODO: here we could call nFlow server swagger resource - if we had one

    Swagger2MarkupConverter.from("../nflow-rest-api/target/swagger-docs/swagger.json").build()
        .intoFolder("src/main/asciidoc");

    // Then validate that three AsciiDoc files have been created
    String[] files = new File("src/main/asciidoc").list();
    assertEquals(4, files.length);
  }
}
