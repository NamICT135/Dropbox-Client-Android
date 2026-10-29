package vn.edu.usth.dropboxclient;

import org.junit.Test;

import vn.edu.usth.dropboxclient.ui.folder.FolderValidationHelper;
import vn.edu.usth.dropboxclient.ui.folder.FolderValidationHelper.ValidationError;

import static org.junit.Assert.*;

public class FolderValidationHelperTest {

    @Test
    public void acceptsValidFolderNames() {
        assertEquals(ValidationError.NONE, FolderValidationHelper.validateFolderName("Documents"));
        assertEquals(ValidationError.NONE, FolderValidationHelper.validateFolderName("My Project 2026"));
        assertEquals(ValidationError.NONE, FolderValidationHelper.validateFolderName("Tài liệu học tập"));
        assertEquals(ValidationError.NONE, FolderValidationHelper.validateFolderName("Ảnh chụp gia đình"));
        assertEquals(ValidationError.NONE, FolderValidationHelper.validateFolderName("Đồ án tốt nghiệp"));
        assertEquals(ValidationError.NONE, FolderValidationHelper.validateFolderName("a"));
        assertEquals(ValidationError.NONE, FolderValidationHelper.validateFolderName("folder-name_with.allowed.dots"));
    }

    @Test
    public void rejectsEmptyOrWhitespaceNames() {
        assertEquals(ValidationError.EMPTY, FolderValidationHelper.validateFolderName(null));
        assertEquals(ValidationError.EMPTY, FolderValidationHelper.validateFolderName(""));
        assertEquals(ValidationError.EMPTY, FolderValidationHelper.validateFolderName("   "));
        assertEquals(ValidationError.EMPTY, FolderValidationHelper.validateFolderName("\t\n"));
    }

    @Test
    public void rejectsInvalidCharacters() {
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateFolderName("my/folder"));
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateFolderName("my\\folder"));
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateFolderName("folder:name"));
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateFolderName("folder*name"));
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateFolderName("folder?name"));
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateFolderName("folder\"name"));
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateFolderName("folder<name"));
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateFolderName("folder>name"));
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateFolderName("folder|name"));
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateFolderName("folder\nname"));
    }

    @Test
    public void rejectsTrailingPeriodOrSpace() {
        assertEquals(ValidationError.ENDS_WITH_PERIOD_OR_SPACE, FolderValidationHelper.validateFolderName("folder."));
        assertEquals(ValidationError.ENDS_WITH_PERIOD_OR_SPACE, FolderValidationHelper.validateFolderName("folder "));
        assertEquals(ValidationError.ENDS_WITH_PERIOD_OR_SPACE, FolderValidationHelper.validateFolderName(" folder"));
    }

    @Test
    public void rejectsNamesExceedingMaxLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb.append("a");
        }
        assertEquals(ValidationError.TOO_LONG, FolderValidationHelper.validateFolderName(sb.toString()));
    }

    @Test
    public void validatesRenameCorrectly() {
        assertEquals(ValidationError.SAME_NAME, FolderValidationHelper.validateRename("Projects", "Projects"));
        assertEquals(ValidationError.SAME_NAME, FolderValidationHelper.validateRename("Projects", "projects"));
        assertEquals(ValidationError.ENDS_WITH_PERIOD_OR_SPACE,
                FolderValidationHelper.validateRename("Projects", "  projects  "));
        assertEquals(ValidationError.NONE, FolderValidationHelper.validateRename("OldName", "NewName"));
        assertEquals(ValidationError.INVALID_CHARACTERS, FolderValidationHelper.validateRename("OldName", "New/Name"));
    }

    @Test
    public void buildsChildPathCorrectly() {
        assertEquals("/child", FolderValidationHelper.buildChildPath("/", "child"));
        assertEquals("/child", FolderValidationHelper.buildChildPath("", "child"));
        assertEquals("/child", FolderValidationHelper.buildChildPath(null, "child"));
        assertEquals("/parent/child", FolderValidationHelper.buildChildPath("/parent", "child"));
        assertEquals("/parent/child", FolderValidationHelper.buildChildPath("/parent/", "child"));
        assertEquals("/parent/child", FolderValidationHelper.buildChildPath("parent", "child"));
        assertEquals("/parent/sub/child", FolderValidationHelper.buildChildPath("/parent/sub", "child"));
        assertThrows(IllegalArgumentException.class,
                () -> FolderValidationHelper.buildChildPath("/parent", "bad/name"));
    }

    @Test
    public void extractsParentPathCorrectly() {
        assertEquals("/", FolderValidationHelper.getParentPath("/"));
        assertEquals("/", FolderValidationHelper.getParentPath("/folder"));
        assertEquals("/", FolderValidationHelper.getParentPath("/folder/"));
        assertEquals("/parent", FolderValidationHelper.getParentPath("/parent/child"));
        assertEquals("/a/b", FolderValidationHelper.getParentPath("/a/b/c"));
        assertEquals("/", FolderValidationHelper.getParentPath(null));
        assertEquals("/", FolderValidationHelper.getParentPath(""));
    }

    @Test
    public void extractsNameFromPathCorrectly() {
        assertEquals("", FolderValidationHelper.getNameFromPath("/"));
        assertEquals("folder", FolderValidationHelper.getNameFromPath("/folder"));
        assertEquals("folder", FolderValidationHelper.getNameFromPath("/folder/"));
        assertEquals("child", FolderValidationHelper.getNameFromPath("/parent/child"));
        assertEquals("c", FolderValidationHelper.getNameFromPath("/a/b/c"));
        assertEquals("", FolderValidationHelper.getNameFromPath(null));
        assertEquals("", FolderValidationHelper.getNameFromPath(""));
    }
}
