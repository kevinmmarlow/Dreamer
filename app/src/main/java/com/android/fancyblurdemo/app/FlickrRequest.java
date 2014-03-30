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

/**
 * Created by kevin.marlow on 3/20/14.
 */
public class FlickrRequest extends JsonRequest<List<FlickrPhoto>> {

    private final String jsonReqsponseWrapper = "jsonFlickrApi(";

    public FlickrRequest(String url, Response.Listener<List<FlickrPhoto>> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
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
                photo.title = obj.getString("title");
                photo.isPublic = obj.getInt("ispublic") != 0;
                photo.isFriend = obj.getInt("isfriend") != 0;
                photo.isFamily = obj.getInt("isfamily") != 0;
                String base = "http://farm" + photo.farm + ".staticflickr.com/" + photo.server + "/" + photo.id + "_" + photo.secret;
                photo.photoUrl = base + "_z.jpg";
                photo.highResUrl = base + "_b.jpg";

                String[] titleArray = photo.title.split(" ");
                int maxLength = 1;
                for (String string : titleArray) {
                    maxLength = Math.max(maxLength, string.length());
                }

                int maxThresh = (int) Math.round(maxLength * 1.25);

                String newTitle = "";
                for (int j = 0; j < titleArray.length; j++) {
                    String first = titleArray[j];
                    int count = 0;
                    for (int k = j + 1; k < titleArray.length; k++) {
                        String toAdd = titleArray[k];
                        if (toAdd.length() == 0) {
                            continue;
                        }
                        if (first.length() + toAdd.length() < maxThresh) {
                            first = first + " " + toAdd;
                            count++;
                        } else {
                            break;
                        }
                    }
                    newTitle = newTitle + first + (j == titleArray.length - 1 ? "" : "\n");
                    j += count;
                }
                photo.title = newTitle;

                photo.lineCount = (int) Math.min(5, Math.ceil(photo.title.length() / maxLength));
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
