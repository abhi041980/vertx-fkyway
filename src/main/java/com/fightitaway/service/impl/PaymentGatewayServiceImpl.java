package com.fightitaway.service.impl;

import java.time.Duration;
import java.time.Instant;

import com.fightitaway.common.HttpClientHandler;
import com.fightitaway.common.MongoRepositoryWrapper;
import com.fightitaway.service.ConfirmPaymentRequest;
import com.fightitaway.service.CreateOrder;
import com.fightitaway.service.PaymentGatewayService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;

/**
 * Payment related activities.
 * 
 * @author Smartdietplanner
 * @since 08-Aug-2020
 *
 */
public class PaymentGatewayServiceImpl extends MongoRepositoryWrapper implements PaymentGatewayService {
	private HttpClientHandler handler;

	public PaymentGatewayServiceImpl(Vertx vertx, JsonObject config, HttpClientHandler handler) {
		super(vertx, config);
		this.handler = handler;
	}
	
	/**
	 * Create order - payment
	 * 
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return PaymentGatewayService
	 *
	 */
	@Override
	public PaymentGatewayService createOrder(CreateOrder request, String uri, String traceId,
			Handler<AsyncResult<CreateOrder>> resultHandler) {
		Instant start = Instant.now();
		String method = "PaymentGatewayServiceImpl createOrder() " + traceId + "-[" + request.getEmail() + "]";
		super.logCustRequest(request.getEmail(), "CREATEORDER (PAID)", traceId)
				.compose(data -> super.getCouponDetail(request.getEmail(), request.getCouponCode(), traceId))
				.compose(data -> this.handler.createOrder(request.getEmail(), data, traceId))
				.compose(data -> super.createOrder(data, request.getEmail(), traceId)).onComplete(resultHandler);

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " CREATE ORDER [" + timeElapsed + " milliseconds]");
		return this;
	}
	
	/**
	 * Confirm payment.
	 * 
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return PaymentGatewayService
	 *
	 */
	@Override
	public PaymentGatewayService confirmPayment(ConfirmPaymentRequest request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		Instant start = Instant.now();
		String method = "PaymentGatewayServiceImpl confirmPayment() " + traceId + "-[" + request.getEmailId() + "]";
		logger.info("##### " + method + " (CONFIRMPAYMENT) IS ACCOUNT FREE -->> " + request.getIsAccountFree());

		if (request.getIsAccountFree()) {
			super.logCustRequest(request.getEmailId(), "CREATEORDER + CONFIRMPAYMENT (FREE)", traceId)
					.compose(data -> super.getCouponDetail(request, traceId))
					.compose(data -> super.createOrderForFreeAccount(request, traceId))
					.compose(data -> super.confirmPaymentForFreeAccount(request, traceId))
					.compose(data -> super.subcribePlan(data, traceId))
					.compose(data -> this.fetchCustProfileReg(request.getEmailId(), data, "free", traceId))
					.compose(data -> super.updateCustPofile(request.getEmailId(), data, traceId))
					.onComplete(resultHandler);
		} else {
			logger.info("##### " + method + " CONFIRMPAYMENT -->> " + request.toString());
			super.logCustRequest(request.getEmailId(), "CONFIRMPAYMENT (PAID)", traceId)
					.compose(data -> super.getPaymentDetailForCoupon(request, traceId))
					.compose(data -> super.confirmPayment(data, traceId))
					.compose(data -> super.subcribePlan(data, traceId))
					.compose(data -> this.fetchCustProfileReg(request.getEmailId(), data, "payment", traceId))
					.compose(data -> super.updateCustPofile(request.getEmailId(), data, traceId))
					.compose(data -> super.getProfile(request.getEmailId(), data, null, traceId))
//					.compose(data -> super.sendEmailPostPaymentBySendInBlue(request.getEmailId(), data, traceId))
					.compose(data -> super.sendEmailPostPaymentByGmail(request.getEmailId(), data, traceId))
					.onComplete(resultHandler);
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " CONFIRM PAYMENT [" + request.getEmailId() + "] - [" + timeElapsed
				+ " milliseconds]");
		return this;
	}
	
	/**
	 * Refund payment
	 * 
	 * @param email
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return PaymentGatewayService
	 *
	 */
	@Override
	public PaymentGatewayService refund(String email, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		Instant start = Instant.now();
		String method = "PaymentGatewayServiceImpl refund() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.logCustRequest(email, "REFUNDPAYMENT", traceId).compose(data -> super.getPaymentDetail(email, traceId))
				.compose(data -> this.handler.refund(email, data.getPaymentId(), data.getAmount().intValue(), traceId))
				.compose(data -> super.updateSubscriptionPlan(data, email, traceId)).onComplete(resultHandler);

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " REFUND [" + email + "] - [" + timeElapsed + " milliseconds]");
		return this;
	}
	
	/**
	 * Cancel plan/refund - payment
	 * 
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return PaymentGatewayService
	 *
	 */
	@Override
	public PaymentGatewayService getCancelledPlan(String email, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		Instant start = Instant.now();
		String method = "PaymentGatewayServiceImpl getCancelledPlan() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.logCustRequest(email, "CANCELLEDPLAN + REFUNDPAYMENT", traceId)
				.compose(data -> super.getSubscribedPlanPaymentDetail(email, traceId))
				.compose(data -> this.handler.refund(email, data.getPaymentId(), data.getAmount().intValue(), traceId))
				.compose(data -> super.updateSubscriptionPlan(data, email, traceId)).onComplete(resultHandler);

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " PLAN CANCELLED [" + email + "] - [" + timeElapsed + " milliseconds]");
		return this;
	}
}
