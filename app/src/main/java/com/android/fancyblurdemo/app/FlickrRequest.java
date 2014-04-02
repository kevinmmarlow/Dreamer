package com.android.fancyblurdemo.app;

import com.android.fancyblurdemo.volley.NetworkResponse;
import com.android.fancyblurdemo.volley.ParseError;
import com.android.fancyblurdemo.volley.Response;
import com.android.fancyblurdemo.volley.toolbox.HttpHeaderParser;
import com.android.fancyblurdemo.volley.toolbox.JsonRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by kevin.marlow on 3/20/14.
 */
public class FlickrRequest extends JsonRequest<List<FlickrPhoto>> {

    private final String jsonReqsponseWrapper = "jsonFlickrApi(";
    private final boolean mIsHighRes;

    public FlickrRequest(String url, boolean isHighRes, Response.Listener<List<FlickrPhoto>> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        mIsHighRes = isHighRes;
    }

    @Override
    protected Response<List<FlickrPhoto>> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            String trimmedString = jsonString.substring(jsonReqsponseWrapper.length(), jsonString.length() - 1);
            JSONObject rootObj = new JSONObject(trimmedString);
            JSONObject photosObj = rootObj.getJSONObject("photos");
            JSONArray photosArray = photosObj.getJSONArray("photo");

            List<FlickrPhoto> photos = new ArrayList<FlickrPhoto>();

            for (int i = 0; i < photosArray.length(); i++) {
                JSONObject obj = photosArray.getJSONObject(i);
                FlickrPhoto photo = new FlickrPhoto();
                photo.id = obj.getString("id");
                photo.owner = obj.getString("owner");
                photo.secret = obj.getString("secret");
                photo.server = obj.getString("server");
                photo.farm = obj.getInt("farm");
                photo.title = obj.getString("title").trim().toUpperCase(Locale.US);
                photo.isPublic = obj.getInt("ispublic") != 0;
                photo.isFriend = obj.getInt("isfriend") != 0;
                photo.isFamily = obj.getInt("isfamily") != 0;
                String base = "http://farm" + photo.farm + ".staticflickr.com/" + photo.server + "/" + photo.id + "_" + photo.secret;
                photo.photoUrl = base + "_z.jpg";
                photo.highResUrl = base + "_b.jpg";

                if (photo.title.contains("...")) {
                    photo.title = photo.title.replace("...", "... ");
                }
                photo.title = photo.title.replaceAll("[^\\x00-\\x7f]+", "").trim();

                String[] split = photo.title.split(" ");
                String newTitle = "";
                String longest = "";
                if (split.length != 0) {
                    int avgCharCount = Math.round(photo.title.length() / split.length);
                    if (avgCharCount > 14) {
                        avgCharCount /= 2;
                    }

                    for (String string : split) {
                        if (string.length() > ((mIsHighRes ? 3 : 2) * avgCharCount)) {
                            String part1 = string.substring(0, avgCharCount);
                            String part2 = string.substring(avgCharCount, string.length());
                            newTitle = newTitle + " " + part1 + "\n" + part2;
                            if (part1.length() > longest.length() || part2.length() > longest.length()) {
                                longest = (part1.length() > part2.length()) ? part1 : part2;
                            }
                        } else {
                            newTitle = newTitle + " " + string;
                            if (string.length() > longest.length()) {
                                longest = string;
                            }
                        }
                    }
                }

                int count = longest.length();
                longest = "";
                for (int j = count; j > 1; j--) {
                    longest += "W";
                }

                photo.title = newTitle.trim();
                photo.preferredWidthStr = longest;
                photos.add(photo);
            }

            return Response.success(photos, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
}
