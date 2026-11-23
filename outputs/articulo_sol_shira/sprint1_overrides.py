def reset_slot(index, texts):
    start = paras[index]._p
    stop = paras[index + 1]._p
    node = start.getnext()
    while node is not None and node is not stop:
        nxt = node.getnext()
        if node.tag == qn('w:p') and not node.xpath('./w:pPr/w:sectPr'):
            node.getparent().remove(node)
        node = nxt
    paragraphs(index, texts)

title = 'Sistema web de reservas para el Complejo Turístico Sol de Shirá mediante Scrum durante el primer sprint'
fill(3, title)
fill(4, 'Web reservation system for Complejo Turístico Sol de Shirá using Scrum during the first sprint')
abstract = ('El Complejo Turístico Sol de Shirá, ubicado en Trujillo, Perú, coordina consultas de disponibilidad, reservas y pagos mediante WhatsApp y registros manuales. El objetivo es implementar un sistema digital que centralice el ciclo de hospedaje. Este reporte describe exclusivamente el Sprint 1, contrastando historias de usuario, tareas, bitácora, tablero y código del commit 776a2ec, fechado el 5 de noviembre de 2026. Se emplea SWEBOK para organizar las actividades de ingeniería, Scrum para la gestión iterativa y OpenUP como referente de comparación. La arquitectura integra Angular, un gateway, cinco servicios funcionales y persistencia MySQL. El alcance seleccionado comprende ocho historias, desde autenticación y catálogo hasta adelanto, consulta de reservas y check-in. La suma nominal de la columna moda es 42 puntos, mientras el burndown registra 43 unidades iniciales y cero finales. Esta diferencia impide interpretar todavía la gráfica como velocidad verificada. Se incorpora el modelo de datos proporcionado y se documentan sus diferencias con el código histórico. La experiencia ofrece una base trazable de construcción, cuya aceptación empresarial y rendimiento operacional requieren validación posterior.')
eng = ('Complejo Turístico Sol de Shirá, located in Trujillo, Peru, coordinates availability enquiries, reservations and payments through WhatsApp and manual records. The project aims to implement a digital system that centralizes the accommodation workflow. This report describes Sprint 1 exclusively by comparing user stories, tasks, work logs, the board and source code from commit 776a2ec, dated November 5, 2026. SWEBOK organizes the engineering activities, Scrum supports iterative management, and OpenUP provides a comparative reference. The architecture integrates Angular, a gateway, five functional services and MySQL persistence. The selected scope includes eight stories covering authentication, catalogue browsing, advance payments, reservation queries and check-in. The nominal sum of estimates in the mode column is 42 points, whereas the burndown records 43 initial units and zero remaining units. This discrepancy prevents interpreting the chart as verified velocity. The supplied data model is included, and its differences from historical source code are documented. The experience provides a traceable construction baseline whose business acceptance and operational performance require further validation.')
fill(9, 'Resumen— ' + abstract)
fill(11, 'Abstract— ' + eng)
metadata[0] = 'Sistema de Gestión de Reservas Sol de Shirán. Corte técnico del Sprint 1.'
metadata[2] = 'https://github.com/CaDaGC/Sol-de-Shiran · Commit 776a2ec, 05/11/2026. Análisis del código histórico, sin modificar la rama.'
metadata[4] = 'Sprint 1. Tablero 31/10–04/11/2026 y revisión de integración el 05/11. La bitácora incluye trabajo preparatorio desde el 22/10. Duración pactada por confirmar.'
metadata[6] = 'Angular, API Gateway, autenticación, usuarios, catálogo, reservas, pagos, Eureka, Config Server y MySQL.'
for row, value in zip(tables[0].rows[1:], metadata):
    replace(row.cells[2].paragraphs[0], value)

