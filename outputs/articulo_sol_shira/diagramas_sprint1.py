# Executed inside the article builder after font/box helpers are defined.
import math
diagram_dir = out/'diagramas'
diagram_dir.mkdir(exist_ok=True)
def save_diagram(image, name, source):
    image.save(diagram_dir/(name+'.png'))
    (diagram_dir/(name+'.mmd')).write_text(source,encoding='utf-8')

im,dr=image_canvas(1000,640)
box(dr,(365,15,635,110),'Cliente\n[Persona]',size=26)
box(dr,(20,250,270,365),'Trabajador\n[Persona]',size=26)
box(dr,(350,245,670,385),'Sol de Shirá\n[Sistema de reservas]\nCiclo de hospedaje',size=26)
box(dr,(745,245,985,385),'Mercado Pago\n[Sistema externo]',size=25)
arrow(dr,500,110,500,245)
dr.text((515,165),'Consulta, reserva\ny adelanto',font=font(23),fill='black')
arrow(dr,270,305,350,305)
dr.text((50,395),'Panel, detalle,\nsaldo y check-in',font=font(23),fill='black')
arrow(dr,670,285,745,285)
arrow(dr,745,355,670,355)
dr.multiline_text((695,430),'Checkout y\nnotificación verificada',font=font(22),fill='black',align='center')
dr.text((200,575),'C4 nivel 1 · Alcance del Sprint 1 · Commit 776a2ec',font=font(24),fill='black')
save_diagram(im,'01_c4_contexto',"""flowchart LR
 Cliente["Cliente [Persona]"] -->|"Consulta, reserva y adelanto"| Sistema["Sol de Shirá [Sistema]"]
 Trabajador["Trabajador [Persona]"] -->|"Panel, detalle, saldo y check-in"| Sistema
 Sistema -->|"Crear checkout"| MP["Mercado Pago [Sistema externo]"]
 MP -->|"Notificación verificada"| Sistema
""")

im,dr=image_canvas(1000,1180)
dr.rounded_rectangle((10,10,990,1060),radius=15,outline='#44625A',width=3)
dr.text((25,25),'Sol de Shirá · límite del sistema · Sprint 1',font=font(26),fill='black')
box(dr,(285,80,715,175),'Aplicación web Angular\n[Contenedor · TypeScript]',size=26)
box(dr,(285,230,715,325),'API Gateway\n[Contenedor · Spring Cloud]',size=26)
arrow(dr,500,175,500,230)
labels=[('auth-service','Autenticación',30),('usuario-service','Cuentas y roles',350),('catalogo-service','Cabañas',670)]
for name,purpose,x in labels:
    box(dr,(x,395,x+300,490),name+'\n[Java · '+purpose+']',size=23)
    arrow(dr,500,325,x+150,395)
