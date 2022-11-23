package com.fightitaway.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ConfirmPaymentRequest {

	private String paymentId;

	private String orderId;

	private String signature;

	private String status;

	private String emailId;

	private String txnId;

	private Double amount;

	private Boolean isAccountFree;

	private Integer validityInDays;

	private String couponCode;

	private String expiryDate;

	public String getTxnId() {
		return txnId;
	}

	public void setTxnId(String txnId) {
		this.txnId = txnId;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	/**
	 * @return the amount
	 */
	public Double getAmount() {
		return amount;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * @return the isAccountFree
	 */
	public Boolean getIsAccountFree() {
		return isAccountFree;
	}

	/**
	 * @param isAccountFree the isAccountFree to set
	 */
	public void setIsAccountFree(Boolean isAccountFree) {
		this.isAccountFree = isAccountFree;
	}

	/**
	 * @return the validityInDays
	 */
	public Integer getValidityInDays() {
		return validityInDays;
	}

	/**
	 * @param validityInDays the validityInDays to set
	 */
	public void setValidityInDays(Integer validityInDays) {
		this.validityInDays = validityInDays;
	}

	/**
	 * @return the couponCode
	 */
	public String getCouponCode() {
		return couponCode;
	}

	/**
	 * @param couponCode the couponCode to set
	 */
	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}

	/**
	 * @return the expiryDate
	 */
	public String getExpiryDate() {
		return expiryDate;
	}

	/**
	 * @param expiryDate the expiryDate to set
	 */
	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}

	public ConfirmPaymentRequest() {
	}

	public ConfirmPaymentRequest(JsonObject json) {
		ConfirmPaymentRequestConverter.fromJson(json, this);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		ConfirmPaymentRequestConverter.toJson(this, json);
		return json;
	}

	@Override
	public String toString() {
		return "ConfirmPaymentRequest [paymentId=" + paymentId + ", orderId=" + orderId + ", signature=" + signature
				+ ", status=" + status + ", emailId=" + emailId + ", txnId=" + txnId + ", amount=" + amount
				+ ", isAccountFree=" + isAccountFree + ", validityInDays=" + validityInDays + ", couponCode="
				+ couponCode + "]";
	}
}
