package io.github.vitaa1.gestorvp.security;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

@Component
class ClientIpResolver {

	private static final int MAX_IP_LENGTH = 45;

	private final String trustedProxyHeader;

	ClientIpResolver(AuthenticationRateLimitProperties properties) {
		this.trustedProxyHeader = properties.trustedProxyHeader();
	}

	String resolve(HttpServletRequest request) {
		if (StringUtils.hasText(trustedProxyHeader)) {
			String forwardedAddress = request.getHeader(trustedProxyHeader);
			if (isNumericIpAddress(forwardedAddress)) {
				return forwardedAddress;
			}
		}
		return request.getRemoteAddr();
	}

	private boolean isNumericIpAddress(String value) {
		if (!StringUtils.hasText(value) || value.length() > MAX_IP_LENGTH || value.contains(",")) {
			return false;
		}
		return value.contains(":") ? isIpv6Literal(value) : isIpv4Literal(value);
	}

	private boolean isIpv4Literal(String value) {
		String[] octets = value.split("\\.", -1);
		if (octets.length != 4) {
			return false;
		}
		for (String octet : octets) {
			if (octet.isEmpty() || octet.length() > 3 || !octet.chars().allMatch(Character::isDigit)
					|| Integer.parseInt(octet) > 255) {
				return false;
			}
		}
		return true;
	}

	private boolean isIpv6Literal(String value) {
		if (!value.matches("[0-9A-Fa-f:.]+")) {
			return false;
		}
		if (value.contains(".")) {
			int ipv4Separator = value.lastIndexOf(':');
			if (ipv4Separator < 0 || !isIpv4Literal(value.substring(ipv4Separator + 1))) {
				return false;
			}
		}

		String[] compressedParts = value.split("::", -1);
		if (compressedParts.length > 2) {
			return false;
		}

		int groups = countIpv6Groups(compressedParts[0]);
		if (groups < 0) {
			return false;
		}
		if (compressedParts.length == 2) {
			int trailingGroups = countIpv6Groups(compressedParts[1]);
			return trailingGroups >= 0 && groups + trailingGroups < 8;
		}
		return groups == 8;
	}

	private int countIpv6Groups(String part) {
		if (part.isEmpty()) {
			return 0;
		}

		String[] groups = part.split(":", -1);
		int count = 0;
		for (int index = 0; index < groups.length; index++) {
			String group = groups[index];
			if (group.contains(".")) {
				if (index != groups.length - 1 || !isIpv4Literal(group)) {
					return -1;
				}
				count += 2;
			}
			else if (group.isEmpty() || group.length() > 4 || !group.matches("[0-9A-Fa-f]+")) {
				return -1;
			}
			else {
				count++;
			}
		}
		return count;
	}
}