reset_slot(23, [
    'Alami y Krancher [4] relacionan la calidad con colaboración, transparencia e inspección a partir de 39 entrevistas y dos estudios de caso. Para el primer sprint de Sol de Shirán, esta evidencia orienta a contrastar el cierre de tareas con una versión integrada y pruebas reproducibles.',
    'Hron y Obwegeser [5] sintetizan los objetivos y estrategias con los que distintas organizaciones adaptan Scrum. Su revisión ayuda a interpretar el uso de hojas compartidas en un contexto académico y la necesidad de explicitar los acuerdos de trabajo.',
    'Özkan y Özcan-Top [6] estudian decisiones de adaptación y sus efectos en prácticas, roles y artefactos. Las dificultades de comprensión del objetivo y de retroalimentación descritas justifican verificar la correspondencia entre historias, tablero y acuerdos de cierre.',
    'Sassa et al. [7] aportan una revisión sistemática de la investigación sobre Scrum que contextualiza el marco de gestión. El caso presentado examina un ciclo de hospedaje concreto, incluyendo adelanto y check-in, y detalla su trazabilidad técnica. La literatura revisada se concentra en calidad y adaptación de procesos, por lo que no permite atribuir al campestre reducciones de tiempo o de errores sin mediciones locales.'
])
reset_slot(27, ['Se presenta un reporte descriptivo de experiencia académica con trabajo cooperativo. Las unidades de análisis pertenecen al Sprint 1. La revisión del repositorio se fijó en 776a2ec, que incorpora la integración de HU5, HU6 y HU7 y ajustes de pagos al 05/11/2026. El Excel se utiliza para identificar selección y estimación, no para trasladar funcionalidades posteriores al corte. SWEBOK organiza requisitos, diseño, construcción, pruebas y configuración [1]. Scrum aporta la gestión iterativa [2], y Kniberg ofrece ejemplos prácticos [8]. OpenUP se utiliza como comparación. El uso de Scrum se justifica por la necesidad de entregar incrementos y revisar el alcance con el negocio, sin afirmar una evaluación experimental frente a Kanban o XP.'])
reset_slot(30, [
    'El Project Charter constituye el proyecto de desarrollo e implementación. Sus objetivos comprenden describir el problema, delimitar alcance, identificar stakeholders, definir entregables y arquitectura, organizar responsabilidades y planificar el desarrollo iterativo. El objetivo del producto es digitalizar reservas y servicios. Este reporte examina el primer incremento de hospedaje, desde la consulta hasta el ingreso presencial.',
    'Los stakeholders identificados por función son huésped, trabajador responsable de la atención, administrador y equipo de desarrollo. La EDT del alcance revisado comprende requisitos y planificación, interfaz Angular, servicios de dominio, persistencia, integración de adelanto, pruebas y documentación. Los riesgos observados incluyen inconsistencias en códigos de HU, dependencia de la pasarela y coordinación entre pago y ocupación.',
    'La matriz normalizada contiene 10 RF y 4 RNF relacionados con el Sprint 1. Cada fila distingue evidencia del código histórico y prueba asociada. El nombre de autenticación se usa para conciliar HU21 en la selección y HU22 en el tablero, sin mezclarlo con el registro público desarrollado posteriormente. Como control de cambios se propone registrar motivo, historia afectada, decisión y prueba antes de actualizar estados.'
])
reset_slot(33, [
    'Carlos Gonzales desempeña Scrum Master y Julio Pantoja Product Owner, según la información aportada. Miguel Acevedo trabaja en análisis y pruebas, Lee Orbezo en desarrollo y Antonella Baldeon en base de datos. Estas especialidades contribuyen al trabajo de Developers. El objetivo del Sprint 1 es centralizar el flujo desde la selección de la cabaña hasta el ingreso presencial. Se seleccionan HU1 a HU7 y autenticación, denominada HU21 en la pestaña Sprints.',
    'La bitácora incluye actividades preparatorias del 22 al 29/10 y una revisión de integración el 05/11. El tablero registra cortes del 31/10 al 04/11, con repetición del 04/11 en el último día. Los cortes permiten seguir estados, pero la realización y duración de Daily, Review y Retrospective requiere examinar las imágenes y acuerdos que el equipo indica tener. La DoD debe contrastarse con calidad e integración, además de criterios de aceptación.'
])
reset_slot(37, [
    'La arquitectura C4 se reconstruye con los módulos y rutas existentes en 776a2ec [9]. El cliente y el trabajador acceden mediante Angular. El gateway dirige solicitudes a autenticación, usuarios, catálogo, reservas y pagos. Eureka proporciona descubrimiento y Config Server configuración. Los datos se organizan en los esquemas usuario_db, db_catalogo, db_reservas y db_pagos configurados para MySQL.',
    'El esquema compartido como imagen representa cabana, cabana_imagenes, cabana_comodidades, reserva, transaccion_pago y webhook_log, con usuario como referencia externa. Es una evidencia visual de modelado. El código histórico usa tarifa_noche donde la imagen indica precio_base_noche y también contiene pago_restante, id_reserva_ocupante y campos de idempotencia del checkout. cabana_comodidades no aparece como entidad en el corte consultado.',
    'Las relaciones entre dominios se representan como referencias lógicas y comunicación HTTP. Las líneas entre cabaña, reserva y pagos no acreditan claves foráneas físicas entre esquemas. La asociación opcional de webhook_log a transaccion_pago está marcada como propuesta en la imagen. Los tipos NUMBER y VARCHAR2 de la captura deben conciliarse con MySQL antes de usarla como DDL. El diagrama de datos reconstruye esa captura y presenta pago_restante como extensión sustentada en código.',
    'Las decisiones técnicas observadas incluyen JWT, BCrypt, REST y JPA. Angular [10] consume la API y Spring [11] implementa los servicios. Mercado Pago [12] interviene en el adelanto y las notificaciones verificadas. Las secuencias distinguen crear una solicitud de confirmar su pago, y registrar el saldo presencial de actualizar el check-in. Las rutas y clases se documentan con el corte histórico.'
])
reset_slot(41, ['Se identificaron pruebas disponibles en el corte para consulta y detalle de reservas, check-in, saldo restante, pasarela y control de acceso. La tabla de pruebas vincula escenarios representativos con las clases históricas. Los reportes actuales de target mezclan versiones y fechas, por lo que se excluyen como resultado del Sprint 1. La cantidad de clases o métodos no se interpreta como porcentaje de requisitos aprobados. El resultado de ejecución de la versión histórica queda por reproducir en un ambiente de pruebas.'])
fill(47, 'Tabla 2. Alcance documental y estimación nominal del Sprint 1.')
for cell, value in zip(tables[4].rows[1].cells, ['1', 'Reserva hasta ingreso presencial', '8 seleccionadas; cierre por validar', '42 nominales / cierre por reconciliar']):
    replace(cell.paragraphs[0], value)
