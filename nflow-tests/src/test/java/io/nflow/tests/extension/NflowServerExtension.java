package io.nflow.tests.extension;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NflowServerExtension implements BeforeAllCallback, AfterAllCallback {
    private static Logger logger = LoggerFactory.getLogger(NflowServerExtension.class);
    private Class<?> testClass;
    private NflowServerConfig config;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        testClass = context.getRequiredTestClass();
        List<Field> fields = Arrays.stream(testClass.getFields())
                .filter(field ->
                        field.getType() == NflowServerConfig.class
                                && Modifier.isStatic(field.getModifiers()))
        .collect(Collectors.toList());

        if (fields.size() != 1) {
            throw new IllegalArgumentException("Classes that are annotated with @NflowServerExtension " +
                    "must have exactly one static field of type NflowServerConfig\n" +
                    "For example:\n" +
                    "public static NflowServerConfig server = new NflowServerConfig.Builder().build();");
        }
        Field configField = fields.get(0);
        config = (NflowServerConfig) configField.get(null);
        logger.debug("Initialize with {}: {}", NflowServerConfig.class.getSimpleName(), config);
        config.before();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        config.after();
    }

}
