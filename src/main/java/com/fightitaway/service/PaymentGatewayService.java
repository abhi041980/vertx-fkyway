package com.fightitaway.service;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface PaymentGatewayService {

	String SERVICE_NAME = "payment-gateway-service";

	/**
	 * The address on which the service is published
	 */
	String SERVICE_ADDRESS = "payment-gateway-service.address";

	@Fluent
	public PaymentGatewayService createOrder(CreateOrder request, String uri, String traceId,
			Handler<AsyncResult<CreateOrder>> resultHandler);

	@Fluent
	public PaymentGatewayService confirmPayment(ConfirmPaymentRequest request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public PaymentGatewayService refund(String email, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public PaymentGatewayService getCancelledPlan(String email, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

}
