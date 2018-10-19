
package studios.xhedra.vaid;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ColorSpace;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import studios.xhedra.vaid.Symptom;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class Chatbot extends AppCompatActivity implements AIListener {
    RequestQueue mRequestQueue = null;
    private Button listenButton;
    private TextView resultTextView;
    private AIService aiService;
    private static final String TAG = "Chatbot";
    private ListView lv;
    private ArrayList<Symptom> symptomArrayList;
    private CustomAdapter customAdapter;
    private Button btnselect, btndeselect, btnnext;
    private  String[] symptomlist = new String[]{"Cough", "Cold", "Fever", "Headache", "Stomach Ache"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);
        listenButton = (Button) findViewById(R.id.listenButton);
        resultTextView = (TextView) findViewById(R.id.resultTextView);
        final AIConfiguration config = new AIConfiguration("0c2fafd54b244d64858e6ba7ccd6edc0",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);
        aiService.setListener(this);
        listenButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                requestRecordAudioPermission();

            }

        });
        lv = (ListView) findViewById(R.id.lv);
        btnselect = (Button) findViewById(R.id.select);
        btndeselect = (Button) findViewById(R.id.deselect);
        btnnext = (Button) findViewById(R.id.next);

        symptomArrayList = getSymptom(false);
        customAdapter = new CustomAdapter(this,symptomArrayList);
        lv.setAdapter(customAdapter);

        btnselect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                symptomArrayList = getSymptom(true);
                customAdapter = new CustomAdapter(Chatbot.this,symptomArrayList);
                lv.setAdapter(customAdapter);
            }
        });
        btndeselect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                symptomArrayList = getSymptom(false);
                customAdapter = new CustomAdapter(Chatbot.this,symptomArrayList);
                lv.setAdapter(customAdapter);
            }
        });
        btnnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Integer> selectedSymptomesIndexes = customAdapter.getSelectedList();
                HashMap<String,String> params = new HashMap<>();
                for(int i = 0; i < selectedSymptomesIndexes.size(); i++){
                    params.put("Sym" + i , symptomlist[i]);
                }
                Toast.makeText(getApplicationContext(),params.toString(),Toast.LENGTH_LONG).show();

                final String URL = "http://192.168.137.1:8082/api";
                // Post params to be sent to the server

                JsonObjectRequest req = new JsonObjectRequest(URL, new JSONObject(params),
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Toast.makeText(getApplicationContext(),"Symptonic Response", Toast.LENGTH_LONG).show();
                            }

                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.e("Error: ", error.getMessage());
                    }
                });



                // add the request object to the queue to be executed
                if (mRequestQueue == null) {
                    mRequestQueue = Volley.newRequestQueue(getApplicationContext());
                }
                mRequestQueue.add(req);

            }
        });
    }
    private ArrayList<Symptom> getSymptom(boolean isSelect){
        ArrayList<Symptom> list = new ArrayList<>();
        for(int i = 0; i < 4; i++){

            Symptom symptom = new Symptom();
            symptom.setSelected(isSelect);
            symptom.setAnimal(symptomlist[i]);
            list.add(symptom);
        }
        return list;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestRecordAudioPermission() {

        String requiredPermission = Manifest.permission.RECORD_AUDIO;

        // If the user previously denied this permission then show a message explaining why
        // this permission is needed
        if (this.checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED) {
            aiService.startListening();
        } else {

            Toast.makeText(this, "This app needs to record audio through the microphone....", Toast.LENGTH_SHORT).show();
            requestPermissions(new String[]{requiredPermission}, 101);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == 101 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            aiService.startListening();
        }

    }

    @Override
    public void onResult(AIResponse response) {
        Result result = response.getResult();
        Log.d(TAG, "onResult: " + response.toString());
        // Get parameters
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }

        // Show results in TextView.
        //resultTextView.setText("Query:" + result.getResolvedQuery() +
        //        "\nAction: " + result.getAction() +
        //       "\nParameters: " + parameterString);
        final String URL = "https://api.dialogflow.com/v1/query?v=20150910";
        // Post params to be sent to the server
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("query", result.getResolvedQuery());
        params.put("lang", "en");
        params.put("sessionId", "12345");

        JsonObjectRequest req = new JsonObjectRequest(URL, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject results = response.getJSONObject("result");
                            resultTextView.setText(results.getJSONObject("fulfillment").getString("speech"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        }){
            @Override
            public Map<String, String> getHeaders () {

                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer 0c2fafd54b244d64858e6ba7ccd6edc0");
                return headers;
            }


        };



        // add the request object to the queue to be executed
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        mRequestQueue.add(req);

    }



    @Override
    public void onError(AIError error) {
        resultTextView.setText(error.toString());
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }
}