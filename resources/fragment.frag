#version 330 core
out vec4 FragColor;

in vec3 fColor;
in vec2 texCoord;

uniform sampler2D texture1;

void main()
{
    vec4 texColor = texture(texture1, texCoord);

    if (texColor.a < 0.5) {
        discard;
    }

    FragColor = texColor;
}