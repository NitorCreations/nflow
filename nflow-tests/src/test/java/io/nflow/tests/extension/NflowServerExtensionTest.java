package io.nflow.tests.extension;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Disabled("Used only for testing utility tests")
@ExtendWith({NflowServerExtension.class})
public class NflowServerExtensionTest {

    public static NflowServerConfig server = new NflowServerConfig.Builder().build();

    @Test
    public void test1() {}

    @Test
    public void test2() {}
}
