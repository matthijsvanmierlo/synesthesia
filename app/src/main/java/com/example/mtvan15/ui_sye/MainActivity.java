package com.example.mtvan15.ui_sye;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;


/**
 * MainActivity maintains control/focus of sensor and audio input. The MainActivity also handles
 * distinct user permissions when the application is first started to ensure proper application
 * behavior. Additionally, MainActivity is responsible for drawing visuals that the user sees
 * once interacting with the application. MainActivity contains a canvas that dynamically draws
 * visuals based on environmental cues, noise, and volume. The user may control different types of
 * interactions using a BottomNavigationView containing various options. Users have the option
 * of navigating to settings, enabling touch drawing, enabling sensor drawing, clearing the canvas,
 * as well as saving desired images. MainActivity also contains a globe button to enable community
 * sharing and interactions.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener, AudioProcessor {

    // Values that are used to actively map sensor values within certain ranges
    // These values represent the max input seen thus far for scaling purposes.
    private float magnitude = 3.14f;
    private float colorMagnitude = 1f;
    private float lightMagnitude = 1f;
    private float sizeMagnitude = 1f;

    // Booleans to check whether certain sensors are or are not enabled in the application
    // These booleans control conditional logic, especially related to drawSomething( ) as well
    // as enableSensor( )
    private boolean gyroIsEnabled = true;
    private boolean touchIsEnabled = false;
    private boolean settings = false;
    private boolean audioThread = false;

    // Setup basic UI elements for the MainActivity screen. This includes a canvas, a bitmap,
    // as well as an image view.
    private Canvas canvas;
    private Paint paint;
    private Bitmap bitmap;
    private ImageView imageView;


    // Sensor Manager and the Sensors
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor gyroscopeSensor;
    private SilenceDetector silenceDetector;
    private double threshold;

    // Keep a reference to our touchListener class
    private TouchListener touchListener;

    // Keep a reference to a the spiral and radial classes for animation purposes
    // These keep track of points internally, and create subsequent drawing coordinates
    // Based on internal parameters and parametrics.
    private Spiral spiral;
    private Radial radial;

    // Important drawing elements for the canvas (parameters)
    private int color[] = {0, 0, 0};
    private int sizeRadius = 1;
    private int opacity = 0;
    private int x = -1;
    private int y = -1;
    private int width;
    private int height;

    // Determine whether sensors can start writing values
    // This boolean was implemented because of concurrency issues
    // related to the combination of sensors and active audio threads
    private boolean begin = false;

    // Setup Firebase Cloud Storage
    private StorageReference mStorageRef;
    FirebaseDatabase database = FirebaseDatabase.getInstance();


    // Keep track of permissions for the application along with the activity
    private final static int PERMISSION_REQUEST_CODE = 999;
    private boolean permissions_granted;
    private final static String LOGTAG = MainActivity.class.getSimpleName();

    // Variables representing image encoding and identification when working with
    // FireBase as the database platform
    private String saveString;
    private String descriptionString;
    private int imageNum = -1;
    private String encodedImage;
    private String baseImage;


    // Variables that represent different queries from SharedPreferences
    // These store important information about the application state throughout
    // the application life cycle.
    SharedPreferences sharedPreferences;
    SwitchPreference radialSwitchPref;
    SwitchPreference spiralSwitchPref;

    String username;
    Boolean light_switch;
    Boolean volume_switch;
    Boolean radialMotion;
    Boolean spiralMotion;
    String strokeType;

    Thread audioThreadRef;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * onResume( ) - Android life cycle method. Register the sensorListeners in here assuring they are ready for data.
     * This method queries entries in SharedPreferences such that the application state will
     * be remembered and updated over time. This includes settings selected in the specific
     * Settings Activity for light input, volume detection, as well as various other drawing aspects.
     * This method also initializes a saved bitmap IF one was selected from the community page.
     * Selecting an image from the community page for REMIX pulls the entry from FireBase, and allows
     * the user to build upon the image, and truly make it their own creation.
     */
    @Override
    protected void onResume() {
        super.onResume();



        // Load User Preferences For...
        // username
        // light_switch
        // volume_Switch
        // strokeType
        // spiralMotion
        // radialMotion
        username = sharedPreferences.getString("username", "");
        light_switch = sharedPreferences.getBoolean("light_switch", true);
        volume_switch = sharedPreferences.getBoolean("volume_switch", true);
        strokeType = sharedPreferences.getString("stroke_type", "0");
        spiralMotion = sharedPreferences.getBoolean("spiralMotion", false);
        radialMotion = sharedPreferences.getBoolean("radialMotion", false);

        // Load a shared bitmap if one exists in the current application state
        // Otherwise, initialize the string to an empty string, representing NO selection from
        // the community gallery page.
        String encodedBitmap = sharedPreferences.getString("background", "");

        // If there was a selected image from the community gallery page, then load a scaled
        // version of it to the imageView such that it matches the dimensions of the current drawing
        // canvas. This ensures a consistent experience across devices without cropping one's image
        // or artistic creation.
        if(!encodedBitmap.equals("")){
            this.width = sharedPreferences.getInt("width", 0);
            this.height = sharedPreferences.getInt("height", 0);
            byte[] decodedString = Base64.decode(encodedBitmap, Base64.DEFAULT);
            this.bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length).copy(Bitmap.Config.ARGB_8888, true);
            this.bitmap = Bitmap.createScaledBitmap(this.bitmap, this.width , this.height, false);
            this.imageView.setImageBitmap(bitmap);
            sharedPreferences.edit().putString("background","").apply();

        }

        // Settings = True represents navigation to MainActivity FROM the Settings Activity.
        // Settings = False represents the current state of the application in MainActivity.
        // This variable was implemented to allow for a consistent drawing experience regardless
        // of navigation patterns or frequency there of.
        if (settings) {
            begin = true;
            settings = false;
        }


        // Check to see if an audio thread already exists. It is important to only make an audio thread
        // if the correct permissions were granted in the application. This was placed in onResume( )
        // to avoid concurrency issues with checkSelfPermissions( ) and the createAudioThread( ) methods
        // since they operate very independently from one another.
        if (!audioThread && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            this.permissions_granted = true;
            createAudioThread();
        }


        // Register a listener for the proximity sensory
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_GAME);
        // Register a listener for the gyroscope sensor
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        // Now we are ready to start!!!

    }

    /**
     * onCreate( ) - Android life cycle method. Handles UI elements and checks for permissions prior to creating
     * dependent objects (such as the AudioThread, etc.). onCreate( ) also enables read and write
     * quereies to the FireBase noSQL database by keeping a global reference to the database object.
     * The BottomNavigationView is attached to a listener which enables active detection of navigation
     * events and settings
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // check all permissions
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE
            );

        }



        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Setup Firebase Storage by getting an instance of the databse with a particular reference
        // or identifer. The myRef object takes sa specific input parameter representing a key
        // (corresponding to data) that wants to actively be retrieved.
        mStorageRef = FirebaseStorage.getInstance().getReference();
        DatabaseReference myRef = database.getReference("message");


        imageView = findViewById(R.id.imageView);

        // Set an onTouchListener for the imageView which will allow for touch input and drawing
        // as long as touchIsEnabled = True. This value is controlled by the corresponding user
        // setting of Touch Draw in the main application window.
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(touchIsEnabled){
                    x = (int)event.getX();
                    y = (int)event.getY();
                    drawSomething(x, y, bitmap);
                    return true;
                }
                return false;
            }
        });

        // Set an initial drawing location that is in the middle of the screen, determined by the
        // width and height of the imageView attached to the drawing canvas object.
        this.x = width /2;
        this.y = height / 2;

        // Construct a new Paint( ) object which is extensively used in the drawSomething( ) method.
        // The Paint( ) object is a build in Java/Android library which allows for convenient grouping
        // of graphics parameters such as color, stroke width, opacity, etc.
        paint = new Paint();

        // Setting a default Paint( ) object color upon creation. This color never actually gets used
        // but it was initialized as such for debugging purposes, and for maintining consistent code
        // style.
        paint.setColor(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));

        // Creating a Sensor Manager Object. The SensorManager is a built in Android Java class that
        // allows for multiple sensor management, data retrieval, and more. It is based off of the
        // Sensor_Service which runs consistently in the background, thus allowing for continuous data
        // retrieval.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

    }


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * OnNavigationItemSelectedListener( ) - BottomNavigationView is the primary navigation element in our application.
     * This method handles events based on which bottom navigation tab was selected.
     * Options include enabling touch drawing, sensor drawing, settings, clearing, as well as
     * image saving/uploading.
     */
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.sensorDraw:
                    enableSensor(true);
                    return true;
                case R.id.touchDraw:
                    enableSensor(false);
                    return true;
                case R.id.settings:
                    begin = false;
                    toSettings();
                    return true;
                case R.id.save:
                    saveImage();
                    return true;
                case R.id.clearButton:
                    clearCanvas();
                    return true;
            }
            return false;
        }
    };

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * onRequestPermissionsResult( ) - Checks the permissions for AUDIO, READ/WRITE_EXTERNAL_STORAGE. These are necessary for our
     * application to function properly. As a result, if the permissions are denied in any way shape
     * or form, then the application quits gracefully with an explanation of why those permissions
     * were required in the first place. The explanation is shown in an AlertDialog box such that
     * it appears only if a permission was DENIED, and a rationale is required. Built in Android
     * permissions methods allow for easy retrieval of this information, and a custom layout
     * was created to use inside of the AlertDialog box for extensibility if more in depth explanations
     * were to be provided at a later time.
     * @param requestCode a custom requestCode which was determined at the beginning of this application
     * @param permissions a list of specified permissions that the application requests access to
     * @param grantResults a list of the permissions that were given by the user and can be checked
     *                     programmatically based on their function in the application.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // If the request code matches what was originally asked for in the application upon start...
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // If all permissions were successfully granted upon starting/installing the application
                // then continue with the rest of the application.
                this.permissions_granted = true;

            }
            else {
                // Not all permissions were granted. We check to see which permission was denied,
                // and show an appropriate dialog box with a rational and explanation about why
                // we need to the desired features for our app to function properly.
                this.permissions_granted = false;
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Please Grant Permissions for External Storage");

                    // Create a custom LinearLayout for the AlertDialog box that contains a TextView
                    // with custom padding, margins, and text. This is all done programmatically based
                    // on what explanation needs to be shown at the given point in time.
                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    final TextView text = new TextView(this);
                    text.setPadding(30, 30, 30, 30);
                    text.setText("To save your creations, we require read and write access to your external storage. Without it, this app will not run. The app will proceed to close. Please consider granting permissions next time.");
                    layout.addView(text);
                    builder.setView(layout);
                    builder.setPositiveButton("K.", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // This Android method gracefully exits and quits all background
                            // events related to the application itself. This is important to call
                            // when the proper permissions were not supplied at the beginning of the
                            // application.
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    });
                    builder.show();
                }else if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)){
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Please Grant Permissions for Audio");

                    // Create a custom LinearLayout for the AlertDialog box that contains a TextView
                    // with custom padding, margins, and text. This is all done programmatically based
                    // on what explanation needs to be shown at the given point in time.
                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    final TextView text = new TextView(this);
                    text.setPadding(30, 30, 30, 30);
                    text.setText("To engage with your art, we require audio access. Without it, this app will not run. The app will proceed to close. Please consider granting permissions next time.");
                    layout.addView(text);
                    builder.setView(layout);
                    builder.setPositiveButton("K.", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // This Android method gracefully exits and quits all background
                            // events related to the application itself. This is important to call
                            // when the proper permissions were not supplied at the beginning of the
                            // application.
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    });
                    builder.show();
                }
                //this.finish();

            }
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * onCreateOptionsMenu( ) - The ActionBarMenu allows for user interaction near the top of the application screen.
     * @param menu is an ActionBarMenu to-be initialized. This menu is in turn inflated for creation.
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * onOptionsItemSelected( ) - Returns an ID of the button that was selected. In our application
     * this serves the purposes of identifying when the communityButton was selected to start
     * an intent to the corresponding community gallery activity.
     * @param item is the ActionBarMenu item that was selected. The ID refers to the ID of the
     *             corresponding UI element declared in XML.
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.communityButton){
            // Do stuff
            // Put intent here
            // TODO disable sensors in a more robust way...
            this.begin = false;
            this.settings = true;
            sharedPreferences.edit().putInt("width", this.width).apply();
            sharedPreferences.edit().putInt("height", this.height).apply();
            Intent intent = new Intent(this, Community.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * toSettings( ) - Method calling an intent to the Setting Activity. This method is accessible from the context
     * of the BottomNavigationView listener. This allows for fast and succinct navigation without
     * cluttering the BottomNavigationView listener with tedious/redundant code specific to intents.
     */
    private void toSettings(){
        settings = true;
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * clearCanvas( ) - Method called through the BottomNavigationView listener upon user interaction
     * with the CLEAR button. This resets all scaling values used in our MapRange( ) function,
     * essentially resetting the drawing parameters for new, dynamic input detection. This method
     * essentially clears the currently stored bitmap (instance variable) and automatically enables
     * touch drawing, which was decided as an appropriate setting. This gives users control over what
     * get's drawn when as soon as the canvas is cleared.
     */
    private void clearCanvas(){
        // Reset the scaling factors used in our mapRange( ) function that relate to color, size and light.
        // These values dynamically change depending on the largest input seen thus far by our program
        // as well as the sensors.
        begin = false;
        magnitude = 3.14f;
        colorMagnitude = 1f;
        lightMagnitude = 1f;
        sizeMagnitude = 1f;
        this.bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
        this.bitmap.eraseColor(Color.WHITE);
        this.imageView.setImageBitmap(bitmap);
        enableSensor(false);
    }

    /**
     * enableSensor( ) - Enables sensors in our application based on the option selected.
     * The main sensor we toggle ON/OFF based on the navigation bar option is the GYROSCOPE
     * @param gyro corresponds to whether gyroscope sensor input will be used to dynamically map
     *             positions on the drawing canvas.
     */
    private void enableSensor(boolean gyro){
        if (gyro) {
            gyroIsEnabled = true;
            touchIsEnabled = false;
            // Get the latest preferences for enabling/disabling spiralMotion in addition to radialMotion
            // This is important when users switch between settings. If touch was disabled, we must
            // retrieve the user preference specifically for the amimations or the "special motion".
            spiralMotion = sharedPreferences.getBoolean("spiralMotion", false);
            radialMotion = sharedPreferences.getBoolean("radialMotion", false);

        } else {
            gyroIsEnabled = false;
            touchIsEnabled = true;
            spiralMotion = false;
            radialMotion = false;
        }
        // If there is no current bitmap, make a new bitmap with a white background fill color.
        // Our code should never reach this point, but as a precaution, this block of code
        // was added to avoid null pointer exceptions when users switch between various drawing modes
        // as they use the application for drawing purposes.
        if(this.bitmap == null) {
            this.bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
            this.bitmap.eraseColor(Color.WHITE);
        }

        // Retrieve the current imageView dimensions. This was added for when enableSensor( ) is
        // called upon clearing the canvas. Width and height should NOT change. But we added these
        // assignment statements to assure that the instance variables for width and height would
        // always correspond to the current imageView object.
        width = imageView.getWidth();
        height = imageView.getHeight();

        // Generate new spiral/radial object animations whenever the user changes modes. This resets
        // the internal instance variables of these objects, thus starting the animation from the same
        // canvas drawing point whenever settings are modified.
        this.spiral = new Spiral(width, height);
        this.radial = new Radial(width, height);

        this.begin = true;

    }

    /**
     * saveImage( ) - Saves the current bitmap to the phone/tablet's gallery and potentially uploads it to the
     * community FireBase database along with a title, description, and optional username. Upon clicking
     * the Save button on the BottomNavigationView, this corresponding saveImage( ) method is called.
     * The user is presented with a pop-up allowing one to enter a Title and Description for the
     * generated Canvas bitmap. Clicking Save JUST locally stores the image/artwork to the phone's
     * gallery. Clicking Save/Upload uploads the image to FireBase AND saves it to the phone's
     * local gallery folder. An AlertDialog was used for consistent UI purposes and appropriate
     * user interaction during the saving process.
     */
    private void saveImage(){
        // Stop the animation in background such that the user is saving the most current state of
        // their application. Set begin = false.
        begin = false;

        // Create an AlertDialog that prompts the user for a Title, Description, and the option
        // to save the Bitmap locally as a JPG, or also upload it to the cloud through FireBase to
        // appear in the community gallery.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save/Upload Image");

        // Create a custom LinearLayout to hold the corresponding EditText objects as well as
        // the positive/negative buttons, corresponding to Save and Save/Upload respectively.
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText title = new EditText(this);
        title.setHint("t i t l e");
        final EditText description = new EditText(this);
        description.setHint("d e s c r i p t i o n");

        // Specify the type of input expected. This is for the image name as well as the description.
        title.setInputType(InputType.TYPE_CLASS_TEXT);
        description.setInputType(InputType.TYPE_CLASS_TEXT);

        // Add the corresponding UI elements to the dynamically created Linear Layout.
        layout.addView(title);
        layout.addView(description);

        // Set the builder view to the layout that was just created programmatically. This is
        // equivalent to writing a separate XML file. However, we found it simpler to implement
        // this feature programmatically because of its small size and limited implementation.
        builder.setView(layout);

        // A POSITIVE button usually refers to an OK option in such a dialog. Clicking on Save stores
        // the image locally in the phone gallery, and dismisses the popup automatically so that the
        // user can efficiently return to the canvas view and continue creating dynamic artwork.
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                saveString = title.getText().toString();
                descriptionString = description.getText().toString() + " - " + username;
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, saveString, descriptionString);

            }
        });

        // A NEGATIVE button usually refers to a CANCEL option. However, we re-purposed this to include
        // a second separate upload option, allowing the user to upload their generated Bitmap to the
        // community gallery.
        builder.setNegativeButton("Save and Upload", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                saveString = title.getText().toString();
                descriptionString = description.getText().toString() + " - " + username;
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, saveString, descriptionString);

                // To work with our Realtime Database in FireBase, we converted the Bitmap to a
                // Base64 String, which allows us to store it directly in our object hierarchy in the cloud.
                // First we create a byte output stream, compress the bitmap, and then convert the bitmap
                // to a ByteArray [ ]. This in turn allows us to create an encoded image using the Base64 class
                // in Java/Android.
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); //bm is the bitmap object
                byte[] byteArray = baos.toByteArray();

                // Save the final encoded image in an appropriate String.
                encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);



                // Get the count of the last picture in the data base
                // Write a message to the database
                final FirebaseDatabase database = FirebaseDatabase.getInstance();
                final DatabaseReference myRef = database.getReference("imageNum");

                // Read from the database
                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // This method is called once with the initial value and again
                        // whenever data at this location is updated.
                        long value = dataSnapshot.getValue(Long.class);
                        imageNum = (int) value;


                        imageNum++;

                        DatabaseReference newImageRef = database.getReference(String.valueOf(imageNum));

                        ImageFB imagefb = new ImageFB(saveString, descriptionString, encodedImage);

                        newImageRef.setValue(imagefb);

                        myRef.setValue(imageNum);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });



        builder.show();

    }

    /**
     * createAudioThread( ) - Create an AudioThread for tarosDSP such that we can detect PITCH and LOUDNESS. We will then
     * appropriately map values to various aspects of the user's drawing experience, integrating the
     * environment with dynamic artistic expression.
     * This method is required according to the attached documentation for tarsosDSP.
     * https://github.com/JorenSix/TarsosDSP
     */
    public void createAudioThread(){

        // Audio Instance Variables: indicate that we have already started a thread. This is important
        // to keep track of. If more threads are made, the input size doubles/triples depending on how
        // many more thread instances are actively running.
        audioThread = true;

        // Get the default audio device from the Android phone/tablet. This is a method provided
        // directly by the TarsosDSP library and audio implementations.
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);

        //Setup loudness detection.
        this.threshold = SilenceDetector.DEFAULT_SILENCE_THRESHOLD;
        silenceDetector = new SilenceDetector(threshold, false);
        dispatcher.addAudioProcessor(silenceDetector);
        dispatcher.addAudioProcessor(this);


        // Whenever a pitch is detected, call the processPitch( ) function which converts a value
        // in Hx to a corresponding color value.This thread uses an FFT pitching detection/approximation
        // algorithm based strong/clear tones or tonic changes. The PitchDetectionHandler( ) runs for
        // the duration of our application, as long as it is open. This allows users to record
        // environmental conditions/loudness/brightness even when the app is in a paused application state.
        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                final float pitchInHz = pitchDetectionResult.getPitch();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processPitch(pitchInHz);
                    }
                });
            }
        };

        // Create a new pitchProcessor, which references the PitchDetectionHandler( ) created above. This gives it
        // direct access to the correct audio thread (separate from the UI thread)
        AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pdh);
        dispatcher.addAudioProcessor(pitchProcessor);

        // Create and run a new AudioThread which will begin to collect data and funnel it to the PitchDetectionHandler.
        audioThreadRef = new Thread(dispatcher, "Audio Thread");
        audioThreadRef.start();

    }

    /**
     * Process incoming Hz sound data and convert it to a unique color.
     * @param pitchInHz generated from the pitchProcessor( ) and sent to the PitchDetectionHandler( ) reference.
     */
    public void processPitch(float pitchInHz) {

        if(pitchInHz != -1) {

            this.color = soundToColor(pitchInHz);
        }
    }

    /**
     * mapRange( ) - MapRange function that takes a given value VAL and scales it between a minimum destination
     * value and a maximum destination value. We separated the various cases for debugging purposes, since it
     * highlights what return values are being used/associated with a particular variable.
     * @param min destination value
     * @param max destination value
     * @param val value to scale that is within range magnitude
     * @return scaled number between min and max
     */
    public int mapRange(int aCase, int min, int max, float val) {

        switch(aCase){
            case 0:
                if (Math.abs(val) > magnitude) {
                    magnitude = Math.abs(val);
                }

                return min + (((int)(val * 1000) + (int)(magnitude*1000))*(max - min) / (int)(magnitude*2000));
            case 1:
                if (Math.abs(val) > colorMagnitude) {
                    colorMagnitude = Math.abs(val);
                }
                return min + (((int)(val * 1000) + (int)(colorMagnitude*1000))*(max - min) / (int)(colorMagnitude*2000));
            case 2:
                if (Math.abs(val) > lightMagnitude) {
                    lightMagnitude = Math.abs(val);
                }

                return min + (((int)(val * 1000) + (int)(lightMagnitude*1000))*(max - min) / (int)(lightMagnitude*2000));
            case 3:
                if (Math.abs(val) > sizeMagnitude) {
                    sizeMagnitude = Math.abs(val);
                }

                return min + (((int)(val * 1000) + (int)(sizeMagnitude*1000))*(max - min) / (int)(sizeMagnitude*2000));
        }

        return -1;
    }

    /**
     * drawSomething( ) -  This function draws either a square, circle, or rectangle at a given
     * location on the drawing canvas. It retrieves the latest color determined by our soundToColor( )
     * method, and applies it to the paint object. There are 3 different strokes for the 3 different
     * shapes, and the stroke is retrieved from SharedUserPreferences during onResume( ) when all
     * relevant preferences are loaded into our application.
     * @param x location to draw a circle, square, or triangle
     * @param y location to draw a circle, square, or triangle
     * @param bitmap represents the current bitmap to draw on. It is passed by reference.
     */
    public void drawSomething(int x, int y, Bitmap bitmap) {
        // As long as we have a valid canvas to draw on. We had to add this in because of concurrency
        // issues with when sensor data was being read in to the application.
        if (width > 0) {
            int radius = this.sizeRadius;

            paint.setARGB(this.opacity, this.color[0], this.color[1],this.color[2]);

            // Our true mod( ) function will wrap values as expected. We use this for the X and Y coordinates.
            int locX = mod(x, width);
            int locY = mod(y, height);

            // Associate imageView with the passed bitmap
            imageView.setImageBitmap(bitmap);

            // Initialize canvas and give it the passed bitmap
            canvas = new Canvas(bitmap);

            // Get the current stroke type from SharedUserPreferences (saved as instance data)
            int stroke = Integer.parseInt(strokeType);
            switch(stroke){
                case 0:
                    // Draw a circle onto the canvas with the given paint object
                    canvas.drawCircle(locX, locY, radius, paint);
                    break;
                case 1:
                    // Draw a rectangle onto the canvas with the given paint object
                    canvas.drawRect(locX - sizeRadius/2, locY - sizeRadius/2, locX + sizeRadius/2, locY + sizeRadius/2, paint);
                    break;
                case 2:
                    // 3 Point( ) objects define a triangle
                    paint.setStyle(Paint.Style.FILL);
                    Point point1_draw = new Point();
                    Point point2_draw = new Point();
                    Point point3_draw = new Point();

                    // Set the coordinates of the 3 points appropriately
                    point1_draw.set(locX, locY);
                    point2_draw.set(locX + sizeRadius / 2, locY - sizeRadius/2);
                    point3_draw.set(locX - sizeRadius / 2, locY - sizeRadius/2);

                    // Generate a Path( ) object with the new points
                    Path path = new Path();
                    path.setFillType(Path.FillType.EVEN_ODD);
                    path.moveTo(point1_draw.x,point1_draw.y);
                    path.lineTo(point2_draw.x,point2_draw.y);
                    path.lineTo(point3_draw.x,point3_draw.y);
                    path.lineTo(point1_draw.x,point1_draw.y);
                    path.close();

                    // Draw the 3 Point( ) path onto the canvas with the given paint object.
                    canvas.drawPath(path, paint);
                    break;
            }
            // Tell the canvas we are done writing to it before exiting the drawSomething( ) function
            imageView.invalidate();
        }
    }

    /**
     * A true mod function that computes the modulus similar to Python. X % Y where X and Y refer
     * to the parameters of the function.
     * @param x mod(x, y) = x % y
     * @param y mod(x, y) = x % y
     * @return the modulus as it is computed in Python.
     */
    public int mod(int x, int y){
        if(y == 0) return 0;
        if(x > 0) return x % y;
        return y - (Math.abs(x) % y);
    }

    /**
     * onSensorChanged( ) - gets various sensor inputs as they are detected by the device's sensors.
     * Sensors include GYROSCOPE & LIGHT. This method also contains control flow for automated
     * drawing mechanisms represented by our Spiral and Radial classes. This gives the user control
     * and flexibility over what drawing mode they would like to use to create their drawing.
     * @param sensorEvent is the sensor that was detected by the onSensorChange( ) listener.
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Get the type of the event triggering this method call
        int sensorType = sensorEvent.sensor.getType();

        if(begin) {

            switch (sensorType) {

                case Sensor.TYPE_GYROSCOPE:
                    if(this.radialMotion && radial != null){
                        int[] coordinates = radial.next();
                        for(int i = 0; i < 8; i += 2){
                            drawSomething(coordinates[i], coordinates[i+1], this.bitmap);
                        }
                    }
                    else if(this.spiralMotion && spiral != null){
                        int[] coordinates = spiral.next();
                        this.x = coordinates[0];
                        this.y = coordinates[1];
                        drawSomething(this.x, this.y, this.bitmap);
                    }else if(gyroIsEnabled) {
                        float[] gyroscopeValues = sensorEvent.values;
                        this.x = mapRange(0, 0, imageView.getWidth(), gyroscopeValues[1]);
                        this.y = mapRange(0, 0, imageView.getHeight(), gyroscopeValues[0]);
                        drawSomething(this.x, this.y, this.bitmap);
                    }else if(!touchIsEnabled) {
                        drawSomething(this.x, this.y, this.bitmap);
                    }
                    break;
                case Sensor.TYPE_LIGHT:
                    if(light_switch) {
                        float currentValue = sensorEvent.values[0];
                        this.opacity = 75 - mapRange(2, 0, 70, currentValue);
                    }else{
                        this.opacity = 150;
                    }
                    break;
            }

        }
    }

    /**
     * onAccuracyChanged( ) - Override method to integrate with the onSensorChanged( ) method as part
     * of basic sensor implementations in Android.
     * @param sensor
     * @param i
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * soundToColor( ) - Function defined to map hertz values directly to corresponding colors in
     * the classic representation of a color wheel. Colors go from red to orange, yellow, green, to blue.
     * This function directly uses our mapRange( ) function, showing its versatility throughout our
     * application.
     * @param hertz representing the detected pitch by the PitchDetectionHandler object and thread. Hz
     *              is -1 if no pitch is detected.
     * @return a color array of [r, g, b] after the computation
     */
    public int[] soundToColor(float hertz){

        if (hertz > 0) {


            // Map Hz from 25.4 and 1760 -----> 0 and 1530
            int color = mapRange(1, 200, 1530, hertz);
            int[] returnColor = new int[3];
            int mod = mod(color, 255);

            if (color <= 255) {
                returnColor[0] = 255;
                returnColor[1] = mod;
                returnColor[2] = 0;
            } else if (color <= 255 * 2) {
                returnColor[0] = 255 - mod;
                returnColor[1] = 255;
                returnColor[2] = 0;
            } else if (color <= 255 * 3) {
                returnColor[0] = 0;
                returnColor[1] = 255;
                returnColor[2] = mod;
            } else if (color <= 255 * 4) {
                returnColor[0] = 0;
                returnColor[1] = 255 - mod;
                returnColor[2] = 255;
            } else if (color <= 255 * 5) {
                returnColor[0] = mod;
                returnColor[1] = 0;
                returnColor[2] = 255;
            } else {
                returnColor[0] = 255;
                returnColor[1] = 0;
                returnColor[2] = 255 - mod;
            }

            return returnColor;

        }

        return new int[]{color[0], color[1], color[2]};
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        handleSound();
        return true;
    }

    /**
     * handleSound( ) - function to change the size of the stroke width based on detected volume.
     * This function assumes a baseline silence level (defined by TarsosDSP). This function also
     * uses our defined mapRange( ) function to dynamically adjust to the loudest volume level,
     * and provide an interactive user experience while getting sensor input.
     */
    private void handleSound(){

        if(silenceDetector.currentSPL() > threshold && volume_switch){
            this.sizeRadius = mapRange(3, 10, height, (float) silenceDetector.currentSPL());

        }else{
            this.sizeRadius = 10;
        }
    }

    @Override
    public void processingFinished() {

    }
}
