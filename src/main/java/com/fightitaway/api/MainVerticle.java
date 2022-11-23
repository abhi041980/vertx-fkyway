package com.fightitaway.api;



import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.rxjava.config.ConfigRetriever;

public class MainVerticle extends io.vertx.rxjava.core.AbstractVerticle{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MainVerticle.class);

	private  void deploy(final Class<? extends AbstractVerticle> clazz, final DeploymentOptions options) {
	    vertx.deployVerticle(clazz.getName(), options, handler -> {
	      if (handler.succeeded()) {
	    	  logger.info("{} started successfully (deployment identifier: {})"+ clazz.getSimpleName()+ handler.result());
	        OpenAPIServer.INSTANCE_NAME=handler.result();
	      } else {
	    	  logger.info("{} deployment failed due to: "+ clazz.getSimpleName()+handler.cause());
	      }
	    });
	}
	
	
	
	@Override
	public void start() throws Exception {
		
		String logFactory = System.getProperty("org.vertx.logger-delegate-factory-class-name");
        if (logFactory == null) {
            System.setProperty("org.vertx.logger-delegate-factory-class-name",
                    SLF4JLogDelegateFactory.class.getName());
        }
        
        
		ConfigStoreOptions file = new ConfigStoreOptions().setType("file").setFormat("json").setConfig(new JsonObject().put("path", "config.json"));
		ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(file);
		ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
		retriever.getConfig(confHandler -> { 
			logger.info("##### MainVerticle start()");
			int instance=confHandler.result().getInteger("instance",2);
			deploy(OpenAPIServer.class, new DeploymentOptions().setConfig(confHandler.result()).setInstances(instance));
		});
		
	}
	
}
