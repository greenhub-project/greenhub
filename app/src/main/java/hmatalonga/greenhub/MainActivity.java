package hmatalonga.greenhub;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.os.PowerProfileHelper;

import java.net.InetAddress;

import hmatalonga.greenhub.fragments.HomeFragment;
import hmatalonga.greenhub.fragments.HomeFragment;
import hmatalonga.greenhub.protocol.RegisterHandler;
import hmatalonga.greenhub.sampling.Inspector;
import hmatalonga.greenhub.utils.FontManager;
import hmatalonga.greenhub.utils.NetworkWatcher;

public class MainActivity extends AppCompatActivity {
    private static GreenHub app;
    private static Context context;

    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        context = getApplicationContext();
        app = new GreenHub(context);
        app.initModules();

        final String androidId = Inspector.getAndroidId(context);
        final String msg = "Hello";

        Typeface iconFont = FontManager.getTypeface(getApplicationContext(), FontManager.FONTAWESOME);
        FontManager.markAsIconContainer(findViewById(R.id.main_container), iconFont);

//        TextView textViewAndroidId = (TextView) findViewById(R.id.textViewAndroidId);
//        assert textViewAndroidId != null;
//
//        textViewAndroidId.setText("GreenHub Id: " + androidId);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (NetworkWatcher.hasInternet(context)) {
//                    app.device = app.registerHandler.registerClient();
//                }
//                Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new HomeFragment(), "Home");
        viewPager.setAdapter(adapter);
    }
}
