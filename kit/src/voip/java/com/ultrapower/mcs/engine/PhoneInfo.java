package com.ultrapower.mcs.engine;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

class PhoneInfo {
		 private static final String TAG = "PhoneInfo";
		 private ActivityManager mActivityManager; 
		 private Context context = null;
		 public PhoneInfo(Context context)
		 {
			 this.context = context;
		 }
	     public String[] getTotalMemory() {  
		            String[] result = {"",""};  //1-total 2-avail  
		            mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();    
		            mActivityManager.getMemoryInfo(mi);    
		            long mTotalMem = 0;  
		            long mAvailMem = mi.availMem;  
		            String str1 = "/proc/meminfo";  
		            String str2;  
		            String[] arrayOfString;  
		            try {  
		                FileReader localFileReader = new FileReader(str1);  
		                BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);  
		                str2 = localBufferedReader.readLine();  
		                arrayOfString = str2.split("\\s+");  
		                mTotalMem = Integer.valueOf(arrayOfString[1]).intValue() * 1024;  
		                localBufferedReader.close();  
		            } catch (IOException e) {  
		               e.printStackTrace();  
		            }  
		            
		           result[0] = String.valueOf(mTotalMem);  
		           result[1] = String.valueOf(mAvailMem); 
		           return result;  
		       }  
	     
	     public String[] getCpuInfo() {  
	                 String str1 = "/proc/cpuinfo";  
	                 String str2 = "";  
	                 String[] cpuInfo = {"", ""};  //1-cpu??��??�?  //2-cpu棰�??�?  
	                 String[] arrayOfString;  
	                 try {  
	                     FileReader fr = new FileReader(str1);  
	                     BufferedReader localBufferedReader = new BufferedReader(fr, 8192);  
	                     str2 = localBufferedReader.readLine();  
	                     arrayOfString = str2.split("\\s+");  
	                     for (int i = 2; i < arrayOfString.length; i++) {  
	                         cpuInfo[0] = cpuInfo[0] + arrayOfString[i] + " ";  
	                     }  
	                     str2 = localBufferedReader.readLine();  
	                     arrayOfString = str2.split("\\s+");  
	                     cpuInfo[1] += arrayOfString[2];  
	                     localBufferedReader.close();  
	                 } catch (IOException e) {  
	                	 
	                 }  
	                 return cpuInfo;  
	             }  
	     /*
	      * ??��??CPU??��?????
	      */
	     public int getNumCores() {
	    	    //Private Class to display only CPU devices in the directory listing
	    	    class CpuFilter implements FileFilter {
	    	        @Override
	    	        public boolean accept(File pathname) {
	    	            //Check if filename is "cpu", followed by a single digit number
	    	            if(Pattern.matches("cpu[0-9]", pathname.getName())) {
	    	                return true;
	    	            }
	    	            return false;
	    	        }
  
	    	    }

	    	    try {
	    	        //Get directory containing CPU info
	    	        File dir = new File("/sys/devices/system/cpu/");
	    	        //Filter to only list the devices we care about
	    	        File[] files = dir.listFiles(new CpuFilter());
	    	        Log.d(TAG, "CPU Count: "+files.length);
	    	        //Return the number of cores (virtual CPU devices)
	    	        return files.length;
	    	    } catch(Exception e) {
	    	        //Print exception
	    	        Log.d(TAG, "CPU Count: Failed.");
	    	        e.printStackTrace();
	    	        //Default to return 1 core
	    	        return 1;
	    	    }
	    	}
	     /*
	      * ??��??CPU???�?�????
	      */
	     public long getCpuFrequence() {  
	         ProcessBuilder cmd;  
	         try {  
	             String[] args = { "/system/bin/cat",  
	                     "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };  
	             cmd = new ProcessBuilder(args);  
	   
	             Process process = cmd.start();  
	             BufferedReader reader = new BufferedReader(new InputStreamReader(  
	                     process.getInputStream()));  
	             String line = reader.readLine();  
	             return Long.parseLong(line);//StringUtils.parseLongSae(line, 10, 0);  
	         } catch (IOException ex) {  
	             ex.printStackTrace();  
	         }  
	         return 0;  
	     }  
	     /*
	      * ??��??CPU???�?�????
	      */
	     public long getMinCpuFreq() {  
	            String result = "";  
	            ProcessBuilder cmd;  
	            try {  
	                    String[] args = { "/system/bin/cat",  
	                                    "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq" };  
	                    cmd = new ProcessBuilder(args);  
	                    Process process = cmd.start();  
	                    InputStream in = process.getInputStream();  
	                    byte[] re = new byte[24];  
	                    while (in.read(re) != -1) {  
	                            result = result + new String(re);  
	                    }  
	                    in.close();  
	            } catch (IOException ex) {  
	                    ex.printStackTrace();  
	                    result = "N/A";  
	            }  
	            return Long.parseLong(result.trim());  
	    }  
	    /*
	     * ??��??CPU�????�????
	     */
	    public long getCurCpuFreq() {  
             String result = "N/A";  
             try {  
                     FileReader fr = new FileReader(  
                                     "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");  
                     BufferedReader br = new BufferedReader(fr);  
                     String text = br.readLine();  
                     result = text.trim();  
             } catch (FileNotFoundException e) {  
                     e.printStackTrace();  
             } catch (IOException e) {  
                     e.printStackTrace();  
             }  
             return Long.parseLong(result);  
     }  
	   /*
	    * ??��??CPU???�?
	    */
	 public String getCpuName() {  
            try {  
                    FileReader fr = new FileReader("/proc/cpuinfo");  
                    BufferedReader br = new BufferedReader(fr);  
                    String text = br.readLine();  
                    String[] array = text.split(":\\s+", 2);  
                    for (int i = 0; i < array.length; i++) {  
                    }  
                    return array[1];  
            } catch (FileNotFoundException e) {  
                    e.printStackTrace();  
            } catch (IOException e) {  
                    e.printStackTrace();  
            }  
            return null;  
    } 
	 
	 public int [] getCameraInfo()
	 {
		 Size size[] = new Size[3];

		 int height = 0,width=0;
		 int numberCamera = android.hardware.Camera.getNumberOfCameras();
		 Parameters parameters;
		 android.hardware.Camera camera = null;
		 for(int i=0;i<numberCamera;i++)
		 {
			 camera =  android.hardware.Camera.open(i);
			 parameters = camera.getParameters();
			 size[i] = parameters.getPictureSize();
			 camera.release();
			 if(size[i].height > height)
			 {
				 height = size[i].height;
				 width = size[i].width;
			 }

		 }
		 return new int[]{height,width}; 
	 }
}
/*
 * NO.1-NO.10
??? A8 ?????? 2.0ghz �?iPhone6???iPad6�?�?
�????805�?8084�? ?????? 2.5ghz �?诺�?��??Lumia1820???�????S6�?�?
�????801�?8974AC�? ?????? 2.5ghz �?�????S5�?�?
�????800�?8974AB�? ?????? 2.2ghz �?诺�?��??Lumia1520???�????S4�?�?
?????�座5420 ?????? 1.8ghz �?�????S4???�????noto3�?�?
??��??�?tegra 4 ?????? 1.8ghz �?HTC One x�?�?
??? A7 ?????? 1.3ghz �?iPhone5S???iPad Air�?�?
?????�座5410 ?????? 1.6ghz �?�????S4�?�?
�????615�?8936�? ?????? 1.7ghz �?�????S6�?�?
??????�?6592turbo ?????? 2.0ghz �????3X�?�?
�????600�?8064T�? ?????? 1.7ghz �?�?2S�?�?

NO.11--NO.20
??????�?6592 ?????? 1.7ghz 
�????APQ8064 ?????? 1.5ghz 
???A6x ?????? 1.4ghz 
??��??Z2580 ?????? 2.0ghz 
海�??kirin910 ?????? 1.6ghz
�????8960T ?????? 1.7ghz 
??? A6 ?????? 1.0ghz 
�????MSM8628/ ?????? 1.6ghz 
?????�座4412 ?????? 1.4ghz 
??��??�?tegra 3 ?????? 1.5ghz

NO.21-NO.50
海�??K3V2E ?????? 1.5ghz 
�????MSM8628/8228 ?????? 1.4ghz 
??????�?6589T ?????? 1.5ghz 
??????�?6582 ?????? 1.3ghz 
�????MSM8960/8260A ?????? 1.5ghz 
德�??�????4470 ?????? 1.5ghz 
??????�?6589 ?????? 1.2ghz 
??? A5x ?????? 1.0ghz 
??��??Z2480 ?????? 2.0ghz 
�????MSM8230 ?????? 1.2ghz 
�????MSM8260/8660 ?????? 1.5ghz 
?????�座4210 ?????? 1.4ghz 
???A5 ?????? 1.0ghz 
德�??�????4460 ?????? 1.2ghz 
�????MSM8225Q ?????? 1.2ghz 
�????MSM8227 ?????? 1.0ghz 
德�??�????4430 ?????? 1.2ghz 
??��??�?tegra 2 ?????? 1.2ghz 
??????�?6577 ?????? 1.0ghz 
??????�?6572 ?????? 1.2ghz 
�????MSM8225 ?????? 1.2ghz 
�????MSM8255T ?????? 1.4ghz 
�???????�? ?????? 1.0ghz 
???A4 ?????? 1.0ghz 
??????�?6575 ?????? 1.0ghz 
�????MSM8255 ?????? 1.0ghz 
�????MSM7227A ?????? 1.0ghz 
�????QSD8250 ?????? 1.0ghz 
德�??�????3620 ?????? 1.0ghz 
�????MSM7227 ?????? 0.6ghz
*/
