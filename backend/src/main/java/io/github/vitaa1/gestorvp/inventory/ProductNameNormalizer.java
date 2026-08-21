package io.github.vitaa1.gestorvp.inventory;

import java.util.Locale;

public final class ProductNameNormalizer {
	private static final String ACCENTED_CHARACTERS = "áàâãäéèêëíìîïóòôõöúùûüçñýÿ";
	private static final String PLAIN_CHARACTERS = "aaaaaeeeeiiiiooooouuuucnyy";

	private ProductNameNormalizer() {
	}

	public static String displayName(String value) {
		return java.text.Normalizer.normalize(
				value.strip().replaceAll("\\s+", " "), java.text.Normalizer.Form.NFC);
	}

	public static String legacyNormalizedName(String value) {
		return displayName(value).toLowerCase(Locale.ROOT);
	}

	public static String searchName(String value) {
		String normalized = java.text.Normalizer
			.normalize(displayName(value), java.text.Normalizer.Form.NFC)
			.toLowerCase(Locale.ROOT);
		StringBuilder result = new StringBuilder(normalized.length());
		normalized.chars().forEach(character -> {
			int accentIndex = ACCENTED_CHARACTERS.indexOf(character);
			result.append(accentIndex < 0 ? (char) character : PLAIN_CHARACTERS.charAt(accentIndex));
		});
		return result.toString();
	}
}
