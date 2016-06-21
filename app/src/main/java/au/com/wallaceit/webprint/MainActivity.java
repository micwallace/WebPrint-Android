package au.com.wallaceit.webprint;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
    private Button serverbtn;
    private TextView stattxt;
    private EditText sourceport;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);
        serverbtn = (Button) findViewById(R.id.serverbtn);
        stattxt = (TextView) findViewById(R.id.stattxt);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        sourceport = (EditText) findViewById(R.id.sourceport);
        sourceport.setText(prefs.getString("prefsourceport", "8080"));
        sourceport.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit().putString("prefsourceport", sourceport.getText().toString()).apply();
            }
        });

        boolean serverstarted = isServiceRunning(RelayService.class);
        setButton(serverstarted);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startService(){
        Intent sintent = new Intent(MainActivity.this, RelayService.class);
        sintent.putExtra("sourceport", ((TextView) findViewById(R.id.sourceport)).getText().toString());
        startService(sintent);
        // set new button click
        setButton(true);
    }

    public void stopService(){
        Intent sintent = new Intent(MainActivity.this, RelayService.class);
        stopService(sintent);
        // set new button click
        setButton(false);
    }

    private void setButton(boolean started){
        if (started){
            serverbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopService();
                }
            });
            serverbtn.setText(R.string.stop_server);
            stattxt.setText(R.string.server_running);
        } else {
            serverbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startService();
                }
            });
            serverbtn.setText(R.string.start_server);
            stattxt.setText(R.string.server_stopped);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

}
