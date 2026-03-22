package com.example.christophers.soundgeneration;

import android.content.Context;
import android.util.ArraySet;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by christopherS on 09.03.2017.
 */

public class ProfileHandler {
    Set<Profile> currentProfiles;
    MainActivity main;
    Context currentContext;

    public ProfileHandler(Context currentContext, MainActivity main){
        currentProfiles = new LinkedHashSet<>();
        this.main = main;
        this.currentContext = currentContext;

        File profilesFile = new File(currentContext.getFilesDir().getAbsolutePath()+"/profiles.ser");
        try{
            loadProfilesFromDisk();
        }catch (Exception e) {
            e.printStackTrace();
            createNewProfiles();
        }
    }

    private void createNewProfiles() {
        Profile defaultProf = new Profile(100,100,false,4,0,0, "Standard", false, false);
        currentProfiles.clear();
        currentProfiles.add(defaultProf);
    }

    public boolean deleteProfile(String name){
        Profile dummyProfile = new Profile(0,0,false,0,0,0,name, false, false);
        boolean success = currentProfiles.remove(dummyProfile);
        if(success){
            safeProfilesToDisk();
            return true;
        }
        else return false;
    }


    public boolean storeProfile(Profile profile){
        if(contains(profile)){  // Kollisionsabfrage der Namen
            return false;
        }
        else {
            currentProfiles.add(profile);
            safeProfilesToDisk();
            return true;
        }
    }

    private boolean contains(Profile newProf) {
        return currentProfiles.contains(newProf);
    }

    public boolean overwriteProfile(Profile profile){
        if(currentProfiles.remove(profile)){
            currentProfiles.add(profile);
            return true;
        }
        return false;
    }

    public Set<Profile> getAvailableProfiles(){
        return currentProfiles;
    }

    private void safeProfilesToDisk(){
        try {
            FileOutputStream fos = currentContext.openFileOutput("profiles.ser", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(currentProfiles);
            os.close();
            fos.close();
        }   catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadProfilesFromDisk() throws Exception {
        FileInputStream fis = currentContext.openFileInput("profiles.ser");
        ObjectInputStream is = new ObjectInputStream(fis);
        currentProfiles = (Set<Profile>) is.readObject();
        is.close();
        fis.close();
    }
}
