package xaos.utils;

import java.nio.ByteBuffer;


public class ImageData {

	private final String fileName;
	private final int width;
	private final int height;
	private final ByteBuffer imagePixels;
	private final int format;


	public ImageData (int width, int height, ByteBuffer imagePixels, int format) {
		this (null, width, height, imagePixels, format);
	}


	public ImageData (String fileName, int width, int height, ByteBuffer imagePixels, int format) {
		this.fileName = fileName;
		this.width = width;
		this.height = height;
		this.imagePixels = imagePixels;
		this.format = format;
	}


	public ImageData (TextureData textureData) {
		this.fileName = textureData.getFileName ();
		this.width = textureData.getWidth ();
		this.height = textureData.getHeight ();
		this.imagePixels = textureData.getImagePixels ();
		this.format = textureData.getFormat ();
	}


	public String getFileName () {
		return fileName;
	}


	public int getWidth () {
		return width;
	}


	public int getHeight () {
		return height;
	}


	public int getFormat () {
		return format;
	}


	public ByteBuffer getImagePixels () {
		imagePixels.rewind ();
		imagePixels.limit (imagePixels.capacity ());
		return imagePixels;
	}


	public void clearPixels () {
		if (imagePixels != null) {
			imagePixels.clear ();
		}
	}
}
