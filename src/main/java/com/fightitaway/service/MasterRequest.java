package com.fightitaway.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class MasterRequest {
	
	private String name;
	
	private String code;
	

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MasterRequest() {
	}
	
	public MasterRequest(JsonObject json) {
		MasterRequestConverter.fromJson(json, this);
	  }

	  public JsonObject toJson() {
	    JsonObject json = new JsonObject();
	    MasterRequestConverter.toJson(this, json);
	    return json;
	  }
	
	
}
