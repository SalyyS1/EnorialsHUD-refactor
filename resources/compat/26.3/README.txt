Native shader entry points from the official Minecraft Java 26.3 client.
Source: assets/minecraft/shaders/core/{text,entity}.{vsh,fsh}
Release metadata:
https://piston-meta.mojang.com/v1/packages/96c00d95a31328714d3811cfade2804bb050e455/26.3.json
Client archive SHA-1: e877b6a07acd633fb3bb475002175cec036e7b87
Resources: 97.1. Protocol: 777. Released 2026-09-15.

ModernShaders preserves these world-rendering/OIT entry points and adapts the
existing Enorials GUI/minimap code. Do not replace the GUI branch with a native
world pipeline or declare unbound Fog uniforms in the GUI pipeline.
Minecraft shader code belongs to Mojang/Microsoft; Enorials does not claim its authorship.
