package com.fightitaway.api;

import java.util.function.BiConsumer;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.rxjava.circuitbreaker.CircuitBreaker;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;

/**
 * Base verticle.
 * 
 * @author Smartdietplanner
 * @since 08-Aug-2020
 *
 */
public class BaseRxVerticle extends AbstractVerticle {

	protected void badRequest(RoutingContext context, String errorMessage) {
		context.response().setStatusCode(400).putHeader("content-type", "application/json")
				.end(new JsonObject().put("error", errorMessage).encodePrettily());
	}

	protected void setCrossDomain(io.vertx.ext.web.Router router, HttpServerOptions serverOptions) {

		router.route().handler(io.vertx.ext.web.handler.CorsHandler.create("*")
				.allowedMethod(io.vertx.core.http.HttpMethod.GET).allowedMethod(HttpMethod.POST)
				.allowedMethod(HttpMethod.DELETE).allowedMethod(HttpMethod.PUT).allowedMethod(HttpMethod.OPTIONS));
		router.route().handler(StaticHandler.create());
		vertx.getDelegate().createHttpServer(serverOptions).requestHandler(router).listen();

	}

//protected void setCrossDomain(io.vertx.ext.web.Router router,HttpServerOptions serverOptions) {
//		
//		router.route().handler(io.vertx.ext.web.handler.CorsHandler.create("*")
//				.allowedMethod(io.vertx.core.http.HttpMethod.GET).allowedMethod(HttpMethod.POST)
//				.allowedMethod(HttpMethod.DELETE).allowedMethod(HttpMethod.PUT)
//				.allowedMethod(HttpMethod.OPTIONS).allowedHeader("Access-Control-Request-Method")
//				.allowedHeader("Access-Control-Allow-Method")
//				.allowedHeader("Access-Control-Allow-Credentials").allowedHeader("Access-Control-Allow-Origin")
//				.allowedHeader("Access-Control-Allow-Headers").allowedHeader("Access-Control-Request-Headers")
//				.allowedHeader("Content-Type").allowedHeader("cross-origin").allowedHeader("Authorization")
//				.allowedHeader("application/json"));
//			router.route().handler(StaticHandler.create());
//			vertx.getDelegate().createHttpServer(serverOptions).requestHandler(router).listen();
//			
//	}

	protected void notFound(RoutingContext context) {
		context.response().setStatusCode(404).putHeader("content-type", "application/json")
				.end(new JsonObject().put("message", "not_found").encodePrettily());
	}

	protected void internalError(RoutingContext context, String errorMessage) {
		context.response().setStatusCode(500).putHeader("content-type", "application/json")
				.end(new JsonObject().put("error", errorMessage).encodePrettily());
	}

	protected void notImplemented(RoutingContext context) {
		context.response().setStatusCode(501).putHeader("content-type", "application/json")
				.end(new JsonObject().put("message", "not_implemented").encodePrettily());
	}

	protected void failToSendSMS(RoutingContext context, Throwable e) {

		JsonObject response = new JsonObject();
		response.put("responseCode", "0001");
		response.put("responseMessage", "unable to send SMS");
		response.put("error", e.getMessage());

		context.response().setStatusCode(200).putHeader("content-type", "application/json")
				.end(response.encodePrettily());
	}

	protected Future<JsonObject> get(String url, CircuitBreaker breaker) {
		final Promise<JsonObject> promise = Promise.promise();
		WebClientOptions optionsClient = new WebClientOptions().setSsl(true).setTrustAll(true).setVerifyHost(false);
		optionsClient.setKeepAlive(false);
		WebClient client = WebClient.create(vertx, optionsClient);

		breaker.rxExecuteWithFallback(
				future1 -> client.getAbs(url).rxSend().map(x -> this.mapData(x, future1)).subscribe(f -> {
					future1.complete(f);
				}, future1::fail), t -> {
					return new JsonObject().put("responseMessage", "Error").put("responseCode", "10023");

				}).subscribe(json -> {
					promise.complete(json);
				});

		return promise.future();
	}

	protected Future<JsonObject> get(String url) {
		final Promise<JsonObject> promise = Promise.promise();

		WebClientOptions optionsClient = new WebClientOptions().setSsl(true).setTrustAll(true).setVerifyHost(false);
		optionsClient.setKeepAlive(false);
		WebClient client = WebClient.create(vertx, optionsClient);

		client.getAbs(url).rxSend().map(x -> this.mapData(x)).subscribe(x -> {
			promise.complete(x);
		}, t -> {
			JsonObject error = new JsonObject().put("responseMessage", t.getMessage()).put("responseCode", "10023");
			promise.fail(error.encode());
		});

		return promise.future();
	}

