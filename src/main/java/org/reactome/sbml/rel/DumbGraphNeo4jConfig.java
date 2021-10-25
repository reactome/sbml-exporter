//package org.reactome.sbml.rel;
//
//import org.neo4j.ogm.config.Configuration;
//import org.neo4j.ogm.session.SessionFactory;
//import org.reactome.server.graph.config.Neo4jConfig;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
//import org.springframework.dao.support.PersistenceExceptionTranslator;
//import org.springframework.transaction.PlatformTransactionManager;
//
///**
// * A dumb configuration so that aspectJ is not enabled to avoid querying a Neo4j database. After trial and test,
// * the following implementation provides a minimum requirement to make the code work for a relational database.
// */
//@org.springframework.context.annotation.Configuration
////@ComponentScan(basePackages = {"org.reactome.server.graph"})
////@EnableTransactionManagement
////@EnableNeo4jRepositories(basePackages = {"org.reactome.server.graph.repository"})
////@EnableSpringConfigured
//public class DumbGraphNeo4jConfig extends Neo4jConfig {
//
//    private static final Logger logger = LoggerFactory.getLogger(DumbGraphNeo4jConfig.class);
//
//    private SessionFactory sessionFactory;
//
//    @Bean
//    public Configuration getConfiguration() {
//        Configuration config = new Configuration();
////        config.driverConfiguration()
////                .setDriverClassName("org.neo4j.ogm.drivers.http.driver.HttpDriver")
////                .setURI("http://".concat(System.getProperty("neo4j.host")).concat(":").concat(System.getProperty("neo4j.port")))
////                .setCredentials(System.getProperty("neo4j.user"), System.getProperty("neo4j.password"));
//        return config;
//    }
//
//    @Override
//    @Bean
//    public SessionFactory getSessionFactory() {
//        if (sessionFactory == null) {
//            logger.info("Creating a dumb Neo4j SessionFactory");
//            sessionFactory = new SessionFactory(getConfiguration(), "org.reactome.server.graph.domain" );
//        }
//        return sessionFactory;
//    }
//
//    @Override
//    public PersistenceExceptionTranslator persistenceExceptionTranslator() {
//        return null;
//    }
//
//    @Override
//    public PersistenceExceptionTranslationInterceptor translationInterceptor() {
//        return null;
//    }
//
//    @Override
//    public PlatformTransactionManager transactionManager() throws Exception {
//        return null;
//    }
//
//}
