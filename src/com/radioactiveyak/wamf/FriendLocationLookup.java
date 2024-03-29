package com.radioactiveyak.wamf;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.util.Log;

/**
 * Static class used to find the location of each contact
 * in the Contact Content Provider by reverse-geocoding their
 * home address.
 * 
 * @author Reto Meier
 * Author of Professional Android Application Development
 * http://www.amazon.com/gp/product/0470344717?tag=interventione-20
 *
 */
public class FriendLocationLookup {
 
  /**
   * Contact list query result cursor.
   */
  public static Cursor cursor;
  
  /**
   * Contact's home address query result cursor.
   */
  public static Cursor addressCursor;  
  
  /**
   * Return a hash of contact names and the
   * reverse-geocoded location of their home addresses.
   * 
   * @param context Calling application's context
   * @return Hash of contact names with their physical locations
   */
  public static HashMap<String, Location> GetFriendLocations(Context context) {
    HashMap<String, Location> result = new HashMap<String, Location>();

    // Return a query result of all the peope in the contact list.
    cursor = context.getContentResolver().query(People.CONTENT_URI, null, null, null, null);

    // Use the convenience properties to get the index of the columns
    int nameIdx = cursor.getColumnIndexOrThrow(People.NAME);
    int personID = cursor.getColumnIndexOrThrow(People._ID);    
    
    if (cursor.moveToFirst())
    do {
      // Extract the name.
      String name = cursor.getString(nameIdx);
      String id = cursor.getString(personID);
        
      // Extract the address.
      String where = Contacts.ContactMethods.PERSON_ID + " == " + id + 
                     " AND " +
                     Contacts.ContactMethods.KIND + " == " + Contacts.KIND_POSTAL;

      addressCursor = context.getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, null, where, null, null);

      // Extract the postal address from the cursor
      int postalAddress = addressCursor.getColumnIndexOrThrow(Contacts.ContactMethodsColumns.DATA);
      String address = "";
      if (addressCursor.moveToFirst())
        address = addressCursor.getString(postalAddress);
      addressCursor.close();
      
      // Reverse geocode the postal address to get a location.
      Location friendLocation = new Location("reverseGeocoded");
      
      if (address != null) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
          List<Address> addressResult = geocoder.getFromLocationName(address, 1);
          if (!addressResult.isEmpty()) {
            Address resultAddress = addressResult.get(0);
            friendLocation.setLatitude(resultAddress.getLatitude());
            friendLocation.setLongitude(resultAddress.getLongitude());
          }
        } catch (IOException e) {
         Log.d("Contact Location Lookup Failed", e.getMessage());
        }
      }      
      
      // Populate the result hash
      result.put(name, friendLocation);
      
    } while(cursor.moveToNext());

    // Cleanup the cursor.
    cursor.close();
    
    return result;
  }
}