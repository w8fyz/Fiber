package sh.fyz.fiber.util;

//Define at load a true random number that we use as a seed for our random number generator
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.Collections;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RandomUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final String ALPHABET_UPPER = ALPHABET.toUpperCase();
    private static final String ALPHANUMERIC = ALPHABET + ALPHABET_UPPER + "0123456789";

    public static int nextInt() {
        return RANDOM.nextInt();
    }

    public static int nextInt(int bound) {
        return RANDOM.nextInt(bound);
    }

    public static int nextInt(int origin, int bound) {
        return RANDOM.nextInt(bound - origin) + origin;
    }

    public static long nextLong() {
        return RANDOM.nextLong();
    }

    public static double nextDouble() {
        return RANDOM.nextDouble();
    }

    public static boolean nextBoolean() {
        return RANDOM.nextBoolean();
    }

    public static String randomAlphabetic(int length) {
        return randomFromChars(ALPHABET + ALPHABET_UPPER, length);
    }

    public static String randomLowercase(int length) {
        return randomFromChars(ALPHABET, length);
    }

    public static String randomUppercase(int length) {
        return randomFromChars(ALPHABET_UPPER, length);
    }

    public static String randomAlphanumeric(int length) {
        return randomFromChars(ALPHANUMERIC, length);
    }

    private static String randomFromChars(String chars, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static <T> void shuffle(List<T> list) {
        Collections.shuffle(list, RANDOM);
    }

    public static <T> List<T> shuffledCopy(List<T> list) {
        return list.stream().collect(Collectors.collectingAndThen(Collectors.toList(), l -> {
            Collections.shuffle(l, RANDOM);
            return l;
        }));
    }
}


