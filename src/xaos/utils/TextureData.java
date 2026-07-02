package xaos.utils;

import java.nio.ByteBuffer;

/**
 * Basically an ImageData object with additional assigned textureID
 *
 * @author Florian Frankenberger
 */
public class TextureData extends ImageData {

    private final int textureID;

    public TextureData(String fileName, int width, int height, ByteBuffer imagePixels, int format, int textureID) {
        super(fileName, width, height, imagePixels, format);
        this.textureID = textureID;
    }

    public TextureData(ImageData image, int textureID) {
        super(image.getFileName(), image.getWidth(), image.getHeight(), image.getImagePixels(), image.getFormat());
        this.textureID = textureID;
    }

    public int getTextureID() {
        return textureID;
    }

}
