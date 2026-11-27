package app.shizu;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private WebView web;
    private TextToSpeech voz;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        voz = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) voz.setLanguage(new Locale("pt", "BR"));
        });
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest pedido) {
                if (!HOST.equals(pedido.getUrl().getHost())) return null;
                String nome = pedido.getUrl().getLastPathSegment();
                if (nome == null) nome = "index.html";
                String tipo = nome.endsWith(".html") ? "text/html"
                        : nome.endsWith(".png") ? "image/png" : "application/json";
                try {
                    return new WebResourceResponse(tipo, "UTF-8", getAssets().open(nome));
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest pedido) {
                return !HOST.equals(pedido.getUrl().getHost());
            }
        });
        web.addJavascriptInterface(new Ponte(), "ShizuNativo");
        setContentView(web);
        web.loadUrl("https://" + HOST + "/index.html");
    }

    private class Ponte {
        @JavascriptInterface
        public void falar(String texto) {
            voz.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "shizu");
        }

        @JavascriptInterface
        public void parar() {
            voz.stop();
        }

        @JavascriptInterface
        public void ouvir() {
            runOnUiThread(() -> {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
                try {
                    startActivityForResult(i, 1);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "Sem reconhecimento de voz", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int codigo, int resultado, Intent dados) {
        super.onActivityResult(codigo, resultado, dados);
        if (codigo != 1 || resultado != RESULT_OK || dados == null) return;
        ArrayList<String> frases = dados.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (frases == null || frases.isEmpty()) return;
        web.evaluateJavascript("window.shizuOuviu(" + JSONObject.quote(frases.get(0)) + ")", null);
    }

    @Override
    protected void onDestroy() {
        voz.shutdown();
        super.onDestroy();
    }
}
