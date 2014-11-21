package com.xchat.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtil {
	public static byte[] getBitmapByte(Bitmap bitmap){  
	    ByteArrayOutputStream out = new ByteArrayOutputStream();  
	    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);  
	    try {  
	        out.flush();  
	        out.close();  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    }  
	    return out.toByteArray();  
	}  
	  
	  
	public static Bitmap getBitmapFromByte(byte[] temp){  
	    if(temp != null){  
	        Bitmap bitmap = BitmapFactory.decodeByteArray(temp, 0, temp.length);  
	        return bitmap;  
	    }else{  
	        return null;  
	    }  
	}
	
	public static String getBitmapString(Bitmap bitmap){
		return Base64.encodeBase64String(getBitmapByte(bitmap));
	}
	
	public Bitmap getBitmapFromString(String bitmap){
		return getBitmapFromByte(Base64.decodeBase64(bitmap));
	}
	
	public static String getImageByString(String filePath){
		Bitmap bitmap = BitmapFactory.decodeFile(filePath);
		return getBitmapString(bitmap);
	}
}
