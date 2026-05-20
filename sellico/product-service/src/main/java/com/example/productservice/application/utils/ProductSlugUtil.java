package com.example.productservice.application.utils;


import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

public final class ProductSlugUtil {

    private ProductSlugUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{Alnum}]+");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    private static final Map<String, String> COMMON_REPLACEMENTS = Map.of(
            "&", " and ",
            "@", " at ",
            "%", " percent ",
            "+", " plus ",
            "#", " number "
    );

    private static final int MAX_UNIQUE_ATTEMPTS = 1000;

    /* ================= BASIC SLUG ================= */

    public static String slugify(String input) {
        return slugify(input, "-", 0, true);
    }

    public static String slugify(String input, int maxLen) {
        return slugify(input, "-", maxLen, true);
    }

    public static String slugify(String input, String separator, int maxLen, boolean toLower) {
        if (input == null || input.isBlank()) return "n-a";
        if (separator == null || separator.isBlank()) {
            throw new IllegalArgumentException("separator must not be null or blank");
        }

        input = applyCommonReplacements(input);

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKD);
        String noDiacritics = COMBINING_MARKS.matcher(normalized).replaceAll("");

        String slug = NON_ALNUM.matcher(noDiacritics).replaceAll(separator);

        slug = collapseAndTrim(slug, separator);

        if (toLower) {
            slug = slug.toLowerCase(Locale.ROOT);
        }

        if (maxLen > 0 && slug.length() > maxLen) {
            slug = truncateAtWordBoundary(slug, maxLen, separator);
        }

        return slug.isEmpty() ? "n-a" : slug;
    }

    /* ================= UNIQUE PER SHOP ================= */

    /**
     * @param productName tên sản phẩm
     * @param shopId      id shop / seller
     * @param exists      (shopId, slug) -> true nếu đã tồn tại
     */
    public static String uniqueProductSlug(
            String productName,
            UUID shopId,
            BiPredicate<UUID, String> exists
    ) {
        return uniqueProductSlug(productName, shopId, exists, 0, "-");
    }

    public static String uniqueProductSlug(
            String productName,
            UUID shopId,
            BiPredicate<UUID, String> exists,
            int maxLen,
            String separator
    ) {
        Objects.requireNonNull(shopId, "shopId must not be null");
        Objects.requireNonNull(exists, "exists predicate must not be null");

        String base = slugify(productName, separator, maxLen, true);

        if (!exists.test(shopId, base)) return base;

        for (int i = 2; i <= MAX_UNIQUE_ATTEMPTS; i++) {
            String candidate = appendSuffix(base, i, maxLen, separator);
            if (!exists.test(shopId, candidate)) return candidate;
        }

        // fallback cực đoan (rất hiếm)
        return base + separator + System.currentTimeMillis();
    }

    /* ================= HELPERS ================= */

    private static String truncateAtWordBoundary(String slug, int maxLen, String separator) {
        if (maxLen <= 0 || slug.length() <= maxLen) return slug;

        String cut = slug.substring(0, maxLen);
        int lastSep = cut.lastIndexOf(separator);

        if (lastSep >= 0 && lastSep >= maxLen / 2) {
            cut = cut.substring(0, lastSep);
        }

        return trimSeparator(cut, separator);
    }

    private static String appendSuffix(String base, int index, int maxLen, String separator) {
        String suffix = separator + index;

        if (maxLen > 0 && base.length() + suffix.length() > maxLen) {
            int cutLen = Math.max(0, maxLen - suffix.length());
            base = truncateAtWordBoundary(base, cutLen, separator);
            base = trimSeparator(base, separator);
        }

        return base + suffix;
    }

    private static String collapseAndTrim(String s, String separator) {
        String q = Pattern.quote(separator);
        return s
                .replaceAll(q + "{2,}", separator)
                .replaceAll("^" + q + "+|" + q + "+$", "");
    }

    private static String trimSeparator(String s, String separator) {
        String q = Pattern.quote(separator);
        return s.replaceAll("^" + q + "+|" + q + "+$", "");
    }

    private static String applyCommonReplacements(String s) {
        String out = s;
        for (Map.Entry<String, String> e : COMMON_REPLACEMENTS.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }
        return out;
    }
}
