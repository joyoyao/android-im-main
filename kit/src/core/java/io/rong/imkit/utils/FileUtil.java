package io.rong.imkit.utils;

import android.net.Uri;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

	public static InputStream getFileInputStream(String path) {

		FileInputStream fileInputStream = null;

		try {
			fileInputStream = new FileInputStream(new File(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return fileInputStream;
	}

	public static byte[] getByteFromUri(Uri uri) {
		InputStream input = getFileInputStream(uri.getPath());
		try {
			int count = 0;
			while (count == 0) {
				count = input.available();
			}

			byte[] bytes = new byte[count];
			input.read(bytes);

			return bytes;
		} catch (Exception e) {
			return null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static void writeByte(Uri uri, byte[] data) {
		File fileFolder = new File(uri.getPath().substring(0, uri.getPath().lastIndexOf("/")));
		fileFolder.mkdirs();
		File file = new File(uri.getPath());
		
		try {
			OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
			os.write(data);
			os.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}