package com.fightitaway.service;

import java.util.List;

public class DietItems {

	List<String> drinks;
	List<String> fruits;
	List<String> pulsesOrCurries;
	List<String> pulsesOrCurriesWithNV;
	List<String> snacks;
	List<String> dishes;
	List<String> dishesWithNV;
	List<String> rice;

	/**
	 * @return the drinks
	 */
	public List<String> getDrinks() {
		return drinks;
	}

	/**
	 * @param drinks the drinks to set
	 */
	public void setDrinks(List<String> drinks) {
		this.drinks = drinks;
	}

	/**
	 * @return the fruits
	 */
	public List<String> getFruits() {
		return fruits;
	}

	/**
	 * @param fruits the fruits to set
	 */
	public void setFruits(List<String> fruits) {
		this.fruits = fruits;
	}

	/**
	 * @return the pulsesOrCurries
	 */
	public List<String> getPulsesOrCurries() {
		return pulsesOrCurries;
	}

	/**
	 * @param pulsesOrCurries the pulsesOrCurries to set
	 */
	public void setPulsesOrCurries(List<String> pulsesOrCurries) {
		this.pulsesOrCurries = pulsesOrCurries;
	}

	/**
	 * @return the pulsesOrCurriesWithNV
	 */
	public List<String> getPulsesOrCurriesWithNV() {
		return pulsesOrCurriesWithNV;
	}

	/**
	 * @param pulsesOrCurriesWithNV the pulsesOrCurriesWithNV to set
	 */
	public void setPulsesOrCurriesWithNV(List<String> pulsesOrCurriesWithNV) {
		this.pulsesOrCurriesWithNV = pulsesOrCurriesWithNV;
	}

	/**
	 * @return the snacks
	 */
	public List<String> getSnacks() {
		return snacks;
	}

	/**
	 * @param snacks the snacks to set
	 */
	public void setSnacks(List<String> snacks) {
		this.snacks = snacks;
	}

	/**
	 * @return the dishes
	 */
	public List<String> getDishes() {
		return dishes;
	}

	/**
	 * @param dishes the dishes to set
	 */
	public void setDishes(List<String> dishes) {
		this.dishes = dishes;
	}

	/**
	 * @return the dishesWithNV
	 */
	public List<String> getDishesWithNV() {
		return dishesWithNV;
	}

	/**
	 * @param dishesWithNV the dishesWithNV to set
	 */
	public void setDishesWithNV(List<String> dishesWithNV) {
		this.dishesWithNV = dishesWithNV;
	}

	/**
	 * @return the rice
	 */
	public List<String> getRice() {
		return rice;
	}

	/**
	 * @param rice the rice to set
	 */
	public void setRice(List<String> rice) {
		this.rice = rice;
	}

	@Override
	public String toString() {
		return "DietItems [drinks=" + drinks + ", fruits=" + fruits + ", pulsesOrCurries=" + pulsesOrCurries
				+ ", pulsesOrCurriesWithNV=" + pulsesOrCurriesWithNV + ", snacks=" + snacks + ", dishes=" + dishes
				+ ", dishesWithNV=" + dishesWithNV + ", rice=" + rice + "]";
	}
}
