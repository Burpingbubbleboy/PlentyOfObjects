#version 330 core
layout (location = 0) in vec2 aPos;
layout (location = 1) in vec3 aColor;
layout (location = 2) in vec2 aTexCoord;
layout (location = 3) in vec2 aOffset;

out vec3 fColor;
out vec2 texCoord;

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

void main()
{
    gl_Position = projectionMatrix * viewMatrix * vec4(aPos + aOffset, 0.0, 1.0);
    fColor = aColor;
    texCoord = aTexCoord;
}