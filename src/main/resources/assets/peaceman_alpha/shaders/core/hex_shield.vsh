#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord;
out vec3 localPos;

void main() {
    // Standard-Transformation für Minecraft 3D-Objekte
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // Wir geben die Koordinaten an den Fragment-Shader weiter
    texCoord = UV0;
    localPos = Position;
}