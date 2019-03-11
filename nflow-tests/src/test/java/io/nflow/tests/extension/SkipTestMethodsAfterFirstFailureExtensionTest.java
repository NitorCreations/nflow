package io.nflow.tests.extension;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@Disabled("Used only for testing utility tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith({SkipTestMethodsAfterFirstFailureExtension.class})
public class SkipTestMethodsAfterFirstFailureExtensionTest {


    @Order(1)
    @Test
    public void test1() {}

    @Order(2)
    @Test
    public void test2() {
        Assertions.fail();
    }

    @Order(3)
    @Test
    public void test3() {}
}
