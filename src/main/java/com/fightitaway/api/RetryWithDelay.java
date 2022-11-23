package com.fightitaway.api;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func1;

public class RetryWithDelay implements Func1<Observable<? extends Throwable>, Observable<?>> {

	private final int maxRetries;
	private final int retryDelayMillis;
	private int retryCount;

	public RetryWithDelay(final int maxRetries, final int retryDelayMillis) {
		this.maxRetries = maxRetries;
		this.retryDelayMillis = retryDelayMillis;
		this.retryCount = 0;
	}

	@Override
	public Observable<?> call(Observable<? extends Throwable> attempts) {
		return attempts.flatMap(new Func1<Throwable, Observable<?>>() {
			@Override
			public Observable<?> call(Throwable throwable) {
				if (++retryCount < maxRetries) {
					System.out.println(throwable.getMessage());
					return Observable.timer(retryDelayMillis, TimeUnit.MILLISECONDS);
				}
				return Observable.error(throwable);
			}
		});
	}
}