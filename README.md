# Manhunt Manager 1.0.0 — Fabric / Minecraft Java 26.3

Mod para gestionar partidas Manhunt en un mundo individual abierto a LAN. El host instala el mod; la lógica se ejecuta en el servidor integrado.

## Requisitos
- Minecraft Java 26.3
- Fabric Loader 0.20.1 o posterior compatible
- Fabric API 0.166.2+26.3
- Java Development Kit 25 para compilar
- Loom 1.19-SNAPSHOT y Gradle 9.6 o posterior

## Compilar en Windows
1. Instala JDK 25 e IntelliJ IDEA.
2. Abre esta carpeta como proyecto Gradle.
3. Espera a que Gradle sincronice y descargue las dependencias.
4. Ejecuta `Tasks > build > build` o `gradle build`.
5. El archivo se genera en `build/libs/manhunt-manager-1.0.0.jar`.

No se incluye un wrapper de Gradle porque debe generarse con la distribución de Gradle instalada en tu equipo.

## Instalar
1. Instala Fabric Loader para Minecraft 26.3.
2. Descarga Fabric API 0.166.2+26.3 y ponlo en `.minecraft/mods`.
3. Copia `manhunt-manager-1.0.0.jar` de `build/libs` a `.minecraft/mods`.
4. Inicia Minecraft con el perfil Fabric.
5. Entra en tu mundo, activa trucos y abre a LAN.

Solo el host necesita instalar el mod para la lógica de partida en un mundo individual abierto a LAN.

## Uso
Pulsa **AltGr (Alt derecho)** para abrir/cerrar Manhunt Manager. También puedes cambiar la tecla en Opciones > Controles > Asignación de teclas > Manhunt Manager.

- Haz clic en un jugador para asignarlo como Runner si aún no hay Runner.
- Haz clic en otros jugadores para añadirlos/quitarles como Hunters.
- Haz clic en el Runner seleccionado para quitarlo y elegir otro.
- Pulsa PREPARAR PARTIDA.
- Pulsa INICIAR cuando todos estén listos.
- DETENER pausa la partida.
- REINICIAR quita roles y limpia las puntuaciones de partida.

Comandos alternativos:
- `/manhunt setup <Runner> <Hunter1,Hunter2>`
- `/manhunt start`
- `/manhunt stop`
- `/manhunt status`
- `/manhunt reset`

## Automatizaciones
- Crea/usa objetivos de scoreboard para muertes, dragón y estado de victoria.
- Etiqueta Runner y Hunters.
- Fija el spawnpoint de cada Hunter a su posición al preparar.
- Mueve el world spawn al Runner periódicamente para que las brújulas lo señalen.
- Entrega brújulas al inicio y tras cada muerte de Hunter.
- Elimina brújulas tiradas.
- Detecta muerte del Runner y victoria al matar al Ender Dragon.
- Anuncia el ganador una sola vez.
- Cronómetro en la actionbar mientras la partida está activa.

## Limitaciones y seguridad
- El mod modifica etiquetas de jugadores, scoreboard, world spawn y spawnpoints de Hunters. Haz una copia del mundo antes de probarlo.
- El punto de aparición de los Hunters se establece al preparar la partida; no se modifica en cada respawn.
- El menú enumera jugadores conectados al abrirlo. Si alguien entra después, cierra y vuelve a abrir el menú.
- Si usas mods que cambian el comportamiento de brújulas, respawn o scoreboard, podrían interferir.
- Esta entrega es código fuente. No se ha podido ejecutar una compilación de Gradle en este entorno; compílala localmente y comparte el primer error de Gradle si aparece para corregirlo.

## Version
1.0.0 — interfaz de selección de roles, botones de gestión y atajo AltGr.
