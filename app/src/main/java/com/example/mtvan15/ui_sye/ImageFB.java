package com.example.mtvan15.ui_sye;

/**
 * ImageFB( ) class is used to read/write image information to FireBase, including Titles, Descriptions, and the Base64 Image String.
 * FireBase calls write/read JSON objects into instances of the ImageFB class for simplicity. This allows
 * for extensive database calls, while maintaining an object oriented approach to application design.
 */
public class ImageFB {
    // Instance variables for the data that we will be reading in from the database or...
    // Writing into the database
    public String title;
    public String description;
    public String image;

    /**
     * Default ImageFB( ) constructor for FireBase implementations.
     */
    public ImageFB(){

    }

    /**
     * ImageFB( ) constructor to read in instance data for the object and set instance variables accordingly.
     * @param title string representation of the title of the image
     * @param description string representation of the description of the image
     * @param image Base64 string representation of an image bitmap.
     */
    public ImageFB(String title, String description, String image){
        // Set the instance variables accordingly.
        this.title = title;
        this.description = description;
        this.image = image;
    }

    /**
     * getTitle( ) - getter method for TITLE instance data.
     * @return the string representation of the image.
     */
    public String getTitle() {
        return title;
    }

    /**
     * setTitle( ) - setter method for TITLE instance data.
     * @param title the string representation of the image.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * getDescription( ) - getter method for DESCRIPTION instance data.
     * @return the string representation of the image description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * setDescription( ) - setter method for DESCRIPTION instance data.
     * @param description the string representation of the image description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * getImage( ) - getter method for IMAGE instance data. Returns Base64 representation of an image.
     * @return the Base64 string representation of an image bitmap
     */
    public String getImage() {
        return image;
    }

    /**
     * setImage( ) - setter method for IMAGE instance data.
     * @param image the Base64 string representation of an image bitmap.
     */
    public void setImage(String image) {
        this.image = image;
    }
}
