from pathlib import Path
from copy import deepcopy
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from PIL import Image, ImageDraw, ImageFont
import hashlib,json
from docx.shared import Cm
from xml.etree import ElementTree as ET

root=Path.cwd()
out=root/'outputs'/'articulo_sol_shira'
out.mkdir(parents=True,exist_ok=True)
template=Path(r'C:/Users/tania/Downloads/Plantilla_Articulo_Corto_Ing_Software_I 3er año.docx')
doc=Document(template)
paras=list(doc.paragraphs)
tables=list(doc.tables)
original_sections=[ET.tostring(s._sectPr) for s in doc.sections]
contract='Reference '+str(template)+'\nSHA256 '+hashlib.sha256(template.read_bytes()).hexdigest()+'\nA4 portrait, margins 2 cm top/bottom and 1.8 cm sides. Three original sections retained, final section in two columns. All section properties, headers, footers, tables and paragraph properties retained. Replace placeholder prose and clone existing paragraph/table-row patterns when needed. Figures occupy original figure slots.\n'
(out/'artifact.md').write_text(contract,encoding='utf-8')
def replace(p,text):
 if p.runs:
  p.runs[0].text=text
  for r in p.runs[1:]: r.text=''
 else: p.add_run(text)
 for r in p.runs:
  if r.font.color.rgb: r.font.color.rgb=RGBColor(0,0,0)
  r.italic=False
def fill(i,text): replace(paras[i],text)
def paragraphs(i,texts):
 p=paras[i]
 replace(p,texts[0])
 anchor=p
 for text in texts[1:]:
  el=deepcopy(p._p)
  anchor._p.addnext(el)
  from docx.text.paragraph import Paragraph
  anchor=Paragraph(el,p._parent)
  replace(anchor,text)
def remove(i):
 p=paras[i]
 if p._p.pPr is not None and p._p.pPr.sectPr is not None:
  replace(p,'')
 else: p._p.getparent().remove(p._p)

title='Desarrollo de un sistema web de reservas para el Complejo Turístico Sol de Shirá mediante Scrum en Trujillo Perú'
fill(0,'REPORTE DE EXPERIENCIA')
fill(3,title)
fill(4,'Development of a web reservation system for Complejo Turístico Sol de Shirá using Scrum in Trujillo Peru')
fill(5,'Carlos Gonzales¹, Julio Pantoja¹, Miguel Acevedo¹, Lee Orbezo¹ y Antonella Baldeon¹')
fill(7,'Contacto del equipo para este borrador: tania.baldeon@gmail.com. Autor de correspondencia y ORCID por confirmar.')
paragraphs(7,[
 'Contacto del equipo: tania.baldeon@gmail.com. Autor de correspondencia y ORCID por confirmar.',
 'Carlos Gonzales: 60880134@upeu.edu.pe · Julio Pantoja: 72653954@upeu.edu.pe · Lee Orbezo: 60773687@upeu.edu.pe.',
 'Miguel Acevedo: 70622248@upeu.edu.con (dominio por confirmar). Antonella Baldeon: tania.baldeon@gmail.com.'
])
fill(8,'Curso: Ingeniería de Software I · Proyecto integrador · Universidad Peruana Unión · Fecha de corte: 16/11/2026')
abstract='El Complejo Turístico Sol de Shirá, ubicado en Trujillo, Perú, gestiona hospedaje temporal y servicios recreativos mediante consultas por WhatsApp y registros manuales. Este contexto plantea dificultades para coordinar disponibilidad, reservas y pagos. El objetivo del proyecto es implementar una plataforma digital que centralice dichos procesos. Se presenta un reporte de experiencia académica basado en revisión del repositorio y de las historias de usuario, tableros y bitácoras de dos sprints. SWEBOK organiza las actividades de ingeniería, Scrum orienta la planificación iterativa y OpenUP se utiliza como referente comparativo. El producto integra Angular, servicios Spring Boot, MySQL y una pasarela de pagos. Se documentan ocho historias seleccionadas en el Sprint 1 y seis en el Sprint 2, con estimaciones nominales de 42 y 31 puntos mediante la columna moda. El burndown disponible registra 43 unidades iniciales y cero finales, aunque requiere reconciliación con las estimaciones. La experiencia evidencia una base implementada para el ciclo de hospedaje, pero la ausencia de una línea base única de pruebas y de conformidad empresarial impide cuantificar todavía mejoras operativas.'
eng='Complejo Turístico Sol de Shirá, located in Trujillo, Peru, manages temporary accommodation and recreational services through WhatsApp enquiries and manual records. This context creates difficulties in coordinating availability, reservations and payments. The project aims to implement a digital platform that centralizes these processes. This academic experience report reviews the repository, user stories, boards and work logs associated with two sprints. SWEBOK organizes the engineering activities, Scrum supports iterative planning, and OpenUP provides a comparative reference. The product integrates Angular, Spring Boot services, MySQL and a payment gateway. The documents identify eight stories selected for Sprint 1 and six for Sprint 2, with nominal estimates of 42 and 31 points using the mode column. The available burndown records 43 initial units and zero remaining units, although these values require reconciliation with story estimates. The experience shows an implemented foundation for the accommodation workflow. However, the absence of a single testing baseline and documented business acceptance currently prevents a quantitative assessment of operational improvements.'
fill(9,'Resumen— '+abstract)
fill(10,'Palabras clave— Scrum, ingeniería de software, reservas, trazabilidad, microservicios.')
fill(11,'Abstract— '+eng)
fill(12,'Keywords— Software engineering, software testing, information systems, software architecture, project management.')
metadata=[
'Sistema de Gestión de Reservas Sol de Shirán. Versión académica de trabajo.',
'Complejo Turístico Sol de Shirá, Trujillo, Perú. Hospedaje y recreación. Autorización documental por confirmar.',
'https://github.com/CaDaGC/Sol-de-Shiran · HEAD a46233b, 10/11/2026. La revisión incluye modificaciones locales posteriores.',
'Google Sheets y archivo Historias de usuario.xlsx. No se ha verificado un tablero Jira.',
'Dos sprints revisados. Tablero 1: 31/10–04/11, con revisión de integración el 05/11. Tablero 2: 06/11–11/11. Duración acordada por confirmar.',
'Carlos Gonzales, Scrum Master. Julio Pantoja, Product Owner. Developers con especialidades de análisis/pruebas, desarrollo y base de datos.',
'Cliente web Angular, API Gateway, servicios REST y esquemas MySQL por dominio.',
'TypeScript, Angular 22, Java 21, Spring Boot, Spring Cloud, JPA, MySQL y Mercado Pago.',
'Git y ramas por funcionalidad. Etiqueta de entrega consolidada por confirmar.',
'JDK 21, dependencias Maven y npm, MySQL configurado en puerto 3307 y servicios de infraestructura activos.',
'Licencia y condiciones de publicación por acordar con la organización y los autores.',
'tania.baldeon@gmail.com']
for row,val in zip(tables[0].rows[1:],metadata): replace(row.cells[2].paragraphs[0],val)
for i in [14,17,22,26,29,32,36,40,43,52,55,58,68]: remove(i)

