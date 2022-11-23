package com.fightitaway.service;

public class CancelRequest {

	private Double amount;

	private String couponCode;

	private String email;

	private String paymentId;

	private String statusCode;

	private Integer refundedAmount;

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getCouponCode() {
		return couponCode;
	}

	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	/**
	 * @return the statusCode
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @return the refundedAmount
	 */
	public Integer getRefundedAmount() {
		return refundedAmount;
	}

	/**
	 * @param refundedAmount the refundedAmount to set
	 */
	public void setRefundedAmount(Integer refundedAmount) {
		this.refundedAmount = refundedAmount;
	}
}
