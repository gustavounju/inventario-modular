package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAttemptService {

	private static final int MAX_TRACKED_KEYS = 4096;

	private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();
	private final int maxFailures;
	private final Duration blockDuration;
	private final Clock clock;

	@Autowired
	public LoginAttemptService(
			@Value("${inventario.security.login.max-failures:5}") int maxFailures,
			@Value("${inventario.security.login.block-seconds:300}") long blockSeconds) {
		this(maxFailures, Duration.ofSeconds(blockSeconds), Clock.systemUTC());
	}

	LoginAttemptService(int maxFailures, Duration blockDuration, Clock clock) {
		this.maxFailures = Math.max(1, maxFailures);
		this.blockDuration = blockDuration.isNegative() || blockDuration.isZero()
				? Duration.ofMinutes(5)
				: blockDuration;
		this.clock = clock;
	}

	public boolean isBlocked(String username, String remoteAddress) {
		String k = key(username, remoteAddress);
		AttemptState state = attempts.get(k);
		if (state == null) {
			return false;
		}
		Instant now = Instant.now(clock);
		if (state.isExpired(now, blockDuration)) {
			attempts.remove(k, state);
			return false;
		}
		if (state.blockedUntil() == null) {
			return false;
		}
		if (now.isBefore(state.blockedUntil())) {
			return true;
		}
		attempts.remove(k, state);
		return false;
	}

	public void loginFailed(String username, String remoteAddress) {
		Instant now = Instant.now(clock);
		if (attempts.size() >= MAX_TRACKED_KEYS) {
			evictExpiredOrOldest(now);
		}
		String key = key(username, remoteAddress);
		attempts.compute(key, (ignored, current) -> {
			boolean expired = current != null && current.isExpired(now, blockDuration);
			int failures = (current == null || expired) ? 1 : current.failures() + 1;
			Instant blockedUntil = failures >= maxFailures ? now.plus(blockDuration) : null;
			return new AttemptState(failures, blockedUntil, now);
		});
	}

	public void loginSucceeded(String username, String remoteAddress) {
		attempts.remove(key(username, remoteAddress));
	}

	private void evictExpiredOrOldest(Instant now) {
		attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(now, blockDuration));
		if (attempts.size() >= MAX_TRACKED_KEYS) {
			attempts.entrySet().stream()
					.min(Comparator.comparing(e -> e.getValue().lastAttemptAt()))
					.map(Map.Entry::getKey)
					.ifPresent(attempts::remove);
		}
	}

	private String key(String username, String remoteAddress) {
		String normalizedUser = StringUtils.hasText(username)
				? username.trim().toLowerCase(Locale.ROOT)
				: "anonymous";
		String normalizedAddress = StringUtils.hasText(remoteAddress) ? remoteAddress.trim() : "unknown";
		return normalizedUser + "@" + normalizedAddress;
	}

	private record AttemptState(int failures, Instant blockedUntil, Instant lastAttemptAt) {
		boolean isExpired(Instant now, Duration window) {
			if (blockedUntil != null) {
				return !now.isBefore(blockedUntil);
			}
			return lastAttemptAt != null && now.isAfter(lastAttemptAt.plus(window));
		}
	}
}
