package com.fightitaway.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class DietDetail {
	private String code;

	private String description;
	
	private String type;
	
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public DietDetail() {
		// TODO Auto-generated constructor stub
	}
	public DietDetail(JsonObject json) {
		DietDetailConverter.fromJson(json, this);
	  }

	  public JsonObject toJson() {
	    JsonObject json = new JsonObject();
	    DietDetailConverter.toJson(this, json);
	    return json;
	  }
	
	
}
