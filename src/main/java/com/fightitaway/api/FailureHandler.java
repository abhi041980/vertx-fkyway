package com.fightitaway.api;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class FailureHandler implements Handler<RoutingContext> {
  
    public void handle(RoutingContext context) {
        Throwable thrown = context.failure();
        recordError(thrown);
        context.response().setStatusCode(500).end();
    }
  
    private void recordError(Throwable throwable) {
        // Your logging/tracing/metrics framework here
    }
}