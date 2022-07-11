package io.nflow.tests.extension;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NflowServerExtension implements BeforeAllCallback, AfterEachCallback, AfterAllCallback {
  private static Logger logger = LoggerFactory.getLogger(NflowServerExtension.class);
  private Class<?> testClass;
  private final List<NflowServerConfig> configs = new ArrayList<>();
  private Object testInstance;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    testClass = context.getRequiredTestClass();
    List<Field> fields = stream(testClass.getFields())
        .filter(field -> field.getType() == NflowServerConfig.class)
        .filter(field -> isStatic(field.getModifiers()))
        .collect(toList());

    for (Field configField : fields) {
      var config = (NflowServerConfig) configField.get(null);
      logger.debug("Initialize with {}: {}", NflowServerConfig.class.getSimpleName(), config);
      configs.add(config);
      config.before(testClass.getSimpleName());
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    testInstance = context.getRequiredTestInstance();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    invokeBeforeServerStop();
    configs.forEach(NflowServerConfig::after);
  }

  private void invokeBeforeServerStop() {
    stream(testClass.getMethods())
        .filter(field -> field.getAnnotation(BeforeServerStop.class) != null)
        .forEach(this::invokeTestInstanceMethod);
  }

  private void invokeTestInstanceMethod(Method method) {
    try {
      method.invoke(testInstance);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Target({ ANNOTATION_TYPE, METHOD })
  @Retention(RUNTIME)
  public @interface BeforeServerStop {
  }
}
