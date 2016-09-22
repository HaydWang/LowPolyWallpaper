package com.droidrise.lowpolywallpaper;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.uniquestudio.lowpoly.LowPoly;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static android.support.v4.content.FileProvider.getUriForFile;

public class MainActivity extends AppCompatActivity {
    public final static String PRES_NAME = "prefs_lowpolywallpaper";
    public final static String PRES_ORIGINAL_BACKUP = "prefs_backup";

    private final static int PICK_PICTURE = 10;

    private WallpaperManager wallpaperManager;
    private Bitmap backupWallpaper;
    private Bitmap originalBitmap;
    private Bitmap polyBitmap;
    private ImageView imageView;

    private ProgressBar progressBar;
    private FloatingActionButton fab_apply;
    private SeekBar seekBar;
    private Snackbar snackBar;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        wallpaperManager = WallpaperManager.getInstance(this);
        originalBitmap = ((BitmapDrawable) wallpaperManager.getDrawable()).getBitmap();
        imageView = (ImageView) findViewById(R.id.iv);
        imageView.setImageBitmap(originalBitmap);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (originalBitmap != null) {
                        imageView.setImageBitmap(originalBitmap);
                    }
                    return true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (polyBitmap != null) {
                        imageView.setImageBitmap(polyBitmap);
                    }
                    return true;
                }
                return false;
            }
        });

//        SharedPreferences prefs = getSharedPreferences(
//                MainActivity.PRES_NAME, Context.MODE_PRIVATE);
//        if (!prefs.getBoolean(PRES_ORIGINAL_BACKUP, false)) {
//            // Backup original wallpaper
//            String date = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.getDefault()).format(new java.util.Date());
//            MediaStore.Images.Media.insertImage(
//                    getContentResolver(), originalBitmap,
//                    "Wallpaper_" + date + ".png", "Wallpaper");
//            prefs.edit().putBoolean(PRES_ORIGINAL_BACKUP, true).apply();
//        }

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        fab_apply = (FloatingActionButton) findViewById(R.id.fab_apply);
        fab_apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab_apply.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                if (backupWallpaper == null) backupWallpaper = originalBitmap;
                if (setPolyWallpaper(polyBitmap != null ? polyBitmap : originalBitmap)) {
                    snackBar = Snackbar.make(view, getString(R.string.wallpaper_applied), Snackbar.LENGTH_LONG)
                            .setActionTextColor(Color.MAGENTA)
                            .setAction(getString(R.string.recall), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    progressBar.setVisibility(View.VISIBLE);
                                    if (snackBar != null) {
                                        snackBar.dismiss();
                                    }

                                    setPolyWallpaper(backupWallpaper);
                                    imageView.setImageBitmap(backupWallpaper);
                                    originalBitmap = backupWallpaper;
                                    backupWallpaper = null;
                                    progressBar.setVisibility(View.INVISIBLE);
                                    Snackbar.make(view, getString(R.string.recalled), Snackbar.LENGTH_SHORT).show();
                                }
                            });
                    snackBar.show();
                } else {
                    Snackbar.make(view, getString(R.string.wallpaper_applied_failed), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                progressBar.setVisibility(View.INVISIBLE);
                fab_apply.setEnabled(true);
            }
        });

        FloatingActionButton fabShare = (FloatingActionButton) findViewById(R.id.fab_share);
        fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (polyBitmap != null) {
                    Uri uri = polyToCache();
                    if (uri != null) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        shareIntent.setDataAndType(uri, getContentResolver().getType(uri));
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        Intent intent = Intent.createChooser(shareIntent, getString(R.string.share));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            }
        });

        FloatingActionButton fabReload = (FloatingActionButton) findViewById(R.id.fab_reload);
        fabReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backupWallpaper = null;
                originalBitmap = ((BitmapDrawable) wallpaperManager.getDrawable()).getBitmap();
                imageView.setImageBitmap(originalBitmap);
                Snackbar.make(view, getString(R.string.wallpaper_reload), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        FloatingActionButton fabOpen = (FloatingActionButton) findViewById(R.id.fab_open);
        fabOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_PICTURE);
            }
        });

        FloatingActionButton fabAbout = (FloatingActionButton) findViewById(R.id.fab_about);
        fabAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });

        seekBar = (SeekBar) findViewById(R.id.seekbar_triangle);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing during changing.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Apply triangle parameter
                // Available range is 50 ~ 6000
                int i = progress;
                if (i < 100) {
                    i = i/2;
                } else if (i <= 200) {
                    i = 50 + (i - 100) * 2;
                } else if (i <= 300) {
                    i = 250 + (i - 200) * 4;
                } else if (i <= 400) {
                    i = 650 + (i - 300) * 8;
                }

                generatePoly(i);
            }
        });
        seekBar.setProgress(50);
        generatePoly(25);
    }

    private void generatePoly(final int i) {
        fab_apply.setEnabled(false);
        seekBar.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        new PolyTask().execute(i);
    }

    private Uri polyToCache() {
        if (polyBitmap != null) {
            try {
                File folder = new File(getCacheDir(), "cache");
                if (!folder.exists()) {
                    if (!folder.mkdirs()) {
                        return null;
                    }
                } else {
                    for (File child : folder.listFiles()) {
                        child.delete();
                    }
                }

                File file = new File(folder, String.valueOf(System.currentTimeMillis()) + ".jpg");
                FileOutputStream ostream = new FileOutputStream(file);
                polyBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                ostream.close();

                return getUriForFile(this, "com.droidrise.lowpolywallpaper.fileprovider", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private class PolyTask extends AsyncTask<Integer, Integer, Void> {
        @Override
        protected Void doInBackground(Integer... i) {
            if (i[0] == 0) {
                polyBitmap = originalBitmap;
            } else {
                long lastTime = System.currentTimeMillis();
                polyBitmap = LowPoly.generate(originalBitmap, i[0]);
                Log.d("TAG", "a22460. poly: " + i[0] + " time:" + (System.currentTimeMillis() - lastTime) + "ms");
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            imageView.setImageBitmap(polyBitmap);
            progressBar.setVisibility(View.INVISIBLE);
            seekBar.setEnabled(true);
            fab_apply.setEnabled(true);
        }
    }

    private static final int MY_PERMISSIONS_REQUEST_SET_WALLPAPER = 1;
    private boolean setPolyWallpaper(Bitmap bitmap) {
        if (bitmap == null) return false;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SET_WALLPAPER},
                    MY_PERMISSIONS_REQUEST_SET_WALLPAPER);
        } else {
            try {
                wallpaperManager.setBitmap(bitmap);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SET_WALLPAPER: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setPolyWallpaper(polyBitmap != null ? polyBitmap : originalBitmap);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_PICTURE:
                if (resultCode == RESULT_OK) {
                    backupWallpaper = ((BitmapDrawable) wallpaperManager.getDrawable()).getBitmap();
                    Uri uri = data.getData();
                    try {
                        InputStream input = getContentResolver().openInputStream(uri);
                        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
                        onlyBoundsOptions.inSampleSize = 4;
                        originalBitmap = BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
                        input.close();
                        seekBar.setProgress(50);
                        generatePoly(25);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}
