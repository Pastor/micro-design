package it.micro.breaker;

import lombok.Builder;

import java.util.Date;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public interface CircuitBreaker<P, R> {

    static void main(String[] args) throws InterruptedException {
        Random random = new Random(new Date().getTime());
        Settings settings = Settings.builder()
                .failureThreshold(50)
                .timeout(1000)
                .halfSuccesses(2)
                .collectCount(5)
                .build();

        CircuitBreaker<String, Integer> circuitBreaker = new Default<>(settings);

        circuitBreaker.call("123", param -> {
            System.out.println("PROCS: " + param);
            return Integer.parseInt(param);
        }).print();
        circuitBreaker.call("not a number", param -> {
            System.out.println("PROCS: " + param);
            return Integer.parseInt(param);
        }).print();
        for (int i = 0; i < 10; i++) {
            circuitBreaker.call("fail", param -> {
                throw new RuntimeException("Simulated failure");
            }).print();
            int slept = random.nextInt(250);
            Thread.sleep(slept);
        }
        circuitBreaker.call("test", param -> 42).print();
        Thread.sleep(settings.timeout + 500);
        circuitBreaker.call("test", param -> 42).print();
    }

    Result<R, String> call(P param, Function<P, R> guardedCall);

    record Result<R, E>(R result, E error) {
        private static <R, E> Result<R, E> success(R result) {
            return new Result<>(result, null);
        }

        private static <R, E> Result<R, E> error(E error) {
            return new Result<>(null, error);
        }

        private void print() {
            if (error() == null) {
                System.out.println("OK   : " + result());
            } else {
                System.out.println("ERROR: " + error());
            }
        }
    }

    @Builder(toBuilder = true)
    record Settings(int failureThreshold, int successThreshold, int timeout, int halfSuccesses, int collectCount) {
    }


    final class Default<P, R> implements CircuitBreaker<P, R> {
        private final Settings settings;
        private final Queue<Boolean> callResults;
        private final AtomicLong halfOpenSuccessCount = new AtomicLong();
        private final AtomicLong lastOpenTime = new AtomicLong();
        private volatile State state = State.SUCCESS;

        public Default(Settings settings) {
            this.settings = settings;
            this.callResults = new ArrayBlockingQueue<>(settings.collectCount + 2);
        }

        private synchronized void next(State newState) {
            state = newState;
            switch (newState) {
                case FAILED -> lastOpenTime.set(System.currentTimeMillis());
                case SUCCESS, HALF -> halfOpenSuccessCount.set(0);
            }
        }

        private boolean allowRequest() {
            State currentState = state;
            switch (currentState) {
                case FAILED -> {
                    if (System.currentTimeMillis() - lastOpenTime.longValue() > settings.timeout) {
                        next(State.HALF);
                        return true;
                    }
                    return false;
                }
                case SUCCESS -> {
                    return true;
                }
                case HALF -> {
                    return halfOpenSuccessCount.longValue() < settings.halfSuccesses;
                }
            }
            return false;
        }

        @Override
        public Result<R, String> call(P param, Function<P, R> guardedCall) {
            if (!allowRequest()) {
                return Result.error("Circuit breaker waiting for timeout. Past " + (System.currentTimeMillis() - lastOpenTime.longValue()) + " ms");
            }
            try {
                R applied = guardedCall.apply(param);
                onSuccess();
                return Result.success(applied);
            } catch (Exception e) {
                onFailure();
                return Result.error(e.getMessage());
            }
        }

        public void onSuccess() {
            if (state == State.HALF) {
                if (halfOpenSuccessCount.incrementAndGet() >= settings.halfSuccesses) {
                    next(State.SUCCESS);
                }
            } else {
                addCallResult(true);
            }
        }

        public void onFailure() {
            if (state == State.HALF) {
                next(State.FAILED);
            } else {
                addCallResult(false);
                failureRate();
            }
        }

        private void addCallResult(boolean success) {
            callResults.add(success);
            while (callResults.size() > settings.collectCount) {
                callResults.poll();
            }
        }

        private void failureRate() {
            if (callResults.size() < settings.collectCount) {
                return;
            }

            int failures = 0;
            for (boolean result : callResults) {
                if (!result) {
                    failures++;
                }
            }

            double failureRate = (double) failures / settings.collectCount * 100;
            if (failureRate >= settings.failureThreshold) {
                next(State.FAILED);
            }
        }

        @Override
        public String toString() {
            return String.format("State: %7s, Hosc: %2d", state, halfOpenSuccessCount.intValue());
        }

        private enum State {
            FAILED, SUCCESS, HALF
        }
    }
}
