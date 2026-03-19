#version 150

in vec2 texCoord;
in vec3 localPos;

uniform float GameTime;
uniform vec3 ImpactPos;
uniform float EnergyLevel;
uniform float ImpactTimer;

out vec4 fragColor;

// Eine mathematische Hilfsfunktion, um die Kanten eines Hexagons zu berechnen
float hexDist(vec2 p) {
    p = abs(p);
    float c = dot(p, normalize(vec2(1.0, 1.7320508)));
    return max(c, p.x);
}

void main() {
    // 1. Skaliere die UV-Koordinaten, um das Waben-Gitter dichter zu machen
    vec2 uv = texCoord * 12.0;

    // 2. Hexagon-Gitter Mathematik (Prozedurale Generierung ohne Textur-Datei!)
    vec2 r = vec2(1.0, 1.7320508);
    vec2 h = r * 0.5;
    vec2 a = mod(uv, r) - h;
    vec2 b = mod(uv - h, r) - h;
    vec2 gv = dot(a, a) < dot(b, b) ? a : b;

    // Distanz zum Rand der aktuellen Wabe berechnen
    float dist = hexDist(gv);

    // 3. Nur die Ränder der Waben zeichnen (glatter Übergang für Neon-Effekt)
    float edge = smoothstep(0.4, 0.5, dist);

    // 4. Ein sanftes Pulsieren des gesamten Schildes basierend auf der Spielzeit
    float pulse = (sin(GameTime * 3.0) * 0.5 + 0.5) * 0.3 + 0.7;

    // Die Grundfarbe des Schildes: Ein strahlendes Cyan/Hellblau
    vec3 color = vec3(0.1, 0.8, 1.0) * pulse;

    // 5. RIPPLE EFFEKT (Die ausbreitende Schockwelle)
        float distToImpact = length(localPos - ImpactPos);
        float ripple = 0.0;

        // Ein Einschlag ist ca. 40 Ticks (2 Sekunden) lang sichtbar
        if (ImpactTimer > 0.0 && ImpactTimer < 40.0) {

            // Die Welle breitet sich aus: Radius = Zeit * Geschwindigkeit
            float waveRadius = ImpactTimer * 1.5;

            // Wie weit ist DIESER Pixel vom Rand der Schockwelle entfernt?
            float distToWave = abs(distToImpact - waveRadius);

            // Wenn der Pixel nah an der Welle ist (Dicke der Welle = 3.0 Blöcke)
            if (distToWave < 3.0) {
                // Weicher Übergang (0.0 am Rand der Welle, 1.0 exakt auf der Welle)
                ripple = 1.0 - (distToWave / 3.0);

                // Je älter (größer) die Welle wird, desto schwächer/transparenter wird sie
                ripple *= 1.0 - (ImpactTimer / 40.0);
            }
        }

        // Bei einem Einschlag leuchtet die Welle extrem grell (weiß-blau) auf
        color += vec3(0.8, 0.95, 1.0) * ripple * 3.5;

    // 6. Transparenz (Alpha) berechnen
    // Nur die Ränder der Hexagone und die Schockwelle sind sichtbar.
    // Wenn die Energie sinkt, wird das Schild insgesamt durchsichtiger.
    float alpha = (edge * 0.6 + ripple) * EnergyLevel;

    // Performance-Boost: Völlig unsichtbare Pixel werden von der Grafikkarte verworfen
    if (alpha < 0.05) {
        discard;
    }

    // 7. Das finale Ergebnis auf den Bildschirm malen!
    fragColor = vec4(color, alpha);
}