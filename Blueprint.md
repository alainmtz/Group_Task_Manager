🏗️ Proceso de Prototipado y Desarrollo de ColaborativeTasks APP
FASE 1: Definición de la Aplicación y Modelo de Datos
Objetivo: Crear un gestor de tareas que soporte listas personales y colaboración en grupos.
1. Descripción de la App:
> "Una aplicación de gestión de tareas que permite a los usuarios: crear listas separadas, 
> añadir tareas con fecha límite y prioridad. 
> La clave es la colaboración: soporta la creación de grupos y la asignación de tareas a uno 
> o más miembros. La aplicación debe manejar subtareas y un flujo de cierre controlado 
> donde solo el creador puede finalizar la tarea principal después de ser revisada."

2. Modelo de Datos (Schema - Indicación para Firebase Firestore):
   | Entidad | Campos Clave (Datos a Guardar) | Finalidad |
   |---|---|---|
   | Usuario | ID de Usuario, Nombre, Email | Autenticación y Grupos. |
   | Grupo/Proyecto | ID, Nombre, Lista de ID de Usuarios Miembros | Colaboración y Permisos. |
   | Tarea Principal | ID, Título, Descripción, Fecha Límite, Prioridad, ID de Grupo, ID de Creador, ID de Usuarios Asignados (Lista), Estado (Pendiente/Revisión/Completada) | Detalle y Asignación. |
   | Subtarea | ID, Título, Estado (Completada/Pendiente), ID de Tarea Principal, ID de Usuarios Asignados (Lista) | Jerarquía, Asignación Específica y Flujo de Finalización. |
   FASE 2: Diseño de Interfaz y Lógica (Jetpack Compose)
3. Pantallas Esenciales (Wireframing):
* Pantalla de Autenticación (Registro / Inicio de Sesión).
* Pantalla Principal de Navegación (Cambio entre Listas Personales / Grupos).
* Pantalla de Lista de Tareas (Núcleo de la aplicación).
* Pantalla de Creación/Edición de Tarea (Formulario de asignación).
* Pantalla de Gestión de Grupos (Flujo de invitación).
4. Indicaciones de Lógica de la Lista (Filtros y Ordenación):
* Ordenación por Defecto: La consulta a la base de datos debe aplicar un doble ordenamiento:
    * Prioridad Descendente (Alta primero).
    * Luego, Fecha Límite Ascendente (la más cercana primero).
* Filtros (Diálogo): Implementar un botón de Filtros que active un ModalBottomSheet o AlertDialog. Este debe incluir:
    * Filtros por Prioridad: Opciones de selección única (Alta, Media, Baja, Todas).
    * Filtros por Rango de Fecha: Selectores de fecha para definir una fecha de inicio y una fecha de fin.
      FASE 3: Flujos Críticos de Colaboración
5. Flujo de Invitación a Grupo (Indicación de Backend/Auth):
* UI: En la Pantalla de Gestión de Grupos, el anfitrión introduce el email del invitado.
* Lógica: La aplicación usa Firebase Functions para:
    * Verificar la existencia del email.
    * Generar un Enlace Dinámico de Firebase con el ID del Grupo.
    * Enviar un correo electrónico con el enlace.
* Aceptación: Cuando el invitado hace clic en el enlace, la app se abre (o se descarga). Se muestra un Dialog con los detalles de la invitación, y al hacer clic en "Aceptar", el ID de Usuario del invitado se añade al campo Lista de ID de Usuarios Miembros del grupo en Firestore.
6. Flujo de Asignación y Finalización Controlada (Business Logic):
* Asignación (UI): En la Pantalla de Creación/Edición de Tarea, usar componentes de Selección Múltiple para asignar la tarea principal y/o las subtareas a uno o más miembros del grupo.
* Finalización (Regla de Negocio):
    * Los miembros asignados solo pueden marcar sus Subtareas (o las que tengan asignadas) como completadas.
    * La Tarea Principal cambia a estado "Pendiente de Revisión" cuando todas las subtareas han sido completadas por los miembros correspondientes.
    * Solo el Creador de la Tarea Principal (guardado en `ID de Creador`) puede marcarla como "Completada" de forma final.
      🌟 Siguiente Acción Recomendada
      Para continuar, el escenario de prueba crucial para validar toda esta lógica es:

> Escenario de Prueba: "Un anfitrión crea una Tarea Principal con 2 subtareas. Asigna la Subtarea 1 al Miembro A y la Subtarea 2 a los Miembros B y C. los Miembros B y c pueden hacer Bid Por la el precio de la tarea, y pueden solicitar posponer la fecha de finalizacion . Verificar que la Tarea Principal solo pasa a 'Pendiente de Revisión' cuando los tres miembros han completado sus subtareas , cada tarea al completarse se debe subir una imagen como prueba de finalizacion , pasaria a pendiente , aun asi solo el creador de la subtarea o la tarea principal puede completar la finalizacion de la subtarea , respectivas, y que no se puede marcar como 'Completada' hasta que el creador la aprueba."
>