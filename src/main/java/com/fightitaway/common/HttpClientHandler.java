package com.fightitaway.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.fightitaway.service.CreateOrder;
import com.fightitaway.service.RefundRequest;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.client.WebClient;

public class HttpClientHandler {
	public static final Logger logger = LoggerFactory.getLogger(HttpClientHandler.class);

	private WebClient webClient;
	
	private JsonObject config;

	public HttpClientHandler(WebClient webClient, JsonObject config) {
		this.webClient = webClient;
		this.config = config;
	}
	
	/** 
	 * IT IS USED FOR BELOW PAYMENTS:
	 * 1. NEW PLAN
	 * 2. UPGRADE PLAN
	 * 
	 * NOTE: THE AMOUNT SHOULD BE IN PAISE.
	 * 
	 * @param email - String
	 * @param request - CreateOrder
	 * 
	 * @return request
	 */
	public Future<CreateOrder> createOrder(String email, CreateOrder request, String traceId) {
		String method = "HttpClientHandler createOrder() " + traceId + "-[" + email + "]";
		request.setEmail(email);
		Promise<CreateOrder> promise = Promise.promise();
		String txnId = UUID.randomUUID().toString();
		JsonObject options = new JsonObject();
		options.put("amount", request.getAmount() * 100);
		options.put("currency", "INR");
		options.put("receipt", txnId);
		options.put("payment_capture", 1);
		logger.info("##### " + method + " AMOUNT                 -->> " + request.getAmount());
		logger.info("##### " + method + " EMAIL                  -->> " + request.getEmail());
		logger.info("##### " + method + " TRANSCTION ID          -->> " + txnId);

		webClient.postAbs("https://api.razorpay.com/v1/orders")
				.basicAuthentication(this.config.getJsonObject("pay").getString("apiKey"),
						this.config.getJsonObject("pay").getString("apiSecret"))
				.rxSendJsonObject(options).subscribe(res -> {
					logger.info(
							"##### " + method + "  CREATE ORDER RESPONSE -->> " + res.bodyAsJsonObject().toString());
					if (res.statusCode() == 200) {
						String orderId = res.bodyAsJsonObject().getString("id");
						request.setOrderId(orderId);
						request.setTxnId(txnId);
						request.setKeyId(this.config.getJsonObject("pay").getString("apiKey"));
						logger.info("##### " + method + " ORDERID [HTTP 200]     -->> " + orderId);
						logger.info("##### " + method + " KEY     [HTTP 200]     -->> " + request.getKeyId());
						promise.complete(request);
					} else {
						JsonObject error = res.bodyAsJsonObject().getJsonObject("error");
						logger.info("##### " + method + " ERROR                  -->> ["
								+ error.getString("description") + "]");
						promise.fail(error.getString("description"));
					}

				}, ex -> {
					promise.fail(ex.getMessage());
				});

		return promise.future();
	}
	
	/**
	 * IT IS USED TO REFUND PAYMENT IF ONE AND ONLY ONE PLAN EXISTS WIOUT UPGRADE.
	 * 
	 * @param email     - String
	 * @param paymentId - String
	 * @param amount    - Integer
	 * 
	 * @return refundRequest
	 */  
	public Future<RefundRequest> refund(String email, String paymentId, Integer amount, String traceId) {
		String method = "HttpClientHandler refund() " + traceId;
		Promise<RefundRequest> promise = Promise.promise();
		RefundRequest refundRequest = new RefundRequest();
		refundRequest.setPaymentId(paymentId);
		refundRequest.setRefundedAmount(amount);
		refundRequest.setEmail(email);
		JsonObject options = new JsonObject();
		// amount = amount * 100;
		Integer amountToBeRefunded = amount;
		logger.info(
				"##### " + method + "                  ADMIN CHARGES -->> " + this.config.getInteger("admincharges"));
		if (amountToBeRefunded > this.config.getInteger("admincharges"))
			amountToBeRefunded = amount - this.config.getInteger("admincharges");
		refundRequest.setRefundedAmount(amountToBeRefunded);
		options.put("amount", amountToBeRefunded * 100); // Note: The amount should be in paise.
		logger.info("##### " + method + " 				           EMAIL -->> " + email);
		logger.info("##### " + method + "			           PAYMENTID -->> " + paymentId);
		logger.info("##### " + method + "          AMOUNT TO BE REFUNDED -->> " + amountToBeRefunded);
		String url = "https://api.razorpay.com/v1/payments/" + paymentId + "/refund";
		logger.info("URL " + url);
		webClient.postAbs(url)
				.basicAuthentication(this.config.getJsonObject("pay").getString("apiKey"),
						this.config.getJsonObject("pay").getString("apiSecret"))
				.rxSendJsonObject(options).subscribe(res -> {
					logger.info("##### " + method + "    REFUND RESPONSE -->> " + res.bodyAsJsonObject().toString());
					JsonObject jsonObject = new JsonObject();
					JsonObject error = res.bodyAsJsonObject().getJsonObject("error");
					if (res.statusCode() == 200) {
						refundRequest.setStatusCode("200");
						refundRequest.setIsRefunded(true);
						logger.info("##### " + method + " HTTP STATUS CODE - [200]");
						jsonObject.put("code", "0000").put("message", "Succuess");
						refundRequest.setRefundedDate(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date()));
						promise.complete(refundRequest);
					} else {
						refundRequest.setStatusCode(error.getString("code"));
						refundRequest.setDescription(error.getString("description"));
						logger.info("##### " + method + "          ERROR -->> " + error.getString("description"));
						promise.complete(refundRequest);
					}
				}, ex -> {
					promise.fail(ex.getMessage());
				});

		return promise.future();
	}
}