	protected Future<JsonObject> post(String url, JsonObject reuest, CircuitBreaker breaker) {
		final Promise<JsonObject> promise = Promise.promise();
		WebClientOptions optionsClient = new WebClientOptions().setSsl(true).setTrustAll(true).setVerifyHost(false);
		optionsClient.setKeepAlive(false);
		WebClient client = WebClient.create(vertx, optionsClient);

		breaker.rxExecuteWithFallback(future1 -> client.postAbs(url).rxSendJsonObject(reuest)
				.map(x -> this.mapData(x, future1)).subscribe(future1::complete, future1::fail), t -> {
					return new JsonObject().put("responseMessage", "Error").put("responseCode", "10023");
				}).subscribe(json -> {
					promise.complete(json);
				});

		return promise.future();

	}

	protected Future<JsonObject> post(String url, JsonObject reuest) {
		final Promise<JsonObject> promise = Promise.promise();
		WebClientOptions optionsClient = new WebClientOptions().setSsl(true).setTrustAll(true).setVerifyHost(false);
		optionsClient.setKeepAlive(false);
		WebClient client = WebClient.create(vertx, optionsClient);
		client.postAbs(url).rxSendJsonObject(reuest).map(x -> this.mapData(x)).subscribe(x -> {
			promise.complete(x);
		}, t -> {
			JsonObject error = new JsonObject().put("responseMessage", t.getMessage()).put("responseCode", "10023");
			promise.fail(error.encode());
		});

		return promise.future();
	}

	protected Future<JsonObject> invokeEventBus(String address, JsonObject request) {
		final Promise<JsonObject> promise = Promise.promise();
		vertx.eventBus().<JsonObject>rxRequest(address, request).subscribe(m -> {
			System.out.println(m.body().toString());
			promise.complete(m.body());
		}, ex -> {
			System.out.println(ex.getMessage());
			JsonObject error = new JsonObject().put("responseMessage", ex.getMessage()).put("responseCode", "0002");
			promise.fail(error.encode());

		});

		return promise.future();
	}

	protected void failToSendPush(RoutingContext context, Throwable e) {

		JsonObject response = new JsonObject();
		response.put("responseCode", "0001");
		response.put("responseMessage", "unable to send push");
		response.put("error", e.getMessage());
		context.response().setStatusCode(200).putHeader("content-type", "application/json")
				.end(response.encodePrettily());
	}

	protected void fail(RoutingContext context, Throwable e) {

		JsonObject response = new JsonObject();
		response.put("responseCode", "0001");
		response.put("responseMessage", e.getMessage());
		context.response().setStatusCode(200).putHeader("content-type", "application/json")
				.end(response.encodePrettily());
	}

	@SuppressWarnings("rawtypes")
	protected JsonObject mapData(HttpResponse res, Promise f) {

		if (res.statusCode() != 200) {
			JsonObject response = new JsonObject();
			response.put("responseCode", res.statusCode());
			response.put("responseMessage", res.statusMessage());
			f.fail(response.toString());
			return response;
		} else {
			return res.bodyAsJsonObject();
		}
	}

	@SuppressWarnings("rawtypes")
	protected JsonObject mapData(HttpResponse res) {

		if (res.statusCode() != 200) {
			JsonObject response = new JsonObject();
			response.put("responseCode", res.statusCode());
			response.put("responseMessage", res.statusMessage());
			return response;
		} else {
			return res.bodyAsJsonObject();
		}
	}

	protected void fail(RoutingContext context, Throwable e, String message) {

		JsonObject response = new JsonObject();
		response.put("responseCode", "0001");
		response.put("responseMessage", "unable to send push");
		response.put("error", e.getMessage());
		context.response().setStatusCode(200).putHeader("content-type", "application/json")
				.end(response.encodePrettily());
	}

	protected void successWithMessageId(RoutingContext context, String messageId) {
		JsonObject response = new JsonObject();
		response.put("responseCode", "0000");
		response.put("responseMessage", "Succees");
		response.put("messageId", messageId);
		context.response().setStatusCode(200).putHeader("content-type", "application/json")
				.end(response.encodePrettily());
	}

	protected void badGateway(String errorMessage, RoutingContext context) {
		context.response().setStatusCode(502).putHeader("content-type", "application/json")
				.end(new JsonObject().put("error", "bad_gateway").encodePrettily());
	}

	protected void validateUser(RoutingContext context, BiConsumer<RoutingContext, JsonObject> biHandler) {

		biHandler.accept(context, context.getBodyAsJson());
	}
}