paragraphs(18,[
'El Complejo Turístico Sol de Shirá ofrece hospedaje temporal y espacios recreativos en Trujillo. De acuerdo con la descripción del negocio proporcionada por el equipo, las consultas de precios y disponibilidad se reciben por WhatsApp. El personal contrasta manualmente las fechas, solicita un adelanto obligatorio del 50 % y confirma la reserva después de validar el pago. Al ingreso se cobra el saldo y se coordina la preparación del ambiente.',
'La gestión fragmentada de reservas y pagos dificulta mantener una disponibilidad compartida entre los actores del negocio. La necesidad de consultar registros separados puede ocasionar demoras, duplicidad de coordinaciones y riesgo de cruce de fechas. Estos problemas constituyen la motivación del proyecto. Su magnitud todavía requiere una línea base con tiempos de respuesta, incidencias y reservas superpuestas, porque la documentación revisada no aporta esas mediciones.',
'El beneficiario también presta servicios de restaurante, limpieza y mantenimiento. El alcance estratégico comprende cabañas, instalaciones y atención gastronómica. Para el corte analizado, la evidencia de implementación se concentra en el ciclo de hospedaje. Los pedidos de restaurante aparecen en la planificación posterior y requieren evaluación independiente antes de declararlos entregados.'
])
fill(19,'El trabajo emplea SWEBOK para ordenar requisitos, diseño, construcción, pruebas y configuración [1]. Scrum aporta un marco de inspección y adaptación por sprints [2]. El objetivo general es implementar un sistema digital de reservas que optimice la gestión de cabañas, instalaciones y restaurante. Los objetivos específicos son analizar RF y RNF, elaborar la arquitectura, construir los componentes mediante sprints y probar y desplegar una versión operativa. La optimización se plantea como objetivo a evaluar con la organización, sin atribuir porcentajes de mejora aún no medidos.')
fill(20,'Las contribuciones son una arquitectura por dominios para integrar reservas y pagos, una matriz normalizada que relaciona requisitos e historias con evidencias del repositorio, y una revisión de consistencia de la planificación y las pruebas. El reporte describe una experiencia de desarrollo y sus límites. La comparación con OpenUP utiliza las fases de Inicio, Elaboración, Construcción y Transición como referencia para organizar entregables de ingeniería [3].')