box(dr,(190,560,490,660),'reservas-service\n[Java · Estancias]',size=24)
box(dr,(520,560,820,660),'pagos-service\n[Java · Pagos]',size=24)
arrow(dr,500,325,340,560)
arrow(dr,500,325,670,560)
arrow(dr,330,435,350,435)
dr.text((100,510),'Consulta identidad',font=font(20),fill='black')
arrow(dr,490,610,520,610)
dr.text((285,690),'Coordinación por API HTTP',font=font(23),fill='black')
box(dr,(50,760,950,860),'MySQL [Almacén de datos]\nusuario_db · db_catalogo · db_reservas · db_pagos',size=25)
for x in (340,670):arrow(dr,x,660,x,760)
arrow(dr,500,490,500,760)
arrow(dr,820,490,820,760)
box(dr,(50,920,475,1025),'Eureka [Contenedor]\nDescubrimiento de servicios',size=24)
box(dr,(525,920,950,1025),'Config Server [Contenedor]\nConfiguración centralizada',size=24)
box(dr,(580,1080,970,1170),'Mercado Pago\n[Sistema externo]',size=24)
dr.line((820,610,975,610,975,1070,780,1070),fill='#44625A',width=3)
arrow(dr,780,1070,780,1080)
save_diagram(im,'02_c4_contenedores',"""flowchart TB
 Persona["Cliente / trabajador"] --> WEB
 subgraph SISTEMA["Sol de Shirá · Sprint 1"]
 WEB["Aplicación web Angular [TypeScript]"] -->|"HTTPS/REST"| GW["API Gateway [Spring Cloud]"]
 GW --> AUTH["auth-service [Spring Boot]"]
 GW --> USR["usuario-service [Spring Boot]"]
 GW --> CAT["catalogo-service [Spring Boot]"]
 GW --> RES["reservas-service [Spring Boot]"]
 GW --> PAG["pagos-service [Spring Boot]"]
 AUTH -->|"HTTP"| USR
 RES -->|"HTTP"| CAT
 PAG -->|"HTTP"| RES
 USR --> DBU[("MySQL usuario_db")]
 CAT --> DBC[("MySQL db_catalogo")]
 RES --> DBR[("MySQL db_reservas")]
 PAG --> DBP[("MySQL db_pagos")]
 EU["Eureka [descubrimiento]"] -.-> AUTH
 EU -.-> USR
 EU -.-> CAT
 EU -.-> RES
 EU -.-> PAG
 CF["Config Server [configuración]"] -.-> GW
 end
 PAG -->|"Checkout"| MP["Mercado Pago [externo]"]
 MP -->|"Webhook"| GW
""")
im.save(out/'c4.png')

# Faithful logical diagram of the supplied screenshot. REF means cross-domain reference.
im,dr=image_canvas(1300,1100)
def entity(rect,title,fields,color='#E8F0F5'):
    x1,y1,x2,y2=rect
    dr.rectangle(rect,fill='white',outline='#526778',width=2)
    dr.rectangle((x1,y1,x2,y1+48),fill=color)
    dr.text((x1+10,y1+8),title,font=font(25),fill='black')
    for i,line in enumerate(fields):
        dr.text((x1+10,y1+65+i*34),line,font=font(21),fill='black')
entity((15,20,395,405),'cabana',['PK id_cabana','nombre · descripcion','aforo_maximo','num_habitaciones · num_banos','precio_base_noche','estado · imagen_principal','fecha_creacion'])
entity((450,20,845,470),'reserva',['PK id_reserva','REF id_cabana','REF id_usuario','fecha_ingreso · fecha_salida','cantidad_huespedes','precio_noche_aplicado','monto_total · monto_adelanto','estado_reserva · expira_en','fecha_creacion'])
entity((900,20,1285,405),'transaccion_pago',['PK id_transaccion','REF id_reserva','UK id_externo_pasarela','monto · moneda','metodo_pago','estado_transaccion','fecha_transaccion'])
entity((15,495,395,695),'cabana_imagenes',['PK id_imagen','FK id_cabana','url_imagen'])
entity((15,770,395,970),'cabana_comodidades',['PK id_comodidad','FK id_cabana','nombre_comodidad'])
entity((450,555,845,720),'usuario externa',['PK id_usuario','Estructura no proporcionada'])
entity((900,495,1285,810),'webhook_log',['PK id_log','FK? id_transaccion','id_transaccion_pasarela','payload_json','estado_procesamiento','fecha_recepcion'])
for coords in [(205,405,205,495),(395,200,450,200),(845,200,900,200),(650,470,650,555),(1090,405,1090,495)]:
    arrow(dr,*coords)
