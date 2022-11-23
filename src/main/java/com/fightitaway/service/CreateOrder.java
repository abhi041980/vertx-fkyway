package com.fightitaway.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class CreateOrder {

	private Double amount;

	private String couponCode;

	private String email;

	private String orderId;

	private String txnId;

	private String keyId;

	private String status;

	private Integer validityInMonth;

	private Integer validityInDays;

	public Integer getValidityInMonth() {
		return validityInMonth;
	}

	public void setValidityInMonth(Integer validityInMonth) {
		this.validityInMonth = validityInMonth;
	}

	public String getCouponCode() {
		return couponCode;
	}

	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public String getTxnId() {
		return txnId;
	}

	public void setTxnId(String txnId) {
		this.txnId = txnId;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
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
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	public CreateOrder() {
	}

	public CreateOrder(JsonObject json) {
		CreateOrderConverter.fromJson(json, this);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		CreateOrderConverter.toJson(this, json);
		return json;
	}

	@Override
	public String toString() {
		return "CreateOrder [amount=" + amount + ", couponCode=" + couponCode + ", email=" + email + ", orderId="
				+ orderId + ", txnId=" + txnId + ", keyId=" + keyId + ", status=" + status + ", validityInMonth="
				+ validityInMonth + ", validityInDays=" + validityInDays + "]";
	}

}