paragraphs(23,[
'Alami y Krancher [4] analizaron la calidad del software mediante 39 entrevistas y dos estudios de caso. Relacionan la mejora con colaboración, transparencia, inspección y prácticas de ingeniería, y describen condiciones que impiden materializarla. Para Sol de Shirán, esto respalda contrastar el tablero con pruebas y una versión integrada antes de interpretar las tareas terminadas como calidad demostrada.',
'Hron y Obwegeser [5] sintetizaron las modificaciones de Scrum reportadas en la literatura. Identifican objetivos y estrategias de adaptación a contextos diversos. Su aporte permite interpretar el entorno académico y las herramientas elegidas, pero exige hacer explícitas las adaptaciones. En este proyecto, Google Sheets registra el trabajo y las especialidades técnicas se describen dentro de Developers.',
'Özkan y Özcan-Top [6] revisaron las decisiones de adaptación de Scrum y los problemas reportados al alterar prácticas, roles y artefactos. Su revisión señala dificultades de retroalimentación y comprensión del objetivo. La comparación resulta pertinente ante los objetivos alternativos del Sprint 2 y la necesidad de preservar acuerdos verificables.',
'Sassa et al. [7] presentan una revisión sistemática de la investigación sobre Scrum. Su síntesis ofrece contexto para discutir su uso en proyectos complejos. Las revisiones y los casos citados se centran en prácticas de desarrollo y calidad. El presente reporte aporta un caso académico de reservas turísticas con trazabilidad entre necesidades, historias, endpoints y pruebas, sin atribuir generalización a partir de una sola organización.'
])
fill(27,'Se adoptó un reporte descriptivo de experiencia en aprendizaje basado en problemas y trabajo cooperativo. Las unidades de análisis son historias de usuario, tareas, cortes del tablero, bitácoras, controladores y reportes de pruebas. La extracción registra la fuente y distingue planificación, código presente y ejecución histórica. La selección de Scrum responde al alcance funcional cambiante y a la entrega por incrementos. Kanban puede complementar el flujo visual y XP aportar prácticas técnicas, pero no se ha realizado una comparación experimental entre enfoques. OpenUP sirve como contraste metodológico. Las técnicas del libro de Kniberg [8] se usan como apoyo contextual a la Guía Scrum 2020.')
paragraphs(30,[
'El Project Charter tiene como propósito constituir el desarrollo e implementación de la plataforma. Sus objetivos abarcan describir el problema, delimitar alcance, identificar stakeholders, definir producto y entregables, organizar el equipo y establecer el desarrollo iterativo. Los stakeholders identificados por función son cliente, administrador, propietario y trabajador del campestre, además del equipo académico. Debe confirmarse la persona que representa al negocio para la aceptación.',
'Los entregables de la EDT se agrupan en requisitos y planificación, interfaz web, servicios REST, persistencia, integración de pagos, pruebas y documentación. Los riesgos identificados en la revisión son inconsistencia entre identificadores de HU, dependencia de servicios externos y discrepancia entre estado documental y versión ejecutada. Estos riesgos requieren acuerdos del equipo para su priorización formal.',
'La matriz original mezclaba columnas, alcances futuros y estados pendientes genéricos. La normalización presentada en el anexo conserva los requisitos alineados a las HU y separa condiciones verificadas en código de propuestas por validar. Se definen 15 RF y 5 RNF candidatos. La numeración nueva permite trazar cada registro. Ante un cambio se debe registrar motivo, HU afectada, decisión y prueba relacionada antes de actualizar el estado. Este procedimiento se propone para consolidar las fuentes existentes.'
])
paragraphs(33,[
'El equipo informó a Carlos Gonzales como Scrum Master y a Julio Pantoja como Product Owner. Miguel Acevedo aporta análisis y pruebas, Lee Orbezo desarrollo y Antonella Baldeon administración de base de datos. Las especialidades forman parte del trabajo de Developers. El objetivo del Sprint 1 abarca la reserva hasta el ingreso presencial. El Sprint 2 incorpora administración de cabañas, check-out, historial, reseñas, métricas y registro de clientes, según la versión actual del Excel.',
'Los tableros presentan tres cortes de estado por día. Esos registros permiten examinar pendientes y desplazamientos, pero no acreditan por sí solos una reunión Daily de 15 minutos. El equipo indica que dispone de imágenes de eventos y un video de entrevista. Al no estar adjuntos al corte, las fechas y acuerdos de Review y Retrospective se mantienen pendientes de corroboración. La Definition of Done debe contrastarse con la integración y las pruebas, además de los criterios de aceptación por historia.'
])
paragraphs(37,[
'La arquitectura C4 de contenedores se reconstruyó a partir de módulos y rutas [9]. Angular consume la API a través del gateway. Auth-service autentica y solicita información a usuario-service. Catálogo administra cabañas, reservas coordina estancias, pagos gestiona adelantos y saldos, y reseñas publica las valoraciones. Los servicios usan esquemas MySQL asociados a su dominio. Eureka resuelve servicios y Config Server distribuye configuración.',
'La separación por dominio favorece la identificación de responsabilidades, aunque aumenta dependencias de despliegue y coordinación. La consistencia entre pago y reserva exige validaciones en la comunicación interna. Las claves de acceso y credenciales se omiten del artículo. Se revisó HEAD a46233b y modificaciones locales de HU22, que deben consolidarse en una etiqueta antes de reproducir una entrega.',
'Las decisiones observadas son uso de REST, JWT para acceso por rol, BCrypt para contraseñas y persistencia con JPA. Angular [10] organiza vistas y servicios HTTP. Spring [11] proporciona la base de servicios Java. Mercado Pago [12] interviene como sistema externo para el adelanto. La disponibilidad del código constituye evidencia de construcción, y el funcionamiento integral requiere un ambiente coordinado.'
])
fill(41,'La estrategia distingue pruebas de lógica de negocio, controladores HTTP e integración con persistencia de prueba. Se localizaron clases para consulta y detalle de reservas, check-in, pagos, seguridad, ocupación y registro. La matriz asocia casos representativos a HU. Los reportes Surefire disponibles contienen 84 ejecuciones distribuidas en 18 suites sin fallos registrados. Son reportes históricos de momentos y configuraciones diferentes. No constituyen una ejecución única del sistema al corte ni una cobertura del 100 % de requisitos. La validación con MySQL y la pasarela debe ejecutarse en la versión etiquetada.')
fill(44,'Tabla 1. Comparación del proceso revisado con OpenUP.')
comparison=[
['Ciclo de vida','Sprints y backlog','Inicio, Elaboración, Construcción, Transición','Documentar alcance, arquitectura y despliegue por iteración.'],
['Roles','PO, SM y Developers','Analista, arquitecto, desarrollador y tester','Especialidades técnicas del equipo dentro del trabajo Scrum.'],
['Artefactos','Backlogs e incremento','Visión, arquitectura, build y pruebas','Charter y UML disponibles. Formalizar decisiones de arquitectura.'],
['Iteraciones','Objetivo e inspección','Iteración orientada a riesgos','Usar pruebas de integración para revisar riesgos críticos.']]
for row,vals in zip(tables[3].rows[1:],comparison):
 for c,v in zip(row.cells,vals): replace(c.paragraphs[0],v)
