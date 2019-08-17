/*
 * Copyright (c) 2019 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.core.app.ActivityCompat;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;

import de.tadris.fitness.data.Workout;
import de.tadris.fitness.location.LocationListener;
import de.tadris.fitness.location.MyLocationOverlay;
import de.tadris.fitness.location.WorkoutRecorder;
import de.tadris.fitness.map.HumanitarianTileSource;

public class RecordWorkoutActivity extends Activity implements LocationListener.LocationChangeListener {

    MapView mapView;
    MyLocationOverlay locationOverlay;
    TileDownloadLayer downloadLayer;
    WorkoutRecorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_workout);

        this.mapView= new MapView(this);

        mapView.setZoomLevelMin((byte) 18);
        mapView.setZoomLevelMax((byte) 18);
        mapView.setBuiltInZoomControls(false);

        TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache", mapView.getModel().displayModel.getTileSize(), 1f, this.mapView.getModel().frameBufferModel.getOverdrawFactor(), true);

        HumanitarianTileSource tileSource = HumanitarianTileSource.INSTANCE;
        tileSource.setUserAgent("mapsforge-android");
        downloadLayer = new TileDownloadLayer(tileCache, mapView.getModel().mapViewPosition, tileSource, AndroidGraphicFactory.INSTANCE);

        mapView.getLayerManager().getLayers().add(downloadLayer);

        locationOverlay= new MyLocationOverlay(Instance.getInstance(this).locationListener, getDrawable(R.drawable.location_marker));

        mapView.getLayerManager().redrawLayers();

        mapView.setZoomLevel((byte) 18);
        mapView.setCenter(new LatLong(52, 13));

        ((ViewGroup)findViewById(R.id.recordMapViewrRoot)).addView(mapView);

        checkPermissions();

        recorder= new WorkoutRecorder(this, Workout.WORKOUT_TYPE_RUNNING);
        recorder.start();
    }

    private void stopAndSave(){
        recorder.stop();
        if(recorder.getSampleCount() > 3){
            recorder.save();
        }
        finish();
    }

    void checkPermissions(){
        if (!hasPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
        }
    }

    public boolean hasPermission(){
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (hasPermission()) {
            Instance.getInstance(this).locationListener.enableMyLocation();
        }
    }

    @Override
    public void onLocationChange(Location location) {
        mapView.getModel().mapViewPosition.animateTo(LocationListener.locationToLatLong(location));
        locationOverlay.setPosition(location.getLatitude(), location.getLongitude(), location.getAccuracy());
    }

    @Override
    protected void onDestroy() {
        recorder.stop();
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }

    @Override
    public void onPause(){
        super.onPause();
        downloadLayer.onPause();
        Instance.getInstance(this).locationListener.unregisterLocationChangeListeners(this);
    }

    public void onResume(){
        super.onResume();
        downloadLayer.onResume();
        Instance.getInstance(this).locationListener.registerLocationChangeListeners(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.record_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.actionRecordingStop){
            stopAndSave();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
