# Registro de Cambios (Release Notes) - Agenda Colaborativa

Este documento detalla las funcionalidades implementadas en la aplicación hasta la fecha, basándose en el plan original y las nuevas características añadidas.

## Version Actual 1.1.3-1

### Migration to kotlin Language

- **UI CHANGES** :

  - **Navegación Principal**:
    - Implementación de BottomNavigationBar con secciones de Inicio, Tareas, Mensajes y Perfil.
  - **Badges de Notificación**:
    - Indicador visual de mensajes no leídos en el icono de navegación.
  - **Chat**: Interfaz completa de chat (Lista y Detalle) con soporte para envío de imágenes y previsualización.
  - **Detalle de Grupo**: Nueva opción para compartir el código de grupo mediante Intent nativo.
  - **Componentes Reutilizables**: AvatarStack para usuarios asignados, Dialogs de confirmación.

- **Nuevas Caracteristicas** :

  - **Sistema de Mensajería**:

    - Soporte para chats 1:1 y grupales, persistencia en Firestore (`chatThreads`), y adjuntos (imágenes).
    - **Gestión de Estados de Lectura**: Lógica para marcar mensajes como leídos y contar no leídos por usuario.

    - **Búsqueda de Usuarios**: Soporte para búsqueda por correo electrónico y número de teléfono.

    - **Notificaciones**: Generación de notificaciones en Firestore para eventos críticos (Mensajes, Asignaciones, Cambios de Estado).

    - **Subida de Archivos**: Integración con Firebase Storage para imágenes de chat y evidencia de tareas.

    - **Recorte de Imágenes**: Implementación de `ImageCropView` para recortar fotos de perfil antes de subirlas.

## Versión Actual: v1.1.2

### ✨ Nuevas Características (v1.1.x)

#### v1.1.2 - Gestión de Mensajería

- **Eliminar Chats:** Se ha añadido la funcionalidad para eliminar hilos de conversación (chats) de la lista de mensajes mediante un gesto de deslizamiento (swipe).
- **Vaciar Chat:** Se ha incorporado la opción de "Vaciar Chat" dentro del menú de la conversación, permitiendo borrar todos los mensajes sin eliminar el hilo.

#### v1.1.1 - Gestión de Tiempos en Subtareas

- **Fechas Límite en Subtareas:** Ahora es posible establecer una fecha límite específica para cada subtarea.
- **Solicitud de Aplazamiento:** Los miembros asignados pueden solicitar posponer la fecha límite indicando una razón.
- **Aprobación de Aplazamientos:** El creador de la tarea debe aceptar o rechazar las solicitudes de cambio de fecha.

#### v1.1.0 - Gestión de Presupuesto y Pujas

- **Presupuesto por Subtarea:** El creador puede asignar un presupuesto monetario a cada subtarea.
- **Sistema de Pujas:** Los miembros pueden "pujar" o proponer un costo para realizar la subtarea.
- **Aceptación de Presupuesto:** El creador puede aceptar una puja específica, fijando el presupuesto final.

### 🚀 Versiones Anteriores (v1.0.x)

#### v1.0.9 - Asignación y Aceptación de Tareas

- **Asignación de Miembros:** Posibilidad de asignar uno o más miembros específicos a una subtarea.
- **Flujo de Aceptación:** Los miembros asignados deben "Aceptar" o "Rechazar" la asignación antes de comenzar.
- **Indicadores de Estado:** Visualización clara del estado de la asignación (Pendiente, Aceptado, Rechazado).

#### v1.0.7 / v1.0.8 - Evidencia y Caché

- **Evidencia Fotográfica:** Requisito obligatorio de subir una foto al completar una subtarea.
- **Caché de Imágenes:** Implementación de `cached_network_image` para guardar las imágenes localmente y ahorrar datos/tiempo de carga.
- **Visualización:** Miniaturas y visor de imágenes para las confirmaciones.

#### v1.0.5 - Comunicación y Notificaciones

- **Chat por Tarea:** Sistema de mensajería integrado dentro de cada tarea para discutir detalles.
- **Notificaciones Push:** Alertas para asignaciones, mensajes nuevos y actualizaciones de estado.

#### 🛠️ Funcionalidades Base (Core)

Basado en el *Blueprint* original:

- **Gestión de Usuarios:** Registro e inicio de sesión (Email/Password y Google).
- **Gestión de Grupos:** Creación de grupos de trabajo y adición de miembros.
- **Tareas Principales:** Creación de tareas con título, descripción, prioridad y fecha límite general.
- **Subtareas:** Desglose de tareas en pasos más pequeños.
- **Flujo de Revisión:**
- Las tareas no se marcan como completadas automáticamente.
- Pasan a estado "En Revisión" cuando todas las subtareas están listas.
- Solo el creador puede aprobar y finalizar la tarea.
- **Filtros y Ordenación:** Organización por prioridad y fechas.

---

#### *Última actualización: 21 de Noviembre de 2025*
