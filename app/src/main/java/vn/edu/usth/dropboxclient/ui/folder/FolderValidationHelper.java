package vn.edu.usth.dropboxclient.ui.folder;

import java.util.regex.Pattern;

public final class FolderValidationHelper {

    private static final int MAX_NAME_LENGTH = 255;
    // Disallowed characters in Dropbox file and folder names: / \ : * ? " < > | and controls.
    private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    public enum ValidationError {
        NONE,
        EMPTY,
        INVALID_CHARACTERS,
        TOO_LONG,
        ENDS_WITH_PERIOD_OR_SPACE,
        SAME_NAME
    }

    private FolderValidationHelper() {
        // Utility class
    }

    /**
     * Validates a folder name.
     */
    public static ValidationError validateFolderName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationError.EMPTY;
        }

        if (name.length() > MAX_NAME_LENGTH) {
            return ValidationError.TOO_LONG;
        }

        if (INVALID_CHARS_PATTERN.matcher(name).find()) {
            return ValidationError.INVALID_CHARACTERS;
        }

        for (int i = 0; i < name.length(); i++) {
            if (Character.isISOControl(name.charAt(i))) return ValidationError.INVALID_CHARACTERS;
        }

        if (!name.equals(name.trim()) || name.endsWith(".")) {
            return ValidationError.ENDS_WITH_PERIOD_OR_SPACE;
        }

        return ValidationError.NONE;
    }

    /**
     * Validates a new folder name against the current folder name when renaming.
     */
    public static ValidationError validateRename(String currentName, String newName) {
        ValidationError basicValidation = validateFolderName(newName);
        if (basicValidation != ValidationError.NONE) {
            return basicValidation;
        }

        if (currentName != null && currentName.trim().equalsIgnoreCase(newName.trim())) {
            return ValidationError.SAME_NAME;
        }

        return ValidationError.NONE;
    }

    /**
     * Safely constructs a child Dropbox path from a parent path and child name.
     * Normalized paths always begin with a single forward slash.
     */
    public static String buildChildPath(String parentPath, String childName) {
        ValidationError error = validateFolderName(childName);
        if (error != ValidationError.NONE) {
            throw new IllegalArgumentException("Invalid child name: " + error);
        }

        String cleanChild = childName;
        if (parentPath == null || parentPath.trim().isEmpty() || parentPath.trim().equals("/")) {
            return "/" + cleanChild;
        }

        String cleanParent = parentPath.trim();
        if (cleanParent.endsWith("/")) {
            cleanParent = cleanParent.substring(0, cleanParent.length() - 1);
        }
        if (!cleanParent.startsWith("/")) {
            cleanParent = "/" + cleanParent;
        }

        return cleanParent + "/" + cleanChild;
    }

    /**
     * Extracts the parent directory path from a full path.
     */
    public static String getParentPath(String fullPath) {
        if (fullPath == null || fullPath.trim().isEmpty() || fullPath.trim().equals("/")) {
            return "/";
        }

        String cleanPath = fullPath.trim();
        if (cleanPath.endsWith("/")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
        }

        int lastSlash = cleanPath.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }

        return cleanPath.substring(0, lastSlash);
    }

    /**
     * Extracts the item name from a full path.
     */
    public static String getNameFromPath(String fullPath) {
        if (fullPath == null || fullPath.trim().isEmpty() || fullPath.trim().equals("/")) {
            return "";
        }

        String cleanPath = fullPath.trim();
        if (cleanPath.endsWith("/")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
        }

        int lastSlash = cleanPath.lastIndexOf('/');
        if (lastSlash < 0) {
            return cleanPath;
        }

        return cleanPath.substring(lastSlash + 1);
    }
}