dr.line((5,405,5,870,15,870),fill='#44625A',width=3)
dr.text((25,1000),'REF = referencia lógica entre dominios. FK? = vinculación opcional propuesta en la captura.',font=font(22),fill='black')
dr.text((25,1040),'Modelo recibido. NUMBER / VARCHAR2 pendientes de conciliación con MySQL.',font=font(22),fill='black')
save_diagram(im,'03_datos_modelo_recibido',"""erDiagram
 cabana ||--o{ cabana_imagenes : "FK local"
 cabana ||--o{ cabana_comodidades : "FK en modelo recibido"
 cabana ||..o{ reserva : "REF id_cabana"
 usuario ||..o{ reserva : "REF id_usuario"
 reserva ||..o{ transaccion_pago : "REF id_reserva"
 transaccion_pago o|..o{ webhook_log : "asociacion opcional propuesta"
 cabana { long id_cabana PK string nombre decimal precio_base_noche string estado }
 cabana_imagenes { long id_imagen PK long id_cabana FK string url_imagen }
 cabana_comodidades { long id_comodidad PK long id_cabana FK string nombre_comodidad }
 reserva { long id_reserva PK long id_cabana long id_usuario date fecha_ingreso date fecha_salida decimal monto_total decimal monto_adelanto string estado_reserva }
 usuario { long id_usuario PK }
 transaccion_pago { long id_transaccion PK long id_reserva string id_externo_pasarela UK decimal monto string estado_transaccion }
 webhook_log { long id_log PK long id_transaccion string payload_json string estado_procesamiento }
""")

def sequence(name,title,actors,messages,source):
    h=180+len(messages)*85
    im,dr=image_canvas(1100,h)
    dr.text((20,10),title,font=font(28),fill='black')
    positions=[100+i*(900/(len(actors)-1)) for i in range(len(actors))]
    for x,actor in zip(positions,actors):
        box(dr,(x-95,65,x+95,145),actor,size=23)
        for y in range(145,h-20,18):
            dr.line((x,y,x,y+9),fill='#8B9894',width=2)
    for i,(a,b,text) in enumerate(messages):
        y=205+i*85
        x1,x2=positions[a],positions[b]
        arrow(dr,x1,y,x2,y)
        dr.text(((x1+x2)/2,y-25),str(i+1)+'. '+text,font=font(22),fill='black',anchor='mm')
    save_diagram(im,name,source)
sequence('04_secuencia_reserva_adelanto','Reserva y adelanto del 50 % · HU3 y HU4',
         ['Cliente /\nAngular','Gateway','Reservas','Pagos','Mercado\nPago'],
         [(0,1,'Solicitar disponibilidad'),(1,2,'Consultar rango y capacidad'),(2,0,'Disponibilidad e importe'),
          (0,1,'Crear solicitud de reserva'),(1,2,'Guardar reserva pendiente'),
          (0,1,'Iniciar adelanto'),(1,3,'Crear checkout por reserva'),(3,4,'Solicitar orden de pago'),
          (4,0,'Completar pago'),(4,1,'Notificar resultado'),(1,3,'Validar firma y consultar orden'),
          (3,2,'Aplicar adelanto aprobado'),(2,0,'Consultar reserva confirmada')],
         """sequenceDiagram
 actor C as Cliente
 participant A as Angular
 participant G as API Gateway
 participant R as reservas-service
 participant P as pagos-service
 participant M as Mercado Pago
 C->>A: Cabaña, fechas y huéspedes
 A->>G: Consultar disponibilidad
 G->>R: Validar rango y capacidad
 R-->>A: Disponibilidad e importe
 A->>G: Crear reserva
 G->>R: Guardar solicitud pendiente
 A->>G: Iniciar adelanto de reserva
 G->>P: Crear checkout
 P->>M: Crear orden
 M-->>A: Enlace o interacción de pago
 C->>M: Completar pago
 M->>G: Webhook
 G->>P: Procesar notificación
 P->>P: Validar firma y consultar estado real
 alt Pago aprobado
 P->>R: Aplicar adelanto validado
 else Pendiente o rechazado
 P->>P: Conservar estado correspondiente
 end
 A->>G: Consultar reserva
 G->>R: Obtener estado
 R-->>A: Estado confirmado o pendiente
""")
sequence('05_secuencia_saldo_checkin','Saldo presencial y check-in · HU7',
         ['Trabajador /\nAngular','Gateway','Pagos','Reservas','Catálogo'],
         [(0,1,'Consultar detalle de reserva'),(1,3,'Obtener estado y saldo'),(3,0,'Mostrar detalle autorizado'),
          (0,1,'Registrar saldo y método'),(1,2,'Validar rol e idempotencia'),
          (2,3,'Consultar reserva y saldo'),(3,2,'Estado e importe pendiente'),
          (2,3,'Solicitar check-in'),(3,4,'Aplicar ocupación'),(4,3,'Ocupación confirmada'),
          (3,2,'Ingreso aplicado'),(2,0,'Resultado y saldo registrado')],
         """sequenceDiagram
 actor T as Trabajador
 participant A as Angular
 participant G as API Gateway
 participant P as pagos-service
 participant R as reservas-service
 participant K as catalogo-service
 T->>A: Abrir detalle de reserva
 A->>G: Consultar detalle
 G->>R: Obtener datos y saldo
 R-->>A: Reserva autorizada
 T->>A: Medio de pago y operación
 A->>G: POST pago restante
 G->>G: Validar token y rol
 G->>P: Registrar saldo con clave de idempotencia
 P->>R: Consultar reserva y saldo
 R-->>P: Estado e importe
 alt Solicitud nueva y válida
 P->>P: Registrar operación recuperable
 P->>R: Aplicar check-in
 R->>K: Registrar ocupación de cabaña
 K-->>R: Ocupación aplicada
 R-->>P: Check-in aplicado
 P->>P: Completar pago restante
 P-->>A: Resultado
 else Reintento
 P-->>A: Resultado existente o continuación
 end
""")

