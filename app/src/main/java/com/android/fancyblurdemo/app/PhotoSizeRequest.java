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
public class PhotoSizeRequest extends JsonRequest<String> {

    private final String jsonReqsponseWrapper = "jsonFlickrApi(";

    public PhotoSizeRequest(String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            String trimmedString = jsonString.substring(jsonReqsponseWrapper.length(), jsonString.length() - 1);
            JSONObject rootObj = new JSONObject(trimmedString);
            JSONObject sizesObj = rootObj.getJSONObject("sizes");
            JSONArray sizeArray = sizesObj.getJSONArray("size");

            String largeImageUrl = null;

            for (int i = 0; i < sizeArray.length(); i++) {
                JSONObject obj = sizeArray.getJSONObject(i);
                if (obj.getString("label").equals("Large")) {
                    largeImageUrl = obj.getString("source");
                    break;
                }
            }

            return Response.success(largeImageUrl, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
}
