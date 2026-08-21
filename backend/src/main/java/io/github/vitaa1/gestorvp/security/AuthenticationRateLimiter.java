package io.github.vitaa1.gestorvp.security;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
class AuthenticationRateLimiter {

	private final Clock clock;
	private final AuthenticationRateLimitProperties properties;
	private final Map<AttemptKey, Deque<Instant>> identityFailures;
	private final Map<String, Deque<Instant>> clientFailures;

	AuthenticationRateLimiter(Clock clock, AuthenticationRateLimitProperties properties) {
		this.clock = clock;
		this.properties = properties;
		this.identityFailures = new LinkedHashMap<>(128, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<AttemptKey, Deque<Instant>> eldest) {
				return size() > properties.maxKeys();
			}
		};
		this.clientFailures = new LinkedHashMap<>(128, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Deque<Instant>> eldest) {
				return size() > properties.maxKeys();
			}
		};
	}

	synchronized boolean isBlocked(String identity, String clientIp) {
		return reachedLimit(identityFailures, key(identity, clientIp), properties.maxFailures())
				|| reachedLimit(clientFailures, clientIp, properties.maxFailuresPerIp());
	}

	synchronized void recordFailure(String identity, String clientIp) {
		recordFailure(identityFailures, key(identity, clientIp));
		recordFailure(clientFailures, clientIp);
	}

	synchronized void recordSuccess(String identity, String clientIp) {
		identityFailures.remove(key(identity, clientIp));
	}

	private <K> boolean reachedLimit(Map<K, Deque<Instant>> failures, K key, int limit) {
		Deque<Instant> attempts = failures.get(key);
		if (attempts == null) {
			return false;
		}

		removeExpired(attempts);
		if (attempts.isEmpty()) {
			failures.remove(key);
			return false;
		}
		return attempts.size() >= limit;
	}

	private <K> void recordFailure(Map<K, Deque<Instant>> failures, K key) {
		Deque<Instant> attempts = failures.computeIfAbsent(key, ignored -> new ArrayDeque<>());
		removeExpired(attempts);
		attempts.addLast(clock.instant());
	}

	private AttemptKey key(String identity, String clientIp) {
		return new AttemptKey(identity.toLowerCase(Locale.ROOT), clientIp);
	}

	private void removeExpired(Deque<Instant> attempts) {
		Instant threshold = clock.instant().minus(properties.window());
		while (!attempts.isEmpty() && attempts.getFirst().isBefore(threshold)) {
			attempts.removeFirst();
		}
	}

	private record AttemptKey(String identity, String clientIp) {
	}
}