# Supplemental table extracted from the historical source, not from the supplied image.
im,dr=image_canvas(1100,550)
entity((80,35,1020,430),'pago_restante · entidad adicional verificada en 776a2ec',[
    'PK id · UK idReserva · UK claveIdempotencia',
    'idTrabajador · monto · metodoPago · numeroOperacion',
    'completado · fechaRegistro',
    'Propósito: saldo presencial y recuperación de reintentos',
    'Se relaciona lógicamente con reserva mediante idReserva'
])
dr.text((80,480),'Extensión del modelo recibido que sustenta la HU7.',font=font(26),fill='black')
save_diagram(im,'06_datos_extension_checkin',"""erDiagram
 reserva ||..o| pago_restante : "idReserva unica"
 pago_restante {
 long id PK
 long idReserva UK
 string claveIdempotencia UK
 long idTrabajador
 decimal monto
 string metodoPago
 string numeroOperacion
 boolean completado
 datetime fechaRegistro
 }
""")

# Insert diagrams in the architecture section using the template caption and body pattern.
from docx.text.paragraph import Paragraph
def insert_figure_before(anchor,path,caption):
    base=deepcopy(paras[38]._p)
    anchor._p.addprevious(base)
    picture=Paragraph(base,anchor._parent)
    replace(picture,'')
    picture.add_run().add_picture(str(path),width=Inches(3.05))
    cap=deepcopy(paras[38]._p)
    anchor._p.addprevious(cap)
    replace(Paragraph(cap,anchor._parent),caption)
insert_figure_before(paras[37],diagram_dir/'01_c4_contexto.png','Fig. 2a. C4 nivel 1 del primer incremento.')
# Existing placeholder takes the C4 level 2.
fill(38,'Fig. 2b. C4 nivel 2 del Sprint 1, corte 776a2ec.')
for filename,caption in [
    ('03_datos_modelo_recibido.png','Fig. 2c. Modelo de datos recibido, con referencias lógicas entre dominios.'),
    ('06_datos_extension_checkin.png','Fig. 2d. Entidad adicional de saldo presencial verificada en el código del Sprint 1.'),
    ('04_secuencia_reserva_adelanto.png','Fig. 2e. Secuencia del flujo principal de reserva y adelanto.'),
    ('05_secuencia_saldo_checkin.png','Fig. 2f. Secuencia del pago restante y del check-in.')]:
    insert_figure_before(paras[39],diagram_dir/filename,caption)

