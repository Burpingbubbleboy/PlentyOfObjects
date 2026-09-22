package net.bupy;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class Main {

    // --------------------------------------------------
    // main method
    // --------------------------------------------------
    static void main() {
        var application = new Main();
        application.initialize();
        application.begin();
    }

    // --------------------------------------------------
    // instance stuff
    // --------------------------------------------------

    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    public static final int NUM_OBJECTS = 100_000;

    public static final double TMIE_TO_PRINT_FPS = 3.0;

    // thank you LearnOpenGL for saving me the headache of typing this out myself
    private final float[] quadVertices = {
            // positions     // colors   // texcoords
            -10f,  10f,  1.0f, 0.0f, 0.0f,  0f, 1f,
            10f, -10f,  0.0f, 1.0f, 0.0f,   1f, 0f,
            -10f, -10f,  0.0f, 0.0f, 1.0f,  0f, 0f,

            -10f,  10f,  1.0f, 0.0f, 0.0f,  0f, 1f,
            10f, -10f,  0.0f, 1.0f, 0.0f,   1f, 0f,
            10f,  10f,  0.0f, 1.0f, 1.0f,   1f, 1f,
    };

    private final Matrix4f viewMatrix;
    private final Matrix4f projectionMatrix;
    private final Matrix4f worldMatrix;
    private final DoomRandom doomRandom;

    private long windowHandle;
    private double previousTime;
    private double timeToPrintFPS;
    private int instanceVBOHandle;
    private int VAOHandle;
    private int VBOHandle;

    public Main() {
        projectionMatrix = new Matrix4f().ortho(0f, WINDOW_WIDTH, WINDOW_HEIGHT, 0f, -1f, 1f);
        worldMatrix = new Matrix4f();
        viewMatrix = new Matrix4f();
        doomRandom = new DoomRandom();
    }


    /// Does the GLFW and OpenGL boilerplate. Should be called once in the main() method.
    public void initialize() {
        if (!glfwInit()) {
            throw new IllegalStateException("Fatal error! Could not initialize GLFW!");
        }

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        long windowHandle = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Plenty of Objects", NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Fatal error! Could not create GLFW window!");
        }

        // totally yoinked this window centering code from LWJGL3 hahah
        try ( MemoryStack stack = MemoryStack.stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            glfwGetWindowSize(windowHandle, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // center window
            glfwSetWindowPos(
                    windowHandle,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1);
        glfwShowWindow(windowHandle);

        GL.createCapabilities();

        glClearColor(0f, 0f, 0f, 1f);

        this.windowHandle = windowHandle;
    }

    /// Constructs and processes the main loop. Should be called once in the main() method after {@code initialize()}.
    public void begin() {

        // set up shader program
        ShaderProgram shaderProgram = ShaderProgram.create(
                "resources/vertex.vert",
                "resources/fragment.frag"
        );
        shaderProgram.use();
        shaderProgram.setUniform1i("texture1", 0);
        shaderProgram.stopUsing();

        // set up texture
        Texture texture = Texture.create("resources/texture.png");

        // set up translations/offsets array. * 2 because we're storing both an x and y component for each object.
        float[] translations = generateTranslationsArray();

        // ok do some set up with the "per-instance" VBO
        instanceVBOHandle = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBOHandle);
        glBufferData(GL_ARRAY_BUFFER, translations, GL_DYNAMIC_DRAW); // oh yeah we're modifying this A LOT
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // ok do some set up with the "per-vertex" VAO and VBO
        VAOHandle = glGenVertexArrays();
        glBindVertexArray(VAOHandle);

        VBOHandle = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, VBOHandle);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 7 * 4, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 7 * 4, 2 * 4);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 7 * 4, 5 * 4);
        glEnableVertexAttribArray(2);

        // we also need to jam the instanceVBO data into the VAO too
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBOHandle);
        glVertexAttribPointer(3, 2, GL_FLOAT, false, 2 * 4, 0);
        glEnableVertexAttribArray(3);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glVertexAttribDivisor(3, 1); // "hey! OpenGL! this is an instanced vertex attribute!"

        // main processing/rendering loop
        while (!glfwWindowShouldClose(windowHandle)) {

            // calculate deltaTime, which exists for calculating the current FPS
            double deltaTime = glfwGetTime() - previousTime;
            previousTime = glfwGetTime();
            timeToPrintFPS -= deltaTime;

            if (timeToPrintFPS <= 0) {
                timeToPrintFPS = TMIE_TO_PRINT_FPS;
                System.out.printf("current FPS: %f%n", 1.0 / deltaTime);
            }


            // jiggle all the translations for the movement
            performRandomJiggle(translations);
            updateInstanceVBO(translations);

            glClear(GL_COLOR_BUFFER_BIT);

            shaderProgram.use();
            texture.use();

            shaderProgram.setUniformMatrix4fv("projectionMatrix", false, projectionMatrix);
            shaderProgram.setUniformMatrix4fv("viewMatrix", false, viewMatrix);

            glBindVertexArray(VAOHandle);
            glDrawArraysInstanced(GL_TRIANGLES, 0, 6, translations.length / 2);

            glfwSwapBuffers(windowHandle);
            glfwPollEvents();

            // "handle" error codes.
            // aka complain and whine about some invalid OpenGL state but don't actually do anything.
            // ...unless you somehow run out of memory then you should crash.
            var errorCode = glGetError();
            if (errorCode != GL_NO_ERROR) {
                switch (errorCode) {
                    case GL_INVALID_ENUM -> System.out.printf("OpenGL error! (%d) Invalid enum passed as argument!%n", errorCode);
                    case GL_INVALID_VALUE -> System.out.printf("OpenGL error! (%d) Invalid value passed as argument!%n", errorCode);
                    case GL_INVALID_OPERATION -> System.out.printf("OpenGL error! (%d) Invalid operation in current state!%n", errorCode);
                    case GL_INVALID_FRAMEBUFFER_OPERATION -> System.out.printf("OpenGL error! (%d) Invalid framebuffer state!%n", errorCode);
                    case GL_OUT_OF_MEMORY -> throw new IllegalStateException(String.format("Fatal OpenGL error! (%d) Out of memory!%n", errorCode));
                    case GL_STACK_OVERFLOW -> System.out.printf("OpenGL error! (%d) Stack overflow!%n", errorCode);
                    case GL_STACK_UNDERFLOW -> System.out.printf("OpenGL error! (%d) Stack underflow!%n", errorCode);
                }
            }
        }
    }

    /// Assumes {@code translations} has the layout [x, y], both of each being floats.
    /// This will, for each component, randomly add or subtract a single float from each component for each stride.
    private void performRandomJiggle(float[] translations) {
        for (int i = 0; i < translations.length - 1; i++) {
            // transforms the random 0-255 number to the range of 0-2, then transforms it to the range -1f to 1f
            float random1 = (doomRandom.getRandomNumber() / 127.5f) - 1f;
            float random2 = (doomRandom.getRandomNumber() / 127.5f) - 1f;

            translations[i] += random1;
            translations[i + 1] += random2;
        }
    }

    /// Does exactly as stated. Updated the instance VBO with the current translations.
    private void updateInstanceVBO(float[] translations) {
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBOHandle);
        glBufferData(GL_ARRAY_BUFFER, translations, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    /// Generates the translations array. This method really just exists for containing all that logic
    /// So I don't have to look at it
    private float[] generateTranslationsArray() {
        float[] translations = new float[NUM_OBJECTS * 2];

        for (int i = 0; i < translations.length; i += 2) {
            // uses a different random number generation algorithm so that every position is unique
            translations[i] = ThreadLocalRandom.current().nextFloat(0f, WINDOW_WIDTH); // x
            translations[i + 1] = ThreadLocalRandom.current().nextFloat(0f, WINDOW_HEIGHT); // y
        }

        return translations;
    }
}
