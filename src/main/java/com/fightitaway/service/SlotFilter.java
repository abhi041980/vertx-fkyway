package com.fightitaway.service;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonObject;

public class SlotFilter {
	private List<JsonObject> dataList;
	private boolean isLocked;
	public List<JsonObject> getDataList() {
		if(dataList==null) {
			dataList=new ArrayList<JsonObject>();	
		}
		return dataList;
	}
	public void setDataList(List<JsonObject> dataList) {
		this.dataList = dataList;
	}
	public boolean isLocked() {
		return isLocked;
	}
	public void setLocked(boolean isLocked) {
		this.isLocked = isLocked;
	}
	
}
