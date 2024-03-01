package org.syncninja.util;

public enum ResourceBundleEnum {

    DIRECTORY_INITIALIZED_SUCCESSFULLY("directory_initialized_successfully"),
    DIRECTORY_ALREADY_INITIALIZED("directory_already_initialized"),
    INVALID_INPUT("invalid_input"),
    DIRECTORY_NOT_INITIALIZED("directory_not_initialized"),
    BRANCH_NAME_EXISTS("branch_name_exists"),
    FILE_ALREADY_EXISTS("file_already_exists"),
    SUB_DIRECTORY_ALREADY_EXISTS("sub_directory_already_exists"),
    FILES_READY_TO_BE_COMMITTED("files_to_be_committed"),
    FILE_NOT_FOUND("File_is_not_found!"),
    UNTRACKED_FILES("Untracked_files"),
    SUCCESSFULLY_ADDED("successfully_added");



    private final String key;

    ResourceBundleEnum(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}