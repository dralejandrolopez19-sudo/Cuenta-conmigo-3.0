# Reparación de Conexión con Google y Sistema de Privacidad Inteligente "Modo Calle"

Este plan soluciona de raíz los fallos al seleccionar una cuenta en el inicio de sesión con Google y añade un escudo de privacidad integral (**Modo Calle**) que oculta o camufla con Inteligencia Artificial todos los datos clínicos y emocionales sensibles cuando el usuario se encuentra en espacios públicos o rodeado de personas.

### User Review & Critical Decisions

> [!IMPORTANT]
> Se han confirmado las siguientes decisiones clave a partir de tus respuestas para guiar la construcción:

- **Decisión Confirmada 1 — Reparación Integral de Google Sign-In**: Se corregirá el cierre abrupto o error al elegir cuenta mediante la re-vinculación de certificados SHA-1/SHA-256 en Firebase, la extracción segura del contexto de `Activity`, la separación estricta entre el inicio automático silencioso y el botón interactivo para evitar colisiones del selector de cuentas, y un flujo de reintento limpio si el certificado o la credencial previa falla.
- **Decisión Confirmada 2 — Modo Calle para Ocultar/Difuminar Datos al Instante**: Cuando el **Modo Calle** esté activo, todos los textos sensibles (títulos y textos del Diario, nombres y dosis de Pastillas, notas de Estado de Ánimo, evaluaciones clínicas PHQ-9/GAD-7 y detalles de Citas en Agenda) se difuminarán u ocultarán automáticamente con máscaras discretas y resúmenes neutros generados con IA, permitiendo revelarlos temporalmente con un toque individual ("Toca para ver discretamente").
- **Decisión Confirmada 3 — Activación Rápida + Detección Inteligente de Movimiento y Entorno**: Se incluirá un botón directo de escudo de privacidad (**Modo Calle**) en la barra superior (junto a SOS) y un detector inteligente basado en los sensores del dispositivo (acelerómetro/inclinación brusca al caminar o girar la pantalla cuando alguien se acerca) que activa automáticamente la protección visual, además de bloqueo contra capturas de pantalla (`FLAG_SECURE`).

---

### 1. Overview & Core Concept

- **Qué Hace**:
  1. **Conexión con Google Estable y Resiliente**: Garantiza que al pulsar *"Continuar con Google"* el selector de cuentas de Android (`CredentialManager`) se abra sin bloqueos ni cierres inesperados, valide el token de identidad contra Firebase Auth sin cancelar la corrutina al cambiar de pantalla, y ofrezca recuperación automática limpiando el estado de credenciales si Google Play Services reporta un bloqueo previo.
  2. **Escudo de Privacidad "Modo Calle" con IA**: Protege la intimidad del paciente cuando usa la app en el transporte público, la calle, la sala de espera o el trabajo. Al activarse (manualmente desde la barra superior o automáticamente al detectar movimiento de caminata o giro brusco del teléfono), oculta y difumina al instante cualquier información médica o emocional sensible, bloquea la vista previa en el selector de apps recientes de Android y ofrece **Camuflaje Inteligente con IA (Gemini)** para mostrar alias cotidianos neutros en pantalla pública sin alterar los datos reales guardados.
- **Público Objetivo / Contexto de Uso**: Usuarios que registran pensamientos íntimos, síntomas emocionales, tomas de medicación psiquiátrica/médica o citas terapéuticas mientras están fuera de casa y necesitan absoluta discreción frente a miradas de terceros (*shoulder surfing*).
- **Valor Principal**: Confianza total al iniciar sesión con su cuenta de Google y tranquilidad inmediata para consultar o registrar su bienestar en cualquier lugar público sin exponer diagnósticos, medicamentos ni notas personales.

---

### 2. User Experience & Visual Design

