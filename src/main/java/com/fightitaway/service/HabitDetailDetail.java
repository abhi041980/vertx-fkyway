package com.fightitaway.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class HabitDetailDetail {
	private String code;
	private String status;
	private String description;
	private String iconUrl;
	private String date;
	


	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public HabitDetailDetail() {
		// TODO Auto-generated constructor stub
	}
	public HabitDetailDetail(JsonObject json) {
		HabitDetailDetailConverter.fromJson(json, this);
	  }

	  public JsonObject toJson() {
	    JsonObject json = new JsonObject();
	    HabitDetailDetailConverter.toJson(this, json);
	    return json;
	  }

	@Override
	public String toString() {
		return "##### HabitDetailDetail [code=" + code + ", status=" + status + ", description=" + description + ", iconUrl="
				+ iconUrl + ", date=" + date + "]";
	}	
}