reset_slot(48, [
    'Fig. 3. Burndown disponible para el alcance analizado.',
    'La columna moda asigna 38 puntos a HU1–HU7 y 4 a HU21, para un total nominal de 42. El burndown parte de 43 unidades y termina en cero. El archivo no identifica de manera inequívoca esas unidades como puntos de HU y la serie planificada termina en −1. Se conserva la gráfica fuente y se registra la diferencia antes de calcular una velocidad o afirmar entrega completa.',
    'El tablero y la bitácora ofrecen evidencia de avance y revisión. El código integrado al 05/11 permite localizar las funciones de consulta, pagos e ingreso. La aprobación de cada historia requiere contrastar los criterios de aceptación y la DoD con pruebas de la versión histórica.'
])
tests = [
    ['CP01', 'HU5', 'Consultar panel con filtros y paginación', 'Clase histórica disponible; ejecución por reproducir'],
    ['CP02', 'HU6', 'Consultar detalle y saldo de reserva', 'Clase histórica disponible; ejecución por reproducir'],
    ['CP03', 'HU7', 'Formalizar check-in y ocupación', 'CheckInTest disponible; ejecución por reproducir'],
    ['CP04', 'HU7', 'Registrar saldo e impedir duplicado', 'PagoRestanteTest disponible; ejecución por reproducir'],
    ['CP05', 'HU4', 'Procesar adelanto y reintentos', 'OrdersFlowTests disponible; ejecución por reproducir']
]
for row, values in zip(tables[5].rows[1:], tests):
    for cell, value in zip(row.cells, values):
        replace(cell.paragraphs[0], value)
