package ch.ti8m.iotmeetsbusiness.util;

/**
 * Created by sa005 on 15.04.2016.
 */
public class ThingSpeakHelper {

    private static final String LOG_TAG = "ThingSpeakHelper";
    private static final String url = "https://api.thingspeak.com/channels";
    private static final String apiKey = "4PULA5YY773OR5A9";

    public static String getApiKey() {
        return apiKey;
    }

    public static String getUrl() {
        return url;
    }
}
