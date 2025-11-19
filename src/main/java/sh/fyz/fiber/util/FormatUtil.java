package sh.fyz.fiber.util;

public class FormatUtil {

    public static String prettyPrint(String input) {
        if (input == null || input.isEmpty()) return input;

        String normalized = input.replace('_', ' ');

        normalized = normalized.replaceAll("([a-z])([A-Z])", "$1 $2");

        String[] words = normalized.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
                result.append(' ');
            }
        }

        return result.toString().trim();
    }

}