fill(45,'4. RESULTADOS')
fill(47,'Tabla 2. Alcance y estimaciones documentales por sprint.')
metrics=[
['1','Reserva a check-in','8 seleccionadas, cierre por validar','42 nominales / cierre por reconciliar'],
['2','Salida y experiencia del huésped','6 seleccionadas, 4 con tablero disponible','31 nominales / cierre por validar'],
['3','Pedidos de restaurante','Planificado','Por consolidar']]
for row,vals in zip(tables[4].rows[1:4],metrics):
 for c,v in zip(row.cells,vals): replace(c.paragraphs[0],v)
paragraphs(48,[
'Fig. 3. Burndown transcrito de la hoja de cálculo.',
'Los puntos nominales usan la columna moda de Sprints. HU1 a HU7 suman 38 y HU21 añade 4, para 42. HU8, HU9, HU10, HU11, HU19 y HU22 suman 31. Se aplica esta convención para lectura del documento, pendiente de confirmar el acuerdo de estimación del equipo. La autenticación aparece con identificadores distintos entre hojas y el tablero 1 repite el 04/11 en los días 5 y 6.',
'El burndown registra 43 unidades iniciales y cero finales. La serie planificada termina en −1, lo que revela un desajuste de una unidad. No está identificado de manera inequívoca como puntos de HU. Por ello se presenta tal como fue registrado y se evita calcular una velocidad media hasta reconciliar unidades y cierre de historias. El tablero 2 cubre HU8 a HU11, mientras el listado de planificación añade HU19 y HU22.'
])
tests=[
['CP01','HU5','Consulta filtrada de reservas','2 pruebas históricas, sin fallos'],
['CP02','HU6','Consulta HTTP del detalle','5 pruebas históricas, sin fallos'],
['CP03','HU7','Aplicación del check-in','4 pruebas históricas, sin fallos'],
['CP04','HU7','Registro del saldo restante','5 pruebas históricas, sin fallos'],
['CP05','HU22','Registro, validación y duplicados','3 pruebas históricas, sin fallos']]
base=tables[5].rows[-1]
while len(tables[5].rows)<len(tests)+1: tables[5]._tbl.append(deepcopy(base._tr))
for row,vals in zip(tables[5].rows[1:],tests):
 for c,v in zip(row.cells,vals): replace(c.paragraphs[0],v)