- **Flujos de Usuario Clave**:
  1. **Flujo Reparado de Inicio de Sesión con Google**:
     - Al abrir la pantalla de bienvenida, el intento de inicio silencioso no bloquea ni compite con el botón *"Continuar con Google"*.
     - Al pulsar *"Continuar con Google"*, se limpia cualquier estado colgado previo y se lanza una solicitud exclusiva `GetSignInWithGoogleOption` vinculada a la `Activity` activa.
     - Si el entorno o el dispositivo presenta una sesión en caché corrupta o cancelación del selector, el sistema realiza un segundo intento limpio con `GetGoogleIdOption` sin filtro restrictivo o muestra un diagnóstico claro con botón de reintento inmediato y acceso de respaldo para que el usuario nunca quede bloqueado fuera de la app.
  2. **Activación en 1 Toque desde la Barra Superior**:
     - En la barra superior (`TopBar`), siempre visible desde cualquier pestaña, aparece el interruptor compacto de **Modo Calle** (icono de escudo/ojo de privacidad).
     - Con un solo toque, el botón cambia a estado **Activo (Verde Azulado / Teal Intenso)** y despliega una cinta discreta informando: *"Modo Calle activo • Datos sensibles protegidos"*.
  3. **Detección Inteligente de Movimiento y "Gesto Antimiradas"**:
     - El sensor de movimiento (acelerómetro) detecta dos situaciones en tiempo real cuando la opción de protección inteligente está habilitada:
       - **Movimiento de caminata constante**: Si detecta el patrón rítmico de ir caminando en la calle, sugiere o activa automáticamente el Modo Calle.
       - **Giro o inclinación brusca (*Tilt-to-Hide*)**: Si alguien se acerca y el usuario inclina o baja rápidamente el teléfono, el Modo Calle se activa al instante en menos de 200 ms.
  4. **Visualización Protegida y Camuflaje con IA (Gemini)**:
     - Mientras el Modo Calle está activo:
       - **Pastillas**: Los nombres de fármacos y dosis (ej. *"Sertralina 50 mg"*) se muestran difuminados o bajo un alias neutro (ej. *"Suplemento diario • Toma programada"*).
       - **Diario y Estado de Ánimo**: Los títulos, emociones y reflexiones íntimas se ocultan tras tarjetas con efecto esmerilado (*blur* / máscara de privacidad) o pueden transformarse mediante **Camuflaje IA con Gemini (`gemini-3.8-flash`)** en notas neutras de productividad/rutina mientras estás en público, manteniendo intacto el texto original privado.
       - **Agenda y Citas**: Los títulos de *"Terapia / Psicología"* o *"Consulta Médica"* se muestran en pantalla como *"Compromiso personal"* hasta que el usuario toque una tarjeta específica para revelarla durante 5 segundos.
- **Identidad Visual y Tema**:
  - *Dirección Estética*: Calidez orgánica y clínica serena (*Warm Organic & Discreet Shield*).
  - *Paleta de Colores*: Fondo crema suave (`#FAF7F2`), acentos principales en Coral Suave (`#E07A5F`) y Verde Azulado Sereno (`#2A9D8F`), e indicadores del Modo Calle en Azul Pizarra Nocturno (`#1E293B`) y Verde Esmeralda Suave (`#0D9488`) para transmitir seguridad sin generar alarma visual.
  - *Jerarquía Visual*: Tarjetas protegidas con bordes punteados suaves, píldoras de estado *"Protegido"* con icono de candado/ojo, y micro-animaciones fluidas al alternar entre vista pública y vista privada.

---

### 3. Key Product Decisions & Trade-Offs

- **Decisión 1: Separación de Corrutinas y Solicitudes en Google Sign-In (`CredentialManager`)**
  - *Enfoque Elegido*: Ejecutar el guardado y sincronización del perfil de Google en el alcance global de la sesión autenticada (`MainActivity` / `ViewModel`) una vez completado el `signInWithCredential`, extraer de forma segura el `ComponentActivity` mediante desempacado de `ContextWrapper`, y evitar que `attemptAutoSignIn` y `onGoogleSignInClicked` disparen peticiones simultáneas al `CredentialManager`. Además, incorporar un paso de fallback con `setFilterByAuthorizedAccounts(false)` cuando `GetSignInWithGoogleOption` devuelva cancelación espuria del selector.
  - *Por Qué*: En Android 14–16, lanzar dos solicitudes de `CredentialManager` concurrentes o cancelar el `CoroutineScope` del botón justo cuando `AuthStateListener` desmonta `AuthScreen` provoca que el selector se cierre o lance error al elegir la cuenta.
- **Decisión 2: Protección Visual Multicapa (Difuminado Instantáneo + `FLAG_SECURE` + Camuflaje IA)**
  - *Enfoque Elegido*: Combinar ocultamiento visual instantáneo en cliente (0 ms de latencia al pulsar el botón o mover el teléfono), protección de ventana `WindowManager.LayoutParams.FLAG_SECURE` (para que nadie vea la pantalla al cambiar de app ni se tomen capturas en modo calle) y generación opcional de textos de camuflaje neutro con **Firebase AI Logic (`gemini-3.8-flash`)**.
  - *Por Qué*: El ocultamiento local responde en milisegundos ante una emergencia de privacidad en la calle, mientras que Gemini permite disfrazar la pantalla como un cuaderno de notas cotidianas si alguien está mirando de cerca.
