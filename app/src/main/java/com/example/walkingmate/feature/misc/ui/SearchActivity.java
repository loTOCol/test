package com.example.walkingmate.feature.misc.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.walkingmate.R;

public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        WebView webView=findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new BridgeInterface(), "Android");

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.loadUrl("javascript:sample2_execDaumPostcode();");
            }
        });


        //처음 웹뷰 로드
        webView.loadUrl("https://walkingmate-f1d2e.web.app");
    }

    private class BridgeInterface {
        @JavascriptInterface
        public void processDATA(String data){
            //다음 주소 검색 결과 값이 브릿지 통로를 통해 전달 받음(js로 부터)
            Intent intent=new Intent();
            intent.putExtra("data", data);
            setResult(RESULT_OK, intent);
            finish();
        }

    }
}