fill(50, 'Tabla 3. Escenarios y pruebas localizados en el corte histórico.')
fill(53, 'La descripción operativa proporcionada por el equipo sustenta las necesidades del campestre y la regla del adelanto del 50 %. El video de entrevista permanece pendiente de revisión, transcripción y confirmación de uso. Para afirmar conformidad del primer incremento se requiere identificar al representante, la versión revisada y su retroalimentación. El corte disponible no permite estimar una reducción medida de tiempos, errores o sobre-reservas.')
reset_slot(56, [
    'El Sprint 1 permite describir una integración que va desde catálogo y disponibilidad hasta adelanto, panel, detalle e ingreso. Los controladores y las pruebas presentes en la versión histórica respaldan el análisis técnico del alcance. La consistencia del producto debe comprobarse con una ejecución reproducible antes de interpretar el cierre de tareas como aceptación empresarial.',
    'La experiencia coincide con Alami y Krancher [4] en la relevancia de la colaboración y la verificación para la calidad. Las revisiones de Hron y Obwegeser [5] y Özkan y Özcan-Top [6] orientan a documentar las adaptaciones académicas y conservar acuerdos. OpenUP aporta una referencia para articular requisitos, arquitectura, riesgos y pruebas durante la iteración.',
    'Las limitaciones incluyen un único caso, falta de línea base operacional, fechas repetidas en el tablero, diferencias entre estimaciones y burndown, evidencia audiovisual pendiente y discrepancias entre el esquema de datos recibido y las entidades. Las actividades reconstruidas en la bitácora requieren confirmación del equipo antes de servir como prueba de reuniones. Estas condiciones delimitan las conclusiones al avance técnico del primer sprint.'
])
reset_slot(59, [
    'Se relacionaron 10 requisitos funcionales del Sprint 1 con historias y evidencia del código histórico. La matriz permite distinguir autenticación, consulta, reserva, adelanto, panel, detalle y check-in, así como las condiciones técnicas que requieren pruebas.',
    'La arquitectura del primer sprint contiene Angular, gateway, cinco servicios funcionales y persistencia MySQL con soporte de Eureka y Config Server. Los diagramas C4 y las secuencias describen cómo intervienen esos componentes en la reserva y el ingreso.',
    'El primer sprint aporta evidencia documental y de construcción. Las discrepancias entre puntos y unidades de la gráfica impiden calcular una velocidad validada. La aceptación empresarial y el despliegue productivo requieren verificación posterior sobre una versión identificable.'
])
fill(60, 'El trabajo futuro del alcance revisado comprende reproducir el commit 776a2ec en un entorno de pruebas, ejecutar los casos críticos con MySQL y pasarela de prueba, reconciliar las unidades del burndown y validar la DoD. También se debe contrastar el modelo visual con el DDL real y revisar el video con la organización. El impacto puede evaluarse midiendo tiempo de confirmación, cruces de reservas y esfuerzo de conciliación de pagos antes y después.')
fill(63, 'Uso de IA generativa. ChatGPT/Codex apoyó la revisión del repositorio histórico, la redacción del borrador y la elaboración de figuras y trazabilidad. Los autores deben verificar referencias, resultados y atribuciones antes de su presentación.')
