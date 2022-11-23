package com.fightitaway.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class PrefRequestDetail {
	private String code;
	private String type;
	
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public PrefRequestDetail() {
	}
	
	public PrefRequestDetail(JsonObject json) {
		PrefRequestDetailConverter.fromJson(json, this);
	  }

	  public JsonObject toJson() {
	    JsonObject json = new JsonObject();
	    PrefRequestDetailConverter.toJson(this, json);
	    return json;
	  }

}
