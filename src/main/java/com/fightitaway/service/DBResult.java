package com.fightitaway.service;

import java.util.List;

import io.vertx.core.json.JsonObject;

public class DBResult {

	private FilterData filterData;
	private List<JsonObject> data;
	private boolean isDietAvailable;

	private List<DietDetail> prefList;
	private List<FoodDetail> prefCode;

	private List<JsonObject> masterPlan;

	public List<JsonObject> getMasterPlan() {
		return masterPlan;
	}

	public void setMasterPlan(List<JsonObject> masterPlan) {
		this.masterPlan = masterPlan;
	}

	public List<DietDetail> getPrefList() {
		return prefList;
	}

	public void setPrefList(List<DietDetail> prefList) {
		this.prefList = prefList;
	}

	public List<FoodDetail> getPrefCode() {
		return prefCode;
	}

	public void setPrefCode(List<FoodDetail> prefCode) {
		this.prefCode = prefCode;
	}

	public FilterData getFilterData() {
		return filterData;
	}

	public void setFilterData(FilterData filterData) {
		this.filterData = filterData;
	}

	public List<JsonObject> getData() {
		return data;
	}

	public void setData(List<JsonObject> data) {
		this.data = data;
	}

	/**
	 * @return the isDietAvailable
	 */
	public boolean isDietAvailable() {
		return isDietAvailable;
	}

	/**
	 * @param isDietAvailable the isDietAvailable to set
	 */
	public void setDietAvailable(boolean isDietAvailable) {
		this.isDietAvailable = isDietAvailable;
	}

	@Override
	public String toString() {
		return "DBResult [filterData=" + filterData + ", data=" + data + ", isDietAvailable=" + isDietAvailable
				+ ", prefList=" + prefList + ", prefCode=" + prefCode + ", masterPlan=" + masterPlan + "]";
	}
}