fill(50,'Tabla 3. Casos representativos trazados a HU y reportes históricos.')
fill(53,'La descripción suministrada del proceso operativo sustenta la necesidad de centralizar reservas y pagos. El equipo declara disponer de un video de entrevista, pendiente de revisión y transcripción con fecha, participante y consentimiento de uso. Una entrevista de elicitación permite fundamentar requisitos. Para declarar conformidad del producto se necesita evidencia de revisión de una versión o de aceptación de sus funciones. No se dispone aún de una comparación medida de tiempos, errores o ingresos antes y después.')
paragraphs(56,[
'El producto permite organizar técnicamente el ciclo de hospedaje mediante dominios de catálogo, reservas y pagos. La trazabilidad ayuda a delimitar qué historias cuentan con código y pruebas. La discusión coincide con Alami y Krancher [4] en que la calidad requiere prácticas técnicas y retroalimentación, además de organización del proceso. El trabajo por sprints facilitó identificar entregables, pero los documentos requieren reconciliación para sostener métricas comparables.',
'Las adaptaciones académicas deben explicitarse para mantener el propósito de inspección y adaptación descrito por Hron y Obwegeser [5] y Özkan y Özcan-Top [6]. El uso de una hoja compartida puede dar visibilidad al trabajo cuando conserva historial y acuerdos. Las bitácoras reconstruidas con horarios propuestos deben contrastarse con registros contemporáneos antes de usarlas como prueba de eventos.',
'OpenUP aporta una referencia para organizar arquitectura y revisión de riesgos. La evidencia de UML y la separación de servicios permiten describir decisiones técnicas, pero la adopción explícita de un proceso híbrido debe confirmarse con el equipo. Las amenazas a la validez incluyen un solo caso académico, ausencia de línea base operacional, reportes de pruebas heterogéneos y disponibilidad parcial de evidencias audiovisuales. Los estudios relacionados son mayormente revisiones y estudios de calidad, por lo que no permiten una comparación cuantitativa directa con una organización turística.'
])
paragraphs(59,[
'Se estructuró una matriz de requisitos trazable a las historias y a evidencias de código. La revisión permitió distinguir el alcance de hospedaje implementado del alcance futuro de servicios complementarios y detectar identificadores que deben unificarse.',
'La arquitectura reconstruida integra Angular, seis servicios funcionales, gateway, descubrimiento, configuración y persistencia MySQL. Esta estructura satisface la necesidad de separar responsabilidades técnicas, cuya operación conjunta debe verificarse sobre una versión consolidada.',
'Los dos sprints ofrecen evidencia documental de planificación y desarrollo. Los resultados de pruebas localizados respaldan casos concretos, con el límite de corresponder a ejecuciones históricas. La optimización del negocio y el despliegue productivo requieren medición y aceptación posteriores.'
])
fill(60,'El trabajo futuro comprende etiquetar una versión reproducible, reconciliar HU y unidades del burndown, ejecutar una batería integral con MySQL y pasarela de prueba, revisar el video de entrevista y obtener conformidad de la organización. Para evaluar el impacto se propone medir tiempo de confirmación de reserva, incidencias de superposición y esfuerzo de conciliación de pagos antes y después. El módulo de restaurante debe desarrollarse y verificarse según su propio backlog.')
fill(62,'Contribución de autores. Carlos Gonzales, facilitación y coordinación. Julio Pantoja, gestión del producto y requisitos. Miguel Acevedo, análisis y pruebas. Lee Orbezo, desarrollo. Antonella Baldeon, base de datos y documentación. La correspondencia definitiva con CRediT requiere aprobación de los autores.')
fill(63,'Uso de IA generativa. Se utilizó ChatGPT/Codex para apoyar la implementación de HU22, la revisión documental, la elaboración de figuras y la redacción de este borrador. El equipo debe revisar contenidos, atribuciones y resultados antes de presentar el artículo.')
fill(64,'Disponibilidad de código y datos. Repositorio https://github.com/CaDaGC/Sol-de-Shiran. Planificación en Historias de usuario.xlsx. La publicación de información de la organización y del video de entrevista queda sujeta a autorización.')
fill(65,'Conflicto de intereses. Declaración pendiente de confirmación de todos los autores.')
fill(66,'Agradecimientos. Al equipo de Sol de Shirán por la información del proceso de negocio proporcionada para el proyecto académico. La autorización de publicación debe confirmarse.')
refs=[
'[1] H. Washizaki, Ed., Guide to the Software Engineering Body of Knowledge, v4.0. IEEE Computer Society, 2024. https://www.computer.org/education/bodies-of-knowledge/software-engineering',
'[2] K. Schwaber y J. Sutherland, The Scrum Guide, 2020. https://scrumguides.org/scrum-guide.html',
'[3] P. Kroll y B. MacIsaac, Agility and Discipline Made Easy: Practices from OpenUP and RUP. Addison-Wesley Professional, 2006.',
'[4] A. Alami y O. Krancher, “How Scrum adds value to achieving software quality?”, Empirical Software Engineering, vol. 27, art. 165, 2022. doi: 10.1007/s10664-022-10208-4.',
'[5] M. Hron y N. Obwegeser, “Why and how is Scrum being adapted in practice: A systematic review”, Journal of Systems and Software, vol. 183, art. 111110, 2022. doi: 10.1016/j.jss.2021.111110.',
'[6] Ö. Özkan y Ö. Özcan-Top, “Tailoring Choices Made in Scrum Projects: A Systematic Literature Review”, CEUR Workshop Proceedings, vol. 3272, 2022. https://ceur-ws.org/Vol-3272/IWSM-MENSURA22_paper5.pdf',
'[7] A. C. Sassa, I. A. de Almeida, T. N. F. Pereira y M. S. de Oliveira, “Scrum: A Systematic Literature Review”, International Journal of Advanced Computer Science and Applications, vol. 14, no. 4, 2023. doi: 10.14569/IJACSA.2023.0140420.',
'[8] H. Kniberg, Scrum and XP from the Trenches, 2.ª ed. InfoQ, 2015. https://www.infoq.com/minibooks/scrum-xp-from-the-trenches-2/',
'[9] S. Brown, “The C4 model for visualising software architecture”. https://c4model.com/',
'[10] Angular, “Documentation”. https://angular.dev/overview',
'[11] Spring, “Spring Boot reference documentation”. https://docs.spring.io/spring-boot/',
'[12] Mercado Pago, “Documentación para desarrolladores”. https://www.mercadopago.com.pe/developers/es/docs'
]
for i,ref in enumerate(refs[:4],69):fill(i,ref)
paragraphs(73,refs[4:])

