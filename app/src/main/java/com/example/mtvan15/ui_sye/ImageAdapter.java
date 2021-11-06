package com.example.mtvan15.ui_sye;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Adapter class to effectively read in data, and map it accordingly in a Recycler View UI Object.
 * This class maintains a reference to the corresponding RecyclerView activity (Community) for
 * proper implementation. The ImageAdapter( ) class maintains an active list of Recycler View entries
 * retrieved from the Database to actively display within the Recycler View.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.MyViewHolder> {

    // Keep a reference to the activity.
    private Activity activity;

    // Keep a reference to a Bitmap.
    private Bitmap placeHolder;

    // List for the ImageUpload Objects.
    private List<ImageUpload> imageUploadList;

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // Inflate our own imageView for the Recycler View.
        View imageView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rv_item, viewGroup, false);
        return new MyViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        // Retrieve the i-th ImageUpload object from the imageUploadList.
        final ImageUpload anImage = imageUploadList.get(i);

        // Set the image, title, and description of the view holder using object instance data.
        myViewHolder.image.setImageBitmap(anImage.getBitmap());
        myViewHolder.title.setText(anImage.getTitle());
        myViewHolder.description.setText(anImage.getDescription());

        // Set an onClickListener( ) for the button contained in the RecyclerView ViewHolder.
        // This button will enable users to download bitmaps directly from FireBase, and transfer them
        // to their own canvas for creativity purposes.
        myViewHolder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the most recent SharedPreferences for the application
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);

                // Convert a given bitmap to a Base64 String similarly to the implementation in MainActivity
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                anImage.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
                byte[] byteArray = baos.toByteArray();

                // Encode the byteArray[ ] into a Base64 string object
                String encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);

                // Place the encoded string in SharedPreferences so that it can be accessed from MainActivity
                sharedPreferences.edit().putString("background", encodedImage).apply();
                Log.i("ImageSaved",encodedImage);
                activity.finish();
            }
        });
    }

    /**
     * Return the number of ImageUploadObjects to display in the RecyclerView UI element.
     * @return
     */
    @Override
    public int getItemCount() {
        return imageUploadList.size();
    }

    /**
     * MyViewHolder - class that creates instance states of UI objects for an rv_item (as defined in XML).
     * ViewHolders consist of an image, title, description, and a button. They all have corresponding ID's
     * in the project, and just need to be linked to their instance states in this small class.
     */
    public class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView title;
        TextView description;
        Button button;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.rv_item_image);
            title = itemView.findViewById(R.id.rv_item_title);
            description = itemView.findViewById(R.id.rv_item_description);
            button = itemView.findViewById(R.id.saveButton);
        }
    }

    /**
     * ImageAdapter Constructor. Maintains a reference to the Community Activity, and associates
     * an imageUploadList with instance state data. This is a necessary component of the Recycler View.
     * @param imageUploadList is a list consisting of ImageUpload objects to be read into the Recycler View.
     * @param activity is a reference to Community Activity in the application.
     */
    public ImageAdapter(List<ImageUpload> imageUploadList, Activity activity){
        this.activity = activity;
        this.imageUploadList = imageUploadList;
    }
}
