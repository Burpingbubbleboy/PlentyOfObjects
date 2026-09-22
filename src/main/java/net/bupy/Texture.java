package net.bupy;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Encapsulated OpenGL texture object.
 */
public class Texture implements Disposable {

    private final int textureHandle;

    public static Texture create(String texturePath) {
        int textureHandle = -1;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = STBImage.stbi_load(texturePath, width, height, channels, 4);

            if (pixels == null) {
                throw new RuntimeException("Failed to load texture: " + texturePath + STBImage.stbi_failure_reason());
            }

            textureHandle = glGenTextures();

            glBindTexture(GL_TEXTURE_2D, textureHandle);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            STBImage.stbi_image_free(pixels);
        }

        return new Texture(textureHandle);
    }

    public Texture(int textureHandle) {
        this.textureHandle = textureHandle;
    }

    /**
     * Binds this texture as currently active
     */
    public void use() {
        glActiveTexture(GL_TEXTURE0); // not necessary except on some drivers with funny OpenGL implementations
        glBindTexture(GL_TEXTURE_2D, textureHandle);
    }

    /**
     * Binds the active OpenGL texture to *nothing*
     */
    public void stopUsing() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public void dispose() {
        glDeleteTextures(textureHandle);
    }
}