fontpath='C:/Windows/Fonts/arial.ttf'
def font(size):return ImageFont.truetype(fontpath,size)
def image_canvas(w,h): 
 im=Image.new('RGB',(w,h),'white');return im,ImageDraw.Draw(im)
def box(dr,rect,text,fill='#EEF4F1',size=23):
 dr.rounded_rectangle(rect,radius=10,fill=fill,outline='#44625A',width=2)
 x1,y1,x2,y2=rect
 dr.multiline_text(((x1+x2)/2,(y1+y2)/2),text,font=font(size),fill='black',anchor='mm',align='center',spacing=6)
def arrow(dr,x1,y1,x2,y2):
 dr.line((x1,y1,x2,y2),fill='#44625A',width=3)
 import math
 a=math.atan2(y2-y1,x2-x1)
 for off in (-0.5,0.5):dr.line((x2,y2,x2-12*math.cos(a+off),y2-12*math.sin(a+off)),fill='#44625A',width=3)
im,dr=image_canvas(780,470)
for rect,text in [((20,25,230,125),'Necesidades\ny Product Backlog'),((275,25,485,125),'Sprint Planning\ny Sprint Goal'),((530,25,760,125),'Sprint Backlog\ny tareas'),((530,195,760,300),'Desarrollo\nDaily e integración'),((275,195,485,300),'Incremento\nque cumple la DoD'),((20,195,230,300),'Review\ny retroalimentación'),((205,365,580,450),'Retrospective y mejoras')]:box(dr,rect,text)
for coords in [(230,75,275,75),(485,75,530,75),(645,125,645,195),(530,245,485,245),(275,245,230,245),(125,195,125,125),(390,300,390,365)]:arrow(dr,*coords)
im.save(out/'scrum.png')
im,dr=image_canvas(800,950)
box(dr,(175,10,625,80),'Cliente y personal del campestre',size=23)
box(dr,(175,115,625,200),'Aplicación web Angular\nTypeScript · interfaz y HTTP',size=23)
box(dr,(175,245,625,325),'API Gateway\nRutas y control de acceso JWT',size=23)
arrow(dr,400,80,400,115);arrow(dr,400,200,400,245)
services=[('auth-service','Autenticación'),('usuario-service','Cuentas y roles'),('catalogo-service','Cabañas'),('reservas-service','Estancias'),('pagos-service','Adelantos y saldos'),('resenas-service','Valoraciones')]
for i,(name,desc) in enumerate(services):
 x=15+(i%3)*263;y=380+(i//3)*125
 box(dr,(x,y,x+245,y+95),name+'\nJava · '+desc,size=19)
 arrow(dr,400,325,x+122,y)
box(dr,(110,685,690,780),'MySQL · esquemas por dominio\nUsuarios · catálogo · reservas · pagos · reseñas',size=21)
for x in (137,400,662):arrow(dr,x,600,400,685)
box(dr,(15,825,385,925),'Eureka y Config Server\nDescubrimiento y configuración',size=21)
box(dr,(420,825,785,925),'Mercado Pago\nSistema externo de pagos',size=21)
dr.line((400,600,790,600,790,805,605,805),fill='#44625A',width=3)
arrow(dr,605,805,605,825)
dr.text((20,650),'Servicios funcionales independientes',font=font(20),fill='black')
im.save(out/'c4.png')
im,dr=image_canvas(800,470)
planned=[43,32,22,15,9,-1,-1];actual=[43,32,22,19,10,3,0]
for val in (0,10,20,30,40):
 y=390-(val+2)*7
 dr.line((80,y,760,y),fill='#DDDDDD')
 dr.text((20,y-10),str(val),font=font(18),fill='black')
for vals,color in [(planned,'#687A93'),(actual,'#24634D')]:
 pts=[(80+i*110,390-(v+2)*7) for i,v in enumerate(vals)]
 dr.line(pts,fill=color,width=4)
 for x,y in pts:dr.ellipse((x-4,y-4,x+4,y+4),fill=color)
for i in range(7):dr.text((75+i*110,410),str(i),font=font(20),fill='black')
dr.text((80,15),'Trabajo restante según el archivo fuente',font=font(23),fill='black')
dr.text((100,445),'Día     Planificado (gris) / Registrado (verde)',font=font(20),fill='black')
im.save(out/'burndown.png')
for index,name in [(1,'scrum.png'),(2,'c4.png')]:
 cell=tables[index].cell(0,0)
 replace(cell.paragraphs[0],'')
 cell.paragraphs[0].add_run().add_picture(str(out/name),width=Inches(3.05))
cell=tables[4].rows[4].cells[0]
replace(cell.paragraphs[0],'')
cell.paragraphs[0].add_run().add_picture(str(out/'burndown.png'),width=Inches(3.05))
# Remove inherited fixed table heights and repeat header rows.
requirements=[
('RF01','Seguridad','Autenticar mediante correo y contraseña y validar cuenta activa.','Todos','HU21','1','AuthController; AuthServiceImpl','JWT y cuentas activas','Código presente'),
('RF02','Seguridad','Restringir operaciones según el rol y la identidad autenticada.','Todos','HU21','1','JwtAuthenticationFilter; IdentityFilter','PermisosUsuariosTest','Código y reporte histórico'),
('RF03','Catálogo','Consultar catálogo de cabañas y su información pública.','Cliente','HU1','1','CabanaController','CatalogoHttpTest','Código presente'),
('RF04','Catálogo','Consultar detalle y características de una cabaña.','Cliente','HU2','1','CabanaController; DetalleCabanaComponent','Prueba de detalle por ejecutar en versión etiquetada','Código presente'),
('RF05','Reservas','Consultar disponibilidad por cabaña y rango de fechas.','Cliente','HU3','1','ReservaController; OcupacionInternaController','OcupacionTest','Código y reporte histórico'),
('RF06','Reservas','Crear una solicitud con fechas y datos de estancia sin confirmar superposiciones.','Cliente','HU3','1','ReservaController; GestionReservaService','GestionReservaTest','Código presente'),
('RF07','Pagos','Generar el adelanto del 50 % mediante pasarela y procesar su resultado.','Cliente','HU4','1','PagoController; WebhookController','OrdersFlowTests','Código y reporte histórico'),
('RF08','Reservas','Consultar reservas con filtros y paginación autorizados.','Trabajador','HU5','1','ReservaConsultaController','ReservaConsultaControllerTest','Código y reporte histórico'),
('RF09','Reservas','Consultar detalle, huésped, estancia e importes de una reserva.','Trabajador','HU6','1','ReservaDetalleController','ReservaDetalleControllerTest','Código y reporte histórico'),
('RF10','Pagos','Registrar el saldo restante y formalizar el check-in.','Trabajador','HU7','1','PagoRestanteController; CheckInTest','PagoRestanteTest; CheckInTest','Código y reporte histórico'),
('RF11','Catálogo','Crear, editar y deshabilitar lógicamente las cabañas.','Administrador','HU8','2','GestionCabanaController','GestionCabanaTest','Código presente'),
('RF12','Reservas','Registrar check-out con cobro opcional por daños.','Trabajador','HU9','2','GestionReservaController; PagoDanioInternoController','GestionReservaTest','Código presente'),
('RF13','Reservas','Mostrar al cliente sus reservas activas y pasadas.','Cliente','HU10','2','MisReservasController','MisReservasControllerTest','Código y reporte histórico'),
('RF14','Reseñas','Registrar una valoración de 1 a 5 con comentario para una estancia completada y mostrarla públicamente.','Cliente','HU11','2','ResenaController','ResenaServiceTest','Código presente'),
('RF15','Métricas y cuentas','Consultar indicadores de reservas e ingresos y permitir registro público de clientes.','Administrador / Cliente','HU19 / HU22','2','MetricasReservasController; MetricasIngresosController; AuthController.register','MetricasIngresosControllerTest; RegistroTest','Código presente; registro con cambios locales'),
('RNF01','Seguridad','Guardar contraseñas como hash BCrypt y excluirlas de las respuestas públicas.','Sistema','HU21 / HU22','1–2','PasswordEncoder; DTO públicos','Inspección y prueba de registro','Condición técnica observada'),
('RNF02','Usabilidad','Permitir completar los formularios principales en pantalla móvil de navegador.','Cliente','HU1–HU11 / HU22','1–2','Estilos y formularios Angular','Verificación visual multidispositivo pendiente','Criterio propuesto por validar'),
('RNF03','Integridad','Evitar duplicidad de correo mediante restricción de persistencia.','Sistema','HU22','2','Usuario.email; GestionUsuarioService','PermisosUsuariosTest','Código y reporte histórico'),
('RNF04','Mantenibilidad','Separar responsabilidades mediante servicios por dominio y contratos HTTP.','Equipo','Transversal','1–2','Módulos Spring Boot y API Gateway','Revisión de arquitectura C4','Condición técnica observada'),
('RNF05','Verificabilidad','Disponer de pruebas trazadas a los flujos críticos en la versión de entrega.','Equipo','HU3–HU9 / HU22','1–2','Clases de prueba y colecciones Postman','Ejecución unificada pendiente','Criterio propuesto por validar')
]
# RF15 is split to avoid combining independent requirements.
requirements[14:15]=[
 ('RF15','Métricas','Consultar indicadores de ingresos y frecuencia de reservas.','Administrador','HU19','2','MetricasReservasController; MetricasIngresosController','MetricasIngresosControllerTest','Código presente'),
 ('RF16','Cuentas','Crear una cuenta pública de cliente con validaciones y rol CLIENTE.','Cliente','HU22','2','AuthController.register; UsuarioController.register','RegistroTest','Código con cambios locales')]
(out/'matriz_datos.json').write_text(json.dumps(requirements,ensure_ascii=False),encoding='utf-8')
# Add the compact matrix next to the requirements section, using the template table pattern.
summary=doc.add_table(rows=1,cols=3)
summary.autofit=False
widths=[0.7,5.2,2.2]
for col,width in zip(summary.columns,widths): col.width=Cm(width)
for c,v in zip(summary.rows[0].cells,['ID','Requisito resumido','HU']): replace(c.paragraphs[0],v)
for req in requirements:
 row=summary.add_row()
 for c,v in zip(row.cells,[req[0],req[2],req[4]]): replace(c.paragraphs[0],v)
for row in summary.rows:
 for cell,width in zip(row.cells,widths):
  cell.width=Cm(width)
  for p in cell.paragraphs:
   p.paragraph_format.space_after=Pt(2)
   p.paragraph_format.space_before=Pt(2)
   for r in p.runs:r.font.size=Pt(7)
paras[31]._p.addprevious(summary._tbl)
cap=doc.add_paragraph('Matriz de trazabilidad resumida. 16 RF y 5 RNF. El archivo complementario contiene evidencia y alcance de verificación.')
summary._tbl.addprevious(cap._p)
for p in doc.paragraphs:
 if '15 RF y 5 RNF' in p.text: replace(p,p.text.replace('15 RF y 5 RNF','16 RF y 5 RNF'))
 if 'presentada en el anexo' in p.text: replace(p,p.text.replace('presentada en el anexo','presentada en esta sección'))
for t in doc.tables:
 if len(t.columns)==1: continue
 borders=OxmlElement('w:tblBorders')
 for edge in ('top','left','bottom','right','insideH','insideV'):
  e=OxmlElement('w:'+edge)
  e.set(qn('w:val'),'single');e.set(qn('w:sz'),'4');e.set(qn('w:color'),'D9D9D9');borders.append(e)
 t._tbl.tblPr.append(borders)
for t in doc.tables:
 for row in t.rows:
  for height in row._tr.xpath('./w:trPr/w:trHeight'):height.getparent().remove(height)
  for c in row.cells:
   for p in c.paragraphs:
    p.paragraph_format.keep_with_next=False
 if len(t.rows)>1:
  head=t.rows[0]._tr.get_or_add_trPr()
  head.append(OxmlElement('w:tblHeader'))
# Preserve the original page system exactly.
assert [ET.tostring(s._sectPr) for s in doc.sections]==original_sections
doc.core_properties.title=title
doc.core_properties.author='Carlos Gonzales; Julio Pantoja; Miguel Acevedo; Lee Orbezo; Antonella Baldeon'
output=out/'Articulo_Corto_Sol_de_Shira.docx'
doc.save(output)
print(output)
print('Resumen',len(abstract.split()),'Abstract',len(eng.split()))

