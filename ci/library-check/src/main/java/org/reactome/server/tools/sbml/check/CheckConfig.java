package org.reactome.server.tools.sbml.check;

import org.aspectj.lang.Aspects;
import org.reactome.server.graph.aop.LazyFetchAspect;
import org.reactome.server.graph.config.GraphCoreNeo4jConfig;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * The caller's own graph-core configuration, as the website's service has its own: the library brings none.
 * It scans graph-core alone.
 */
@Configuration
@ComponentScan(basePackages = {"org.reactome.server.graph"})
@EntityScan(basePackages = {"org.reactome.server.graph.domain.model"})
@EnableTransactionManagement
@EnableNeo4jRepositories(basePackages = {"org.reactome.server.graph.repository"})
@EnableSpringConfigured
public class CheckConfig extends GraphCoreNeo4jConfig {

    @Bean
    public LazyFetchAspect lazyFetchAspect() {
        return Aspects.aspectOf(LazyFetchAspect.class);
    }
}
