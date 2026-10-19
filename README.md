# Mis tareas

App Android personal, sin internet. Guarda todo en el teléfono.

## Qué hace

- Agregas tareas con fecha (hoy, mañana, otro día) o sin fecha.
- **Notificación fija** con lo de hoy y lo próximo. Tiene un botón para agregar tareas.
  Si la deslizas, la app la vuelve a poner. Para quitarla, apaga el interruptor dentro de la app.
- **Widget** para la pantalla de inicio: toca una tarea para marcarla como hecha, toca "+" para agregar.
- **Widget personalizable** (menú ⋮ → Personalizar widget): color, tamaño de letra, qué mostrar y opacidad.
- Tocar una tarea en la app la marca como hecha. Mantenerla presionada la borra.

## Cómo obtener el APK (con GitHub)

1. En GitHub crea un repositorio nuevo (privado está bien) y súbele **todo el contenido** de esta carpeta,
   incluida la carpeta oculta `.github`.
2. Entra a la pestaña **Actions**. El flujo "Compilar APK" empieza solo (tarda unos 5 minutos).
   Si no empezó, ábrelo y pulsa **Run workflow**.
3. Cuando termine, ve a la sección **Releases** del repositorio (columna derecha) y descarga
   `app-debug.apk` desde el celular.
4. Ábrelo y permite "instalar apps de orígenes desconocidos" cuando Android lo pida.

Si la compilación falla, abre el paso en rojo, copia el error y pásaselo a Claude para corregirlo.

## Primer uso

1. Abre la app y acepta el permiso de notificaciones.
2. Menú ⋮ → **Añadir widget a la pantalla** (o mantén presionado un espacio vacío del inicio → Widgets → Mis tareas).
3. Para que Android no cierre la notificación fija, en Ajustes → Apps → Mis tareas → Batería, elige "Sin restricciones".

## Compilar en tu computadora (opcional)

Abre la carpeta con Android Studio y usa Build → Build APK(s).
