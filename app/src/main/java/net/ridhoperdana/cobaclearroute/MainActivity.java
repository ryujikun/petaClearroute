package net.ridhoperdana.cobaclearroute;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.geocoder.ui.GeocoderAutoCompleteView;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;
import com.mapbox.services.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.geocoding.v5.models.CarmenFeature;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    MapView mapbox;
    MapboxMap map;
    private LocationManager manager;
    private Location mLastLocation;
    private Geojson json_asli = new Geojson();
//    private Geojson json_alt_1 = new Geojson();
//    private Geojson json_alt_2 = new Geojson();
    private LatLng target_lokasi;
    private LatLng asal_lokasi;
    private ArrayList<Json> coba_json = new ArrayList<>();
    private ArrayList<LatLng> latLngs = new ArrayList<>();
    private Json json_1 = new Json();
//    private Position pos;
//    private GeoJSONObject jsonResponse;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapboxAccountManager.start(this.getApplicationContext(), getString(R.string.access_token));
        setContentView(R.layout.relative_main);

        checkPermission();
        getLocation();
        mapbox = (MapView)findViewById(R.id.mapview);

        mapbox.onCreate(savedInstanceState);

        mapbox.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                        .zoom(15)
                        .tilt(15)
                        .build()
                ));

                MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                        .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                map.addMarker(markerViewOptions);
                asal_lokasi = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            }
        });

        // Deklarasi fungsi Geocoder MapboxAPI
        GeocoderAutoCompleteView autocomplete = (GeocoderAutoCompleteView) findViewById(R.id.autocomplete);

        // Memanggil akses token mapbox api
        autocomplete.setAccessToken(MapboxAccountManager.getInstance().getAccessToken());

        autocomplete.setType(GeocodingCriteria.TYPE_POI);
        autocomplete.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
            @Override
            public void OnFeatureClick(CarmenFeature feature) {
                Position position = feature.asPosition();
                //mendapatkan koordinat tujuan
                target_lokasi = new LatLng(position.getLatitude(), position.getLongitude());

                //memperbaharui peta agar menampilkan lokasi tujuan
                updateMap(position.getLatitude(), position.getLongitude());

                //memanggil fungsi pencarian rute
                getRoute(asal_lokasi, target_lokasi);
            }
        });
    }

    private Icon markerIcon(String golongan)
    {
        Drawable iconDrawable = null;
        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
        if(golongan.equals("target"))
        {
            iconDrawable = ContextCompat.getDrawable(MainActivity.this, R.mipmap.location_blue);
        }
        else if(golongan.equals("berawan"))
        {
            iconDrawable = ContextCompat.getDrawable(MainActivity.this, R.mipmap.cuaca_berawan_2);
        }
        else if(golongan.equals("cerah"))
        {
            iconDrawable = ContextCompat.getDrawable(MainActivity.this, R.mipmap.cuaca_cerah_2);
        }
        else if(golongan.equals("hujan_ringan"))
        {
            iconDrawable = ContextCompat.getDrawable(MainActivity.this, R.mipmap.cuaca_hujan_ringan_2);
        }
        Icon icon = iconFactory.fromDrawable(iconDrawable);
        return icon;
    }

    private void updateMap(double latitude, double longitude) {
        // Build marker
        map.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title("Geocoder result")
                .icon(markerIcon("target")));

        // Animate camera to geocoder result location
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(15)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
    }

    private void getRoute(LatLng latLngAsal, LatLng latLngTujuan)
    {
        //pemanggilan url dari Clearroute API
        StringBuilder urlbaru = new StringBuilder("http://riset.alpro.if.its.ac.id/clearroute/public/index.php/getroutecuaca1/");

        //memanggil longitude asal
        urlbaru.append(latLngAsal.getLongitude()+"/");

        //memanggil latitude asal
        urlbaru.append(latLngAsal.getLatitude()+"/");

        //memanggil longitude tujuan
        urlbaru.append(latLngTujuan.getLongitude()+"/");

        //memanggil latitude asal
        urlbaru.append(latLngTujuan.getLatitude()+"/");

        //deklarasi fungsi retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://clearroute.net")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GetRoute service = retrofit.create(GetRoute.class);
        Call<JsonElement> call = service.getRoute(urlbaru.toString());
        Log.d("url= ", urlbaru.toString());

        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                JsonElement jsonElement = response.body();
                JsonObject jsonArray = jsonElement.getAsJsonObject();
                JsonArray jsonArray1 = jsonArray.get("0").getAsJsonArray();
                JsonArray jsonArray2 = jsonArray.get("1").getAsJsonArray();
                JsonArray jsonArray3 = jsonArray.get("2").getAsJsonArray();
                JsonArray jsonArray1_cuaca = jsonArray.get("cuaca1").getAsJsonArray();
                JsonArray jsonArray2_cuaca = jsonArray.get("cuaca2").getAsJsonArray();
                JsonArray jsonArray3_cuaca = jsonArray.get("cuaca3").getAsJsonArray();
                Double weight_cuaca1 = 0.0;
                Double weight_cuaca2 = 0.0;
                Double weight_cuaca3 = 0.0;
                int cek_cuaca1 = 0;
                int cek_cuaca2 = 0;
                int cek_cuaca3 = 0;
                int cek_jalur1 = 0;
                int cek_jalur2 = 0;
                int cek_jalur3 = 0;
                Log.d("array: ", String.valueOf(jsonArray1_cuaca.size()));

                for(int c = 0; c <jsonArray1_cuaca.size()-1 ; c++)
                {
                    try {
                        Log.d("weight 1: ", jsonArray1_cuaca.get(jsonArray1_cuaca.size()-1).getAsJsonObject().get("weightcurr").getAsString());
                        Double weightcurr = Double.valueOf(jsonArray1_cuaca.get(jsonArray1_cuaca.size()-1).getAsJsonObject().get("weightcurr").getAsString());
//                        Double weightforecast = Double.valueOf(jsonArray1_cuaca.get(c).getAsJsonObject().get("weightfrcst").getAsString());
                        weight_cuaca1 = weightcurr;
                        Log.d("weight 1: ", String.valueOf(weight_cuaca1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                for(int c = 0; c <jsonArray2_cuaca.size()-1 ; c++)
                {
                    try {
                        Log.d("weight 2: ", jsonArray2_cuaca.get(jsonArray2_cuaca.size()-1).getAsJsonObject().get("weightcurr").getAsString());
                        Double weightcurr = Double.valueOf(jsonArray2_cuaca.get(jsonArray2_cuaca.size()-1).getAsJsonObject().get("weightcurr").getAsString());
//                        Double weightforecast = Double.valueOf(jsonArray2_cuaca.get(c).getAsJsonObject().get("weightfrcst").getAsString());
                        weight_cuaca2 = weightcurr;
                        Log.d("weight 2: ", String.valueOf(weight_cuaca2));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                for(int c = 0; c <jsonArray3_cuaca.size()-1 ; c++)
                {
                    try {
                        Log.d("weight 3: ", jsonArray3_cuaca.get(jsonArray3_cuaca.size()-1).getAsJsonObject().get("weightcurr").getAsString());
                        Double weightcurr = Double.valueOf(jsonArray3_cuaca.get(jsonArray3_cuaca.size()-1).getAsJsonObject().get("weightcurr").getAsString());
//                        Double weightforecast = Double.valueOf(jsonArray3_cuaca.get(c).getAsJsonObject().get("weightfrcst").getAsString());
                        weight_cuaca3 = weightcurr;
                        Log.d("weight 3: ", String.valueOf(weight_cuaca3));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                for(int i = 0; i<2; i++)
                {
                    if(weight_cuaca1 < weight_cuaca2 && weight_cuaca2 < weight_cuaca3)
                    {
                        cek_cuaca1 = 1;
                    }
                    else if(weight_cuaca2 < weight_cuaca1 && weight_cuaca2 < weight_cuaca3)
                    {
                        cek_cuaca2 = 1;
                    }
                    else if(weight_cuaca3 < weight_cuaca1 && weight_cuaca3 < weight_cuaca2)
                    {
                        cek_cuaca3 = 1;
                    }
                }

                for(int i = 0; i<2; i++)
                {
                    if(jsonArray1.size() < jsonArray2.size() && jsonArray2.size() < jsonArray3.size())
                    {
                        cek_jalur1 = 1;
                    }
                    else if(jsonArray2.size() < jsonArray1.size() && jsonArray2.size() < jsonArray3.size())
                    {
                        cek_jalur2 = 1;
                    }
                    else if(jsonArray3.size() < jsonArray1.size() && jsonArray3.size() < jsonArray2.size())
                    {
                        cek_jalur3 = 1;
                    }
                }

                for(int a = 0; a < jsonArray2.size(); a++)
                {
                    try {
                        JSONObject jsonobject = new JSONObject(jsonArray2.get(a).getAsJsonObject().get("json").getAsString());
                        JSONObject jsonobjectgeometry = new JSONObject(jsonobject.get("geometry").toString());
                        String string_geocoordinates = jsonobjectgeometry.get("coordinates").toString();

                        JSONArray array_geocoordinate = new JSONArray(string_geocoordinates);
//                        Log.d("i: ", String.valueOf(array_geocoordinate.length()));
                        for(int i=0; i<array_geocoordinate.length(); i++)
                        {
                            JSONArray coord = array_geocoordinate.getJSONArray(i);
                            LatLng latLng = new LatLng(coord.getDouble(1), coord.getDouble(0));
                            latLngs.add(latLng);
                        }
                        if(cek_cuaca2==1 || cek_jalur2==1)
                        {
                            map.addPolyline(new PolylineOptions()
                                    .addAll(latLngs)
                                    .color(Color.parseColor("#3bb2d0"))
                                    .width(4));
                        }
                        else if(cek_jalur2==1)
                        {
                            map.addPolyline(new PolylineOptions()
                                    .addAll(latLngs)
                                    .color(Color.parseColor("#3bb2d0"))
                                    .width(4));
                        }
                        else
                        {
                            map.addPolyline(new PolylineOptions()
                                    .addAll(latLngs)
                                    .color(Color.parseColor("#757575"))
                                    .width(4));
                        }
                        latLngs.clear();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                for(int a = 0; a < jsonArray3.size(); a++)
                {
                    try {
                        JSONObject jsonobject = new JSONObject(jsonArray3.get(a).getAsJsonObject().get("json").getAsString());
                        JSONObject jsonobjectgeometry = new JSONObject(jsonobject.get("geometry").toString());
                        String string_geocoordinates = jsonobjectgeometry.get("coordinates").toString();

                        JSONArray array_geocoordinate = new JSONArray(string_geocoordinates);
//                        Log.d("i: ", String.valueOf(array_geocoordinate.length()));
                        for(int i=0; i<array_geocoordinate.length(); i++)
                        {
                            JSONArray coord = array_geocoordinate.getJSONArray(i);
                            LatLng latLng = new LatLng(coord.getDouble(1), coord.getDouble(0));
                            latLngs.add(latLng);
                        }
                        if(cek_cuaca3==1 || cek_jalur3==1)
                        {
                            map.addPolyline(new PolylineOptions()
                                    .addAll(latLngs)
                                    .color(Color.parseColor("#3bb2d0"))
                                    .width(4));
                        }
                        else if(cek_jalur3==1)
                        {
                            map.addPolyline(new PolylineOptions()
                                    .addAll(latLngs)
                                    .color(Color.parseColor("#3bb2d0"))
                                    .width(4));
                        }
                        else
                        {
                            map.addPolyline(new PolylineOptions()
                                    .addAll(latLngs)
                                    .color(Color.parseColor("#757575"))
                                    .width(4));
                        }
                        latLngs.clear();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                for(int a = 0; a < jsonArray1.size(); a++)
                {
                    try {
                        JSONObject jsonobject = new JSONObject(jsonArray1.get(a).getAsJsonObject().get("json").getAsString());
                        JSONObject jsonobjectgeometry = new JSONObject(jsonobject.get("geometry").toString());
                        String string_geocoordinates = jsonobjectgeometry.get("coordinates").toString();

                        JSONArray array_geocoordinate = new JSONArray(string_geocoordinates);
//                        Log.d("i: ", String.valueOf(array_geocoordinate.length()));
                        for(int i=0; i<array_geocoordinate.length(); i++)
                        {
                            JSONArray coord = array_geocoordinate.getJSONArray(i);
                            LatLng latLng = new LatLng(coord.getDouble(1), coord.getDouble(0));
                            latLngs.add(latLng);
                        }
                        if(cek_cuaca1==1 || cek_jalur1==1)
                        {
                            map.addPolyline(new PolylineOptions()
                                    .addAll(latLngs)
                                    .color(Color.parseColor("#3bb2d0"))
                                    .width(4));
                        }
                        else if(cek_jalur1==1)
                        {
                            map.addPolyline(new PolylineOptions()
                                    .addAll(latLngs)
                                    .color(Color.parseColor("#3bb2d0"))
                                    .width(4));
                        }
                        else
                        {
                            map.addPolyline(new PolylineOptions()
                                    .addAll(latLngs)
                                    .color(Color.parseColor("#757575"))
                                    .width(4));
                        }
                        latLngs.clear();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                for(int c = 0; c <jsonArray1_cuaca.size()-1 ; c++)
                {
                    try {
                        Log.d("ramalan 1: ", jsonArray1_cuaca.get(c).getAsJsonObject().get("kategori").getAsString());
                        Double lat = Double.valueOf(jsonArray1_cuaca.get(c).getAsJsonObject().get("y").getAsString());
                        Double longt = Double.valueOf(jsonArray1_cuaca.get(c).getAsJsonObject().get("x").getAsString());
                        String ket_ramalan = jsonArray1_cuaca.get(c).getAsJsonObject().get("kategori").getAsString();
//                        MarkerViewOptions markerViewOptions = new MarkerViewOptions();
                        if(ket_ramalan.equals("1"))
                        {
                            map.addMarker(new MarkerViewOptions()
                                    .position(new LatLng(lat, longt))
                                    .title("Cuaca Berawan")
                                    .icon(markerIcon("berawan"))
                                    .alpha(255));
                        }
                        else if(ket_ramalan.equals("0"))
                        {
                            map.addMarker(new MarkerViewOptions()
                                    .position(new LatLng(lat, longt))
                                    .title("Cuaca Cerah")
                                    .icon(markerIcon("cerah"))
                                    .alpha(255));
                        }
                        else if(ket_ramalan.equals("2"))
                        {
                            map.addMarker(new MarkerViewOptions()
                                    .position(new LatLng(lat, longt))
                                    .title("Cuaca Hujan Ringan")
                                    .icon(markerIcon("hujan_ringan"))
                                    .alpha(255));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                for(int c = 0; c <jsonArray2_cuaca.size()-1 ; c++)
                {
                    try {
//                        Log.d("ramalan 2: ", jsonArray2_cuaca.get(c).getAsJsonObject().get("ramalan").getAsString());
                        Double lat = Double.valueOf(jsonArray2_cuaca.get(c).getAsJsonObject().get("y").getAsString());
                        Double longt = Double.valueOf(jsonArray2_cuaca.get(c).getAsJsonObject().get("x").getAsString());
                        String ket_ramalan = jsonArray2_cuaca.get(c).getAsJsonObject().get("kategori").getAsString();
//                        if(ket_ramalan.equals("1"))
//                        {
//                            map.addMarker(new MarkerOptions()
//                                    .position(new LatLng(lat, longt))
//                                    .title("Cuaca Berawan")
//                                    .icon(markerIcon("berawan")));
//                        }
//                        else if(ket_ramalan.equals("0"))
//                        {
//                            map.addMarker(new MarkerOptions()
//                                    .position(new LatLng(lat, longt))
//                                    .title("Cuaca Cerah")
//                                    .icon(markerIcon("cerah")));
//                        }
//                        else if(ket_ramalan.equals("2"))
//                        {
//                            map.addMarker(new MarkerOptions()
//                                    .position(new LatLng(lat, longt))
//                                    .title("Cuaca Hujan Ringan")
//                                    .icon(markerIcon("hujan_ringan")));
//                        }
                        if(ket_ramalan.equals("1"))
                        {
                            map.addMarker(new MarkerViewOptions()
                                    .position(new LatLng(lat, longt))
                                    .title("Cuaca Berawan")
                                    .icon(markerIcon("berawan"))
                                    .alpha(255));
                        }
                        else if(ket_ramalan.equals("0"))
                        {
                            map.addMarker(new MarkerViewOptions()
                                    .position(new LatLng(lat, longt))
                                    .title("Cuaca Cerah")
                                    .icon(markerIcon("cerah"))
                                    .alpha(255));
                        }
                        else if(ket_ramalan.equals("2"))
                        {
                            map.addMarker(new MarkerViewOptions()
                                    .position(new LatLng(lat, longt))
                                    .title("Cuaca Hujan Ringan")
                                    .icon(markerIcon("hujan_ringan"))
                                    .alpha(255));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                for(int c = 0; c <jsonArray3_cuaca.size()-1 ; c++)
                {
                    try {
                        Log.d("ramalan 3: ", jsonArray3_cuaca.get(c).getAsJsonObject().get("kategori").getAsString());
                        Double lat = Double.valueOf(jsonArray3_cuaca.get(c).getAsJsonObject().get("y").getAsString());
                        Double longt = Double.valueOf(jsonArray3_cuaca.get(c).getAsJsonObject().get("x").getAsString());
                        String ket_ramalan = jsonArray3_cuaca.get(c).getAsJsonObject().get("kategori").getAsString();
//                        if(ket_ramalan.equals("1"))
//                        {
//                            map.addMarker(new MarkerOptions()
//                                    .position(new LatLng(lat, longt))
//                                    .title("Cuaca Berawan")
//                                    .icon(markerIcon("berawan")));
//                        }
//                        else if(ket_ramalan.equals("0"))
//                        {
//                            map.addMarker(new MarkerOptions()
//                                    .position(new LatLng(lat, longt))
//                                    .title("Cuaca Cerah")
//                                    .icon(markerIcon("cerah")));
//                        }
//                        else if(ket_ramalan.equals("2"))
//                        {
//                            map.addMarker(new MarkerOptions()
//                                    .position(new LatLng(lat, longt))
//                                    .title("Cuaca Hujan Ringan")
//                                    .icon(markerIcon("hujan_ringan")));
//                        }
                        if(ket_ramalan.equals("1"))
                        {
                            map.addMarker(new MarkerViewOptions()
                                    .position(new LatLng(lat, longt))
                                    .title("Cuaca Berawan")
                                    .icon(markerIcon("berawan"))
                                    .alpha(255));
                        }
                        else if(ket_ramalan.equals("0"))
                        {
                            map.addMarker(new MarkerViewOptions()
                                    .position(new LatLng(lat, longt))
                                    .title("Cuaca Cerah")
                                    .icon(markerIcon("cerah"))
                                    .alpha(255));
                        }
                        else if(ket_ramalan.equals("2"))
                        {
                            map.addMarker(new MarkerViewOptions()
                                    .position(new LatLng(lat, longt))
                                    .title("Cuaca Hujan Ringan")
                                    .icon(markerIcon("hujan_ringan"))
                                    .alpha(255));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
//                drawSimplify(latLngs);
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.d("gagal response", t.toString());
            }
        });
    }

//    private void getRoute(LatLng latLngAsal, LatLng latLngTujuan)
//    {
//        //pemanggilan url dari Clearroute API
//        StringBuilder urlbaru = new StringBuilder("http://riset.alpro.if.its.ac.id/clearroute/public/index.php/getroutecuaca1/");
//
//        //memanggil longitude asal
//        urlbaru.append(latLngAsal.getLongitude()+"/");
//
//        //memanggil latitude asal
//        urlbaru.append(latLngAsal.getLatitude()+"/");
//
//        //memanggil longitude tujuan
//        urlbaru.append(latLngTujuan.getLongitude()+"/");
//
//        //memanggil latitude asal
//        urlbaru.append(latLngTujuan.getLatitude()+"/");
//
//        //deklarasi fungsi retrofit
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("http://clearroute.net")
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        GetRoute service = retrofit.create(GetRoute.class);
//        Call<JsonElement> call = service.getRoute(urlbaru.toString());
//        Log.d("url= ", urlbaru.toString());
//
//        call.enqueue(new Callback<JsonElement>() {
//            @Override
//            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
//                //menerima response ke dalam Json Element
//                JsonElement jsonElement = response.body();
//                JsonObject jsonArray = jsonElement.getAsJsonObject();
//
//                //mendapatkan rute utama
//                JsonArray jsonArray1 = jsonArray.get("0").getAsJsonArray();
//
//                //mendapatkan rute alternatif 1
//                JsonArray jsonArray2 = jsonArray.get("1").getAsJsonArray();
//
//                //mendapatkan rute alternatif 2
//                JsonArray jsonArray3 = jsonArray.get("2").getAsJsonArray();
//
//                //perulangan penggambaran rute utama
//                for(int a = 0; a < jsonArray1.size(); a++)
//                {
//                    try {
//                        JSONObject jsonobject = new JSONObject(jsonArray1.get(a).getAsJsonObject().get("json").getAsString());
//                        JSONObject jsonobjectgeometry = new JSONObject(jsonobject.get("geometry").toString());
//                        String string_geocoordinates = jsonobjectgeometry.get("coordinates").toString();
//
//                        JSONArray array_geocoordinate = new JSONArray(string_geocoordinates);
//                        for(int i=0; i<array_geocoordinate.length(); i++)
//                        {
//                            JSONArray coord = array_geocoordinate.getJSONArray(i);
//                            LatLng latLng = new LatLng(coord.getDouble(1), coord.getDouble(0));
//                            latLngs.add(latLng);
//                        }
//                        map.addPolyline(new PolylineOptions()
//                                .addAll(latLngs)
//                                .color(Color.parseColor("#3bb2d0"))
//                                .width(4));
//                        latLngs.clear();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                //perulangan penggambaran rute alternatif 1
//                for(int a = 0; a < jsonArray2.size(); a++)
//                {
//                    try {
//                        JSONObject jsonobject = new JSONObject(jsonArray2.get(a).getAsJsonObject().get("json").getAsString());
//                        JSONObject jsonobjectgeometry = new JSONObject(jsonobject.get("geometry").toString());
//                        String string_geocoordinates = jsonobjectgeometry.get("coordinates").toString();
//
//                        JSONArray array_geocoordinate = new JSONArray(string_geocoordinates);
//                        for(int i=0; i<array_geocoordinate.length(); i++)
//                        {
//                            JSONArray coord = array_geocoordinate.getJSONArray(i);
//                            LatLng latLng = new LatLng(coord.getDouble(1), coord.getDouble(0));
//                            latLngs.add(latLng);
//                        }
//                        map.addPolyline(new PolylineOptions()
//                                .addAll(latLngs)
//                                .color(Color.parseColor("#757575"))
//                                .width(4));
//                        latLngs.clear();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                //perulangan penggambaran rute alternatif 2
//                for(int a = 0; a < jsonArray3.size(); a++)
//                {
//                    try {
//                        JSONObject jsonobject = new JSONObject(jsonArray3.get(a).getAsJsonObject().get("json").getAsString());
//                        JSONObject jsonobjectgeometry = new JSONObject(jsonobject.get("geometry").toString());
//                        String string_geocoordinates = jsonobjectgeometry.get("coordinates").toString();
//
//                        JSONArray array_geocoordinate = new JSONArray(string_geocoordinates);
//                        for(int i=0; i<array_geocoordinate.length(); i++)
//                        {
//                            JSONArray coord = array_geocoordinate.getJSONArray(i);
//                            LatLng latLng = new LatLng(coord.getDouble(1), coord.getDouble(0));
//                            latLngs.add(latLng);
//                        }
//                        map.addPolyline(new PolylineOptions()
//                                .addAll(latLngs)
//                                .color(Color.parseColor("#757575"))
//                                .width(4));
//                        latLngs.clear();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<JsonElement> call, Throwable t) {
//                Log.d("gagal response", t.toString());
//            }
//        });
//    }

//    private void drawSimplify(ArrayList<Position> points) {
//
//        Position[] before = new Position[points.size()];
//        for (int i = 0; i < points.size(); i++) {
//            before[i] = points.get(i);
//        }
//
//        Position[] after = PolylineUtils.simplify(before, 0.00001);
//
//        LatLng[] result = new LatLng[after.length];
//        ArrayList<LatLng> coor = new ArrayList<>();
//
//        for (int i = 0; i < after.length; i++) {
//            coor.add(new LatLng(after[i].getLatitude(), after[i].getLongitude()));
//            Log.d("lat: " + String.valueOf(after[i].getLatitude()) + "long: ", String.valueOf(after[i].getLongitude()));
//        }
//
//        map.addPolyline(new PolylineOptions()
//                .addAll(coor)
//                .color(Color.parseColor("#3bb2d0"))
//                .width(dua));
//
//    }

    private void drawPolyline(final JsonArray geojson)
    {
        AsyncTask<Void, Void, Integer> draw = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
//                Json json_1 = new Json();
                for(int a = 0; a < geojson.size(); a++)
                {
                    try {
                        JSONObject jsonobject = new JSONObject(geojson.get(a).getAsJsonObject().get("json").getAsString());
                        JSONObject jsonobjectgeometry = new JSONObject(jsonobject.get("geometry").toString());

                        Geometry geometry = new Geometry();
                        ArrayList<String> geocoordinate = new ArrayList<String>();
                        String string_geocoordinates = jsonobjectgeometry.get("coordinates").toString();

                        JSONArray array_geocoordinate = new JSONArray(string_geocoordinates);
                        for(int i=0; i<array_geocoordinate.length(); i++)
                        {
//                            Log.d("koordinat: "+String.valueOf(a)+ " -> ", array_geocoordinate.get(i).toString());
                            geocoordinate.add(array_geocoordinate.get(i).toString());
                            String[] koordinat = array_geocoordinate.get(i).toString().split(",");
//                            Log.d(koordinat[0].split("\\[")[1] + ",", koordinat[1].split("\\]")[0]);
//                            Log.d("y: ", koordinat[1].split("\\]")[0]);
                            LatLng koordinat_baru = new LatLng(Double.valueOf(koordinat[1].split("\\]")[0]), Double.valueOf(koordinat[0].split("\\[")[1]));
//                            Position pos = Position.fromCoordinates(Double.valueOf(koordinat[0].split("\\[")[1]), Double.valueOf(koordinat[1].split("\\]")[0]));
                            if(!latLngs.contains(koordinat_baru))
                            {
                                Log.d(koordinat[0].split("\\[")[1] + ",", koordinat[1].split("\\]")[0]);
                                latLngs.add(koordinat_baru);
                            }
//                            ArrayList<LatLng> coor = new ArrayList<LatLng>(new LinkedHashSet<LatLng>(latLngs));
//                            latLngs.clear();
//                            latLngs.addAll(coor);
                        }
                        json_1.setType(jsonobject.get("type").toString());
                        geometry.setType(jsonobjectgeometry.get("type").toString());
                        geometry.setCoordinates(geocoordinate);
                        json_1.setGeometry(geometry);
//                            Log.d("jsonobject: ", jsonobject.get("type").toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return 1;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                json_asli.setFeatures(json_1);
//                drawSimplify(latLngs);
                map.addPolyline(new PolylineOptions()
                        .addAll(latLngs)
                        .color(Color.parseColor("#3bb2d0"))
                        .width(2));
            }
        };
        draw.execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapbox.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapbox.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapbox.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapbox.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapbox.onDestroy();
    }

    protected void checkPermission()
    {
        int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_NETWORK_STATE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.INTERNET)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.INTERNET},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    private void getLocation()
    {
        //memanggil servis lokasi android
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //pengecekan apakah gps sudah diaktifkan
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //pemanggilan fungsi aktifasi gps
            buildAlertMessageNoGps();
        }

        //pengecekkan perijinan akses lokasi
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try{
            //mengambil lokasi dari gps
            mLastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (mLastLocation == null){
                //mengambil lokasi dari jaringan layanan telepon
                mLastLocation = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if(mLastLocation==null)
                {
                    //mengambil lokasi terbaru lewat gps
                    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
                    mLastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            }
            //debug latitude longitude
            else if (mLastLocation != null)
                Log.d("Location : ","Lat = "+ mLastLocation.getLatitude() + " Lng");
        }catch (Exception e)
        {
            //apabila gagal mendapat lokasi
            Log.d("Gagal lokasi terbaru", "fail");
        }
    }

    private void buildAlertMessageNoGps() {
        //membuat alert dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("GPS tidak aktif, tolong aktifkan GPS anda")
                .setCancelable(false)
                .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        finish();
                    }
                })
                .setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
