package com.hyperether.pointsnaps.utils;

/**
 * Static constants
 *
 * @author Marko Katic
 * @version 1.0 - 07/05/2017
 */

public class Constants {

    // Photo quality
    public static final int PHOTO_WIDTH = 1080;
    public static final int PHOTO_COMPRESSION = 90;

    // Permissions
    public static final int TAG_CODE_PERMISSION_LOCATION = 1;
    public static final int TAG_CODE_PERMISSION_EXTERNAL_STORAGE = 2;
    public static final int TAG_CODE_PERMISSION_EXTERNAL_STORAGE_LOAD = 3;

    // Activity result codes
    public static final int RESULT_LOAD_IMG = 0;
    public static final int RESULT_CAPTURE_IMG = 1;

    // Path
    public static final String FILE_PROVIDER_NAME = ".fileProvider";

    // Fragment tags
    public static final String WRITE_FRAGMENT_TAG = "WriteFragment";
    public static final String LOGIN_FRAGMENT_TAG = "LoginFragment";
    public static final String LOCATION_FRAGMENT_TAG = "LocationFragment";
    public static final String REGISTER_FRAGMENT_TAG = "RegisterFragment";
}
