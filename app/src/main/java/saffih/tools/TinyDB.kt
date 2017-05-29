package saffih.tools

/*
 * Copyright 2014 KC Ochibili
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 *  The "‚‗‚" character is not a comma, it is the SINGLE LOW-9 QUOTATION MARK unicode 201A
 *  and unicode 2017 that are used for separating the items in a list.
 */


//import com.google.gson.Gson;

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class TinyDB(appContext: Context) {

    val preferences: SharedPreferences
    private var DEFAULT_APP_IMAGEDATA_DIRECTORY: String? = null
    /**
     * Returns the String path of the last saved image
     * @return string path of the last saved image
     */
    var savedImagePath = ""
        private set

    init {
        preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
    }


    /**
     * Decodes the Bitmap from 'path' and returns it
     * @param path image path
     * *
     * @return the Bitmap from 'path'
     */
    fun getImage(path: String): Bitmap? {
        var bitmapFromPath: Bitmap? = null
        try {
            bitmapFromPath = BitmapFactory.decodeFile(path)

        } catch (e: Exception) {
            // TODO: handle exception
            e.printStackTrace()
        }

        return bitmapFromPath
    }


    /**
     * Saves 'theBitmap' into folder 'theFolder' with the name 'theImageName'
     * @param theFolder the folder path dir you want to save it to e.g "DropBox/WorkImages"
     * *
     * @param theImageName the name you want to assign to the image file e.g "MeAtLunch.png"
     * *
     * @param theBitmap the image you want to save as a Bitmap
     * *
     * @return returns the full path(file system address) of the saved image
     */
    fun putImage(theFolder: String?, theImageName: String?, theBitmap: Bitmap?): String? {
        if (theFolder == null || theImageName == null || theBitmap == null)
            return null

        this.DEFAULT_APP_IMAGEDATA_DIRECTORY = theFolder
        val mFullPath = setupFullPath(theImageName)

        if (mFullPath != "") {
            savedImagePath = mFullPath
            saveBitmap(mFullPath, theBitmap)
        }

        return mFullPath
    }


    /**
     * Saves 'theBitmap' into 'fullPath'
     * @param fullPath full path of the image file e.g. "Images/MeAtLunch.png"
     * *
     * @param theBitmap the image you want to save as a Bitmap
     * *
     * @return true if image was saved, false otherwise
     */
    fun putImageWithFullPath(fullPath: String?, theBitmap: Bitmap?): Boolean {
        return !(fullPath == null || theBitmap == null) && saveBitmap(fullPath, theBitmap)
    }

    /**
     * Creates the path for the image with name 'imageName' in DEFAULT_APP.. directory
     * @param imageName name of the image
     * *
     * @return the full path of the image. If it failed to create directory, return empty string
     */
    private fun setupFullPath(imageName: String): String {
        val mFolder = File(Environment.getExternalStorageDirectory(), DEFAULT_APP_IMAGEDATA_DIRECTORY!!)

        if (isExternalStorageReadable && isExternalStorageWritable && !mFolder.exists()) {
            if (!mFolder.mkdirs()) {
                Log.e("ERROR", "Failed to setup folder")
                return ""
            }
        }

        return mFolder.path + '/' + imageName
    }

    /**
     * Saves the Bitmap as a PNG file at path 'fullPath'
     * @param fullPath path of the image file
     * *
     * @param bitmap the image as a Bitmap
     * *
     * @return true if it successfully saved, false otherwise
     */
    private fun saveBitmap(fullPath: String?, bitmap: Bitmap?): Boolean {
        if (fullPath == null || bitmap == null)
            return false

        var fileCreated = false
        var bitmapCompressed = false
        var streamClosed = false

        val imageFile = File(fullPath)

        if (imageFile.exists())
            if (!imageFile.delete())
                return false

        try {
            fileCreated = imageFile.createNewFile()

        } catch (e: IOException) {
            e.printStackTrace()
        }

        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(imageFile)
            bitmapCompressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)

        } catch (e: Exception) {
            e.printStackTrace()
            bitmapCompressed = false

        } finally {
            if (out != null) {
                try {
                    out.flush()
                    out.close()
                    streamClosed = true

                } catch (e: IOException) {
                    e.printStackTrace()
                    streamClosed = false
                }

            }
        }

        return fileCreated && bitmapCompressed && streamClosed
    }

    // Getters

    /**
     * Get int value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     * @param key SharedPreferences key
     * *
     * @param defaultValue int value returned if key was not found
     * *
     * @return int value at 'key' or 'defaultValue' if key not found
     */
    fun getInt(key: String): Int {
        return preferences.getInt(key, 0)
    }

    /**
     * Get parsed ArrayList of Integers from SharedPreferences at 'key'
     * @param key SharedPreferences key
     * *
     * @return ArrayList of Integers
     */
    fun getListInt(key: String): List<Int> {
        return getListString(key).map { Integer.parseInt(it) }
    }

    /**
     * Get long value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     * @param key SharedPreferences key
     * *
     * @param defaultValue long value returned if key was not found
     * *
     * @return long value at 'key' or 'defaultValue' if key not found
     */
    fun getLong(key: String, defaultValue: Long): Long {
        return preferences.getLong(key, defaultValue)
    }

    /**
     * Get float value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     * @param key SharedPreferences key
     * *
     * @param defaultValue float value returned if key was not found
     * *
     * @return float value at 'key' or 'defaultValue' if key not found
     */
    fun getFloat(key: String): Float {
        return preferences.getFloat(key, 0f)
    }

    /**
     * Get double value from SharedPreferences at 'key'. If exception thrown, return 'defaultValue'
     * @param key SharedPreferences key
     * *
     * @param defaultValue double value returned if exception is thrown
     * *
     * @return double value at 'key' or 'defaultValue' if exception is thrown
     */
    fun getDouble(key: String, defaultValue: Double): Double {
        val number = getString(key)

        try {
            return java.lang.Double.parseDouble(number)

        } catch (e: NumberFormatException) {
            return defaultValue
        }

    }

    /**
     * Get parsed ArrayList of Double from SharedPreferences at 'key'
     * @param key SharedPreferences key
     * *
     * @return ArrayList of Double
     */
    fun getListDouble(key: String): List<Double> {
        return getListString(key).map { java.lang.Double.parseDouble(it) }
    }

    /**
     * Get String value from SharedPreferences at 'key'. If key not found, return ""
     * @param key SharedPreferences key
     * *
     * @return String value at 'key' or "" (empty String) if key not found
     */
    fun getString(key: String): String {
        return preferences.getString(key, "")
    }

    /**
     * Get parsed ArrayList of String from SharedPreferences at 'key'
     * @param key SharedPreferences key
     * *
     * @return ArrayList of String
     */
    fun getListString(key: String): List<String> {
        return getString(key).split("‚‗‚")
    }

    fun getStrings(key: String): List<String> {
        return getStringsItems(key).unzip().second
    }

    fun getStringsItems(key: String): List<Pair<String, String>> {
        val sizeKey = "${key}‚‗‚size"
        val size = getInt(sizeKey)

        return (0..size - 1).map { keyOf(key, it) }.map { it to getString(it) }
    }

    fun putStrings(key: String, lst: List<String>) {
        putInt(key, lst.hashCode())
        val sizeKey = "${key}‚‗‚size"
        val size = getInt(sizeKey)
        val newSize = lst.size

        lst.forEachIndexed { index, i -> putString(keyOf(key, index), i) }
        (newSize..size).forEach { putString(keyOf(key, it), "") }
        putInt(sizeKey, newSize)
    }


    /**
     * Get boolean value from SharedPreferences at 'key'. If key not found, return 'defaultValue'
     * @param key SharedPreferences key
     * *
     * @param defaultValue boolean value returned if key was not found
     * *
     * @return boolean value at 'key' or 'defaultValue' if key not found
     */
    fun getBoolean(key: String): Boolean {
        return preferences.getBoolean(key, false)
    }

    /**
     * Get parsed ArrayList of Boolean from SharedPreferences at 'key'
     * @param key SharedPreferences key
     * *
     * @return ArrayList of Boolean
     */
    fun getListBoolean(key: String): List<Boolean> {
        return getListString(key).map { (it == "true") }
    }


    //    public ArrayList<Object> getListObject(String key, Class<?> mClass){
    //    	Gson gson = new Gson();
    //
    //    	ArrayList<String> objStrings = getListString(key);
    //    	ArrayList<Object> objects =  new ArrayList<Object>();
    //
    //    	for(String jObjString : objStrings){
    //    		Object value  = gson.fromJson(jObjString,  mClass);
    //    		objects.add(value);
    //    	}
    //    	return objects;
    //    }


    //    public  Object getObject(String key, Class<?> classOfT){
    //
    //        String json = getString(key);
    //        Object value = new Gson().fromJson(json, classOfT);
    //        if (value == null)
    //            throw new NullPointerException();
    //        return value;
    //    }


    // Put methods

    /**
     * Put int value into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param value int value to be added
     */
    fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    /**
     * Put ArrayList of Integer into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param intList ArrayList of Integer to be added
     */
    fun putListInt(key: String, intList: List<Int>) {
        val myIntList = intList.toTypedArray()
        val toSave = myIntList.joinToString("‚‗‚")
        preferences.edit().putString(key, toSave).apply()
    }

    /**
     * Put long value into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param value long value to be added
     */
    fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    /**
     * Put float value into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param value float value to be added
     */
    fun putFloat(key: String, value: Float) {
        preferences.edit().putFloat(key, value).apply()
    }

    /**
     * Put double value into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param value double value to be added
     */
    fun putDouble(key: String, value: Double) {

        putString(key, value.toString())
    }

    /**
     * Put ArrayList of Double into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param doubleList ArrayList of Double to be added
     */
    fun putListDouble(key: String, doubleList: List<Double>) {
        checkForNullKey(key)
        val myDoubleList = doubleList.toTypedArray()
        preferences.edit().putString(key, myDoubleList.joinToString("‚‗‚")).apply()
    }

    /**
     * Put String value into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param value String value to be added
     */
    fun putString(key: String, value: String) {
        checkForNullKey(key)
        checkForNullValue(value)
        preferences.edit().putString(key, value).apply()
    }

    /**
     * Put ArrayList of String into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param stringList ArrayList of String to be added
     */
    fun putListString(key: String, stringList: List<String>) {
        val myStringList = stringList.toTypedArray()
        val toSave = myStringList.joinToString("‚‗‚")
        preferences.edit().putString(key, toSave).apply()
    }

    /**
     * Put boolean value into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param value boolean value to be added
     */
    fun putBoolean(key: String, value: Boolean) {

        preferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Put ArrayList of Boolean into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param boolList ArrayList of Boolean to be added
     */
    fun putListBoolean(key: String, boolList: List<Boolean>) {
        checkForNullKey(key)

        val newList = boolList.map { if (it) "true" else "false" }
        putListString(key, newList)
    }

    /**
     * Put ObJect any type into SharedPrefrences with 'key' and save
     * @param key SharedPreferences key
     * *
     * @param obj is the Object you want to put
     */
    //    public void putObject(String key, Object obj){
    //    	checkForNullKey(key);
    //    	Gson gson = new Gson();
    //    	putString(key, gson.toJson(obj));
    //    }
    //
    //    public void putListObject(String key, ArrayList<Object> objArray){
    //    	checkForNullKey(key);
    //    	Gson gson = new Gson();
    //    	ArrayList<String> objStrings = new ArrayList<String>();
    //    	for(Object obj : objArray){
    //    		objStrings.add(gson.toJson(obj));
    //    	}
    //    	putListString(key, objStrings);
    //    }

    /**
     * Remove SharedPreferences item with 'key'
     * @param key SharedPreferences key
     */
    fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    /**
     * Delete image file at 'path'
     * @param path path of image file
     * *
     * @return true if it successfully deleted, false otherwise
     */
    fun deleteImage(path: String): Boolean {
        return File(path).delete()
    }


    /**
     * Clear SharedPreferences (remove everything)
     */
    fun clear() {
        preferences.edit().clear().apply()
    }

    /**
     * Retrieve all values from SharedPreferences. Do not modify collection return by method
     * @return a Map representing a list of key/value pairs from SharedPreferences
     */
    val all: Map<String, *>
        get() = preferences.all


    /**
     * null keys would corrupt the shared pref file and make them unreadable this is a preventive measure
     * @param the pref key
     */
    fun checkForNullKey(key: String?) {
        if (key == null) {
            throw NullPointerException()
        }
    }

    /**
     * null keys would corrupt the shared pref file and make them unreadable this is a preventive measure
     * @param the pref key
     */
    fun checkForNullValue(value: String?) {
        if (value == null) {
            throw NullPointerException()
        }
    }

    companion object {
        fun keyOf(key: String, index: Int) = "${key}‚‗‚${index}"


        /**
         * Check if external storage is writable or not
         * @return true if writable, false otherwise
         */
        val isExternalStorageWritable: Boolean
            get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()

        /**
         * Check if external storage is readable or not
         * @return true if readable, false otherwise
         */
        val isExternalStorageReadable: Boolean
            get() {
                val state = Environment.getExternalStorageState()

                return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
            }
    }
}