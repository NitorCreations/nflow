package com.nitorcreations.nflow.rest.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.nitorcreations.nflow.engine.config.EngineConfiguration;

@Configuration
@Import(EngineConfiguration.class)
@ComponentScan("com.nitorcreations.nflow.rest")
public class RestConfiguration {

}
