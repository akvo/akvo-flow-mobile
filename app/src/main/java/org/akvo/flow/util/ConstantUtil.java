/*
 *  Copyright (C) 2010-2014 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.util;

/**
 * Class to hold all public constants used in the application
 * 
 * @author Christopher Fagiani
 */
public class ConstantUtil {
    /**
     * file system constants
     */
    public static final String FILE_SURVEY_LOCATION_TYPE = "file";
    public static final String ARCHIVE_SUFFIX = ".zip";
    public static final String IMAGE_SUFFIX = ".jpg";
    public static final String VIDEO_SUFFIX = ".mp4";
    public static final String XML_SUFFIX = ".xml";
    public static final String BOOTSTRAP_DB_FILE = "dbinstructions.sql";
    public static final String PROCESSED_OK_SUFFIX = ".processed";
    public static final String PROCESSED_ERROR_SUFFIX = ".error";
    public static final String BOOTSTRAP_ROLLBACK_FILE = "rollback.sql";
    public static final String STACKTRACE_FILENAME = "err-";
    public static final String STACKTRACE_SUFFIX = ".stacktrace";

    /**
     * survey file locations
     */
    public static final String RESOURCE_LOCATION = "res";
    public static final String FILE_LOCATION = "sdcard";

    /**
     * size limits
     */
    public static final String SPACE_WARNING_MB_LEVELS = "100, 50, 25, 10, 5, 1";

    /**
     * question types
     */
    public static final String FREE_QUESTION_TYPE = "free";
    public static final String OPTION_QUESTION_TYPE = "option";
    public static final String GEO_QUESTION_TYPE = "geo";
    public static final String VIDEO_QUESTION_TYPE = "video";
    public static final String PHOTO_QUESTION_TYPE = "photo";
    public static final String SCAN_QUESTION_TYPE = "scan";
    public static final String STRENGTH_QUESTION_TYPE = "strength";
    public static final String DATE_QUESTION_TYPE = "date";

    /**
     * help types
     */
    public static final String VIDEO_HELP_TYPE = "video";
    public static final String IMAGE_HELP_TYPE = "image";
    public static final String TIP_HELP_TYPE = "tip";

    /**
     * rendering options
     */
    public static final String SPINNER_RENDER_MODE = "spinner";
    //public static final String RADIO_RENDER_MODE = "radio";

    /**
     * response types
     */
    public static final String VALUE_RESPONSE_TYPE = "VALUE";
    public static final String IMAGE_RESPONSE_TYPE = "IMAGE";
    public static final String VIDEO_RESPONSE_TYPE = "VIDEO";
    public static final String GEO_RESPONSE_TYPE = "GEO";
    public static final String DATE_RESPONSE_TYPE = "DATE";
    public static final String OTHER_RESPONSE_TYPE = "OTHER";

    /**
     * validation types
     */
    public static final String NUMERIC_VALIDATION_TYPE = "numeric";
    public static final String NAME_VALIDATION_TYPE = "name";

    /**
     * scoring types
     */
    public static final String NUMERIC_SCORING = "numeric";
    public static final String TEXT_MATCH_SCORING = "textmatch";

    /**
     * survey types
     */
    public static final String SURVEY_TYPE = "survey";

    /**
     * media question support
     */
    public static final String MEDIA_FILE_KEY = "filename";

    /**
     * keys for saved state and bundle extras
     */
    public static final String USER_ID_KEY = "UID";
    public static final String SURVEY_ID_KEY = "SID";
    public static final String ID_KEY = "_id";
    public static final String RESPONDENT_ID_KEY = "survey_respondent_id";
    public static final String IMAGE_URL_LIST_KEY = "imageurls";
    public static final String IMAGE_CAPTION_LIST_KEY = "imagecaps";
    public static final String READONLY_KEY = "readonly";
    public static final String SINGLE_SURVEY_KEY = "single_survey";
    public static final String SURVEY_GROUP = "survey_group";
    public static final String SURVEYED_LOCALE_ID = "surveyed_locale_id";

    /**
     * settings keys
     */
    public static final String SURVEY_LANG_SETTING_KEY = "survey.language";
    public static final String SURVEY_LANG_PRESENT_KEY = "survey.languagespresent";
    public static final String USER_SAVE_SETTING_KEY = "user.storelast";
    public static final String CELL_UPLOAD_SETTING_KEY = "data.cellular.upload";
    public static final String LAST_USER_SETTING_KEY = "user.lastuser.id";
    public static final String LOCATION_BEACON_SETTING_KEY = "location.sendbeacon";
    public static final String SERVER_SETTING_KEY = "backend.server";
    public static final String SCREEN_ON_KEY = "screen.keepon";
    public static final String DEVICE_IDENT_KEY = "device.identifier";
    public static final String MAX_IMG_SIZE = "media.img.maxsize";
    public static final String PREF_LOCALE = "pref.locale";

    /**
     * intents
     */
    public static final String DATA_AVAILABLE_INTENT = "org.akvo.flow.DATA_SUBMITTED";
    public static final String GPS_STATUS_INTENT = "com.eclipsim.gpsstatus.VIEW";
    public static final String BARCODE_SCAN_INTENT = "com.google.zxing.client.android.SCAN";

    /**
     * zxing barcode extra keys
     */
    public static final String BARCODE_CONTENT = "SCAN_RESULT";

    /**
     * language codes
     */
    public static final String ENGLISH_CODE = "en";

    /**
     * html colors
     */
    public static final String WHITE_COLOR = "white";
    public static final String BLACK_COLOR = "black";

    /**
     * "code" to prevent unauthorized use of administrative settings/preferences
     */
    public static final String ADMIN_AUTH_CODE = "12345";

    /**
     * property file keys
     */
    public static final String S3_ID = "s3Id";
    public static final String DATA_S3_POLICY = "dataS3Policy";
    public static final String DATA_S3_SIG = "dataS3Sig";
    public static final String IMAGE_S3_POLICY = "imageS3Policy";
    public static final String IMAGE_S3_SIG = "imageS3Sig";
    public static final String DATA_UPLOAD_URL = "dataUploadUrl";
    public static final String SERVER_BASE = "serverBase";
    public static final String SURVEY_S3_URL = "surveyS3Url";
    public static final String API_KEY = "apiKey";

    /**
     * resource related constants
     */
    public static final String RESOURCE_PACKAGE = "org.akvo.flow";
    public static final String RAW_RESOURCE = "raw";
    
    /**
     * SurveyedLocale meta question IDs. Negative IDs to avoid collisions.
     * Irrelevant for the server side, they are used to identify a locale meta-data 
     * response among the rest of the 'real' question answers
     */
    public static final String QUESTION_LOCALE_NAME = "-1";
    public static final String QUESTION_LOCALE_GEO = "-2";
    
    /**
     * Order By
     */
    public static final int ORDER_BY_NONE     = -1;
    public static final int ORDER_BY_DATE     = 0;
    public static final int ORDER_BY_DISTANCE = 1;
    public static final int ORDER_BY_STATUS   = 2;
    public static final int ORDER_BY_NAME     = 3;

    /**
     * Max picture size
     * Values must match the ones set in arrays.
     * TODO: Preferences should be managed with SharedPreferences api, to avoid this error prone references
     */
    public static final int IMAGE_SIZE_320_240  = 0;
    public static final int IMAGE_SIZE_640_480  = 1;
    public static final int IMAGE_SIZE_1280_960 = 2;

    public static final int NOTIFICATION_RECORD_SYNC = 100;
    public static final int NOTIFICATION_DATA_SYNC = 101;

    /**
     * prevent instantiation
     */
    private ConstantUtil() {
    }

}
