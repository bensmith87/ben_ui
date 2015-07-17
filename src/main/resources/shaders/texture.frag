#version 330 core

uniform sampler2D tex;

in vec2 textureCoordinate2;

out vec4 colorOut;

void main() {
    colorOut = texture(tex, textureCoordinate2);
}
