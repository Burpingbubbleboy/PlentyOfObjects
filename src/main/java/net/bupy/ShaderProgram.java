package net.bupy;

import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL33.*;

/**
 * Exists to encapsulate an OpenGL shader program object.
 */
public class ShaderProgram implements Disposable {

    /// The handle to the native OpenGL ShaderProgram object.
    private final int id;

    private final String vertexShaderSource;
    private final String fragmentShaderSource;

    /**
     * Creates a new {@link ShaderProgram} from the given vertex and fragment shaders.
     * @param vertexShaderPath The internal path to the vertex shader.
     * @param fragmentShaderPath The internal path to the fragment shader.
     * @return The newly created {@link ShaderProgram}, or null if the program couldn't be created.
     */
    public static ShaderProgram create(String vertexShaderPath, String fragmentShaderPath) {
        /* first, read the vertex and fragment shaders from disk into a String */
        String vertexShaderSource;
        String fragmentShaderSource;

        try {
            vertexShaderSource = Files.readString(Path.of(vertexShaderPath));
            fragmentShaderSource = Files.readString(Path.of(fragmentShaderPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /* next, let's try to compile the vertex shader */
        int vertexShaderHandle = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShaderHandle, vertexShaderSource);
        glCompileShader(vertexShaderHandle);
        if (glGetShaderi(vertexShaderHandle, GL_COMPILE_STATUS) != GL_TRUE) {
            System.err.printf("Error! Vertex shader with path: %s failed to compile!", vertexShaderPath);
            return null;
        }

        /* next, let's try to compile the fragment shader */
        int fragmentShaderHandle = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShaderHandle, fragmentShaderSource);
        glCompileShader(fragmentShaderHandle);
        if (glGetShaderi(fragmentShaderHandle, GL_COMPILE_STATUS) != GL_TRUE) {
            System.err.printf("Error! Fragment shader with path: %s failed to compile!", fragmentShaderPath);
            glDeleteShader(vertexShaderHandle);
            return null;
        }

        /* finally, let's try to create and link the shader program */
        int programHandle = glCreateProgram();
        glAttachShader(programHandle, vertexShaderHandle);
        glAttachShader(programHandle, fragmentShaderHandle);
        glLinkProgram(programHandle);
        if (glGetProgrami(programHandle, GL_LINK_STATUS) != GL_TRUE) {
            System.err.printf("Error! Shader program failed to link with fragment shader %s and vertex shader %s",
                    fragmentShaderPath, vertexShaderPath);
            glDeleteShader(vertexShaderHandle);
            glDeleteShader(fragmentShaderHandle);
            return null;
        }

        /* delete the shaders as they're no longer needed */
        glDeleteShader(vertexShaderHandle);
        glDeleteShader(fragmentShaderHandle);

        return new ShaderProgram(programHandle, vertexShaderSource, fragmentShaderSource);
    }

    private ShaderProgram(int id, String vertexShaderSource, String fragmentShaderSource) {
        this.id = id;
        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;
    }

    /// Sets this object to be the currently used program.
    public void use() {
        glUseProgram(id);
    }

    /// Sets the active OpenGL shader program to nothing
    public void stopUsing() {
        glUseProgram(0);
    }

    /// Checks to see if the program is currently valid, and guaranteed to execute.
    public boolean isValid() {
        glValidateProgram(id);
        return glGetProgrami(id, GL_VALIDATE_STATUS) == GL_TRUE;
    }

    /**
     * Checks to see if the program is currently valid, and guaranteed to execute.<br>
     * If not, this method will throw an exception.
     */
    public void isValidElseThrow() {
        if (!isValid()) {
            throw new RuntimeException(
                    String.format("Fatal error! Failed to validate program with shaders:%n" +
                                    "Vertex shader: %s%nFragment shader: %s%n",
                            vertexShaderSource, fragmentShaderSource)
            );
        }
    }

    public void setUniformMatrix4fv(String name, boolean transpose, Matrix4f to) {
        glUniformMatrix4fv(glGetUniformLocation(id, name), transpose, to.get(new float[16]));
    }

    public void setUniformMatrix4fv(String name, boolean transpose, FloatBuffer to) {
        glUniformMatrix4fv(glGetUniformLocation(id, name), transpose, to);
    }

    public void setUniform3f(String name, float toX, float toY, float toZ) {
        glUniform3f(glGetUniformLocation(id, name), toX, toY, toZ);
    }

    public void setUniform2f(String name, float toX, float toY) {
        glUniform2f(glGetUniformLocation(id, name), toX, toY);
    }

    public void setUniform1i(String name, int to) {
        glUniform1i(glGetUniformLocation(id, name), to);
    }

    // frees the native OpenGL shader program
    @Override
    public void dispose() {
        glDeleteProgram(id);
    }
}