- **Decisión 3: Detección en Dispositivo con Acelerómetro (`SensorManager`)**
  - *Enfoque Elegido*: Utilizar `Sensor.TYPE_ACCELEROMETER` local sin permisos invasivos de cámara ni ubicación para detectar tanto el movimiento de caminata como el gesto rápido de inclinación (*Tilt-to-Hide*).
  - *Por Qué*: Cumple estrictamente con las políticas de privacidad y consume mínima batería, funcionando al instante sin depender de conexión a internet.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Diagrama de Arquitectura y Componentes**:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (FLAG_SECURE dinámico)                 │
│  ┌──────────────────────┐                  ┌────────────────────────────┐  │
│  │   AuthScreen         │                  │   AppMainContent           │  │
│  │  ┌────────────────┐  │  Auth Success    │  ┌──────────────────────┐  │  │
│  │  │GoogleSignInBtn │──┼─────────────────►│  │ CuentaConmigoTopBar  │  │  │
│  │  │ + Fallback ID  │  │                  │  │ [Escudo Modo Calle]  │  │  │
│  │  └────────────────┘  │                  │  │ [Botón SOS] [Menú]   │  │  │
│  └──────────────────────┘                  │  └──────────┬───────────┘  │  │
└────────────────────────────────────────────┼─────────────┼──────────────┘  │
                                             │             ▼                 │
┌────────────────────────────────────────────┼───────────────────────────────┐
│                         AppViewModel & Privacy Engine                      │
│  ┌─────────────────────────┐    ┌──────────┴───────────┐   ┌────────────┐  │
│  │ StreetPrivacyDetector   │───►│ StreetPrivacyState   │◄──┤ Gemini AI  │  │
│  │ (Acelerómetro: Caminata │    │ - isStreetModeActive │   │ Camouflage │  │
│  │  y Gesto Tilt-to-Hide)  │    │ - autoSensorEnabled  │   │ (Firebase  │  │
│  └─────────────────────────┘    │ - aiDisguiseActive   │   │  AI Logic) │  │
│                                 │ - revealedItemIds    │   └────────────┘  │
│                                 └──────────┬───────────┘                   │
└────────────────────────────────────────────┼───────────────────────────────┘
                                             ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                Pantallas Protegidas con PrivacyShieldMask                  │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌───────────┐  ┌────────────┐  │
│  │  Inicio  │  │  Diario  │  │   Ánimo   │  │ Pastillas │  │   Agenda   │  │
│  └──────────┘  ┌──────────┘  └───────────┘  └───────────┘  └────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

- **Modelo de Estado y Privacidad**:
  - `isStreetModeActive: StateFlow<Boolean>`: Estado global reactivo que indica si el Modo Calle está protegiendo la pantalla.
  - `autoMotionPrivacyEnabled: StateFlow<Boolean>`: Define si el sensor de movimiento/entorno activa automáticamente el Modo Calle al detectar caminata o sacudida/inclinación brusca.
  - `temporarilyRevealedIds: StateFlow<Set<String>>`: Conjunto de IDs de tarjetas individuales que el usuario ha tocado para previsualizar discretamente durante 5 segundos antes de volver a ocultarse automáticamente.
  - `aiCamouflageCache: StateFlow<Map<String, String>>`: Mapa en memoria de resúmenes neutros generados por `gemini-3.8-flash` para disfrazar entradas sensibles sin modificar los documentos reales en Firestore.

- **Mapeo de Interacciones y Recuperación de Errores**:
  - **Inicio de Sesión con Google**:
    - Desempacado seguro de `Context` a `Activity` (`findActivity()`).
    - Bloqueo de colisión (`isAutoSignInRunning`) para que el botón manual cancele cualquier verificación silenciosa en curso antes de abrir el selector de cuentas.
    - Si `GetSignInWithGoogleOption` lanza `GetCredentialCancellationException` o excepción de proveedor, reintenta limpiando el estado de credenciales o usando `GetGoogleIdOption(filterByAuthorizedAccounts = false)` y ofrece acceso inmediato sin bloqueo.
  - **Modo Calle y Sensor de Movimiento**:
    - Al activarse `isStreetModeActive`, `MainActivity` activa `WindowManager.LayoutParams.FLAG_SECURE` en la ventana actual.
    - Un componente reutilizable `PrivacyProtectedText` / `PrivacyMaskedCard` aplica difuminado (`Modifier.blur` en API 31+ más máscara de caracteres/píldora discreta en todas las versiones de Android) y permite revelar un ítem individual por 5 segundos al tocarlo.
    - Desde el diálogo de configuración rápida del escudo en la barra superior, el usuario puede:
      1. Activar/desactivar **Modo Calle** en 1 toque.
      2. Activar/desactivar la **Detección Inteligente de Movimiento (Caminata / Giro brusco)**.
      3. Activar el **Camuflaje con IA (Gemini)** para que los textos sensibles se muestren como tareas cotidianas neutras frente a terceros.
