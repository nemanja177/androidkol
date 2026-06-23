package com.example.kolokvijum2java;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private float lastX, lastY, lastZ;
    private TextView tvLokacija;
    private ImageButton slikaDugme;
    private ImageView prikazSlike;
    private Button accButton;
    private Switch getDataSwitch;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> CameraPermissionLauncher;

    private SQLiteHelper dbHelper;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        tvLokacija = findViewById(R.id.tvLokacija);
        prikazSlike = findViewById(R.id.prikazSlike);
        slikaDugme = findViewById(R.id.imageCaptureButton);
        accButton = findViewById(R.id.accButton);
        getDataSwitch = findViewById(R.id.switch1);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) this
                .getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        dbHelper = new SQLiteHelper(this);
        sharedPreferences = getSharedPreferences("Kolokvijum2Prefs", Context.MODE_PRIVATE);
        
        checkPermissionsAndRequestLocation();
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        if (imageUri != null) {
                            prikazSlike.setImageURI(imageUri);
                            String message = String.format(Locale.getDefault(),"X: %.2f | Y: %.2f | Z: %.2f", lastX, lastY, lastZ);
//                            toast("Image captured");
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        CameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        toast("Camera permission required");
                    }
                }
        );

        slikaDugme.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                openCamera();
            } else {
                CameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getDataSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // ZADATAK 6: Ako je prvi put uključen i baza je prazna
                    if (dbHelper.izbrojPostove() == 0) {
                        getDataFromURL();
                    } else {
                        Postovi prviPost = dbHelper.pronadjiPrvi();
                        if (prviPost != null) {
                            Toast.makeText(MainActivity.this, "Prvi post: " + prviPost.getTitle(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("tekst", tvLokacija.getText().toString());
                    editor.apply();

                    proveriIPrikaziPrviKontakt();
                }
            }
        });

        accButton.setOnClickListener(v -> {
            boolean obrisano = dbHelper.obrisiPrvi();
            // Ukoliko su svi postovi obrisani (ili baza bila prazna), pošalji notifikaciju
            if (!obrisano || dbHelper.izbrojPostove() == 0) {
                posaljiNotifikaciju();
            } else {
                Toast.makeText(MainActivity.this, "Prvi post uspesno obrisan iz tabele!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissionsAndRequestLocation() {
        boolean fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fineLocationGranted) {
            requestLocationUpdates();
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    // --------- deo 3 --------

    private final LocationListener locationListener = location -> {
        updateMapLocation(location);
    };

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {

                        Boolean fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                        if (Boolean.TRUE.equals(fineLocation)) {
                            requestLocationUpdates();
                        } else {
                            Toast.makeText(this,"Location permission denied", Toast.LENGTH_SHORT).show();
                        }
                    });
    private void requestLocationUpdates() {

        if (locationManager == null) {
            return;
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000,10, locationListener);
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,10000, 10, locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnownLocation != null) {
                updateMapLocation(lastKnownLocation);
            }

        } catch (SecurityException e) {
            Toast.makeText(this, "Permission error", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMapLocation(Location location) {

        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            String tekstLokacije = "Geografska sirina: " + latitude + "\nGeografska duzina: " + longitude;
            tvLokacija.setText(tekstLokacije);
        }

    }

    // ------- 4 , kamera ---------

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void openCamera() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File imageFile = createImageFile();
        if (imageFile == null) {
            toast("File error");
            return;
        }

        imageUri = FileProvider.getUriForFile(this,this.getPackageName() + ".provider", imageFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        cameraLauncher.launch(intent);
    }

    private File createImageFile() {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) return null;
        return new File(storageDir, "IMG_" + timeStamp + ".jpg");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    // ------ 4 ---------

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        lastX = event.values[0];
        lastY = event.values.length > 1 ? event.values[1] : 0;
        lastZ = event.values.length > 2 ? event.values[2] : 0;
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            //  Log.d("ZiroskopTest", "X: " + lastX + " Y: " + lastY + " Z: " + lastZ);
        } else if ( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
            String message = String.format(Locale.getDefault(),"X: %.2f | Y: %.2f | Z: %.2f", lastX, lastY, lastZ);
            accButton.setText(message);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerSensor(gyroscope);
        registerSensor(accelerometer);
    }

//    @Override
//    public void onPause() {
//        super.onPause();
//        sensorManager.unregisterListener(this);
//    }
    private void registerSensor(Sensor sensor) {
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }
    /* -------- zadatak 6 ------- */
    private void getDataFromURL() {
        Call<ArrayList<Postovi>> call = ClientUtils.postoviService.getAll();
        call.enqueue(new Callback<ArrayList<Postovi>>() {
            @Override
            public void onResponse(@NonNull Call<ArrayList<Postovi>> call, @NonNull Response<ArrayList<Postovi>> response) {
                List<Postovi> postovi = response.body();
                if ( postovi != null ) {
                    for(int i = 0; i < 10; i++ ) {
                        dbHelper.save(postovi.get(i));
                    }
                    Toast.makeText(MainActivity.this, "Preuzeto 10 postova!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ArrayList<Postovi>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Greska u preuzimanju", Toast.LENGTH_SHORT).show();
                Log.d("URL test", t.toString());
            }
        });
    }

    private void posaljiNotifikaciju() {
        String channelId = "sqlite_channel";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Obavestenje")
                .setContentText("Nema vise postova!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        Log.d("Notifikacija za praznu bazu", "PRAZNA BAZA");
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(1, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SQLite Obaveštenja";
            String description = "Kanal za obaveštenja o bazi podataka";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel("sqlite_channel", name, importance);
            channel.setDescription(description);
            Log.d("Registracija notifikacije", "REGISTROVANO");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void proveriIPrikaziPrviKontakt() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    String imeKontakta = cursor.getString(nameIndex);
                    tvLokacija.setText(imeKontakta);
                }
                cursor.close();
            } else {
                tvLokacija.setText("Nema kontakata");
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 102);
        }
    }
}






/*

package com.example.kolokvijum2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private TextView myTextView;
    private ImageButton myImageButton;
    private ImageView myImageView;
    private Switch mySwitch;
    private Button myButton;

    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private LocationManager locationManager;

    // Menjamo u SQLite helper
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private static final int CAMERA_REQUEST_CODE = 100;
    private float currentGyroX = 0, currentGyroY = 0, currentGyroZ = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myTextView = findViewById(R.id.myTextView);
        myImageButton = findViewById(R.id.myImageButton);
        myImageView = findViewById(R.id.myImageView);
        mySwitch = findViewById(R.id.mySwitch);
        myButton = findViewById(R.id.myButton);

        // Inicijalizacija SQLite baze
        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("Kolokvijum2Prefs", Context.MODE_PRIVATE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // ZADATAK 3: Geolokacija
        proveriDozvoleILokaciju();

        // ZADATAK 4: Kamera
        myImageButton.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        });

        // ZADATAK 6 i 9: Logika za Switch (Uključivanje i isključivanje)
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // ZADATAK 6: Ako je prvi put uključen i baza je prazna
                    if (dbHelper.getPostsCount() == 0) {
                        dobaviSaNetaIUpisiUSQLite();
                    } else {
                        // Svaki sledeći put ispiši "title" prvog posta iz tabele baze
                        Post prviPost = dbHelper.getFirstPostInTable();
                        if (prviPost != null) {
                            Toast.makeText(MainActivity.this, "Prvi post: " + prviPost.getTitle(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    // ZADATAK 9: Kada se Switch prebaci na "off"
                    // 1. Sačuvaj trenutni tekst iz TextView-a u SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("tekst", myTextView.getText().toString());
                    editor.apply();

                    // 2. Pročitaj prvi kontakt i zameni tekst u TextView-u
                    proveriIPrikaziPrviKontakt();
                }
            }
        });

        // ZADATAK 7: Klikom na dugme obriši post na prvoj poziciji u bazi
        myButton.setOnClickListener(v -> {
            boolean obrisano = dbHelper.deleteFirstPost();
            // Ukoliko su svi postovi obrisani (ili baza bila prazna), pošalji notifikaciju
            if (!obrisano || dbHelper.getPostsCount() == 0) {
                posaljiNotifikaciju();
            } else {
                Toast.makeText(MainActivity.this, "Prvi post uspešno obrisan iz tabele!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Retrofit povlačenje i punjenje SQLite tabele (Zadatak 6)
    private void dobaviSaNetaIUpisiUSQLite() {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getPosts().enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Post> listaPostova = response.body();
                    int limit = Math.min(listaPostova.size(), 10);

                    for (int i = 0; i < limit; i++) {
                        dbHelper.insertPost(listaPostova.get(i));
                    }
                    Toast.makeText(MainActivity.this, "Prvih 10 postova upisano u SQLite!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Greška na mreži", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Slanje Notifikacije ako nema više postova (Zadatak 7)
    private void posaljiNotifikaciju() {
        String channelId = "sqlite_channel";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Obaveštenje")
                .setContentText("Nema više postova!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(1, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // Čitanje prvog kontakta preko Cursora iz Contacts aplikacije (Zadatak 9)
    private void proveriIPrikaziPrviKontakt() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    String imeKontakta = cursor.getString(nameIndex);
                    myTextView.setText(imeKontakta);
                }
                cursor.close();
            } else {
                myTextView.setText("Nema kontakata");
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 102);
        }
    }

    // Obrada rezultata kamere i Toast očitavanja žiroskopa (Zadatak 4)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            myImageView.setImageBitmap(bitmap);

            String gyroPoruka = String.format("Žiroskop -> X: %.2f | Y: %.2f | Z: %.2f", currentGyroX, currentGyroY, currentGyroZ);
            Toast.makeText(this, gyroPoruka, Toast.LENGTH_LONG).show();
        }
    }

    // Očitavanje senzora u realnom vremenu (Žiroskop + Akcelerometar za tekst Button-a) (Zadatak 8)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            currentGyroX = event.values[0];
            currentGyroY = event.values[1];
            currentGyroZ = event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            myButton.setText(String.format("X: %.2f | Y: %.2f | Z: %.2f", x, y, z));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // Geolokacija (Zadatak 3)
    private void proveriDozvoleILokaciju() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
            Location zadnjaLokacija = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (zadnjaLokacija != null) {
                onLocationChanged(zadnjaLokacija);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        myTextView.setText(String.format("Lokacija: Širina %.4f | Dužina %.4f", location.getLatitude(), location.getLongitude()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gyroscope != null) sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}

 */