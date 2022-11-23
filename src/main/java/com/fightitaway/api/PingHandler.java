package com.fightitaway.api;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class PingHandler implements Handler<RoutingContext> {
    private String pingResponse;

    public void handle(RoutingContext context) {
        context.response().setStatusCode(200).end(pingResponse);
    }

    public void setPingResponse(String pingResponse) {
        this.pingResponse = pingResponse;
    }
}